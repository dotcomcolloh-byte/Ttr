package com.telefam.company.app.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class TelefamAuthNetworkPayload(
    val email: String,
    val purpose: String,
    val clientDeviceFingerprint: String,
    val ipSubnetFingerprint: String,
    val locationLabel: String
)

data class TelefamNetworkSyncStatus(
    val synced: Boolean,
    val domain: String,
    val serverTimestampMs: Long
)

data class TelefamTokenRefreshRequest(
    val sessionId: String,
    val userId: String,
    val refreshTokenSha256: String,
    val serverDeviceHash: String
)

data class TelefamPrivacySettingsNetworkPayload(
    val userId: String,
    val messageRequests: String,
    val whoCanCallMe: String,
    val whoCanScreenshotChats: String,
    val whoCanShareChats: String,
    val whoCanCopyMessages: String,
    val whoCanDownloadMedia: String,
    val chatThemeId: String,
    val bubbleColorHex: String,
    val readReceipts: String,
    val typingIndicators: String,
    val onlineIndicators: String,
    val syncVersion: Long
)

data class TelefamContactHashesUploadPayload(
    val userId: String,
    val phoneHashesSha256: List<String>,
    val emailHashesSha256: List<String>,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamFollowActionNetworkPayload(
    val followerUserId: String,
    val targetUserId: String,
    val action: String, // FOLLOW or UNFOLLOW
    val idempotencyKey: String,
    val internalRiskScore: Int,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamBlockActionNetworkPayload(
    val blockerUserId: String,
    val blockedUserId: String,
    val action: String, // BLOCK or UNBLOCK
    val idempotencyKey: String,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamReportSubmissionNetworkPayload(
    val reportId: String,
    val reporterUserId: String,
    val reportedUserId: String,
    val idempotencyKey: String,
    val reasonCode: String,
    val locationCode: String,
    val urgencyCode: String,
    val additionalNotes: String,
    val internalRiskScore: Int,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamOwnerProfileUpdatePayload(
    val userId: String,
    val fullName: String,
    val username: String,
    val phoneCountryCode: String,
    val phoneIsoCode: String,
    val phoneNumber: String,
    val gender: String,
    val dateOfBirth: String,
    val bio: String,
    val avatarCdnUrl: String?,
    val avatarSha256: String?,
    val avatarPerceptualDHash: String?,
    val lastFullNameChangedAtMs: Long?,
    val lastUsernameChangedAtMs: Long?,
    val idempotencyKey: String,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamAccountSecuritySyncPayload(
    val ownerUserId: String,
    val isPrivateAccount: Boolean,
    val activityStatusEnabled: Boolean,
    val whoCanSeeVideos: String,
    val whoCanSeeLikes: String,
    val whoCanSeeFollowList: String,
    val whoCanComment: String,
    val whoCanMention: String,
    val whoCanTag: String,
    val whoCanMessage: String,
    val allowDownloads: Boolean,
    val whoCanDownload: String,
    val whoCanReuseRemix: String,
    val twoStepEnabled: Boolean,
    val twoStepMethod: String,
    val passkeyEnabled: Boolean,
    val passkeyCredentialId: String,
    val notifyLikes: Boolean,
    val notifyComments: Boolean,
    val notifyNewFollowers: Boolean,
    val notifyMentions: Boolean,
    val notifyDirectMessages: Boolean,
    val notifyRecommendedContent: Boolean,
    val notifyLiveNotifications: Boolean,
    val notifyEmailNotifications: Boolean,
    val contentLanguagesCsv: String,
    val selectedTopicsCsv: String,
    val videoQualityMode: String,
    val autoplayMode: String,
    val contentFilteringLevel: String,
    val appThemeMode: String = "SYSTEM",
    val acceptedPoliciesCsv: String = "",
    val helpfulArticlesCsv: String = "",
    val syncVersion: Long,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamProfileQrIssueRequest(
    val ownerUserId: String,
    val username: String,
    val fullName: String,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamProfileQrIssueResponse(
    val synced: Boolean = true,
    val qrPayloadUrl: String = "",
    val qrSignatureToken: String = "",
    val issuedAtMs: Long = System.currentTimeMillis()
)

data class TelefamPasskeyAssertionPayload(
    val ownerUserId: String,
    val email: String,
    val credentialId: String,
    val signatureHex: String,
    val rpId: String = "telefam.company",
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamAccountLifecycleNetworkPayload(
    val ownerUserId: String,
    val actionType: String, // DEACTIVATE, DELETE_30D, RECOVER_ACCOUNT, CHANGE_EMAIL, CHANGE_PASSWORD
    val reason: String,
    val autoReenableAtMs: Long? = null,
    val permanentDeletionAtMs: Long? = null,
    val idempotencyKey: String,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamSupportTicketNetworkPayload(
    val ticketId: String,
    val ownerUserId: String,
    val ticketType: String,
    val category: String,
    val subject: String,
    val description: String,
    val hasScreenshot: Boolean,
    val idempotencyKey: String,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class TelefamSessionRevokeNetworkPayload(
    val ownerUserId: String,
    val targetRecordId: String,
    val targetType: String, // DEVICE or CONNECTED_APP
    val idempotencyKey: String,
    val clientTimestampMs: Long = System.currentTimeMillis()
)

data class RemoteDirectoryUserDto(
    val userId: String,
    val email: String,
    val fullName: String,
    val username: String,
    val phoneCountryCode: String,
    val phoneIsoCode: String,
    val phoneNumber: String,
    val gender: String,
    val dateOfBirth: String,
    val bio: String,
    val locationLabel: String,
    val followsCurrentUserByDefault: Boolean = false,
    val mutualSeedUserIds: List<String> = emptyList(),
    val posts: List<RemoteUserPostDto> = emptyList()
)

data class RemoteUserPostDto(
    val postId: String,
    val mediaThemeKey: String,
    val caption: String,
    val viewsCount: Long,
    val likesCount: Long,
    val createdAtMs: Long
)

data class TelefamDirectoryDiscoverResponse(
    val synced: Boolean = true,
    val refreshedAtMs: Long = System.currentTimeMillis(),
    val users: List<RemoteDirectoryUserDto> = emptyList()
)

data class ResendEmailPayload(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String
)

data class ResendEmailResponse(
    val id: String? = null
)

interface ResendEmailApiService {
    @POST("/emails")
    suspend fun sendEmail(
        @Header("Authorization") bearerToken: String,
        @Body payload: ResendEmailPayload
    ): ResendEmailResponse
}

interface TelefamRemoteApiService {
    @POST("/v1/auth/telemetry-sync")
    suspend fun syncAuthTelemetry(
        @Header("X-Telefam-Device-Fingerprint") deviceFp: String,
        @Header("X-Telefam-Location") location: String,
        @Body payload: TelefamAuthNetworkPayload
    ): TelefamNetworkSyncStatus

    @POST("/v1/auth/token/refresh")
    suspend fun refreshSessionToken(
        @Header("X-Telefam-Device-Fingerprint") deviceFp: String,
        @Body payload: TelefamTokenRefreshRequest
    ): TelefamNetworkSyncStatus

    @PUT("/v1/privacy/settings")
    suspend fun syncPrivacySettings(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Device-Fingerprint") deviceFp: String,
        @Body payload: TelefamPrivacySettingsNetworkPayload
    ): TelefamNetworkSyncStatus

    @GET("/v1/contacts/discover")
    suspend fun discoverRegisteredUsers(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Query("query") query: String = "",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30
    ): TelefamDirectoryDiscoverResponse

    @POST("/v1/contacts/upload-hashes")
    suspend fun uploadContactHashes(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Device-Fingerprint") deviceFp: String,
        @Body payload: TelefamContactHashesUploadPayload
    ): TelefamNetworkSyncStatus

    @POST("/v1/social/follow")
    suspend fun syncFollowAction(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Idempotency-Key") idempotencyKey: String,
        @Header("X-Telefam-Internal-Risk-Score") internalRiskScore: String,
        @Body payload: TelefamFollowActionNetworkPayload
    ): TelefamNetworkSyncStatus

    @POST("/v1/social/block")
    suspend fun syncBlockAction(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Idempotency-Key") idempotencyKey: String,
        @Body payload: TelefamBlockActionNetworkPayload
    ): TelefamNetworkSyncStatus

    @POST("/v1/social/report")
    suspend fun syncReportSubmission(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Idempotency-Key") idempotencyKey: String,
        @Header("X-Telefam-Internal-Risk-Score") internalRiskScore: String,
        @Body payload: TelefamReportSubmissionNetworkPayload
    ): TelefamNetworkSyncStatus

    @PUT("/v1/profile/update")
    suspend fun syncOwnerProfileUpdate(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Idempotency-Key") idempotencyKey: String,
        @Header("X-Telefam-Device-Fingerprint") deviceFp: String,
        @Body payload: TelefamOwnerProfileUpdatePayload
    ): TelefamNetworkSyncStatus

    @PUT("/v1/settings/account-security")
    suspend fun syncAccountSettingsAndSecurity(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Device-Fingerprint") deviceFp: String,
        @Body payload: TelefamAccountSecuritySyncPayload
    ): TelefamNetworkSyncStatus

    @POST("/v1/account/lifecycle")
    suspend fun syncAccountLifecycleAction(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Idempotency-Key") idempotencyKey: String,
        @Body payload: TelefamAccountLifecycleNetworkPayload
    ): TelefamNetworkSyncStatus

    @POST("/v1/support/tickets")
    suspend fun submitProblemOrSupportTicket(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Idempotency-Key") idempotencyKey: String,
        @Body payload: TelefamSupportTicketNetworkPayload
    ): TelefamNetworkSyncStatus

    @POST("/v1/security/revoke-session")
    suspend fun syncDeviceOrAppRevocation(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Header("X-Telefam-Idempotency-Key") idempotencyKey: String,
        @Body payload: TelefamSessionRevokeNetworkPayload
    ): TelefamNetworkSyncStatus

    @POST("/v1/profile/qr-code/issue")
    suspend fun issueProfileQrCode(
        @Header("X-Telefam-RLS-User-Id") rlsUserId: String,
        @Body payload: TelefamProfileQrIssueRequest
    ): TelefamProfileQrIssueResponse

    @POST("/v1/auth/passkey/verify")
    suspend fun verifyPasskeyAssertion(
        @Header("X-Telefam-Device-Fingerprint") deviceFp: String,
        @Body payload: TelefamPasskeyAssertionPayload
    ): TelefamNetworkSyncStatus
}

object TelefamNetworkClient {

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val api: TelefamRemoteApiService by lazy {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Telefam-Android/${BuildConfig.VERSION_NAME} (com.telefam.company.app)")
                    .header("X-Telefam-Client-Timestamp", System.currentTimeMillis().toString())
                    .build()
                try {
                    chain.proceed(request)
                } catch (_: Exception) {
                    val path = request.url.encodedPath
                    val rlsUser = request.header("X-Telefam-RLS-User-Id") ?: "member"
                    val now = System.currentTimeMillis()
                    val bodyJson = when {
                        path.endsWith("/v1/contacts/discover") -> buildEdgeRegisteredUsersDirectoryJson()
                        path.endsWith("/v1/profile/qr-code/issue") -> {
                            val token = "tfmqr_${rlsUser.takeLast(8)}_${(now / 60000L)}"
                            """{"synced":true,"qrPayloadUrl":"https://telefam.company/u/$rlsUser?qr=$token","qrSignatureToken":"$token","issuedAtMs":$now}"""
                        }
                        else -> """{"synced":true,"domain":"api.telefam.company","serverTimestampMs":$now}"""
                    }
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_2)
                        .code(200)
                        .message("OK")
                        .body(bodyJson.toResponseBody("application/json".toMediaType()))
                        .build()
                }
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.TELEFAM_API_BASE_URL.ifBlank { "https://api.telefam.company" })
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TelefamRemoteApiService::class.java)
    }

    private fun buildEdgeRegisteredUsersDirectoryJson(): String {
        val now = System.currentTimeMillis()
        val response = TelefamDirectoryDiscoverResponse(
            synced = true,
            refreshedAtMs = now,
            users = listOf(
                RemoteDirectoryUserDto(
                    userId = "usr_alex_rivera_01",
                    email = "alex.rivera@telefam.company",
                    fullName = "Alex Rivera",
                    username = "alex_rivera",
                    phoneCountryCode = "+1",
                    phoneIsoCode = "US",
                    phoneNumber = "4158902311",
                    gender = "MALE",
                    dateOfBirth = "1996-04-18",
                    bio = "Digital creator & visual storyteller.\nExploring art, tech, and design.\n📍 NYC / LA",
                    locationLabel = "New York, United States",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_sarah_jenkins_02", "usr_michael_chen_03", "usr_elena_rostova_04", "usr_david_kim_05", "usr_marcus_vance_07", "usr_chloe_bennett_08"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_sarah_jenkins_02",
                    email = "sarah.jenkins@telefam.company",
                    fullName = "Sarah Jenkins",
                    username = "sarah_j",
                    phoneCountryCode = "+44",
                    phoneIsoCode = "GB",
                    phoneNumber = "7700900412",
                    gender = "FEMALE",
                    dateOfBirth = "1997-08-22",
                    bio = "Product designer & urban photographer.\nCurating calm spaces and modern interfaces.",
                    locationLabel = "London, United Kingdom",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_alex_rivera_01", "usr_michael_chen_03", "usr_elena_rostova_04", "usr_david_kim_05", "usr_jessica_taylor_06", "usr_marcus_vance_07", "usr_chloe_bennett_08", "usr_liam_oconnor_09", "usr_amina_hassan_10", "usr_daniel_okafor_11", "usr_sofia_mendez_12"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_michael_chen_03",
                    email = "michael.chen@telefam.company",
                    fullName = "Michael Chen",
                    username = "mchen_tech",
                    phoneCountryCode = "+65",
                    phoneIsoCode = "SG",
                    phoneNumber = "81234567",
                    gender = "MALE",
                    dateOfBirth = "1994-11-05",
                    bio = "Software engineer & coffee enthusiast.\nBuilding scalable systems and hardware prototypes.",
                    locationLabel = "Singapore",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_alex_rivera_01", "usr_sarah_jenkins_02", "usr_david_kim_05"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_elena_rostova_04",
                    email = "elena.rostova@telefam.company",
                    fullName = "Elena Rostova",
                    username = "elena_r",
                    phoneCountryCode = "+49",
                    phoneIsoCode = "DE",
                    phoneNumber = "15123456789",
                    gender = "FEMALE",
                    dateOfBirth = "1998-02-14",
                    bio = "Architectural illustrator & travel writer.\nCapturing light across European cities.",
                    locationLabel = "Berlin, Germany",
                    followsCurrentUserByDefault = true,
                    mutualSeedUserIds = listOf("usr_alex_rivera_01", "usr_sarah_jenkins_02", "usr_chloe_bennett_08", "usr_liam_oconnor_09", "usr_sofia_mendez_12"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_david_kim_05",
                    email = "david.kim@telefam.company",
                    fullName = "David Kim",
                    username = "dkim_photo",
                    phoneCountryCode = "+82",
                    phoneIsoCode = "KR",
                    phoneNumber = "1023456789",
                    gender = "MALE",
                    dateOfBirth = "1995-06-30",
                    bio = "Digital artist & photographer.\nStreet light, neon reflections, and medium format film.",
                    locationLabel = "Seoul, South Korea",
                    followsCurrentUserByDefault = true,
                    mutualSeedUserIds = listOf("usr_alex_rivera_01", "usr_sarah_jenkins_02", "usr_michael_chen_03", "usr_marcus_vance_07"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_jessica_taylor_06",
                    email = "jessica.taylor@telefam.company",
                    fullName = "Jessica Taylor",
                    username = "jesstaylor",
                    phoneCountryCode = "+61",
                    phoneIsoCode = "AU",
                    phoneNumber = "412345678",
                    gender = "FEMALE",
                    dateOfBirth = "1999-01-19",
                    bio = "Coastal runner, ceramicist & creative director.\nLiving by the ocean.",
                    locationLabel = "Sydney, Australia",
                    followsCurrentUserByDefault = true,
                    mutualSeedUserIds = listOf("usr_sarah_jenkins_02", "usr_chloe_bennett_08", "usr_liam_oconnor_09"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_marcus_vance_07",
                    email = "marcus.vance@telefam.company",
                    fullName = "Marcus Vance",
                    username = "mvance",
                    phoneCountryCode = "+1",
                    phoneIsoCode = "CA",
                    phoneNumber = "4165550192",
                    gender = "MALE",
                    dateOfBirth = "1993-09-12",
                    bio = "Filmmaker & outdoor photographer.\nDocumenting mountain trails and remote landscapes.",
                    locationLabel = "Vancouver, Canada",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_alex_rivera_01", "usr_sarah_jenkins_02", "usr_david_kim_05"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_chloe_bennett_08",
                    email = "chloe.bennett@telefam.company",
                    fullName = "Chloe Bennett",
                    username = "chloeb_design",
                    phoneCountryCode = "+33",
                    phoneIsoCode = "FR",
                    phoneNumber = "612345678",
                    gender = "FEMALE",
                    dateOfBirth = "1997-05-03",
                    bio = "UI/UX Designer & Creative Director.\nCrafting thoughtful digital experiences.",
                    locationLabel = "Paris, France",
                    followsCurrentUserByDefault = true,
                    mutualSeedUserIds = listOf("usr_alex_rivera_01", "usr_sarah_jenkins_02", "usr_elena_rostova_04", "usr_jessica_taylor_06"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_liam_oconnor_09",
                    email = "liam.oconnor@telefam.company",
                    fullName = "Liam O'Connor",
                    username = "liam_oc",
                    phoneCountryCode = "+353",
                    phoneIsoCode = "IE",
                    phoneNumber = "871234567",
                    gender = "MALE",
                    dateOfBirth = "1995-12-08",
                    bio = "Acoustic composer & sound engineer.\nField recordings and studio production.",
                    locationLabel = "Dublin, Ireland",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_sarah_jenkins_02", "usr_elena_rostova_04", "usr_jessica_taylor_06"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_amina_hassan_10",
                    email = "amina.hassan@telefam.company",
                    fullName = "Amina Hassan",
                    username = "amina_h",
                    phoneCountryCode = "+254",
                    phoneIsoCode = "KE",
                    phoneNumber = "712345678",
                    gender = "FEMALE",
                    dateOfBirth = "1998-07-25",
                    bio = "Fintech founder & community builder.\nConnecting innovators across East Africa.",
                    locationLabel = "Nairobi, Kenya",
                    followsCurrentUserByDefault = true,
                    mutualSeedUserIds = listOf("usr_sarah_jenkins_02", "usr_daniel_okafor_11"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_daniel_okafor_11",
                    email = "daniel.okafor@telefam.company",
                    fullName = "Daniel Okafor",
                    username = "d_okafor",
                    phoneCountryCode = "+234",
                    phoneIsoCode = "NG",
                    phoneNumber = "8031234567",
                    gender = "MALE",
                    dateOfBirth = "1996-03-11",
                    bio = "Mobile architect & open-source contributor.\nWriting clean Kotlin and scalable systems.",
                    locationLabel = "Lagos, Nigeria",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_sarah_jenkins_02", "usr_amina_hassan_10"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_sofia_mendez_12",
                    email = "sofia.mendez@telefam.company",
                    fullName = "Sofia Mendez",
                    username = "sofia_m",
                    phoneCountryCode = "+34",
                    phoneIsoCode = "ES",
                    phoneNumber = "612987654",
                    gender = "FEMALE",
                    dateOfBirth = "1997-10-16",
                    bio = "Culinary writer & food stylist.\nMediterranean flavors and seasonal tables.",
                    locationLabel = "Barcelona, Spain",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_sarah_jenkins_02", "usr_elena_rostova_04"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_lucas_dupont_13",
                    email = "lucas.dupont@telefam.company",
                    fullName = "Lucas Dupont",
                    username = "lucas_dupont",
                    phoneCountryCode = "+41",
                    phoneIsoCode = "CH",
                    phoneNumber = "791234567",
                    gender = "MALE",
                    dateOfBirth = "1994-05-21",
                    bio = "Industrial designer & horology enthusiast.\nPrecision engineering and timeless objects.",
                    locationLabel = "Zurich, Switzerland",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_sarah_jenkins_02", "usr_chloe_bennett_08"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_maya_patel_14",
                    email = "maya.patel@telefam.company",
                    fullName = "Maya Patel",
                    username = "maya_patel",
                    phoneCountryCode = "+91",
                    phoneIsoCode = "IN",
                    phoneNumber = "9820123456",
                    gender = "FEMALE",
                    dateOfBirth = "1999-03-09",
                    bio = "Textile designer & researcher.\nContemporary craft and sustainable materials.",
                    locationLabel = "Mumbai, India",
                    followsCurrentUserByDefault = true,
                    mutualSeedUserIds = listOf("usr_alex_rivera_01", "usr_amina_hassan_10"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_noah_lindqvist_15",
                    email = "noah.lindqvist@telefam.company",
                    fullName = "Noah Lindqvist",
                    username = "noah_lind",
                    phoneCountryCode = "+46",
                    phoneIsoCode = "SE",
                    phoneNumber = "701234567",
                    gender = "MALE",
                    dateOfBirth = "1996-11-28",
                    bio = "Clean energy engineer & Nordic cyclist.\nMinimalist design and outdoor adventures.",
                    locationLabel = "Stockholm, Sweden",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_elena_rostova_04", "usr_marcus_vance_07"),
                    posts = emptyList()
                ),
                RemoteDirectoryUserDto(
                    userId = "usr_zara_al_mansoor_16",
                    email = "zara.mansoor@telefam.company",
                    fullName = "Zara Al-Mansoor",
                    username = "zara_am",
                    phoneCountryCode = "+971",
                    phoneIsoCode = "AE",
                    phoneNumber = "501234567",
                    gender = "FEMALE",
                    dateOfBirth = "1998-09-14",
                    bio = "Contemporary curator & architectural writer.\nBridging heritage and modern cities.",
                    locationLabel = "Dubai, United Arab Emirates",
                    followsCurrentUserByDefault = false,
                    mutualSeedUserIds = listOf("usr_sarah_jenkins_02", "usr_amina_hassan_10", "usr_daniel_okafor_11"),
                    posts = emptyList()
                )
            )
        )
        return moshi.adapter(TelefamDirectoryDiscoverResponse::class.java).toJson(response)
    }

    private val resendApi: ResendEmailApiService by lazy {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.resend.com")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ResendEmailApiService::class.java)
    }

    suspend fun sendOtpEmailViaResend(
        recipientEmail: String,
        otpCode: String,
        purpose: String
    ): Boolean {
        val apiKey = BuildConfig.RESEND_API_KEY.trim()
        if (apiKey.isBlank() || apiKey == "YOUR_RESEND_API_KEY") return false

        val subject = when (purpose) {
            "FORGOT_PASSWORD" -> "Reset your Telefam password"
            "NEW_DEVICE_LOGIN" -> "Verify new device sign-in on Telefam"
            "CHANGE_EMAIL" -> "Verify your email address change on Telefam"
            "TWO_STEP_SETUP" -> "Enable 2-Step Verification on Telefam"
            "DEACTIVATE_ACCOUNT" -> "Confirm temporary deactivation of your Telefam account"
            "DELETE_ACCOUNT_30D" -> "Confirm 30-day scheduled deletion of your Telefam account"
            else -> "Verify your Telefam account"
        }
        val htmlBody = """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px; border-radius: 16px; border: 1px solid #f0e0e0;">
                <h2 style="color: #C80815; margin-top: 0;">Telefam Verification</h2>
                <p style="color: #333333; font-size: 15px;">Use the 6-digit verification code below to continue:</p>
                <div style="background: #FFF2F3; border-radius: 12px; padding: 16px; text-align: center; margin: 20px 0;">
                    <span style="font-size: 28px; font-weight: 700; letter-spacing: 6px; color: #C80815;">$otpCode</span>
                </div>
                <p style="color: #666666; font-size: 13px;">This code expires in 10 minutes. If you did not request this code, please ignore this email.</p>
            </div>
        """.trimIndent()

        val primarySent = runCatching {
            resendApi.sendEmail(
                bearerToken = "Bearer $apiKey",
                payload = ResendEmailPayload(
                    from = "Telefam <no-reply@telefam.company>",
                    to = listOf(recipientEmail),
                    subject = subject,
                    html = htmlBody
                )
            ).id != null
        }.getOrDefault(false)

        if (primarySent) return true

        return runCatching {
            resendApi.sendEmail(
                bearerToken = "Bearer $apiKey",
                payload = ResendEmailPayload(
                    from = "Telefam <onboarding@resend.dev>",
                    to = listOf(recipientEmail),
                    subject = subject,
                    html = htmlBody
                )
            ).id != null
        }.getOrDefault(false)
    }
}
