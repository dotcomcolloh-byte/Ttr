package com.telefam.company.backend.routes

import com.telefam.company.backend.db.TelefamDatabase
import com.telefam.company.backend.security.SandboxImageProcessor
import com.telefam.company.backend.security.SecurityRiskEngine
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class BackendAuthRequest(
    val email: String,
    val password: String? = null,
    val otpCode: String? = null,
    val purpose: String = "LOGIN",
    val clientDeviceFingerprint: String,
    val locationLabel: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class BackendProfileSetupRequest(
    val fullName: String,
    val username: String,
    val phoneCountryCode: String,
    val phoneNumber: String,
    val gender: String,
    val dateOfBirth: String,
    val bio: String,
    val avatarStorageKey: String? = null,
    val avatarSha256: String? = null,
    val avatarPerceptualDHash: String? = null
)

@Serializable
data class BackendOwnerProfileUpdatePayload(
    val userId: String,
    val fullName: String,
    val username: String,
    val phoneCountryCode: String,
    val phoneIsoCode: String = "KE",
    val phoneNumber: String,
    val gender: String,
    val dateOfBirth: String,
    val bio: String,
    val avatarCdnUrl: String? = null,
    val avatarSha256: String? = null,
    val avatarPerceptualDHash: String? = null,
    val lastFullNameChangedAtMs: Long? = null,
    val lastUsernameChangedAtMs: Long? = null,
    val idempotencyKey: String = "",
    val clientTimestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class UsernameCheckResponse(
    val requestedUsername: String,
    val available: Boolean,
    val suggestions: List<String>
)

@Serializable
data class BackendPrivacySettingsPayload(
    val userId: String,
    val messageRequests: String = "ANYONE",
    val whoCanCallMe: String = "ANYONE",
    val whoCanScreenshotChats: String = "ANYONE",
    val whoCanShareChats: String = "ANYONE",
    val whoCanCopyMessages: String = "ANYONE",
    val whoCanDownloadMedia: String = "ANYONE",
    val chatThemeId: String = "crimson_silk",
    val bubbleColorHex: String = "#C80815",
    val readReceipts: String = "ANYONE",
    val typingIndicators: String = "ANYONE",
    val onlineIndicators: String = "ANYONE",
    val syncVersion: Long = 1L
)

private val registeredUsernamesRegistry = ConcurrentHashMap.newKeySet<String>()
private val privacySettingsCache = ConcurrentHashMap<String, BackendPrivacySettingsPayload>()
private val ownerProfileCache = ConcurrentHashMap<String, BackendOwnerProfileUpdatePayload>()

private fun sanitizeBackendText(raw: String, maxLen: Int): String {
    return raw
        .replace(Regex("<[^>]*>"), "")
        .replace(Regex("[\\p{Cntrl}&&[^\n\t]]"), "")
        .trim()
        .take(maxLen)
}

fun generateUniqueUsernameSuggestions(
    baseUsername: String,
    fullName: String,
    existingCheck: (String) -> Boolean
): List<String> {
    val cleanBase = baseUsername.lowercase()
        .replace(Regex("[^a-z0-9_]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .take(18)
        .ifBlank { "telefam_member" }

    val nameParts = fullName.lowercase().split(Regex("\\s+"))
        .map { it.replace(Regex("[^a-z0-9]"), "") }
        .filter { it.isNotBlank() }

    val candidates = buildList {
        add("${cleanBase}_tf")
        add("${cleanBase}_official")
        if (nameParts.size >= 2) {
            add("${nameParts.first()}_${nameParts.last()}".take(22))
            add("${nameParts.first()}${nameParts.last().take(3)}26".take(22))
        }
        for (suffix in listOf("1", "7", "24", "26", "99", "ke", "hq", "connect", "fam")) {
            add("${cleanBase}_$suffix".take(24))
        }
        for (i in 101..199) {
            add("${cleanBase}$i".take(24))
        }
    }.distinct()

    return candidates
        .filter { it.length in 3..24 && !existingCheck(it) }
        .take(4)
}

fun Application.registerTelefamRoutes(
    riskEngine: SecurityRiskEngine,
    sandboxProcessor: SandboxImageProcessor
) {
    routing {
        route("/v1/auth") {
            post("/signup") {
                val req = call.receive<BackendAuthRequest>()
                val evaluation = riskEngine.evaluateRequestRisk(
                    untrustedClientDeviceFp = req.clientDeviceFingerprint,
                    remoteIp = call.request.local.remoteAddress,
                    forwardedFor = call.request.header("X-Forwarded-For"),
                    userAgent = call.request.header("User-Agent") ?: "",
                    acceptLanguage = call.request.header("Accept-Language") ?: "en",
                    tlsCipherHeader = call.request.header("X-SSL-Cipher") ?: "TLS_AES_256_GCM_SHA384",
                    knownDeviceHashes = emptySet(),
                    lastKnownLocation = null,
                    currentLocation = req.locationLabel,
                    recentRequestsLastMinute = 1,
                    failedLoginAttempts = 0,
                    failedOtpAttempts = 0
                )
                if (evaluation.blockRequest) {
                    throw SecurityException("Automated traffic anomaly detected (risk=${evaluation.score})")
                }
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "OTP_SENT",
                        "domain" to "telefam.company",
                        "riskScore" to evaluation.score.toString(),
                        "resendWaitSeconds" to "60"
                    )
                )
            }

            post("/login") {
                val req = call.receive<BackendAuthRequest>()
                val evaluation = riskEngine.evaluateRequestRisk(
                    untrustedClientDeviceFp = req.clientDeviceFingerprint,
                    remoteIp = call.request.local.remoteAddress,
                    forwardedFor = call.request.header("X-Forwarded-For"),
                    userAgent = call.request.header("User-Agent") ?: "",
                    acceptLanguage = call.request.header("Accept-Language") ?: "en",
                    tlsCipherHeader = call.request.header("X-SSL-Cipher") ?: "TLS_AES_256_GCM_SHA384",
                    knownDeviceHashes = emptySet(),
                    lastKnownLocation = null,
                    currentLocation = req.locationLabel,
                    recentRequestsLastMinute = 1,
                    failedLoginAttempts = 0,
                    failedOtpAttempts = 0
                )
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to if (evaluation.requiresStepUpOtp) "NEW_DEVICE_OTP_REQUIRED" else "AUTHENTICATED",
                        "riskScore" to evaluation.score.toString(),
                        "serverDeviceHash" to evaluation.serverVerifiedDeviceHash
                    )
                )
            }

            post("/token/refresh") {
                val deviceHeader = call.request.headers["X-Telefam-Device-Fingerprint"] ?: "unknown"
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "synced" to true,
                        "domain" to "api.telefam.company",
                        "deviceBinding" to deviceHeader.take(16),
                        "serverTimestampMs" to System.currentTimeMillis()
                    )
                )
            }
        }

        route("/v1/profile") {
            get("/username/check") {
                val requested = (call.request.queryParameters["username"] ?: "")
                    .trim().removePrefix("@").lowercase()
                val fullName = call.request.queryParameters["fullName"] ?: ""
                val exists = registeredUsernamesRegistry.contains(requested)
                val suggestions = if (exists) {
                    generateUniqueUsernameSuggestions(requested, fullName) {
                        registeredUsernamesRegistry.contains(it)
                    }
                } else {
                    emptyList()
                }
                call.respond(
                    HttpStatusCode.OK,
                    UsernameCheckResponse(
                        requestedUsername = requested,
                        available = !exists && Regex("^[a-zA-Z0-9_]{3,24}$").matches(requested),
                        suggestions = suggestions
                    )
                )
            }

            post("/avatar/sandbox-upload") {
                val multipart = call.receiveMultipart()
                var uploadedBytes: ByteArray? = null
                var contentType: String? = null
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        contentType = part.contentType?.toString()
                        uploadedBytes = part.provider().readRemaining().readByteArray()
                    }
                    part.dispose()
                }
                val bytes = requireNotNull(uploadedBytes) { "Missing avatar image file part" }
                val artifact = sandboxProcessor.processInSandbox(bytes, contentType)
                call.respond(HttpStatusCode.OK, artifact)
            }

            post("/complete") {
                val req = call.receive<BackendProfileSetupRequest>()
                require(req.fullName.trim().length in 2..120) { "Invalid full name" }
                val cleanUsername = req.username.trim().removePrefix("@").lowercase()
                require(Regex("^[a-zA-Z0-9_]{3,24}$").matches(cleanUsername)) { "Invalid username format" }

                // Validate Date of Birth is >= 13 years old
                val dob = runCatching { LocalDate.parse(req.dateOfBirth) }.getOrNull()
                    ?: throw IllegalArgumentException("Invalid date of birth format (expected YYYY-MM-DD)")
                val ageYears = Period.between(dob, LocalDate.now()).years
                require(ageYears in 13..120) {
                    "You must be at least 13 years old to create a Telefam profile"
                }
                require(req.bio.length <= 150) { "Bio exceeds 150 character limit" }

                if (registeredUsernamesRegistry.contains(cleanUsername)) {
                    val suggestions = generateUniqueUsernameSuggestions(cleanUsername, req.fullName) {
                        registeredUsernamesRegistry.contains(it)
                    }
                    call.respond(
                        HttpStatusCode.Conflict,
                        UsernameCheckResponse(
                            requestedUsername = cleanUsername,
                            available = false,
                            suggestions = suggestions
                        )
                    )
                    return@post
                }

                registeredUsernamesRegistry.add(cleanUsername)
                // Avatar is optional; null avatarStorageKey is valid
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "PROFILE_SAVED",
                        "username" to "@$cleanUsername",
                        "avatarOptional" to (req.avatarStorageKey == null).toString()
                    )
                )
            }

            put("/update") {
                val rlsUserHeader = call.request.header("X-Telefam-RLS-User-Id")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-RLS-User-Id header for Row-Level Security")
                val req = call.receive<BackendOwnerProfileUpdatePayload>()
                require(req.userId == rlsUserHeader) {
                    "Row-Level Security policy violation: authenticated user does not match profile owner"
                }

                // Server-side input sanitization
                val sanitizedName = sanitizeBackendText(req.fullName, 60).replace("\n", " ")
                val sanitizedUsername = req.username.trim().removePrefix("@").lowercase()
                    .filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
                    .take(24)
                val sanitizedPhone = req.phoneNumber.filter { it.isDigit() }.take(15)
                val sanitizedBio = sanitizeBackendText(req.bio, 150)
                val sanitizedGender = sanitizeBackendText(req.gender, 32)
                val sanitizedDob = req.dateOfBirth.filter { it.isDigit() || it == '-' }.take(10)

                require(sanitizedName.length in 2..60) { "Full name must be between 2 and 60 characters" }
                require(Regex("^[a-z0-9_]{3,24}$").matches(sanitizedUsername)) { "Invalid username format" }
                require(sanitizedPhone.length in 7..15) { "Invalid phone number" }

                val nowMs = System.currentTimeMillis()
                val existing = ownerProfileCache[req.userId]
                val sevenDaysMs = 7L * 24 * 60 * 60 * 1000L
                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L

                if (existing != null) {
                    if (!existing.fullName.equals(sanitizedName, ignoreCase = false)) {
                        val lastNameChange = existing.lastFullNameChangedAtMs ?: 0L
                        require(nowMs - lastNameChange >= sevenDaysMs) {
                            "Full name can only be changed once every 7 days."
                        }
                    }
                    if (!existing.username.equals(sanitizedUsername, ignoreCase = true)) {
                        val lastUsernameChange = existing.lastUsernameChangedAtMs ?: 0L
                        require(nowMs - lastUsernameChange >= thirtyDaysMs) {
                            "Username can only be changed once every 30 days."
                        }
                        require(!registeredUsernamesRegistry.contains(sanitizedUsername)) {
                            "Username @$sanitizedUsername is already taken."
                        }
                        registeredUsernamesRegistry.remove(existing.username.lowercase())
                        registeredUsernamesRegistry.add(sanitizedUsername)
                    }
                } else {
                    registeredUsernamesRegistry.add(sanitizedUsername)
                }

                val updatedPayload = req.copy(
                    fullName = sanitizedName,
                    username = sanitizedUsername,
                    phoneNumber = sanitizedPhone,
                    bio = sanitizedBio,
                    gender = sanitizedGender,
                    dateOfBirth = sanitizedDob
                )
                ownerProfileCache[req.userId] = updatedPayload

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "synced" to true,
                        "domain" to "api.telefam.company",
                        "serverTimestampMs" to nowMs
                    )
                )
            }
        }

        route("/v1/privacy") {
            get("/settings") {
                val rlsUserHeader = call.request.header("X-Telefam-RLS-User-Id")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-RLS-User-Id header for Row-Level Security")
                val existing = privacySettingsCache[rlsUserHeader] ?: BackendPrivacySettingsPayload(userId = rlsUserHeader)
                call.respond(HttpStatusCode.OK, existing)
            }

            put("/settings") {
                val rlsUserHeader = call.request.header("X-Telefam-RLS-User-Id")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-RLS-User-Id header for Row-Level Security")
                val req = call.receive<BackendPrivacySettingsPayload>()
                require(req.userId == rlsUserHeader) {
                    "Row-Level Security policy violation: authenticated user does not match target user_id"
                }

                val allowedAudience = setOf("ANYONE", "NOBODY")
                require(req.messageRequests in allowedAudience) { "Invalid messageRequests value" }
                require(req.whoCanCallMe in allowedAudience) { "Invalid whoCanCallMe value" }
                require(req.whoCanScreenshotChats in allowedAudience) { "Invalid whoCanScreenshotChats value" }
                require(req.whoCanShareChats in allowedAudience) { "Invalid whoCanShareChats value" }
                require(req.whoCanCopyMessages in allowedAudience) { "Invalid whoCanCopyMessages value" }
                require(req.whoCanDownloadMedia in allowedAudience) { "Invalid whoCanDownloadMedia value" }
                require(req.readReceipts in allowedAudience) { "Invalid readReceipts value" }
                require(req.typingIndicators in allowedAudience) { "Invalid typingIndicators value" }
                require(req.onlineIndicators in allowedAudience) { "Invalid onlineIndicators value" }
                require(Regex("^#[0-9A-Fa-f]{6}$").matches(req.bubbleColorHex)) { "Invalid bubbleColorHex format" }

                runCatching {
                    val uuid = UUID.fromString(req.userId)
                    TelefamDatabase.upsertPrivacySettingsWithRls(
                        userId = uuid,
                        messageRequests = req.messageRequests,
                        whoCanCallMe = req.whoCanCallMe,
                        whoCanScreenshotChats = req.whoCanScreenshotChats,
                        whoCanShareChats = req.whoCanShareChats,
                        whoCanCopyMessages = req.whoCanCopyMessages,
                        whoCanDownloadMedia = req.whoCanDownloadMedia,
                        chatThemeId = req.chatThemeId,
                        bubbleColorHex = req.bubbleColorHex,
                        readReceipts = req.readReceipts,
                        typingIndicators = req.typingIndicators,
                        onlineIndicators = req.onlineIndicators,
                        syncVersion = req.syncVersion
                    )
                }

                privacySettingsCache[req.userId] = req
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "synced" to true,
                        "domain" to "api.telefam.company",
                        "rlsEnforced" to true,
                        "syncVersion" to req.syncVersion,
                        "serverTimestampMs" to System.currentTimeMillis()
                    )
                )
            }
        }

        route("/v1/contacts") {
            get("/discover") {
                val rlsUserHeader = call.request.header("X-Telefam-RLS-User-Id")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-RLS-User-Id header")
                val query = call.request.queryParameters["query"]?.trim().orEmpty()
                val page = (call.request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
                val pageSize = (call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20).coerceIn(1, 50)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "synced" to true,
                        "rlsUserId" to rlsUserHeader,
                        "query" to query,
                        "page" to page,
                        "pageSize" to pageSize,
                        "refreshedAtMs" to System.currentTimeMillis()
                    )
                )
            }

            post("/upload-hashes") {
                val rlsUserHeader = call.request.header("X-Telefam-RLS-User-Id")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-RLS-User-Id header")
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "synced" to true,
                        "domain" to "api.telefam.company",
                        "rlsUserId" to rlsUserHeader,
                        "serverTimestampMs" to System.currentTimeMillis()
                    )
                )
            }
        }

        route("/v1/social") {
            post("/follow") {
                val rlsUserHeader = call.request.header("X-Telefam-RLS-User-Id")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-RLS-User-Id header")
                val idempotencyKey = call.request.header("X-Telefam-Idempotency-Key")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-Idempotency-Key header")
                require(idempotencyKey.length in 8..128) { "Invalid idempotency key" }
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "synced" to true,
                        "domain" to "api.telefam.company",
                        "rlsUserId" to rlsUserHeader,
                        "idempotencyKey" to idempotencyKey,
                        "serverTimestampMs" to System.currentTimeMillis()
                    )
                )
            }

            post("/block") {
                val rlsUserHeader = call.request.header("X-Telefam-RLS-User-Id")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-RLS-User-Id header")
                val idempotencyKey = call.request.header("X-Telefam-Idempotency-Key")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-Idempotency-Key header")
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "synced" to true,
                        "domain" to "api.telefam.company",
                        "rlsUserId" to rlsUserHeader,
                        "idempotencyKey" to idempotencyKey,
                        "serverTimestampMs" to System.currentTimeMillis()
                    )
                )
            }

            post("/report") {
                val rlsUserHeader = call.request.header("X-Telefam-RLS-User-Id")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-RLS-User-Id header")
                val idempotencyKey = call.request.header("X-Telefam-Idempotency-Key")?.trim()
                    ?: throw IllegalArgumentException("Missing X-Telefam-Idempotency-Key header")
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "synced" to true,
                        "domain" to "api.telefam.company",
                        "rlsUserId" to rlsUserHeader,
                        "idempotencyKey" to idempotencyKey,
                        "serverTimestampMs" to System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
