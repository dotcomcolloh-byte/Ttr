package com.telefam.company.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "telefam_users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "password_salt") val passwordSalt: String,
    @ColumnInfo(name = "google_sub_id") val googleSubId: String? = null,
    @ColumnInfo(name = "email_verified") val emailVerified: Boolean = false,
    @ColumnInfo(name = "failed_login_attempts") val failedLoginAttempts: Int = 0,
    @ColumnInfo(name = "login_locked_until_ms") val loginLockedUntilMs: Long? = null,
    @ColumnInfo(name = "failed_otp_attempts") val failedOtpAttempts: Int = 0,
    @ColumnInfo(name = "otp_locked_until_ms") val otpLockedUntilMs: Long? = null,
    @ColumnInfo(name = "last_known_location") val lastKnownLocation: String? = null,
    @ColumnInfo(name = "last_latitude") val lastLatitude: Double? = null,
    @ColumnInfo(name = "last_longitude") val lastLongitude: Double? = null,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_profiles",
    indices = [Index(value = ["username"], unique = true)]
)
data class ProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "phone_country_code") val phoneCountryCode: String,
    @ColumnInfo(name = "phone_iso_code") val phoneIsoCode: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "gender") val gender: String,
    @ColumnInfo(name = "date_of_birth") val dateOfBirth: String,
    @ColumnInfo(name = "bio") val bio: String,
    @ColumnInfo(name = "avatar_local_path") val avatarLocalPath: String? = null,
    @ColumnInfo(name = "avatar_cdn_url") val avatarCdnUrl: String? = null,
    @ColumnInfo(name = "avatar_sha256") val avatarSha256: String? = null,
    @ColumnInfo(name = "avatar_perceptual_dhash") val avatarPerceptualDHash: String? = null,
    @ColumnInfo(name = "avatar_mime_type") val avatarMimeType: String? = null,
    @ColumnInfo(name = "avatar_byte_size") val avatarByteSize: Int? = null,
    @ColumnInfo(name = "last_full_name_changed_at_ms") val lastFullNameChangedAtMs: Long? = null,
    @ColumnInfo(name = "last_username_changed_at_ms") val lastUsernameChangedAtMs: Long? = null,
    @ColumnInfo(name = "subscribers_count") val subscribersCount: Long = 0L,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = false,
    @ColumnInfo(name = "last_synced_at_ms") val lastSyncedAtMs: Long? = null,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_media_fingerprints",
    indices = [Index(value = ["perceptual_dhash"])]
)
data class MediaFingerprintEntity(
    @PrimaryKey
    @ColumnInfo(name = "sha256_checksum") val sha256Checksum: String,
    @ColumnInfo(name = "perceptual_dhash") val perceptualDHash: String,
    @ColumnInfo(name = "secure_file_path") val secureFilePath: String,
    @ColumnInfo(name = "cdn_storage_url") val cdnStorageUrl: String,
    @ColumnInfo(name = "verified_mime_type") val verifiedMimeType: String,
    @ColumnInfo(name = "original_byte_size") val originalByteSize: Int,
    @ColumnInfo(name = "compressed_byte_size") val compressedByteSize: Int,
    @ColumnInfo(name = "width_px") val widthPx: Int = 512,
    @ColumnInfo(name = "height_px") val heightPx: Int = 512,
    @ColumnInfo(name = "reuse_count") val reuseCount: Int = 1,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_reused_at_ms") val lastReusedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_trusted_devices",
    indices = [Index(value = ["user_id", "server_device_hash"], unique = true)]
)
data class TrustedDeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "device_record_id") val deviceRecordId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "untrusted_client_fp") val untrustedClientFp: String,
    @ColumnInfo(name = "server_device_hash") val serverDeviceHash: String,
    @ColumnInfo(name = "ip_subnet_fp") val ipSubnetFp: String,
    @ColumnInfo(name = "os_description") val osDescription: String,
    @ColumnInfo(name = "location_label") val locationLabel: String,
    @ColumnInfo(name = "is_trusted") val isTrusted: Boolean = true,
    @ColumnInfo(name = "last_seen_ms") val lastSeenMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_otp_challenges",
    indices = [Index(value = ["email", "purpose", "is_invalidated"])]
)
data class OtpChallengeEntity(
    @PrimaryKey
    @ColumnInfo(name = "challenge_id") val challengeId: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "purpose") val purpose: String,
    @ColumnInfo(name = "otp_hmac_digest") val otpHmacDigest: String,
    @ColumnInfo(name = "ip_subnet_fp") val ipSubnetFp: String,
    @ColumnInfo(name = "server_device_hash") val serverDeviceHash: String,
    @ColumnInfo(name = "verify_attempts") val verifyAttempts: Int = 0,
    @ColumnInfo(name = "is_invalidated") val isInvalidated: Boolean = false,
    @ColumnInfo(name = "resend_available_at_ms") val resendAvailableAtMs: Long,
    @ColumnInfo(name = "expires_at_ms") val expiresAtMs: Long,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_encrypted_sessions")
data class EncryptedSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "access_token_sha256") val accessTokenSha256: String,
    @ColumnInfo(name = "refresh_token_sha256") val refreshTokenSha256: String = "",
    @ColumnInfo(name = "encrypted_payload_base64") val encryptedPayloadBase64: String,
    @ColumnInfo(name = "iv_base64") val ivBase64: String,
    @ColumnInfo(name = "server_device_hash") val serverDeviceHash: String,
    @ColumnInfo(name = "ip_subnet_fp") val ipSubnetFp: String,
    @ColumnInfo(name = "location_label") val locationLabel: String,
    @ColumnInfo(name = "risk_score") val riskScore: Int,
    @ColumnInfo(name = "is_revoked") val isRevoked: Boolean = false,
    @ColumnInfo(name = "expires_at_ms") val expiresAtMs: Long,
    @ColumnInfo(name = "refresh_expires_at_ms") val refreshExpiresAtMs: Long = expiresAtMs + 30L * 24 * 60 * 60 * 1000L,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_rate_limits")
data class RateLimitBucketEntity(
    @PrimaryKey
    @ColumnInfo(name = "bucket_key") val bucketKey: String,
    @ColumnInfo(name = "action_type") val actionType: String,
    @ColumnInfo(name = "failed_count") val failedCount: Int = 0,
    @ColumnInfo(name = "cooldown_until_ms") val cooldownUntilMs: Long? = null,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_forensic_audit_logs")
data class ForensicAuditLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "audit_id") val auditId: String,
    @ColumnInfo(name = "timestamp_ms") val timestampMs: Long,
    @ColumnInfo(name = "event_category") val eventCategory: String,
    @ColumnInfo(name = "event_outcome") val eventOutcome: String,
    @ColumnInfo(name = "target_identifier") val targetIdentifier: String,
    @ColumnInfo(name = "client_ip") val clientIp: String,
    @ColumnInfo(name = "ip_subnet_fp") val ipSubnetFp: String,
    @ColumnInfo(name = "untrusted_client_fp") val untrustedClientFp: String,
    @ColumnInfo(name = "server_device_hash") val serverDeviceHash: String,
    @ColumnInfo(name = "location_label") val locationLabel: String,
    @ColumnInfo(name = "risk_score") val riskScore: Int,
    @ColumnInfo(name = "risk_signals_csv") val riskSignalsCsv: String,
    @ColumnInfo(name = "endpoint_path") val endpointPath: String,
    @ColumnInfo(name = "forensic_detail") val forensicDetail: String
)

@Entity(tableName = "telefam_user_privacy_settings")
data class PrivacySettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "message_requests") val messageRequests: String = "ANYONE",
    @ColumnInfo(name = "who_can_call_me") val whoCanCallMe: String = "ANYONE",
    @ColumnInfo(name = "who_can_screenshot_chats") val whoCanScreenshotChats: String = "ANYONE",
    @ColumnInfo(name = "who_can_share_chats") val whoCanShareChats: String = "ANYONE",
    @ColumnInfo(name = "who_can_copy_messages") val whoCanCopyMessages: String = "ANYONE",
    @ColumnInfo(name = "who_can_download_media") val whoCanDownloadMedia: String = "ANYONE",
    @ColumnInfo(name = "chat_theme_id") val chatThemeId: String = "crimson_silk",
    @ColumnInfo(name = "bubble_color_hex") val bubbleColorHex: String = "#C80815",
    @ColumnInfo(name = "read_receipts") val readReceipts: String = "ANYONE",
    @ColumnInfo(name = "typing_indicators") val typingIndicators: String = "ANYONE",
    @ColumnInfo(name = "online_indicators") val onlineIndicators: String = "ANYONE",
    @ColumnInfo(name = "sync_version") val syncVersion: Long = 1L,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = false,
    @ColumnInfo(name = "sync_retry_count") val syncRetryCount: Int = 0,
    @ColumnInfo(name = "last_synced_at_ms") val lastSyncedAtMs: Long? = null,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_follow_edges",
    primaryKeys = ["follower_user_id", "target_user_id"],
    indices = [
        Index(value = ["follower_user_id"]),
        Index(value = ["target_user_id"]),
        Index(value = ["idempotency_key"], unique = true)
    ]
)
data class FollowEdgeEntity(
    @ColumnInfo(name = "follower_user_id") val followerUserId: String,
    @ColumnInfo(name = "target_user_id") val targetUserId: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "internal_risk_score") val internalRiskScore: Int = 0,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = false,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_contact_hashes",
    primaryKeys = ["owner_user_id", "contact_hash_sha256"],
    indices = [
        Index(value = ["owner_user_id"]),
        Index(value = ["contact_hash_sha256"])
    ]
)
data class ContactHashEntity(
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "contact_hash_sha256") val contactHashSha256: String,
    @ColumnInfo(name = "hash_type") val hashType: String, // PHONE or EMAIL
    @ColumnInfo(name = "matched_user_id") val matchedUserId: String? = null,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = true,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_user_blocks",
    primaryKeys = ["blocker_user_id", "blocked_user_id"],
    indices = [
        Index(value = ["blocker_user_id"]),
        Index(value = ["blocked_user_id"]),
        Index(value = ["idempotency_key"], unique = true)
    ]
)
data class UserBlockEntity(
    @ColumnInfo(name = "blocker_user_id") val blockerUserId: String,
    @ColumnInfo(name = "blocked_user_id") val blockedUserId: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = true,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_user_reports",
    indices = [
        Index(value = ["reporter_user_id"]),
        Index(value = ["reported_user_id"]),
        Index(value = ["idempotency_key"], unique = true)
    ]
)
data class UserReportEntity(
    @PrimaryKey
    @ColumnInfo(name = "report_id") val reportId: String,
    @ColumnInfo(name = "reporter_user_id") val reporterUserId: String,
    @ColumnInfo(name = "reported_user_id") val reportedUserId: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "reason_code") val reasonCode: String,
    @ColumnInfo(name = "location_code") val locationCode: String,
    @ColumnInfo(name = "urgency_code") val urgencyCode: String,
    @ColumnInfo(name = "additional_notes") val additionalNotes: String = "",
    @ColumnInfo(name = "internal_risk_score") val internalRiskScore: Int = 0,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = true,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_user_posts",
    indices = [Index(value = ["user_id", "created_at_ms"])]
)
data class UserPostEntity(
    @PrimaryKey
    @ColumnInfo(name = "post_id") val postId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "media_theme_key") val mediaThemeKey: String,
    @ColumnInfo(name = "caption") val caption: String,
    @ColumnInfo(name = "duration_label") val durationLabel: String = "0:42",
    @ColumnInfo(name = "post_category") val postCategory: String = "POSTS",
    @ColumnInfo(name = "views_count") val viewsCount: Long,
    @ColumnInfo(name = "likes_count") val likesCount: Long,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)

@Entity(
    tableName = "telefam_user_subscriptions",
    primaryKeys = ["subscriber_user_id", "creator_user_id"],
    indices = [
        Index(value = ["subscriber_user_id"]),
        Index(value = ["creator_user_id"]),
        Index(value = ["idempotency_key"], unique = true)
    ]
)
data class UserSubscriptionEntity(
    @ColumnInfo(name = "subscriber_user_id") val subscriberUserId: String,
    @ColumnInfo(name = "creator_user_id") val creatorUserId: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = false,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_social_sync_queue",
    indices = [Index(value = ["idempotency_key"], unique = true)]
)
data class SocialSyncQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "Operation_id") val operationId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "operation_type") val operationType: String, // FOLLOW, UNFOLLOW, UPLOAD_CONTACTS, BLOCK, UNBLOCK, REPORT, SETTINGS_SYNC, ACCOUNT_STATUS, TICKET_SUBMIT, DEVICE_LOGOUT, PASSKEY_UPDATE
    @ColumnInfo(name = "target_user_id") val targetUserId: String = "",
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "internal_risk_score") val internalRiskScore: Int = 0,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_account_security_settings",
    indices = [Index(value = ["owner_user_id"], unique = true)]
)
data class AccountSettingsAndSecurityEntity(
    @PrimaryKey
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    // Account Recovery & Status
    @ColumnInfo(name = "recovery_email") val recoveryEmail: String = "",
    @ColumnInfo(name = "recovery_phone") val recoveryPhone: String = "",
    @ColumnInfo(name = "backup_recovery_codes_csv") val backupRecoveryCodesCsv: String = "",
    @ColumnInfo(name = "account_status") val accountStatus: String = "ACTIVE", // ACTIVE, DEACTIVATED, PENDING_DELETION_30D
    @ColumnInfo(name = "deactivated_at_ms") val deactivatedAtMs: Long? = null,
    @ColumnInfo(name = "auto_reenable_at_ms") val autoReenableAtMs: Long? = null,
    @ColumnInfo(name = "deactivation_reason") val deactivationReason: String = "",
    @ColumnInfo(name = "deletion_requested_at_ms") val deletionRequestedAtMs: Long? = null,
    @ColumnInfo(name = "permanent_deletion_at_ms") val permanentDeletionAtMs: Long? = null,
    @ColumnInfo(name = "deletion_reason") val deletionReason: String = "",
    // Two-Step Verification & Passkey (FIDO2 / WebAuthn Protocol)
    @ColumnInfo(name = "two_step_enabled") val twoStepEnabled: Boolean = false,
    @ColumnInfo(name = "two_step_method") val twoStepMethod: String = "EMAIL", // EMAIL or AUTHENTICATOR_TOTP
    @ColumnInfo(name = "authenticator_secret_base32") val authenticatorSecretBase32: String = "",
    @ColumnInfo(name = "passkey_enabled") val passkeyEnabled: Boolean = false,
    @ColumnInfo(name = "passkey_credential_id") val passkeyCredentialId: String = "",
    @ColumnInfo(name = "passkey_public_key_cose_hex") val passkeyPublicKeyCoseHex: String = "",
    @ColumnInfo(name = "passkey_rp_id") val passkeyRpId: String = "telefam.company",
    @ColumnInfo(name = "passkey_device_label") val passkeyDeviceLabel: String = "",
    @ColumnInfo(name = "passkey_registered_at_ms") val passkeyRegisteredAtMs: Long? = null,
    // Account Privacy & Detailed Privacy Controls
    @ColumnInfo(name = "is_private_account") val isPrivateAccount: Boolean = false,
    @ColumnInfo(name = "activity_status_enabled") val activityStatusEnabled: Boolean = true,
    @ColumnInfo(name = "who_can_see_videos") val whoCanSeeVideos: String = "EVERYONE",
    @ColumnInfo(name = "who_can_see_likes") val whoCanSeeLikes: String = "EVERYONE",
    @ColumnInfo(name = "who_can_see_follow_list") val whoCanSeeFollowList: String = "EVERYONE",
    @ColumnInfo(name = "who_can_comment") val whoCanComment: String = "EVERYONE",
    @ColumnInfo(name = "who_can_mention") val whoCanMention: String = "EVERYONE",
    @ColumnInfo(name = "who_can_tag") val whoCanTag: String = "EVERYONE",
    @ColumnInfo(name = "who_can_message") val whoCanMessage: String = "EVERYONE",
    @ColumnInfo(name = "allow_downloads") val allowDownloads: Boolean = true,
    @ColumnInfo(name = "who_can_download") val whoCanDownload: String = "EVERYONE",
    @ColumnInfo(name = "who_can_reuse_remix") val whoCanReuseRemix: String = "EVERYONE",
    // System & App Permissions State
    @ColumnInfo(name = "permission_contacts_allowed") val permissionContactsAllowed: Boolean = false,
    @ColumnInfo(name = "permission_camera_allowed") val permissionCameraAllowed: Boolean = false,
    @ColumnInfo(name = "permission_microphone_allowed") val permissionMicrophoneAllowed: Boolean = false,
    @ColumnInfo(name = "permission_photos_allowed") val permissionPhotosAllowed: Boolean = true,
    // Notifications Controls
    @ColumnInfo(name = "notify_likes") val notifyLikes: Boolean = true,
    @ColumnInfo(name = "notify_comments") val notifyComments: Boolean = true,
    @ColumnInfo(name = "notify_new_followers") val notifyNewFollowers: Boolean = true,
    @ColumnInfo(name = "notify_mentions") val notifyMentions: Boolean = true,
    @ColumnInfo(name = "notify_direct_messages") val notifyDirectMessages: Boolean = true,
    @ColumnInfo(name = "notify_recommended_content") val notifyRecommendedContent: Boolean = false,
    @ColumnInfo(name = "notify_live_notifications") val notifyLiveNotifications: Boolean = true,
    @ColumnInfo(name = "notify_email_notifications") val notifyEmailNotifications: Boolean = true,
    @ColumnInfo(name = "push_master_enabled") val pushMasterEnabled: Boolean = true,
    @ColumnInfo(name = "push_quiet_hours_enabled") val pushQuietHoursEnabled: Boolean = false,
    @ColumnInfo(name = "email_security_alerts") val emailSecurityAlerts: Boolean = true,
    @ColumnInfo(name = "email_product_updates") val emailProductUpdates: Boolean = false,
    @ColumnInfo(name = "sms_security_alerts") val smsSecurityAlerts: Boolean = true,
    // Content Preferences
    @ColumnInfo(name = "content_languages_csv") val contentLanguagesCsv: String = "English,Swahili",
    @ColumnInfo(name = "selected_topics_csv") val selectedTopicsCsv: String = "Technology,Community,Education,Music",
    @ColumnInfo(name = "suggested_prioritize_following") val suggestedPrioritizeFollowing: Boolean = true,
    @ColumnInfo(name = "suggested_local_region_boost") val suggestedLocalRegionBoost: Boolean = true,
    @ColumnInfo(name = "video_quality_mode") val videoQualityMode: String = "AUTO", // AUTO, HIGH_1080P, DATA_SAVER
    @ColumnInfo(name = "autoplay_mode") val autoplayMode: String = "WIFI_AND_MOBILE", // WIFI_AND_MOBILE, WIFI_ONLY, NEVER
    @ColumnInfo(name = "content_filtering_level") val contentFilteringLevel: String = "STANDARD", // LESS_SENSITIVE, STANDARD, MORE_SENSITIVE
    // Appearance, Backend QR & Policy/Help Acknowledgements
    @ColumnInfo(name = "app_theme_mode") val appThemeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    @ColumnInfo(name = "backend_qr_payload_token") val backendQrPayloadToken: String = "",
    @ColumnInfo(name = "accepted_policies_csv") val acceptedPoliciesCsv: String = "",
    @ColumnInfo(name = "helpful_articles_csv") val helpfulArticlesCsv: String = "",
    // RLS & Offline Sync Tracking
    @ColumnInfo(name = "sync_version") val syncVersion: Long = 1L,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = false,
    @ColumnInfo(name = "last_synced_at_ms") val lastSyncedAtMs: Long? = null,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_connected_apps",
    indices = [Index(value = ["owner_user_id"])]
)
data class ConnectedAppSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "connection_id") val connectionId: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "developer_domain") val developerDomain: String,
    @ColumnInfo(name = "scopes_granted_csv") val scopesGrantedCsv: String,
    @ColumnInfo(name = "connected_at_ms") val connectedAtMs: Long,
    @ColumnInfo(name = "last_used_at_ms") val lastUsedAtMs: Long,
    @ColumnInfo(name = "is_revoked") val isRevoked: Boolean = false,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = false
)

@Entity(
    tableName = "telefam_problem_support_tickets",
    indices = [
        Index(value = ["owner_user_id"]),
        Index(value = ["idempotency_key"], unique = true)
    ]
)
data class ProblemSupportTicketEntity(
    @PrimaryKey
    @ColumnInfo(name = "ticket_id") val ticketId: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "ticket_type") val ticketType: String, // PROBLEM_REPORT or SUPPORT_REQUEST
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "subject") val subject: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "screenshot_local_path") val screenshotLocalPath: String? = null,
    @ColumnInfo(name = "status") val status: String = "SUBMITTED",
    @ColumnInfo(name = "support_response_note") val supportResponseNote: String = "Your request has been received by Telefam Support. You can add additional details or mark this request resolved at any time.",
    @ColumnInfo(name = "user_follow_up_reply") val userFollowUpReply: String = "",
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "is_pending_sync") val isPendingSync: Boolean = true,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_quick_login_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class QuickLoginAccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "avatar_local_path") val avatarLocalPath: String? = null,
    @ColumnInfo(name = "account_status") val accountStatus: String = "ACTIVE",
    @ColumnInfo(name = "auto_reenable_at_ms") val autoReenableAtMs: Long? = null,
    @ColumnInfo(name = "permanent_deletion_at_ms") val permanentDeletionAtMs: Long? = null,
    @ColumnInfo(name = "last_logged_out_at_ms") val lastLoggedOutAtMs: Long = System.currentTimeMillis()
)

// =============================================================================
// 1:1 E2EE CHATS, MESSAGES, REACTIONS, PINS, FAVOURITES, DISAPPEARING, RECEIPTS,
// SIGNAL X3DH + DOUBLE RATCHET KEYS/SESSIONS, OUTBOX & KLIPY CACHE (OFFLINE-FIRST)
// =============================================================================

@Entity(
    tableName = "telefam_chats",
    indices = [
        Index(value = ["owner_user_id", "peer_user_id"], unique = true),
        Index(value = ["owner_user_id", "last_message_at_ms"])
    ]
)
data class ChatEntity(
    @PrimaryKey
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "peer_user_id") val peerUserId: String,
    @ColumnInfo(name = "peer_full_name") val peerFullName: String,
    @ColumnInfo(name = "peer_username") val peerUsername: String,
    @ColumnInfo(name = "peer_avatar_url") val peerAvatarUrl: String? = null,
    @ColumnInfo(name = "peer_bio") val peerBio: String = "",
    @ColumnInfo(name = "is_peer_online") val peerIsOnline: Boolean = false,
    @ColumnInfo(name = "peer_last_seen_ms") val peerLastSeenMs: Long? = null,
    @ColumnInfo(name = "is_peer_typing") val peerIsTyping: Boolean = false,
    @ColumnInfo(name = "is_peer_account_deleted") val peerIsDeleted: Boolean = false,
    @ColumnInfo(name = "is_blocked_by_me") val isBlockedByMe: Boolean = false,
    @ColumnInfo(name = "is_blocked_by_peer") val isBlockedByPeer: Boolean = false,
    @ColumnInfo(name = "last_message_id") val lastMessageId: String? = null,
    @ColumnInfo(name = "last_message_type") val lastMessageType: String = "TEXT", // TEXT, STICKER, GIF, CLIP, VOICE_NOTE
    @ColumnInfo(name = "last_message_preview") val lastMessagePreview: String = "",
    @ColumnInfo(name = "last_message_sender_id") val lastMessageSenderId: String = "",
    @ColumnInfo(name = "last_message_status") val lastMessageDeliveryStatus: String = "SENT", // SENDING, SENT, DELIVERED, SEEN, REJECTED_BLOCKED, REJECTED_USER_NOT_FOUND
    @ColumnInfo(name = "last_message_media_icon_url") val lastMessageMediaIconUrl: String? = null,
    @ColumnInfo(name = "last_message_at_ms") val lastMessageTimestampMs: Long = 0L,
    @ColumnInfo(name = "unread_count") val unreadCount: Int = 0,
    @ColumnInfo(name = "disappearing_duration_seconds") val disappearingDurationSeconds: Long = 0L, // 0=Off, 86400=24h, 604800=7d, 2592000=1m
    @ColumnInfo(name = "is_e2ee_verified") val e2eeSafetyNumberVerified: Boolean = false,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_chat_messages",
    indices = [
        Index(value = ["chat_id", "created_at_ms"]),
        Index(value = ["owner_user_id", "chat_id"]),
        Index(value = ["reply_to_message_id"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "sender_user_id") val senderUserId: String,
    @ColumnInfo(name = "recipient_user_id") val recipientUserId: String,
    @ColumnInfo(name = "message_type") val messageType: String, // TEXT, STICKER, GIF, CLIP, VOICE_NOTE
    // Decrypted local fields in Room (encrypted on wire; server stores ciphertext only)
    @ColumnInfo(name = "decrypted_text") val decryptedText: String = "",
    @ColumnInfo(name = "media_id") val mediaId: String? = null,
    @ColumnInfo(name = "media_url") val mediaUrl: String? = null,
    @ColumnInfo(name = "media_preview_url") val mediaPreviewUrl: String? = null,
    @ColumnInfo(name = "media_local_path") val mediaLocalPath: String? = null,
    @ColumnInfo(name = "media_title") val mediaTitle: String = "",
    @ColumnInfo(name = "media_mime_type") val mediaMimeType: String = "",
    @ColumnInfo(name = "voice_duration_ms") val voiceDurationMs: Long = 0L,
    @ColumnInfo(name = "voice_waveform_csv") val voiceWaveformCsv: String = "",
    @ColumnInfo(name = "is_view_once") val isViewOnce: Boolean = false,
    @ColumnInfo(name = "is_view_once_consumed") val isViewOnceConsumed: Boolean = false,
    // Reply & Forward metadata
    @ColumnInfo(name = "reply_to_message_id") val replyToMessageId: String? = null,
    @ColumnInfo(name = "reply_to_sender_name") val replyToSenderName: String? = null,
    @ColumnInfo(name = "reply_to_preview_text") val replyToPreviewText: String? = null,
    @ColumnInfo(name = "reply_to_message_type") val replyToMessageType: String? = null,
    @ColumnInfo(name = "is_forwarded") val isForwarded: Boolean = false,
    @ColumnInfo(name = "is_edited") val isEdited: Boolean = false,
    @ColumnInfo(name = "edited_at_ms") val editedAtMs: Long? = null,
    // E2EE Signal Double Ratchet Ciphertext Envelope (stored on server)
    @ColumnInfo(name = "ciphertext_base64") val ciphertextBase64: String,
    @ColumnInfo(name = "ratchet_header_json") val ratchetHeaderJson: String,
    // Delivery & Lifecycle state
    @ColumnInfo(name = "delivery_status") val deliveryStatus: String = "SENDING", // SENDING, SENT, DELIVERED, SEEN, REJECTED_BLOCKED, REJECTED_USER_NOT_FOUND
    @ColumnInfo(name = "delivered_at_ms") val deliveredAtMs: Long? = null,
    @ColumnInfo(name = "seen_at_ms") val seenAtMs: Long? = null,
    @ColumnInfo(name = "expires_at_ms") val expiresAtMs: Long? = null,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_message_reactions",
    primaryKeys = ["message_id", "reactor_user_id"],
    indices = [
        Index(value = ["chat_id"]),
        Index(value = ["message_id"])
    ]
)
data class MessageReactionEntity(
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "reactor_user_id") val reactorUserId: String,
    @ColumnInfo(name = "reaction_key") val reactionKey: String, // HEART, THUMBS_UP, FIRE, LAUGH, SURPRISED, PRAY
    @ColumnInfo(name = "encrypted_reaction_base64") val encryptedReactionBase64: String = "",
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_chat_pins",
    primaryKeys = ["chat_id", "message_id"],
    indices = [Index(value = ["chat_id", "pinned_at_ms"])]
)
data class ChatPinEntity(
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "pinned_by_user_id") val pinnedByUserId: String,
    @ColumnInfo(name = "pinned_at_ms") val pinnedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_chat_favourites",
    primaryKeys = ["owner_user_id", "message_id"],
    indices = [Index(value = ["owner_user_id", "chat_id"])]
)
data class ChatFavouriteEntity(
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "favourited_at_ms") val favouritedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_disappearing_settings")
data class DisappearingSettingEntity(
    @PrimaryKey
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long = 0L, // 0, 86400, 604800, 2592000
    @ColumnInfo(name = "updated_by_user_id") val updatedByUserId: String,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_delivery_receipts",
    primaryKeys = ["message_id", "recipient_user_id"],
    indices = [Index(value = ["chat_id"])]
)
data class DeliveryReceiptEntity(
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "recipient_user_id") val recipientUserId: String,
    @ColumnInfo(name = "receipt_type") val receiptType: String, // DELIVERED or SEEN
    @ColumnInfo(name = "timestamp_ms") val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_chat_drafts")
data class ChatDraftEntity(
    @PrimaryKey
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "draft_text") val draftText: String = "",
    @ColumnInfo(name = "reply_to_message_id") val replyToMessageId: String? = null,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_signal_identity_keys")
data class SignalIdentityKeyEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "registration_id") val registrationId: Int,
    @ColumnInfo(name = "public_key_base64") val publicKeyBase64: String,
    // Private key is wrapped with AndroidKeyStore AES-256-GCM master key; null for remote peers
    @ColumnInfo(name = "keystore_wrapped_private_key_base64") val keystoreWrappedPrivateKeyBase64: String? = null,
    @ColumnInfo(name = "keystore_iv_base64") val keystoreIvBase64: String? = null,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_signal_signed_prekeys",
    primaryKeys = ["user_id", "prekey_id"]
)
data class SignalSignedPreKeyEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "prekey_id") val prekeyId: Int,
    @ColumnInfo(name = "public_key_base64") val publicKeyBase64: String,
    @ColumnInfo(name = "signature_base64") val signatureBase64: String,
    @ColumnInfo(name = "keystore_wrapped_private_key_base64") val keystoreWrappedPrivateKeyBase64: String? = null,
    @ColumnInfo(name = "keystore_iv_base64") val keystoreIvBase64: String? = null,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_signal_one_time_prekeys",
    primaryKeys = ["user_id", "prekey_id"]
)
data class SignalOneTimePreKeyEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "prekey_id") val prekeyId: Int,
    @ColumnInfo(name = "public_key_base64") val publicKeyBase64: String,
    @ColumnInfo(name = "keystore_wrapped_private_key_base64") val keystoreWrappedPrivateKeyBase64: String? = null,
    @ColumnInfo(name = "keystore_iv_base64") val keystoreIvBase64: String? = null,
    @ColumnInfo(name = "is_consumed") val isConsumed: Boolean = false,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_signal_ratchet_sessions",
    primaryKeys = ["owner_user_id", "peer_user_id"]
)
data class SignalRatchetSessionEntity(
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "peer_user_id") val peerUserId: String,
    @ColumnInfo(name = "keystore_wrapped_state_base64") val keystoreWrappedStateBase64: String,
    @ColumnInfo(name = "keystore_iv_base64") val keystoreIvBase64: String,
    @ColumnInfo(name = "local_identity_pub_base64") val localIdentityPubBase64: String,
    @ColumnInfo(name = "peer_identity_pub_base64") val peerIdentityPubBase64: String,
    @ColumnInfo(name = "safety_number_digits") val safetyNumberDigits: String,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_chat_outbox",
    indices = [
        Index(value = ["owner_user_id", "status", "created_at_ms"]),
        Index(value = ["idempotency_key"], unique = true)
    ]
)
data class ChatOutboxEntity(
    @PrimaryKey
    @ColumnInfo(name = "outbox_id") val outboxId: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "target_user_id") val targetUserId: String,
    @ColumnInfo(name = "message_id") val messageId: String? = null,
    @ColumnInfo(name = "action_type") val actionType: String, // SEND, EDIT, UNSEND, DELETE, REACT, PIN, FAVOURITE, FORWARD, BLOCK, REPORT, CLEAR_CHAT, DISAPPEARING, DELIVERY_RECEIPT, SEEN_RECEIPT, VIEW_ONCE_CONSUMED, PUBLISH_PREKEYS
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "status") val status: String = "PENDING", // PENDING, TERMINAL_REJECTED
    @ColumnInfo(name = "terminal_error_code") val terminalErrorCode: String? = null, // BLOCKED, USER_NOT_FOUND
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_klipy_cached_items",
    primaryKeys = ["item_id", "media_tab"],
    indices = [Index(value = ["media_tab", "search_query", "page_Order"])]
)
data class KlipyCachedItemEntity(
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "media_tab") val mediaTab: String, // STICKERS, GIFS, CLIPS
    @ColumnInfo(name = "search_query") val searchQuery: String = "",
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "media_url") val mediaUrl: String,
    @ColumnInfo(name = "preview_url") val previewUrl: String,
    @ColumnInfo(name = "local_cached_path") val localCachedPath: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String = "image/gif",
    @ColumnInfo(name = "width_px") val widthPx: Int = 240,
    @ColumnInfo(name = "height_px") val heightPx: Int = 240,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 2400L,
    @ColumnInfo(name = "accent_hex") val accentHex: String = "#C80815",
    @ColumnInfo(name = "page_Order") val pageOrder: Int = 0,
    @ColumnInfo(name = "cached_at_ms") val cachedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "telefam_klipy_recents",
    primaryKeys = ["owner_user_id", "item_id"],
    indices = [Index(value = ["owner_user_id", "media_tab", "last_used_at_ms"])]
)
data class KlipyRecentItemEntity(
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "media_tab") val mediaTab: String, // STICKERS, GIFS, CLIPS
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "media_url") val mediaUrl: String,
    @ColumnInfo(name = "preview_url") val previewUrl: String,
    @ColumnInfo(name = "local_cached_path") val localCachedPath: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String = "image/gif",
    @ColumnInfo(name = "accent_hex") val accentHex: String = "#C80815",
    @ColumnInfo(name = "last_used_at_ms") val lastUsedAtMs: Long = System.currentTimeMillis()
)

