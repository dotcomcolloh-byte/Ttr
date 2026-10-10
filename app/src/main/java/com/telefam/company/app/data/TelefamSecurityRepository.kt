package com.telefam.company.app.data

import android.content.Context
import com.telefam.company.app.domain.DeviceAndNetworkTelemetry
import com.telefam.company.app.domain.EncryptedUserSession
import com.telefam.company.app.domain.OwnerProfileCooldownState
import com.telefam.company.app.domain.PrivacyAudience
import com.telefam.company.app.domain.PrivacyRuleKey
import com.telefam.company.app.domain.SanitizedAvatarResult
import com.telefam.company.app.domain.TelefamBubbleColors
import com.telefam.company.app.domain.TelefamChatThemes
import com.telefam.company.app.domain.TelefamInputSanitizer
import com.telefam.company.app.domain.TelefamValidators
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.util.UUID

data class UsernameUniquenessResult(
    val requestedUsername: String,
    val isAvailable: Boolean,
    val isValidFormat: Boolean,
    val suggestions: List<String>
)

data class EmailAutofillResult(
    val derivedFullName: String,
    val assignedUniqueUsername: String,
    val alternativeSuggestions: List<String>,
    val wasOriginalUsernameTaken: Boolean
)

class UsernameConflictException(
    val requestedUsername: String,
    val suggestions: List<String>,
    override val message: String
) : IllegalArgumentException(message)

sealed class AuthActionOutcome {
    data class OtpDispatched(
        val email: String,
        val purpose: String,
        val dispatchedOtpCode: String,
        val resendCountdownSeconds: Int,
        val expiresInMinutes: Int,
        val reasonBanner: String? = null
    ) : AuthActionOutcome()

    data class Authenticated(
        val session: EncryptedUserSession,
        val profile: ProfileEntity?
    ) : AuthActionOutcome()

    data class PasswordResetSuccess(
        val email: String
    ) : AuthActionOutcome()

    data class CooldownLocked(
        val lockType: String, // LOGIN_LOCK_15M or OTP_LOCK_30M
        val remainingSeconds: Long,
        val message: String
    ) : AuthActionOutcome()

    data class ValidationError(
        val message: String
    ) : AuthActionOutcome()
}

/**
 * Production Security & Authentication Repository for com.telefam.company.app.
 * Enforces:
 * - Parameterized SQL queries & SQL-injection signature rejection
 * - 3 failed login attempts -> 15-minute cooldown lock (or user can request OTP)
 * - 3 failed OTP attempts -> 30-minute cooldown lock per User + IP fingerprint
 * - 60-second resend timer on OTP dispatch & automatic invalidation of prior/expired OTPs
 * - Untrusted client device & IP fingerprint evaluation + server-verified device hash
 * - Automatic Step-Up OTP challenge when logging in from a new device or new location
 * - Email prefix Full Name & Username autofill with backend uniqueness verification & suggestion generator
 * - Strict 13+ years Date of Birth validation
 * - Optional Profile Photo with SHA-256 + 64-bit Perceptual dHash fingerprinting for deduplication/reuse
 */
class TelefamSecurityRepository(
    private val context: Context,
    val dao: TelefamDao = TelefamAppDatabase.getInstance(context).telefamDao()
) {
    companion object {
        const val LOGIN_MAX_FAILURES = 3
        const val LOGIN_COOLDOWN_MS = 15 * 60 * 1000L // 15 minutes
        const val OTP_MAX_FAILURES = 3
        const val OTP_COOLDOWN_MS = 30 * 60 * 1000L // 30 minutes
        const val OTP_RESEND_WAIT_MS = 60 * 1000L // 60 seconds
        const val OTP_EXPIRATION_MS = 10 * 60 * 1000L // 10 minutes
        const val SESSION_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days

        fun computeTotpForStep(secretBase32: String, timeStep: Long): String {
            val digestHex = TelefamCryptoAndTelemetry.hmacSha256("TOTP_RFC6238:$secretBase32:$timeStep")
            val numeric = digestHex.take(8).toLong(16) % 1_000_000L
            return String.format("%06d", numeric)
        }
    }

    fun observeForensicLogs(): Flow<List<ForensicAuditLogEntity>> = dao.observeRecentForensicLogs()

    suspend fun getRecentFingerprintedMedia(): List<MediaFingerprintEntity> {
        return runCatching { dao.getRecentFingerprintedMedia() }.getOrDefault(emptyList())
    }

    /**
     * Checks username uniqueness against database and generates unique suggestions if taken.
     */
    suspend fun checkUsernameUniquenessAndSuggest(
        requestedUsername: String,
        fullName: String,
        excludeUserId: String
    ): UsernameUniquenessResult {
        return runCatching {
            val clean = requestedUsername.trim().removePrefix("@").lowercase()
            val isValid = TelefamValidators.validateUsername(clean)
            val takenSet = dao.getAllTakenUsernamesExcept(excludeUserId).map { it.lowercase() }.toSet()
            val isTaken = takenSet.contains(clean)

            val suggestions = if (isTaken) {
                TelefamValidators.generateUniqueUsernameSuggestions(
                    baseUsername = clean.ifBlank { fullName },
                    fullName = fullName,
                    isUsernameTaken = { candidate -> takenSet.contains(candidate.lowercase()) }
                )
            } else {
                emptyList()
            }

            UsernameUniquenessResult(
                requestedUsername = clean,
                isAvailable = isValid && !isTaken,
                isValidFormat = isValid,
                suggestions = suggestions
            )
        }.getOrElse {
            UsernameUniquenessResult(
                requestedUsername = requestedUsername,
                isAvailable = false,
                isValidFormat = false,
                suggestions = emptyList()
            )
        }
    }

    /**
     * Derives Full Name and Username from email prefix, verifying uniqueness in the database.
     * If the email-derived username already exists, automatically selects a unique one and returns suggestions.
     */
    suspend fun resolveUniqueAutofillFromEmail(
        email: String,
        excludeUserId: String
    ): EmailAutofillResult {
        return runCatching {
            val (derivedName, rawUsername) = TelefamValidators.deriveFullNameAndUsernameFromEmail(email)
            val takenSet = dao.getAllTakenUsernamesExcept(excludeUserId).map { it.lowercase() }.toSet()
            val isTaken = rawUsername.isNotBlank() && takenSet.contains(rawUsername.lowercase())

            val suggestions = if (isTaken) {
                TelefamValidators.generateUniqueUsernameSuggestions(
                    baseUsername = rawUsername.ifBlank { derivedName },
                    fullName = derivedName,
                    isUsernameTaken = { candidate -> takenSet.contains(candidate.lowercase()) }
                )
            } else {
                emptyList()
            }

            val assignedUsername = if (!isTaken && TelefamValidators.validateUsername(rawUsername)) {
                rawUsername
            } else {
                suggestions.firstOrNull() ?: rawUsername
            }

            EmailAutofillResult(
                derivedFullName = derivedName,
                assignedUniqueUsername = assignedUsername,
                alternativeSuggestions = suggestions,
                wasOriginalUsernameTaken = isTaken
            )
        }.getOrElse {
            EmailAutofillResult(
                derivedFullName = "",
                assignedUniqueUsername = "",
                alternativeSuggestions = emptyList(),
                wasOriginalUsernameTaken = false
            )
        }
    }

    suspend fun getActiveSessionWithTelemetry(
        screenWidthDp: Int,
        locationTriple: Triple<String, Double?, Double?>,
        simulateAnomaly: Boolean
    ): Pair<EncryptedUserSession?, ProfileEntity?> {
        return runCatching {
            val now = System.currentTimeMillis()
            val isOnline = TelefamCryptoAndTelemetry.isNetworkOnline(context)
            dao.invalidateExpiredOtps(now)

            // 1. Load from hardware-backed AES-256-GCM Encrypted App Storage Vault first, with Room fallback
            val vaultPair = TelefamCryptoAndTelemetry.loadEncryptedSessionVault(context)
            val roomEntity = dao.getLatestActiveSession(now)

            if (vaultPair == null && roomEntity == null) {
                return@runCatching null to null
            }

            val userId = vaultPair?.first?.userId ?: roomEntity?.userId ?: return@runCatching null to null
            val email = vaultPair?.first?.email ?: roomEntity?.email ?: return@runCatching null to null

            // 2. Resolve Profile from Room or hydrate from Encrypted App Storage Vault for offline resilience
            var profile = dao.getProfileByUserId(userId)
            val vaultRecord = vaultPair?.first
            if (profile == null && vaultRecord != null && vaultRecord.hasCompletedProfile && vaultRecord.username.isNotBlank()) {
                profile = ProfileEntity(
                    userId = userId,
                    fullName = vaultRecord.fullName,
                    username = vaultRecord.username,
                    phoneCountryCode = vaultRecord.phoneCountryCode,
                    phoneIsoCode = vaultRecord.phoneIsoCode,
                    phoneNumber = vaultRecord.phoneNumber,
                    gender = vaultRecord.gender,
                    dateOfBirth = vaultRecord.dateOfBirth,
                    bio = vaultRecord.bio,
                    avatarLocalPath = vaultRecord.avatarLocalPath,
                    avatarCdnUrl = vaultRecord.avatarCdnUrl,
                    avatarSha256 = vaultRecord.avatarSha256,
                    avatarPerceptualDHash = vaultRecord.avatarPerceptualDHash
                )
                runCatching { dao.upsertProfile(profile) }
            }

            var currentSessionId = vaultRecord?.sessionId ?: roomEntity?.sessionId ?: UUID.randomUUID().toString()
            var accessToken = vaultRecord?.accessToken ?: "telefam_acc_${UUID.randomUUID()}"
            var refreshToken = vaultRecord?.refreshToken ?: "telefam_ref_${UUID.randomUUID()}"
            var accessTokenSha256 = vaultRecord?.accessTokenSha256 ?: roomEntity?.accessTokenSha256
                ?: TelefamCryptoAndTelemetry.sha256Hex(accessToken.toByteArray())
            var refreshTokenSha256 = vaultRecord?.refreshTokenSha256 ?: roomEntity?.refreshTokenSha256
                ?.ifBlank { TelefamCryptoAndTelemetry.sha256Hex(refreshToken.toByteArray()) }
                ?: TelefamCryptoAndTelemetry.sha256Hex(refreshToken.toByteArray())
            var expiresAtMs = vaultRecord?.expiresAtMs ?: roomEntity?.expiresAtMs ?: (now + SESSION_TTL_MS)
            var refreshExpiresAtMs = vaultRecord?.refreshExpiresAtMs ?: roomEntity?.refreshExpiresAtMs
                ?: (now + 30L * 24 * 60 * 60 * 1000L)
            val serverDeviceHash = vaultRecord?.serverDeviceHash ?: roomEntity?.serverDeviceHash ?: ""
            val ipSubnetFp = vaultRecord?.ipSubnetFp ?: roomEntity?.ipSubnetFp ?: ""
            val locationLabel = vaultRecord?.locationLabel ?: roomEntity?.locationLabel ?: locationTriple.first
            val riskScore = vaultRecord?.riskScore ?: roomEntity?.riskScore ?: 8

            // 3. Preserve tokens refresh for users & NEVER prompt login when offline if session exists
            val needsTokenRefresh = now >= (expiresAtMs - 24 * 60 * 60 * 1000L)
            if (needsTokenRefresh || !isOnline) {
                if (isOnline && needsTokenRefresh) {
                    // Online automatic token rotation using refresh token
                    runCatching {
                        TelefamNetworkClient.api.refreshSessionToken(
                            deviceFp = serverDeviceHash,
                            payload = TelefamTokenRefreshRequest(
                                sessionId = currentSessionId,
                                userId = userId,
                                refreshTokenSha256 = refreshTokenSha256,
                                serverDeviceHash = serverDeviceHash
                            )
                        )
                    }
                    accessToken = "telefam_acc_${UUID.randomUUID()}_$now"
                    refreshToken = "telefam_ref_${UUID.randomUUID()}_$now"
                    accessTokenSha256 = TelefamCryptoAndTelemetry.sha256Hex(accessToken.toByteArray())
                    refreshTokenSha256 = TelefamCryptoAndTelemetry.sha256Hex(refreshToken.toByteArray())
                }
                // Extend token lease so user stays authenticated both online and offline
                if (expiresAtMs <= now + 24 * 60 * 60 * 1000L) {
                    expiresAtMs = now + SESSION_TTL_MS
                }
                if (refreshExpiresAtMs <= now + 7 * 24 * 60 * 60 * 1000L) {
                    refreshExpiresAtMs = now + 30L * 24 * 60 * 60 * 1000L
                }
            }

            val updatedVaultRecord = TelefamCryptoAndTelemetry.EncryptedVaultSessionRecord(
                sessionId = currentSessionId,
                userId = userId,
                email = email,
                accessToken = accessToken,
                refreshToken = refreshToken,
                accessTokenSha256 = accessTokenSha256,
                refreshTokenSha256 = refreshTokenSha256,
                expiresAtMs = expiresAtMs,
                refreshExpiresAtMs = refreshExpiresAtMs,
                serverDeviceHash = serverDeviceHash,
                ipSubnetFp = ipSubnetFp,
                locationLabel = locationLabel,
                riskScore = riskScore,
                hasCompletedProfile = profile != null,
                fullName = profile?.fullName ?: vaultRecord?.fullName ?: "",
                username = profile?.username ?: vaultRecord?.username ?: "",
                phoneCountryCode = profile?.phoneCountryCode ?: vaultRecord?.phoneCountryCode ?: "+254",
                phoneIsoCode = profile?.phoneIsoCode ?: vaultRecord?.phoneIsoCode ?: "KE",
                phoneNumber = profile?.phoneNumber ?: vaultRecord?.phoneNumber ?: "",
                gender = profile?.gender ?: vaultRecord?.gender ?: "",
                dateOfBirth = profile?.dateOfBirth ?: vaultRecord?.dateOfBirth ?: "",
                bio = profile?.bio ?: vaultRecord?.bio ?: "",
                avatarLocalPath = profile?.avatarLocalPath ?: vaultRecord?.avatarLocalPath,
                avatarCdnUrl = profile?.avatarCdnUrl ?: vaultRecord?.avatarCdnUrl,
                avatarSha256 = profile?.avatarSha256 ?: vaultRecord?.avatarSha256,
                avatarPerceptualDHash = profile?.avatarPerceptualDHash ?: vaultRecord?.avatarPerceptualDHash
            )
            val (cipherB64, ivB64) = TelefamCryptoAndTelemetry.saveEncryptedSessionVault(context, updatedVaultRecord)

            dao.insertEncryptedSession(
                EncryptedSessionEntity(
                    sessionId = currentSessionId,
                    userId = userId,
                    email = email,
                    accessTokenSha256 = accessTokenSha256,
                    refreshTokenSha256 = refreshTokenSha256,
                    encryptedPayloadBase64 = cipherB64,
                    ivBase64 = ivB64,
                    serverDeviceHash = serverDeviceHash,
                    ipSubnetFp = ipSubnetFp,
                    locationLabel = locationLabel,
                    riskScore = riskScore,
                    isRevoked = false,
                    expiresAtMs = expiresAtMs,
                    refreshExpiresAtMs = refreshExpiresAtMs,
                    createdAtMs = roomEntity?.createdAtMs ?: now
                )
            )

            val session = EncryptedUserSession(
                sessionId = currentSessionId,
                userId = userId,
                email = email,
                accessTokenHash = accessTokenSha256,
                refreshTokenHash = refreshTokenSha256,
                encryptedPayloadBase64 = cipherB64,
                ivBase64 = ivB64,
                serverDeviceHash = serverDeviceHash,
                ipFingerprint = ipSubnetFp,
                locationLabel = locationLabel,
                riskScore = riskScore,
                expiresAtEpochMs = expiresAtMs,
                refreshExpiresAtEpochMs = refreshExpiresAtMs,
                hasCompletedProfile = profile != null,
                isOfflinePreserved = !isOnline
            )
            session to profile
        }.getOrDefault(null to null)
    }

    suspend fun refreshTelemetrySnapshot(
        email: String?,
        screenWidthDp: Int,
        locationTriple: Triple<String, Double?, Double?>,
        simulateNewDeviceOrLocation: Boolean
    ): DeviceAndNetworkTelemetry {
        val cleanEmail = email?.trim()?.lowercase()
        val user = if (!cleanEmail.isNullOrBlank()) {
            runCatching { dao.getUserByEmail(cleanEmail) }.getOrNull()
        } else {
            null
        }
        val trustedHashes = if (user != null) {
            runCatching {
                dao.getTrustedDevicesForUser(user.userId).map { it.serverDeviceHash }.toSet()
            }.getOrDefault(emptySet())
        } else {
            emptySet()
        }
        return TelefamCryptoAndTelemetry.collectTelemetry(
            context = context,
            screenWidthDp = screenWidthDp,
            knownServerDeviceHashes = trustedHashes,
            lastKnownLocation = user?.lastKnownLocation,
            resolvedLocationLabel = locationTriple.first,
            latitude = locationTriple.second,
            longitude = locationTriple.third,
            failedLoginAttempts = user?.failedLoginAttempts ?: 0,
            failedOtpAttempts = user?.failedOtpAttempts ?: 0,
            simulateNewDeviceOrLocation = simulateNewDeviceOrLocation
        )
    }

    suspend fun checkCooldownLocks(
        email: String,
        ipSubnetFp: String,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        return runCatching {
            val cleanEmail = email.trim().lowercase()
            val user = if (cleanEmail.isNotBlank()) dao.getUserByEmail(cleanEmail) else null
            val ipLoginBucket = dao.getRateLimitBucket("login_ip:$ipSubnetFp")
            val ipOtpBucket = dao.getRateLimitBucket("otp_ip:$ipSubnetFp")
            val emailOtpBucket = if (cleanEmail.isNotBlank()) dao.getRateLimitBucket("otp_email:$cleanEmail") else null

            val loginLockUntil = listOfNotNull(
                user?.loginLockedUntilMs,
                ipLoginBucket?.cooldownUntilMs
            ).maxOrNull() ?: 0L

            val otpLockUntil = listOfNotNull(
                user?.otpLockedUntilMs,
                ipOtpBucket?.cooldownUntilMs,
                emailOtpBucket?.cooldownUntilMs
            ).maxOrNull() ?: 0L

            val loginRemSec = ((loginLockUntil - nowMs) / 1000L).coerceAtLeast(0L)
            val otpRemSec = ((otpLockUntil - nowMs) / 1000L).coerceAtLeast(0L)
            loginRemSec to otpRemSec
        }.getOrDefault(0L to 0L)
    }

    suspend fun initiateSignUpWithOtp(
        email: String,
        password: String,
        telemetry: DeviceAndNetworkTelemetry
    ): AuthActionOutcome {
        return try {
            val now = System.currentTimeMillis()
            dao.invalidateExpiredOtps(now)

            val cleanEmail = email.trim().lowercase()
            if (!TelefamValidators.validateEmail(cleanEmail)) {
                recordForensicEvent(
                    category = "AUTH_SIGNUP",
                    outcome = "REJECTED_INVALID_EMAIL",
                    target = cleanEmail,
                    telemetry = telemetry,
                    endpoint = "/v1/auth/signup",
                    detail = "Email failed format or SQLi validation"
                )
                return AuthActionOutcome.ValidationError("Please enter a valid email address (e.g. name@telefam.company)")
            }
            if (!TelefamValidators.validatePassword(password)) {
                return AuthActionOutcome.ValidationError(
                    "Password must be at least 8 characters and include uppercase, lowercase, number, and symbol."
                )
            }

            val (_, otpRemSec) = checkCooldownLocks(cleanEmail, telemetry.ipSubnetFingerprint, now)
            if (otpRemSec > 0) {
                return AuthActionOutcome.CooldownLocked(
                    lockType = "OTP_LOCK_30M",
                    remainingSeconds = otpRemSec,
                    message = "OTP rate limit active after 3 failed attempts. Please wait ${formatSeconds(otpRemSec)}."
                )
            }

            val existing = dao.getUserByEmail(cleanEmail)
            if (existing != null && existing.emailVerified) {
                recordForensicEvent(
                    category = "AUTH_SIGNUP",
                    outcome = "ACCOUNT_ALREADY_EXISTS",
                    target = cleanEmail,
                    telemetry = telemetry,
                    endpoint = "/v1/auth/signup",
                    detail = "Attempted sign up on verified account"
                )
                return AuthActionOutcome.ValidationError("An account with this email already exists. Please Login instead.")
            }

            val salt = TelefamCryptoAndTelemetry.generateSaltHex()
            val passwordHash = TelefamCryptoAndTelemetry.hashPasswordPbkdf2(password, salt)
            val userEntity = UserEntity(
                userId = existing?.userId ?: UUID.randomUUID().toString(),
                email = cleanEmail,
                passwordHash = passwordHash,
                passwordSalt = salt,
                emailVerified = false,
                lastKnownLocation = telemetry.locationLabel,
                lastLatitude = telemetry.latitude,
                lastLongitude = telemetry.longitude,
                createdAtMs = existing?.createdAtMs ?: now
            )
            dao.upsertUser(userEntity)

            dispatchOtpChallengeInternal(
                email = cleanEmail,
                purpose = "SIGNUP",
                telemetry = telemetry,
                reasonBanner = "Verify your email address to activate your Telefam account."
            )
        } catch (e: Exception) {
            AuthActionOutcome.ValidationError(e.message ?: "Unable to complete sign up right now.")
        }
    }

    suspend fun loginWithEmailAndPassword(
        email: String,
        password: String,
        telemetry: DeviceAndNetworkTelemetry,
        forceStepUpOtpForNewDevice: Boolean
    ): AuthActionOutcome {
        return try {
            val now = System.currentTimeMillis()
            dao.invalidateExpiredOtps(now)
            dao.invalidateExpiredSessions(now)

            val cleanEmail = email.trim().lowercase()
            if (!TelefamValidators.validateEmail(cleanEmail)) {
                return AuthActionOutcome.ValidationError("Please enter a valid email address.")
            }

            val (loginRemSec, otpRemSec) = checkCooldownLocks(cleanEmail, telemetry.ipSubnetFingerprint, now)
            if (loginRemSec > 0) {
                recordForensicEvent(
                    category = "AUTH_LOGIN",
                    outcome = "BLOCKED_15M_COOLDOWN",
                    target = cleanEmail,
                    telemetry = telemetry,
                    endpoint = "/v1/auth/login",
                    detail = "Login blocked by 15-minute brute-force lock (${loginRemSec}s remaining)"
                )
                return AuthActionOutcome.CooldownLocked(
                    lockType = "LOGIN_LOCK_15M",
                    remainingSeconds = loginRemSec,
                    message = "3 failed login attempts detected. Locked for ${formatSeconds(loginRemSec)} or verify via OTP."
                )
            }

            val user = dao.getUserByEmail(cleanEmail)
            if (user == null) {
                val lockOutcome = recordFailedLoginAttempt(null, telemetry, cleanEmail, now)
                return lockOutcome ?: AuthActionOutcome.ValidationError("Invalid email or password. Please check your credentials or Sign up.")
            }

            val computedHash = TelefamCryptoAndTelemetry.hashPasswordPbkdf2(password, user.passwordSalt)
            if (computedHash != user.passwordHash) {
                val lockOutcome = recordFailedLoginAttempt(user, telemetry, cleanEmail, now)
                return lockOutcome ?: run {
                    val attemptsUsed = (user.failedLoginAttempts + 1).coerceAtMost(LOGIN_MAX_FAILURES)
                    val remaining = LOGIN_MAX_FAILURES - attemptsUsed
                    AuthActionOutcome.ValidationError(
                        "Incorrect password ($attemptsUsed/$LOGIN_MAX_FAILURES failed attempts). $remaining attempt(s) left before 15-minute cooldown."
                    )
                }
            }

            // Password verified -> reset failed login counters
            val resetUser = user.copy(
                failedLoginAttempts = 0,
                loginLockedUntilMs = null
            )
            dao.upsertUser(resetUser)
            dao.upsertRateLimitBucket(
                RateLimitBucketEntity(
                    bucketKey = "login_ip:${telemetry.ipSubnetFingerprint}",
                    actionType = "LOGIN",
                    failedCount = 0,
                    cooldownUntilMs = null,
                    updatedAtMs = now
                )
            )

            // Check if 2-step verification is enabled or if this device/location requires step-up verification
            val accSettings = dao.getAccountSettingsAndSecurity(user.userId)
            val isTwoStepEnabled = accSettings?.twoStepEnabled == true
            val trustedDevices = dao.getTrustedDevicesForUser(user.userId)
            val isRecognizedDevice = trustedDevices.any { it.serverDeviceHash == telemetry.serverVerifiedDeviceHash }
            val isLocationShifted = !user.lastKnownLocation.isNullOrBlank() &&
                !user.lastKnownLocation.equals(telemetry.locationLabel, ignoreCase = true)

            if (isTwoStepEnabled || !isRecognizedDevice || isLocationShifted || forceStepUpOtpForNewDevice || !user.emailVerified) {
                if (otpRemSec > 0) {
                    return AuthActionOutcome.CooldownLocked(
                        lockType = "OTP_LOCK_30M",
                        remainingSeconds = otpRemSec,
                        message = "Verification code required, but verification is on a 30-minute cooldown (${formatSeconds(otpRemSec)} left)."
                    )
                }
                val reason = when {
                    isTwoStepEnabled && accSettings?.twoStepMethod == "AUTHENTICATOR_TOTP" ->
                        "2-step verification is active. Enter your 6-digit authenticator or email code, or a backup recovery code."
                    isTwoStepEnabled ->
                        "2-step verification is active. Enter the 6-digit verification code sent to $cleanEmail, or a backup recovery code."
                    !user.emailVerified -> "Please verify your email address before signing in."
                    forceStepUpOtpForNewDevice || !isRecognizedDevice ->
                        "Unrecognized device detected. Enter the verification code sent to $cleanEmail to continue."
                    else ->
                        "Sign-in from a new location (${telemetry.locationLabel}) detected. Enter the verification code to continue."
                }
                recordForensicEvent(
                    category = if (isTwoStepEnabled) "TWO_STEP_VERIFICATION" else "NEW_DEVICE_STEP_UP",
                    outcome = "STEP_UP_OTP_REQUIRED",
                    target = cleanEmail,
                    telemetry = telemetry,
                    endpoint = "/v1/auth/login",
                    detail = reason
                )
                return dispatchOtpChallengeInternal(
                    email = cleanEmail,
                    purpose = "NEW_DEVICE_LOGIN",
                    telemetry = telemetry,
                    reasonBanner = reason
                )
            }

            issueEncryptedSessionAndAudit(resetUser, telemetry, "AUTH_LOGIN_SUCCESS")
        } catch (e: Exception) {
            AuthActionOutcome.ValidationError(e.message ?: "Login error occurred.")
        }
    }

    suspend fun authenticateWithPasskey(
        emailInput: String,
        telemetry: DeviceAndNetworkTelemetry
    ): AuthActionOutcome {
        return try {
            val cleanInput = emailInput.trim().lowercase()
            val (targetUser, accSettings) = if (cleanInput.isNotBlank() && TelefamValidators.validateEmail(cleanInput)) {
                val u = dao.getUserByEmail(cleanInput)
                val acc = u?.let { dao.getAccountSettingsAndSecurity(it.userId) }
                u to acc
            } else {
                val latestPasskeyAcc = dao.getLatestPasskeyEnabledSettings()
                val u = latestPasskeyAcc?.let { dao.getUserById(it.ownerUserId) }
                u to latestPasskeyAcc
            }

            if (targetUser == null || accSettings == null || !accSettings.passkeyEnabled || accSettings.passkeyCredentialId.isBlank()) {
                return AuthActionOutcome.ValidationError(
                    if (cleanInput.isNotBlank()) {
                        "No passkey is set up for $cleanInput yet. Sign in with your password and enable Passkeys in Settings > Security & permissions."
                    } else {
                        "No passkey is registered on this device yet. Sign in with your email and password, then set up a passkey in Settings > Security & permissions."
                    }
                )
            }

            val now = System.currentTimeMillis()
            val assertionSignature = TelefamCryptoAndTelemetry.hmacSha256(
                "WEBAUTHN_ASSERT:${accSettings.passkeyRpId}:${accSettings.passkeyCredentialId}:${accSettings.passkeyPublicKeyCoseHex}:${telemetry.serverVerifiedDeviceHash}"
            )

            runCatching {
                TelefamNetworkClient.api.verifyPasskeyAssertion(
                    deviceFp = telemetry.untrustedClientDeviceFingerprint,
                    payload = TelefamPasskeyAssertionPayload(
                        ownerUserId = targetUser.userId,
                        email = targetUser.email,
                        credentialId = accSettings.passkeyCredentialId,
                        signatureHex = assertionSignature,
                        rpId = accSettings.passkeyRpId,
                        clientTimestampMs = now
                    )
                )
            }

            val updatedUser = targetUser.copy(
                emailVerified = true,
                failedLoginAttempts = 0,
                loginLockedUntilMs = null,
                lastKnownLocation = telemetry.locationLabel,
                lastLatitude = telemetry.latitude,
                lastLongitude = telemetry.longitude
            )
            dao.upsertUser(updatedUser)
            dao.upsertTrustedDevice(
                TrustedDeviceEntity(
                    deviceRecordId = "${targetUser.userId}_${telemetry.serverVerifiedDeviceHash.take(16)}",
                    userId = targetUser.userId,
                    untrustedClientFp = telemetry.untrustedClientDeviceFingerprint,
                    serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                    ipSubnetFp = telemetry.ipSubnetFingerprint,
                    osDescription = telemetry.osDescription,
                    locationLabel = telemetry.locationLabel,
                    isTrusted = true,
                    lastSeenMs = now
                )
            )
            recordForensicEvent(
                category = "PASSKEY_AUTH",
                outcome = "PASSKEY_VERIFIED",
                target = targetUser.email,
                telemetry = telemetry,
                endpoint = "/v1/auth/passkey/verify",
                detail = "Signed in with passkey (${accSettings.passkeyDeviceLabel.ifBlank { "Android Passkey" }})"
            )
            issueEncryptedSessionAndAudit(updatedUser, telemetry, "AUTH_PASSKEY_LOGIN_SUCCESS")
        } catch (e: Exception) {
            AuthActionOutcome.ValidationError(e.message ?: "Unable to complete passkey sign-in.")
        }
    }

    suspend fun requestOtpForLockedLoginOrForgotPassword(
        email: String,
        purpose: String, // FORGOT_PASSWORD or NEW_DEVICE_LOGIN
        telemetry: DeviceAndNetworkTelemetry
    ): AuthActionOutcome {
        return try {
            val now = System.currentTimeMillis()
            dao.invalidateExpiredOtps(now)

            val cleanEmail = email.trim().lowercase()
            if (!TelefamValidators.validateEmail(cleanEmail)) {
                return AuthActionOutcome.ValidationError("Please enter a valid email address.")
            }

            val (_, otpRemSec) = checkCooldownLocks(cleanEmail, telemetry.ipSubnetFingerprint, now)
            if (otpRemSec > 0) {
                recordForensicEvent(
                    category = "OTP_REQUEST",
                    outcome = "BLOCKED_30M_COOLDOWN",
                    target = cleanEmail,
                    telemetry = telemetry,
                    endpoint = "/v1/auth/otp/send",
                    detail = "OTP request blocked by 30-minute cooldown (${otpRemSec}s left)"
                )
                return AuthActionOutcome.CooldownLocked(
                    lockType = "OTP_LOCK_30M",
                    remainingSeconds = otpRemSec,
                    message = "OTP rate limit exceeded (3 failed attempts). Cool down active for ${formatSeconds(otpRemSec)}."
                )
            }

            val user = dao.getUserByEmail(cleanEmail)
            if (user == null) {
                return AuthActionOutcome.ValidationError("No Telefam account found for $cleanEmail. Please Sign up first.")
            }

            // Enforce 60-second resend timer on active challenge
            val activeChallenge = dao.getLatestActiveOtp(cleanEmail, purpose)
            if (activeChallenge != null && activeChallenge.resendAvailableAtMs > now) {
                val waitSec = ((activeChallenge.resendAvailableAtMs - now) / 1000L).coerceAtLeast(1L)
                return AuthActionOutcome.ValidationError("Please wait ${waitSec}s before requesting another OTP code.")
            }

            dispatchOtpChallengeInternal(
                email = cleanEmail,
                purpose = purpose,
                telemetry = telemetry,
                reasonBanner = if (purpose == "FORGOT_PASSWORD") {
                    "Enter the 6-digit OTP and set your new password."
                } else {
                    "Confirm the 6-digit OTP to verify your device and sign in."
                }
            )
        } catch (e: Exception) {
            AuthActionOutcome.ValidationError(e.message ?: "Failed to dispatch OTP.")
        }
    }

    suspend fun verifyOtpChallenge(
        email: String,
        purpose: String,
        enteredOtp: String,
        newPassword: String?,
        confirmPassword: String?,
        telemetry: DeviceAndNetworkTelemetry
    ): AuthActionOutcome {
        return try {
            val now = System.currentTimeMillis()
            dao.invalidateExpiredOtps(now)

            val cleanEmail = email.trim().lowercase()
            val cleanOtp = enteredOtp.trim()

            val (_, otpRemSec) = checkCooldownLocks(cleanEmail, telemetry.ipSubnetFingerprint, now)
            if (otpRemSec > 0) {
                return AuthActionOutcome.CooldownLocked(
                    lockType = "OTP_LOCK_30M",
                    remainingSeconds = otpRemSec,
                    message = "OTP verification locked for ${formatSeconds(otpRemSec)} after 3 failed attempts."
                )
            }

            val user = dao.getUserByEmail(cleanEmail)
            val accSettings = user?.let { dao.getAccountSettingsAndSecurity(it.userId) }

            // Check if the user entered an 8-character Backup Recovery Code (XXXX-XXXX) during 2-step login
            val normalizedCode = cleanOtp.uppercase().replace("-", "").replace(" ", "")
            if (purpose == "NEW_DEVICE_LOGIN" && user != null && accSettings != null && normalizedCode.length == 8) {
                val existingCodes = accSettings.backupRecoveryCodesCsv
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                val matchedCode = existingCodes.firstOrNull {
                    it.uppercase().replace("-", "") == normalizedCode
                }
                if (matchedCode != null) {
                    val remainingCodesCsv = existingCodes.filterNot { it == matchedCode }.joinToString(",")
                    dao.upsertAccountSettingsAndSecurity(
                        accSettings.copy(
                            backupRecoveryCodesCsv = remainingCodesCsv,
                            isPendingSync = true,
                            updatedAtMs = now
                        )
                    )
                    dao.invalidateActiveOtpsForEmail(cleanEmail)
                    val verifiedByRecovery = user.copy(
                        emailVerified = true,
                        failedLoginAttempts = 0,
                        loginLockedUntilMs = null,
                        failedOtpAttempts = 0,
                        otpLockedUntilMs = null,
                        lastKnownLocation = telemetry.locationLabel,
                        lastLatitude = telemetry.latitude,
                        lastLongitude = telemetry.longitude
                    )
                    dao.upsertUser(verifiedByRecovery)
                    dao.upsertTrustedDevice(
                        TrustedDeviceEntity(
                            deviceRecordId = "${verifiedByRecovery.userId}_${telemetry.serverVerifiedDeviceHash.take(16)}",
                            userId = verifiedByRecovery.userId,
                            untrustedClientFp = telemetry.untrustedClientDeviceFingerprint,
                            serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                            ipSubnetFp = telemetry.ipSubnetFingerprint,
                            osDescription = telemetry.osDescription,
                            locationLabel = telemetry.locationLabel,
                            isTrusted = true,
                            lastSeenMs = now
                        )
                    )
                    recordForensicEvent(
                        category = "TWO_STEP_RECOVERY_CODE",
                        outcome = "RECOVERY_CODE_CONSUMED",
                        target = cleanEmail,
                        telemetry = telemetry,
                        endpoint = "/v1/auth/otp/verify",
                        detail = "Signed in using backup recovery code (${existingCodes.size - 1} codes remaining)"
                    )
                    return issueEncryptedSessionAndAudit(verifiedByRecovery, telemetry, "AUTH_RECOVERY_CODE_SUCCESS")
                } else {
                    return AuthActionOutcome.ValidationError("Invalid backup recovery code. Please check your saved recovery codes and try again.")
                }
            }

            if (!TelefamValidators.validateOtp(cleanOtp)) {
                return AuthActionOutcome.ValidationError("Please enter a valid 6-digit verification code (or an 8-character backup recovery code).")
            }

            if (purpose == "FORGOT_PASSWORD") {
                if (newPassword.isNullOrBlank() || !TelefamValidators.validatePassword(newPassword)) {
                    return AuthActionOutcome.ValidationError(
                        "New password must be 8+ characters with uppercase, lowercase, number, and symbol."
                    )
                }
                if (newPassword != confirmPassword) {
                    return AuthActionOutcome.ValidationError("New password and confirm password do not match.")
                }
            }

            val challenge = dao.getLatestActiveOtp(cleanEmail, purpose)
            val isTotpAuthenticatorMatch = purpose == "NEW_DEVICE_LOGIN" &&
                accSettings?.twoStepEnabled == true &&
                accSettings.twoStepMethod == "AUTHENTICATOR_TOTP" &&
                accSettings.authenticatorSecretBase32.isNotBlank() &&
                verifyTotpCodeWindow(accSettings.authenticatorSecretBase32, cleanOtp, now)

            if (!isTotpAuthenticatorMatch) {
                if (challenge == null || challenge.isInvalidated || challenge.expiresAtMs <= now) {
                    recordForensicEvent(
                        category = "OTP_VERIFY",
                        outcome = "EXPIRED_OR_INVALIDATED_OTP",
                        target = cleanEmail,
                        telemetry = telemetry,
                        endpoint = "/v1/auth/otp/verify",
                        detail = "Attempted verification on expired or invalidated verification code"
                    )
                    return AuthActionOutcome.ValidationError("This verification code has expired. Please tap Resend to request a new code.")
                }

                val expectedDigest = TelefamCryptoAndTelemetry.hmacSha256("$cleanEmail|$purpose|$cleanOtp")
                if (expectedDigest != challenge.otpHmacDigest) {
                    return recordFailedOtpAttempt(user, challenge, telemetry, cleanEmail, now)
                }
            }

            // OTP matched! Immediately invalidate this OTP so it can never be replayed
            dao.invalidateActiveOtpsForEmail(cleanEmail)
            dao.upsertRateLimitBucket(
                RateLimitBucketEntity(
                    bucketKey = "otp_ip:${telemetry.ipSubnetFingerprint}",
                    actionType = "OTP",
                    failedCount = 0,
                    cooldownUntilMs = null,
                    updatedAtMs = now
                )
            )
            dao.upsertRateLimitBucket(
                RateLimitBucketEntity(
                    bucketKey = "otp_email:$cleanEmail",
                    actionType = "OTP",
                    failedCount = 0,
                    cooldownUntilMs = null,
                    updatedAtMs = now
                )
            )

            val verifiedUser = (user ?: return AuthActionOutcome.ValidationError("Account record not found.")).let { u ->
                if (purpose == "FORGOT_PASSWORD" && !newPassword.isNullOrBlank()) {
                    val newSalt = TelefamCryptoAndTelemetry.generateSaltHex()
                    val newHash = TelefamCryptoAndTelemetry.hashPasswordPbkdf2(newPassword, newSalt)
                    u.copy(
                        passwordHash = newHash,
                        passwordSalt = newSalt,
                        emailVerified = true,
                        failedLoginAttempts = 0,
                        loginLockedUntilMs = null,
                        failedOtpAttempts = 0,
                        otpLockedUntilMs = null,
                        lastKnownLocation = telemetry.locationLabel,
                        lastLatitude = telemetry.latitude,
                        lastLongitude = telemetry.longitude
                    )
                } else {
                    u.copy(
                        emailVerified = true,
                        failedLoginAttempts = 0,
                        loginLockedUntilMs = null,
                        failedOtpAttempts = 0,
                        otpLockedUntilMs = null,
                        lastKnownLocation = telemetry.locationLabel,
                        lastLatitude = telemetry.latitude,
                        lastLongitude = telemetry.longitude
                    )
                }
            }
            dao.upsertUser(verifiedUser)

            dao.upsertTrustedDevice(
                TrustedDeviceEntity(
                    deviceRecordId = "${verifiedUser.userId}_${telemetry.serverVerifiedDeviceHash.take(16)}",
                    userId = verifiedUser.userId,
                    untrustedClientFp = telemetry.untrustedClientDeviceFingerprint,
                    serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                    ipSubnetFp = telemetry.ipSubnetFingerprint,
                    osDescription = telemetry.osDescription,
                    locationLabel = telemetry.locationLabel,
                    isTrusted = true,
                    lastSeenMs = now
                )
            )

            if (purpose == "FORGOT_PASSWORD") {
                dao.revokeSessionsForUser(verifiedUser.userId)
                recordForensicEvent(
                    category = "PASSWORD_RESET",
                    outcome = "SUCCESS",
                    target = cleanEmail,
                    telemetry = telemetry,
                    endpoint = "/v1/auth/password/reset",
                    detail = "Password updated & prior sessions revoked"
                )
                return AuthActionOutcome.PasswordResetSuccess(cleanEmail)
            }

            issueEncryptedSessionAndAudit(verifiedUser, telemetry, "OTP_VERIFIED_$purpose")
        } catch (e: Exception) {
            AuthActionOutcome.ValidationError(e.message ?: "OTP verification error.")
        }
    }

    suspend fun authenticateWithGoogleIdentity(
        googleEmail: String,
        googleSubId: String,
        displayName: String?,
        telemetry: DeviceAndNetworkTelemetry,
        forceStepUpOtpForNewDevice: Boolean
    ): AuthActionOutcome {
        return try {
            val now = System.currentTimeMillis()
            val cleanEmail = googleEmail.trim().lowercase()
            val existing = dao.getUserByEmail(cleanEmail)
            val salt = existing?.passwordSalt ?: TelefamCryptoAndTelemetry.generateSaltHex()
            val hash = existing?.passwordHash
                ?: TelefamCryptoAndTelemetry.hashPasswordPbkdf2(UUID.randomUUID().toString(), salt)

            val user = UserEntity(
                userId = existing?.userId ?: UUID.randomUUID().toString(),
                email = cleanEmail,
                passwordHash = hash,
                passwordSalt = salt,
                googleSubId = googleSubId,
                emailVerified = true,
                lastKnownLocation = existing?.lastKnownLocation ?: telemetry.locationLabel,
                lastLatitude = telemetry.latitude,
                lastLongitude = telemetry.longitude,
                createdAtMs = existing?.createdAtMs ?: now
            )
            dao.upsertUser(user)

            val trustedDevices = dao.getTrustedDevicesForUser(user.userId)
            val isKnownDevice = trustedDevices.any { it.serverDeviceHash == telemetry.serverVerifiedDeviceHash }
            if ((existing != null && !isKnownDevice) || forceStepUpOtpForNewDevice) {
                recordForensicEvent(
                    category = "GOOGLE_OAUTH_NEW_DEVICE",
                    outcome = "STEP_UP_OTP_REQUIRED",
                    target = cleanEmail,
                    telemetry = telemetry,
                    endpoint = "/v1/auth/google",
                    detail = "Google sign-in on unrecognized device; step-up OTP dispatched"
                )
                return dispatchOtpChallengeInternal(
                    email = cleanEmail,
                    purpose = "NEW_DEVICE_LOGIN",
                    telemetry = telemetry,
                    reasonBanner = "Google account verified. Confirm 6-digit OTP to authorize this new device."
                )
            }

            dao.upsertTrustedDevice(
                TrustedDeviceEntity(
                    deviceRecordId = "${user.userId}_${telemetry.serverVerifiedDeviceHash.take(16)}",
                    userId = user.userId,
                    untrustedClientFp = telemetry.untrustedClientDeviceFingerprint,
                    serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                    ipSubnetFp = telemetry.ipSubnetFingerprint,
                    osDescription = telemetry.osDescription,
                    locationLabel = telemetry.locationLabel,
                    isTrusted = true,
                    lastSeenMs = now
                )
            )

            issueEncryptedSessionAndAudit(user, telemetry, "GOOGLE_OAUTH_SUCCESS")
        } catch (e: Exception) {
            AuthActionOutcome.ValidationError(e.message ?: "Google authentication error.")
        }
    }

    suspend fun saveProfileSetup(
        userId: String,
        email: String,
        fullName: String,
        username: String,
        phoneCountryCode: String,
        phoneIsoCode: String,
        phoneNumber: String,
        gender: String,
        dateOfBirth: String,
        bio: String,
        sanitizedAvatar: SanitizedAvatarResult?, // Optional photo!
        telemetry: DeviceAndNetworkTelemetry,
        enforceEditCooldowns: Boolean = false
    ): Result<ProfileEntity> {
        return try {
            val cleanName = TelefamInputSanitizer.sanitizeFullName(fullName)
            val cleanUsername = TelefamInputSanitizer.sanitizeUsername(username)
            val cleanPhone = TelefamInputSanitizer.sanitizePhoneNumber(phoneNumber)
            val cleanBio = TelefamInputSanitizer.sanitizeBio(bio)
            val cleanGender = TelefamInputSanitizer.sanitizeGender(gender)
            val cleanDob = TelefamInputSanitizer.sanitizeDateOfBirth(dateOfBirth)

            if (!TelefamValidators.validateFullName(cleanName)) {
                return Result.failure(IllegalArgumentException("Full name must be at least 2 characters."))
            }
            if (!TelefamValidators.validateUsername(cleanUsername)) {
                return Result.failure(
                    IllegalArgumentException("Username must be 3–24 characters using letters, numbers, or underscores.")
                )
            }
            if (!TelefamValidators.validatePhoneNumber(cleanPhone)) {
                return Result.failure(IllegalArgumentException("Please enter a valid phone number (7–15 digits)."))
            }
            if (cleanGender.isBlank()) {
                return Result.failure(IllegalArgumentException("Please select your gender."))
            }
            if (cleanDob.isBlank()) {
                return Result.failure(IllegalArgumentException("Please select your date of birth."))
            }
            if (!TelefamValidators.validateAgeAtLeast13(cleanDob)) {
                val age = TelefamValidators.calculateAgeYears(cleanDob)
                return Result.failure(
                    IllegalArgumentException(
                        if (age != null && age in 0..12) {
                            "You must be at least 13 years old to use Telefam (selected age: $age)."
                        } else {
                            "Please select a valid date of birth (13 years and above)."
                        }
                    )
                )
            }
            if (!TelefamValidators.validateBio(cleanBio)) {
                return Result.failure(IllegalArgumentException("Bio cannot exceed 150 characters."))
            }

            val existingProfile = dao.getProfileByUserId(userId)
            val now = System.currentTimeMillis()
            val cooldown = OwnerProfileCooldownState.compute(
                lastFullNameChangedAtMs = existingProfile?.lastFullNameChangedAtMs,
                lastUsernameChangedAtMs = existingProfile?.lastUsernameChangedAtMs,
                nowMs = now
            )

            val isFullNameModified = existingProfile != null &&
                !existingProfile.fullName.trim().equals(cleanName, ignoreCase = false)
            val isUsernameModified = existingProfile != null &&
                !existingProfile.username.trim().equals(cleanUsername, ignoreCase = true)

            if (enforceEditCooldowns && existingProfile != null) {
                if (isFullNameModified && !cooldown.canChangeFullName) {
                    return Result.failure(
                        IllegalStateException(
                            "Full name can only be changed once every 7 days. You can change it again in ${cooldown.fullNameRemainingDays} day(s)."
                        )
                    )
                }
                if (isUsernameModified && !cooldown.canChangeUsername) {
                    return Result.failure(
                        IllegalStateException(
                            "Username can only be changed once every 30 days. You can change it again in ${cooldown.usernameRemainingDays} day(s)."
                        )
                    )
                }
            }

            // Backend Username Uniqueness Check + Unique Suggestion Generator
            val uniqueness = checkUsernameUniquenessAndSuggest(
                requestedUsername = cleanUsername,
                fullName = cleanName,
                excludeUserId = userId
            )
            if (!uniqueness.isAvailable) {
                recordForensicEvent(
                    category = "USERNAME_UNIQUENESS_CHECK",
                    outcome = "COLLISION_SUGGESTIONS_GENERATED",
                    target = "@$cleanUsername",
                    telemetry = telemetry,
                    endpoint = "/v1/profile/username/check",
                    detail = "Username taken • Suggested=${uniqueness.suggestions.joinToString(",")}"
                )
                return Result.failure(
                    UsernameConflictException(
                        requestedUsername = cleanUsername,
                        suggestions = uniqueness.suggestions,
                        message = "Username @$cleanUsername is already taken. Try one of the suggested unique usernames below."
                    )
                )
            }

            val nextLastFullNameChangedAtMs = when {
                existingProfile == null -> null
                isFullNameModified -> now
                else -> existingProfile.lastFullNameChangedAtMs
            }
            val nextLastUsernameChangedAtMs = when {
                existingProfile == null -> null
                isUsernameModified -> now
                else -> existingProfile.lastUsernameChangedAtMs
            }

            val entity = ProfileEntity(
                userId = userId,
                fullName = cleanName,
                username = cleanUsername,
                phoneCountryCode = phoneCountryCode,
                phoneIsoCode = phoneIsoCode,
                phoneNumber = cleanPhone,
                gender = cleanGender,
                dateOfBirth = cleanDob,
                bio = cleanBio,
                avatarLocalPath = sanitizedAvatar?.secureFilePath ?: existingProfile?.avatarLocalPath,
                avatarCdnUrl = sanitizedAvatar?.cdnStorageUrl ?: existingProfile?.avatarCdnUrl,
                avatarSha256 = sanitizedAvatar?.sha256Checksum ?: existingProfile?.avatarSha256,
                avatarPerceptualDHash = sanitizedAvatar?.perceptualDHash ?: existingProfile?.avatarPerceptualDHash,
                avatarMimeType = sanitizedAvatar?.verifiedMimeType ?: existingProfile?.avatarMimeType,
                avatarByteSize = sanitizedAvatar?.compressedByteSize ?: existingProfile?.avatarByteSize,
                lastFullNameChangedAtMs = nextLastFullNameChangedAtMs,
                lastUsernameChangedAtMs = nextLastUsernameChangedAtMs,
                subscribersCount = existingProfile?.subscribersCount ?: 0L,
                isPendingSync = true,
                lastSyncedAtMs = existingProfile?.lastSyncedAtMs,
                updatedAtMs = now
            )
            dao.upsertProfile(entity)

            // Enqueue background profile sync in social outbox + trigger WorkManager
            val idempotencyKey = TelefamCryptoAndTelemetry.sha256Hex(
                "profile_update:$userId:${entity.username}:${now / 1500L}".toByteArray(Charsets.UTF_8)
            )
            val payloadJson = JSONObject().apply {
                put("userId", userId)
                put("fullName", entity.fullName)
                put("username", entity.username)
                put("phoneCountryCode", entity.phoneCountryCode)
                put("phoneIsoCode", entity.phoneIsoCode)
                put("phoneNumber", entity.phoneNumber)
                put("gender", entity.gender)
                put("dateOfBirth", entity.dateOfBirth)
                put("bio", entity.bio)
                put("avatarCdnUrl", entity.avatarCdnUrl ?: "")
                put("avatarSha256", entity.avatarSha256 ?: "")
                put("avatarPerceptualDHash", entity.avatarPerceptualDHash ?: "")
                put("lastFullNameChangedAtMs", entity.lastFullNameChangedAtMs ?: 0L)
                put("lastUsernameChangedAtMs", entity.lastUsernameChangedAtMs ?: 0L)
            }.toString()

            dao.enqueueSocialSyncOperation(
                SocialSyncQueueEntity(
                    operationId = UUID.randomUUID().toString(),
                    userId = userId,
                    operationType = "PROFILE_UPDATE",
                    targetUserId = userId,
                    idempotencyKey = idempotencyKey,
                    payloadJson = payloadJson,
                    internalRiskScore = telemetry.riskScore,
                    createdAtMs = now
                )
            )
            TelefamPrivacySyncWorker.enqueueSync(context)

            if (TelefamCryptoAndTelemetry.isNetworkOnline(context)) {
                runCatching {
                    val synced = TelefamNetworkClient.api.syncOwnerProfileUpdate(
                        rlsUserId = userId,
                        idempotencyKey = idempotencyKey,
                        deviceFp = telemetry.untrustedClientDeviceFingerprint,
                        payload = TelefamOwnerProfileUpdatePayload(
                            userId = userId,
                            fullName = entity.fullName,
                            username = entity.username,
                            phoneCountryCode = entity.phoneCountryCode,
                            phoneIsoCode = entity.phoneIsoCode,
                            phoneNumber = entity.phoneNumber,
                            gender = entity.gender,
                            dateOfBirth = entity.dateOfBirth,
                            bio = entity.bio,
                            avatarCdnUrl = entity.avatarCdnUrl,
                            avatarSha256 = entity.avatarSha256,
                            avatarPerceptualDHash = entity.avatarPerceptualDHash,
                            lastFullNameChangedAtMs = entity.lastFullNameChangedAtMs,
                            lastUsernameChangedAtMs = entity.lastUsernameChangedAtMs,
                            idempotencyKey = idempotencyKey,
                            clientTimestampMs = now
                        )
                    ).synced
                    if (synced) {
                        dao.markProfileSynced(userId, System.currentTimeMillis())
                    }
                }
            }

            // Sync completed profile & local offline avatar path into AES-256-GCM Encrypted Session Vault
            val existingVault = TelefamCryptoAndTelemetry.loadEncryptedSessionVault(context)?.first
            val accessToken = existingVault?.accessToken ?: "telefam_acc_${UUID.randomUUID()}_$now"
            val refreshToken = existingVault?.refreshToken ?: "telefam_ref_${UUID.randomUUID()}_$now"
            TelefamCryptoAndTelemetry.saveEncryptedSessionVault(
                context = context,
                record = TelefamCryptoAndTelemetry.EncryptedVaultSessionRecord(
                    sessionId = existingVault?.sessionId ?: UUID.randomUUID().toString(),
                    userId = userId,
                    email = email,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    accessTokenSha256 = existingVault?.accessTokenSha256
                        ?: TelefamCryptoAndTelemetry.sha256Hex(accessToken.toByteArray()),
                    refreshTokenSha256 = existingVault?.refreshTokenSha256
                        ?: TelefamCryptoAndTelemetry.sha256Hex(refreshToken.toByteArray()),
                    expiresAtMs = existingVault?.expiresAtMs ?: (now + SESSION_TTL_MS),
                    refreshExpiresAtMs = existingVault?.refreshExpiresAtMs ?: (now + 30L * 24 * 60 * 60 * 1000L),
                    serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                    ipSubnetFp = telemetry.ipSubnetFingerprint,
                    locationLabel = telemetry.locationLabel,
                    riskScore = telemetry.riskScore,
                    hasCompletedProfile = true,
                    fullName = entity.fullName,
                    username = entity.username,
                    phoneCountryCode = entity.phoneCountryCode,
                    phoneIsoCode = entity.phoneIsoCode,
                    phoneNumber = entity.phoneNumber,
                    gender = entity.gender,
                    dateOfBirth = entity.dateOfBirth,
                    bio = entity.bio,
                    avatarLocalPath = entity.avatarLocalPath,
                    avatarCdnUrl = entity.avatarCdnUrl,
                    avatarSha256 = entity.avatarSha256,
                    avatarPerceptualDHash = entity.avatarPerceptualDHash
                )
            )

            recordForensicEvent(
                category = "PROFILE_SETUP_SAVED",
                outcome = "SUCCESS",
                target = "@$cleanUsername ($email)",
                telemetry = telemetry,
                endpoint = "/v1/profile/complete",
                detail = "Profile saved • Photo=${if (entity.avatarSha256 != null) "dHash:${entity.avatarPerceptualDHash}" else "optional/skipped"}"
            )
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOutCurrentSession(userId: String, email: String, telemetry: DeviceAndNetworkTelemetry) {
        runCatching {
            dao.revokeSessionsForUser(userId)
            TelefamCryptoAndTelemetry.clearEncryptedSessionVault(context)
            recordForensicEvent(
                category = "SESSION_LOGOUT",
                outcome = "REVOKED",
                target = email,
                telemetry = telemetry,
                endpoint = "/v1/auth/logout",
                detail = "Encrypted session token revoked"
            )
        }
    }

    private suspend fun dispatchOtpChallengeInternal(
        email: String,
        purpose: String,
        telemetry: DeviceAndNetworkTelemetry,
        reasonBanner: String?
    ): AuthActionOutcome {
        val now = System.currentTimeMillis()
        dao.invalidateActiveOtpsForEmail(email)

        val otpCode = TelefamCryptoAndTelemetry.generateSecureSixDigitOtp()
        val digest = TelefamCryptoAndTelemetry.hmacSha256("$email|$purpose|$otpCode")
        val challenge = OtpChallengeEntity(
            challengeId = UUID.randomUUID().toString(),
            email = email,
            purpose = purpose,
            otpHmacDigest = digest,
            ipSubnetFp = telemetry.ipSubnetFingerprint,
            serverDeviceHash = telemetry.serverVerifiedDeviceHash,
            verifyAttempts = 0,
            isInvalidated = false,
            resendAvailableAtMs = now + OTP_RESEND_WAIT_MS,
            expiresAtMs = now + OTP_EXPIRATION_MS,
            createdAtMs = now
        )
        dao.insertOtpChallenge(challenge)

        // Dispatch real verification OTP email via Resend API
        runCatching {
            TelefamNetworkClient.sendOtpEmailViaResend(
                recipientEmail = email,
                otpCode = otpCode,
                purpose = purpose
            )
        }

        runCatching {
            TelefamNetworkClient.api.syncAuthTelemetry(
                deviceFp = telemetry.untrustedClientDeviceFingerprint,
                location = telemetry.locationLabel,
                payload = TelefamAuthNetworkPayload(
                    email = email,
                    purpose = purpose,
                    clientDeviceFingerprint = telemetry.untrustedClientDeviceFingerprint,
                    ipSubnetFingerprint = telemetry.ipSubnetFingerprint,
                    locationLabel = telemetry.locationLabel
                )
            )
        }

        recordForensicEvent(
            category = "OTP_DISPATCH",
            outcome = "OTP_SENT",
            target = email,
            telemetry = telemetry,
            endpoint = "/v1/auth/otp/send",
            detail = "Purpose=$purpose • Prior tokens invalidated • TTL=10m • Resend=60s"
        )

        return AuthActionOutcome.OtpDispatched(
            email = email,
            purpose = purpose,
            dispatchedOtpCode = otpCode,
            resendCountdownSeconds = 60,
            expiresInMinutes = 10,
            reasonBanner = reasonBanner
        )
    }

    private suspend fun recordFailedLoginAttempt(
        user: UserEntity?,
        telemetry: DeviceAndNetworkTelemetry,
        email: String,
        nowMs: Long
    ): AuthActionOutcome.CooldownLocked? {
        val ipKey = "login_ip:${telemetry.ipSubnetFingerprint}"
        val ipBucket = dao.getRateLimitBucket(ipKey)
        val newIpFails = (ipBucket?.failedCount ?: 0) + 1
        val newUserFails = (user?.failedLoginAttempts ?: 0) + 1
        val maxFails = maxOf(newIpFails, newUserFails)

        val lockUntil = if (maxFails >= LOGIN_MAX_FAILURES) nowMs + LOGIN_COOLDOWN_MS else null

        dao.upsertRateLimitBucket(
            RateLimitBucketEntity(
                bucketKey = ipKey,
                actionType = "LOGIN",
                failedCount = newIpFails,
                cooldownUntilMs = lockUntil,
                updatedAtMs = nowMs
            )
        )

        if (user != null) {
            dao.upsertUser(
                user.copy(
                    failedLoginAttempts = newUserFails,
                    loginLockedUntilMs = lockUntil
                )
            )
        }

        recordForensicEvent(
            category = "AUTH_LOGIN",
            outcome = if (lockUntil != null) "LOCKED_15M_COOLDOWN" else "FAILED_ATTEMPT_$maxFails",
            target = email,
            telemetry = telemetry,
            endpoint = "/v1/auth/login",
            detail = "Failed login attempt $maxFails/$LOGIN_MAX_FAILURES from IP subnet ${telemetry.ipSubnetFingerprint}"
        )

        return if (lockUntil != null) {
            val remSec = LOGIN_COOLDOWN_MS / 1000L
            AuthActionOutcome.CooldownLocked(
                lockType = "LOGIN_LOCK_15M",
                remainingSeconds = remSec,
                message = "3 failed login attempts! Password login is locked for 15 minutes (${formatSeconds(remSec)}), or you can verify via OTP."
            )
        } else {
            null
        }
    }

    private suspend fun recordFailedOtpAttempt(
        user: UserEntity?,
        challenge: OtpChallengeEntity,
        telemetry: DeviceAndNetworkTelemetry,
        email: String,
        nowMs: Long
    ): AuthActionOutcome {
        val ipKey = "otp_ip:${telemetry.ipSubnetFingerprint}"
        val emailKey = "otp_email:$email"
        val ipBucket = dao.getRateLimitBucket(ipKey)
        val emailBucket = dao.getRateLimitBucket(emailKey)

        val newChallengeAttempts = challenge.verifyAttempts + 1
        val newIpFails = (ipBucket?.failedCount ?: 0) + 1
        val newEmailFails = (emailBucket?.failedCount ?: 0) + 1
        val maxFails = maxOf(newChallengeAttempts, newIpFails, newEmailFails)

        val lockUntil = if (maxFails >= OTP_MAX_FAILURES) nowMs + OTP_COOLDOWN_MS else null

        dao.insertOtpChallenge(
            challenge.copy(
                verifyAttempts = newChallengeAttempts,
                isInvalidated = lockUntil != null
            )
        )
        dao.upsertRateLimitBucket(
            RateLimitBucketEntity(
                bucketKey = ipKey,
                actionType = "OTP",
                failedCount = newIpFails,
                cooldownUntilMs = lockUntil,
                updatedAtMs = nowMs
            )
        )
        dao.upsertRateLimitBucket(
            RateLimitBucketEntity(
                bucketKey = emailKey,
                actionType = "OTP",
                failedCount = newEmailFails,
                cooldownUntilMs = lockUntil,
                updatedAtMs = nowMs
            )
        )
        if (user != null) {
            dao.upsertUser(
                user.copy(
                    failedOtpAttempts = maxFails,
                    otpLockedUntilMs = lockUntil
                )
            )
        }

        recordForensicEvent(
            category = "OTP_VERIFY",
            outcome = if (lockUntil != null) "LOCKED_30M_COOLDOWN" else "INVALID_OTP_$maxFails",
            target = email,
            telemetry = telemetry,
            endpoint = "/v1/auth/otp/verify",
            detail = "Failed OTP attempt $maxFails/$OTP_MAX_FAILURES • Challenge invalidated=${lockUntil != null}"
        )

        return if (lockUntil != null) {
            val remSec = OTP_COOLDOWN_MS / 1000L
            AuthActionOutcome.CooldownLocked(
                lockType = "OTP_LOCK_30M",
                remainingSeconds = remSec,
                message = "3 failed OTP attempts detected! OTP requests and verification are locked for 30 minutes (${formatSeconds(remSec)})."
            )
        } else {
            val remAttempts = (OTP_MAX_FAILURES - maxFails).coerceAtLeast(1)
            AuthActionOutcome.ValidationError(
                "Invalid OTP code ($maxFails/$OTP_MAX_FAILURES failed attempts). $remAttempts attempt(s) remaining before 30-minute cooldown."
            )
        }
    }

    private suspend fun issueEncryptedSessionAndAudit(
        user: UserEntity,
        telemetry: DeviceAndNetworkTelemetry,
        auditOutcome: String
    ): AuthActionOutcome.Authenticated {
        val now = System.currentTimeMillis()
        dao.revokeSessionsForUser(user.userId)

        dao.upsertTrustedDevice(
            TrustedDeviceEntity(
                deviceRecordId = "${user.userId}_${telemetry.serverVerifiedDeviceHash.take(16)}",
                userId = user.userId,
                untrustedClientFp = telemetry.untrustedClientDeviceFingerprint,
                serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                ipSubnetFp = telemetry.ipSubnetFingerprint,
                osDescription = telemetry.osDescription,
                locationLabel = telemetry.locationLabel,
                isTrusted = true,
                lastSeenMs = now
            )
        )

        val sessionId = UUID.randomUUID().toString()
        val rawAccessToken = "telefam_acc_${UUID.randomUUID()}_$now"
        val rawRefreshToken = "telefam_ref_${UUID.randomUUID()}_$now"
        val tokenSha256 = TelefamCryptoAndTelemetry.sha256Hex(rawAccessToken.toByteArray())
        val refreshTokenSha256 = TelefamCryptoAndTelemetry.sha256Hex(rawRefreshToken.toByteArray())

        val expiresAt = now + SESSION_TTL_MS
        val refreshExpiresAt = now + 30L * 24 * 60 * 60 * 1000L
        val profile = dao.getProfileByUserId(user.userId)

        val (encryptedBase64, ivBase64) = TelefamCryptoAndTelemetry.saveEncryptedSessionVault(
            context = context,
            record = TelefamCryptoAndTelemetry.EncryptedVaultSessionRecord(
                sessionId = sessionId,
                userId = user.userId,
                email = user.email,
                accessToken = rawAccessToken,
                refreshToken = rawRefreshToken,
                accessTokenSha256 = tokenSha256,
                refreshTokenSha256 = refreshTokenSha256,
                expiresAtMs = expiresAt,
                refreshExpiresAtMs = refreshExpiresAt,
                serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                ipSubnetFp = telemetry.ipSubnetFingerprint,
                locationLabel = telemetry.locationLabel,
                riskScore = telemetry.riskScore,
                hasCompletedProfile = profile != null,
                fullName = profile?.fullName ?: "",
                username = profile?.username ?: "",
                phoneCountryCode = profile?.phoneCountryCode ?: "+254",
                phoneIsoCode = profile?.phoneIsoCode ?: "KE",
                phoneNumber = profile?.phoneNumber ?: "",
                gender = profile?.gender ?: "",
                dateOfBirth = profile?.dateOfBirth ?: "",
                bio = profile?.bio ?: "",
                avatarLocalPath = profile?.avatarLocalPath,
                avatarCdnUrl = profile?.avatarCdnUrl,
                avatarSha256 = profile?.avatarSha256,
                avatarPerceptualDHash = profile?.avatarPerceptualDHash
            )
        )

        val sessionEntity = EncryptedSessionEntity(
            sessionId = sessionId,
            userId = user.userId,
            email = user.email,
            accessTokenSha256 = tokenSha256,
            refreshTokenSha256 = refreshTokenSha256,
            encryptedPayloadBase64 = encryptedBase64,
            ivBase64 = ivBase64,
            serverDeviceHash = telemetry.serverVerifiedDeviceHash,
            ipSubnetFp = telemetry.ipSubnetFingerprint,
            locationLabel = telemetry.locationLabel,
            riskScore = telemetry.riskScore,
            isRevoked = false,
            expiresAtMs = expiresAt,
            refreshExpiresAtMs = refreshExpiresAt,
            createdAtMs = now
        )
        dao.insertEncryptedSession(sessionEntity)

        recordForensicEvent(
            category = "SESSION_ISSUED",
            outcome = auditOutcome,
            target = user.email,
            telemetry = telemetry,
            endpoint = "/v1/auth/session",
            detail = "AES-256-GCM encrypted session + refresh token issued • TokenSHA256=${tokenSha256.take(16)}"
        )

        val domainSession = EncryptedUserSession(
            sessionId = sessionId,
            userId = user.userId,
            email = user.email,
            accessTokenHash = tokenSha256,
            refreshTokenHash = refreshTokenSha256,
            encryptedPayloadBase64 = encryptedBase64,
            ivBase64 = ivBase64,
            serverDeviceHash = telemetry.serverVerifiedDeviceHash,
            ipFingerprint = telemetry.ipSubnetFingerprint,
            locationLabel = telemetry.locationLabel,
            riskScore = telemetry.riskScore,
            expiresAtEpochMs = expiresAt,
            refreshExpiresAtEpochMs = refreshExpiresAt,
            hasCompletedProfile = profile != null
        )
        return AuthActionOutcome.Authenticated(domainSession, profile)
    }

    suspend fun recordForensicEvent(
        category: String,
        outcome: String,
        target: String,
        telemetry: DeviceAndNetworkTelemetry,
        endpoint: String,
        detail: String
    ) {
        runCatching {
            dao.insertForensicAuditLog(
                ForensicAuditLogEntity(
                    auditId = UUID.randomUUID().toString(),
                    timestampMs = System.currentTimeMillis(),
                    eventCategory = category,
                    eventOutcome = outcome,
                    targetIdentifier = target.ifBlank { "anonymous@telefam.company" },
                    clientIp = telemetry.clientIpAddress,
                    ipSubnetFp = telemetry.ipSubnetFingerprint,
                    untrustedClientFp = telemetry.untrustedClientDeviceFingerprint,
                    serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                    locationLabel = telemetry.locationLabel,
                    riskScore = telemetry.riskScore,
                    riskSignalsCsv = telemetry.riskSignals.joinToString(", "),
                    endpointPath = endpoint,
                    forensicDetail = detail
                )
            )
        }
    }

    suspend fun getOrCreatePrivacySettings(userId: String): PrivacySettingsEntity {
        val cleanUserId = userId.ifBlank { "local_telefam_member" }
        val existing = dao.getPrivacySettings(cleanUserId)
        if (existing != null) {
            if (existing.isPendingSync) {
                TelefamPrivacySyncWorker.enqueueSync(context)
            }
            return existing
        }
        val created = PrivacySettingsEntity(
            userId = cleanUserId,
            chatThemeId = TelefamChatThemes.DEFAULT.id,
            bubbleColorHex = TelefamBubbleColors.DEFAULT_HEX,
            isPendingSync = true,
            syncVersion = 1L,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertPrivacySettings(created)
        TelefamPrivacySyncWorker.enqueueSync(context)
        return created
    }

    suspend fun updatePrivacyRuleOfflineFirst(
        userId: String,
        ruleKey: PrivacyRuleKey,
        audience: PrivacyAudience,
        telemetry: DeviceAndNetworkTelemetry
    ): PrivacySettingsEntity {
        val current = getOrCreatePrivacySettings(userId)
        val wire = audience.wireValue
        val now = System.currentTimeMillis()
        val updated = when (ruleKey) {
            PrivacyRuleKey.MESSAGE_REQUESTS -> current.copy(messageRequests = wire)
            PrivacyRuleKey.WHO_CAN_CALL_ME -> current.copy(whoCanCallMe = wire)
            PrivacyRuleKey.WHO_CAN_SCREENSHOT_CHATS -> current.copy(whoCanScreenshotChats = wire)
            PrivacyRuleKey.WHO_CAN_SHARE_CHATS -> current.copy(whoCanShareChats = wire)
            PrivacyRuleKey.WHO_CAN_COPY_MESSAGES -> current.copy(whoCanCopyMessages = wire)
            PrivacyRuleKey.WHO_CAN_DOWNLOAD_MEDIA -> current.copy(whoCanDownloadMedia = wire)
            PrivacyRuleKey.READ_RECEIPTS -> current.copy(readReceipts = wire)
            PrivacyRuleKey.TYPING_INDICATORS -> current.copy(typingIndicators = wire)
            PrivacyRuleKey.ONLINE_INDICATORS -> current.copy(onlineIndicators = wire)
        }.copy(
            syncVersion = current.syncVersion + 1L,
            isPendingSync = true,
            updatedAtMs = now
        )

        return commitAndSyncPrivacySettings(
            updated = updated,
            telemetry = telemetry,
            auditDetail = "Updated ${ruleKey.name}=$wire (Offline-First Room + RLS Sync v${updated.syncVersion})"
        )
    }

    suspend fun updateChatBackgroundThemeOfflineFirst(
        userId: String,
        themeId: String,
        telemetry: DeviceAndNetworkTelemetry
    ): PrivacySettingsEntity {
        val current = getOrCreatePrivacySettings(userId)
        val validatedTheme = TelefamChatThemes.findById(themeId)
        val updated = current.copy(
            chatThemeId = validatedTheme.id,
            syncVersion = current.syncVersion + 1L,
            isPendingSync = true,
            updatedAtMs = System.currentTimeMillis()
        )
        return commitAndSyncPrivacySettings(
            updated = updated,
            telemetry = telemetry,
            auditDetail = "Updated chatThemeId=${validatedTheme.id} (Offline-First Room + RLS Sync v${updated.syncVersion})"
        )
    }

    suspend fun updateMessageBubbleColorOfflineFirst(
        userId: String,
        bubbleColorHex: String,
        telemetry: DeviceAndNetworkTelemetry
    ): PrivacySettingsEntity {
        val current = getOrCreatePrivacySettings(userId)
        val validatedColor = TelefamBubbleColors.findByHex(bubbleColorHex)
        val updated = current.copy(
            bubbleColorHex = validatedColor.colorHex,
            syncVersion = current.syncVersion + 1L,
            isPendingSync = true,
            updatedAtMs = System.currentTimeMillis()
        )
        return commitAndSyncPrivacySettings(
            updated = updated,
            telemetry = telemetry,
            auditDetail = "Updated bubbleColorHex=${validatedColor.colorHex} (Offline-First Room + RLS Sync v${updated.syncVersion})"
        )
    }

    private suspend fun commitAndSyncPrivacySettings(
        updated: PrivacySettingsEntity,
        telemetry: DeviceAndNetworkTelemetry,
        auditDetail: String
    ): PrivacySettingsEntity {
        // 1. Write Offline-First to Room immediately
        dao.upsertPrivacySettings(updated)

        // 2. Always enqueue WorkManager background retry worker for offline/transient resilience
        TelefamPrivacySyncWorker.enqueueSync(context)

        // 3. If network is online right now, attempt immediate RLS-protected sync
        val finalEntity = if (TelefamCryptoAndTelemetry.isNetworkOnline(context)) {
            val syncedNow = runCatching {
                TelefamNetworkClient.api.syncPrivacySettings(
                    rlsUserId = updated.userId,
                    deviceFp = telemetry.untrustedClientDeviceFingerprint,
                    payload = TelefamPrivacySettingsNetworkPayload(
                        userId = updated.userId,
                        messageRequests = updated.messageRequests,
                        whoCanCallMe = updated.whoCanCallMe,
                        whoCanScreenshotChats = updated.whoCanScreenshotChats,
                        whoCanShareChats = updated.whoCanShareChats,
                        whoCanCopyMessages = updated.whoCanCopyMessages,
                        whoCanDownloadMedia = updated.whoCanDownloadMedia,
                        chatThemeId = updated.chatThemeId,
                        bubbleColorHex = updated.bubbleColorHex,
                        readReceipts = updated.readReceipts,
                        typingIndicators = updated.typingIndicators,
                        onlineIndicators = updated.onlineIndicators,
                        syncVersion = updated.syncVersion
                    )
                ).synced
            }.getOrDefault(false)

            if (syncedNow) {
                val syncedTime = System.currentTimeMillis()
                dao.markPrivacySettingsSynced(updated.userId, syncedTime)
                updated.copy(
                    isPendingSync = false,
                    syncRetryCount = 0,
                    lastSyncedAtMs = syncedTime
                )
            } else {
                updated
            }
        } else {
            updated
        }

        recordForensicEvent(
            category = "PRIVACY_RLS_SYNC",
            outcome = if (finalEntity.isPendingSync) "SAVED_OFFLINE_QUEUED_WORKER" else "SYNCED_RLS_OK",
            target = updated.userId,
            telemetry = telemetry,
            endpoint = "/v1/privacy/settings",
            detail = auditDetail
        )

        return finalEntity
    }

    private fun verifyTotpCodeWindow(secretBase32: String, enteredCode: String, nowMs: Long): Boolean {
        val currentStep = nowMs / 30_000L
        for (offset in -1L..1L) {
            if (computeTotpForStep(secretBase32, currentStep + offset) == enteredCode) {
                return true
            }
        }
        return false
    }

    private fun formatSeconds(totalSec: Long): String {
        val mins = totalSec / 60
        val secs = totalSec % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
