package com.telefam.company.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TelefamDao {

    // --- Users ---
    @Query("SELECT * FROM telefam_users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM telefam_users WHERE user_id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    // --- Profiles & Username Uniqueness ---
    @Query("SELECT * FROM telefam_profiles WHERE user_id = :userId LIMIT 1")
    suspend fun getProfileByUserId(userId: String): ProfileEntity?

    @Query("SELECT * FROM telefam_profiles WHERE user_id = :userId LIMIT 1")
    fun observeProfileByUserId(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM telefam_profiles WHERE is_pending_sync = 1 ORDER BY updated_at_ms ASC")
    suspend fun getPendingSyncProfiles(): List<ProfileEntity>

    @Query("UPDATE telefam_profiles SET is_pending_sync = 0, last_synced_at_ms = :syncedAtMs WHERE user_id = :userId")
    suspend fun markProfileSynced(userId: String, syncedAtMs: Long)

    @Query("SELECT * FROM telefam_profiles WHERE LOWER(username) = LOWER(:username) AND user_id != :excludeUserId LIMIT 1")
    suspend fun findOtherProfileWithUsername(username: String, excludeUserId: String): ProfileEntity?

    @Query("SELECT LOWER(username) FROM telefam_profiles WHERE user_id != :excludeUserId")
    suspend fun getAllTakenUsernamesExcept(excludeUserId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    // --- Media Fingerprints Vault (SHA-256 & 64-bit Perceptual dHash Reuse) ---
    @Query("SELECT * FROM telefam_media_fingerprints WHERE sha256_checksum = :sha256 OR perceptual_dhash = :dHash LIMIT 1")
    suspend fun findMediaByFingerprint(sha256: String, dHash: String): MediaFingerprintEntity?

    @Query("SELECT * FROM telefam_media_fingerprints ORDER BY last_reused_at_ms DESC LIMIT 12")
    suspend fun getRecentFingerprintedMedia(): List<MediaFingerprintEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaFingerprint(entity: MediaFingerprintEntity)

    // --- Trusted Devices ---
    @Query("SELECT * FROM telefam_trusted_devices WHERE user_id = :userId AND is_trusted = 1 ORDER BY last_seen_ms DESC")
    suspend fun getTrustedDevicesForUser(userId: String): List<TrustedDeviceEntity>

    @Query("SELECT * FROM telefam_trusted_devices WHERE user_id = :userId AND is_trusted = 1 ORDER BY last_seen_ms DESC")
    fun observeTrustedDevicesForUser(userId: String): Flow<List<TrustedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrustedDevice(device: TrustedDeviceEntity)

    @Query("DELETE FROM telefam_trusted_devices WHERE device_record_id = :deviceRecordId AND user_id = :userId")
    suspend fun deleteTrustedDevice(deviceRecordId: String, userId: String)

    // --- OTP Challenges & Expiration Invalidation ---
    @Query("UPDATE telefam_otp_challenges SET is_invalidated = 1 WHERE is_invalidated = 0 AND expires_at_ms <= :nowMs")
    suspend fun invalidateExpiredOtps(nowMs: Long)

    @Query("UPDATE telefam_otp_challenges SET is_invalidated = 1 WHERE LOWER(email) = LOWER(:email) AND is_invalidated = 0")
    suspend fun invalidateActiveOtpsForEmail(email: String)

    @Query("SELECT * FROM telefam_otp_challenges WHERE LOWER(email) = LOWER(:email) AND purpose = :purpose AND is_invalidated = 0 ORDER BY created_at_ms DESC LIMIT 1")
    suspend fun getLatestActiveOtp(email: String, purpose: String): OtpChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtpChallenge(challenge: OtpChallengeEntity)

    // --- Encrypted Sessions ---
    @Query("UPDATE telefam_encrypted_sessions SET is_revoked = 1 WHERE is_revoked = 0 AND refresh_expires_at_ms <= :nowMs")
    suspend fun invalidateExpiredSessions(nowMs: Long)

    @Query("UPDATE telefam_encrypted_sessions SET is_revoked = 1 WHERE user_id = :userId")
    suspend fun revokeSessionsForUser(userId: String)

    @Query("SELECT * FROM telefam_encrypted_sessions WHERE is_revoked = 0 AND refresh_expires_at_ms > :nowMs ORDER BY created_at_ms DESC LIMIT 1")
    suspend fun getLatestActiveSession(nowMs: Long = 0L): EncryptedSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncryptedSession(session: EncryptedSessionEntity)

    // --- IP & User Rate Limits ---
    @Query("SELECT * FROM telefam_rate_limits WHERE bucket_key = :bucketKey LIMIT 1")
    suspend fun getRateLimitBucket(bucketKey: String): RateLimitBucketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRateLimitBucket(bucket: RateLimitBucketEntity)

    // --- Forensic Audit Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForensicAuditLog(log: ForensicAuditLogEntity)

    @Query("SELECT * FROM telefam_forensic_audit_logs ORDER BY timestamp_ms DESC LIMIT 60")
    fun observeRecentForensicLogs(): Flow<List<ForensicAuditLogEntity>>

    @Query("SELECT COUNT(*) FROM telefam_forensic_audit_logs")
    suspend fun countForensicLogs(): Int

    // --- User Privacy, Chat Background Theme & Message Bubble Colour (Offline-First Room + Sync Queue) ---
    @Query("SELECT * FROM telefam_user_privacy_settings WHERE user_id = :userId LIMIT 1")
    fun observePrivacySettings(userId: String): Flow<PrivacySettingsEntity?>

    @Query("SELECT * FROM telefam_user_privacy_settings WHERE user_id = :userId LIMIT 1")
    suspend fun getPrivacySettings(userId: String): PrivacySettingsEntity?

    @Query("SELECT * FROM telefam_user_privacy_settings WHERE is_pending_sync = 1 ORDER BY updated_at_ms ASC")
    suspend fun getPendingSyncPrivacySettings(): List<PrivacySettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrivacySettings(settings: PrivacySettingsEntity)

    @Query("UPDATE telefam_user_privacy_settings SET is_pending_sync = 0, sync_retry_count = 0, last_synced_at_ms = :syncedAtMs WHERE user_id = :userId")
    suspend fun markPrivacySettingsSynced(userId: String, syncedAtMs: Long)

    // --- Registered Users & Profiles Discovery ---
    @Query("SELECT * FROM telefam_users WHERE user_id != :currentUserId ORDER BY created_at_ms DESC")
    suspend fun getAllOtherUsers(currentUserId: String): List<UserEntity>

    @Query("SELECT * FROM telefam_profiles WHERE user_id != :currentUserId ORDER BY updated_at_ms DESC")
    suspend fun getAllOtherProfiles(currentUserId: String): List<ProfileEntity>

    @Query("SELECT * FROM telefam_profiles ORDER BY updated_at_ms DESC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    // --- Follow Graph & Idempotency ---
    @Query("SELECT * FROM telefam_follow_edges WHERE follower_user_id = :followerUserId")
    suspend fun getFollowingEdgesForUser(followerUserId: String): List<FollowEdgeEntity>

    @Query("SELECT * FROM telefam_follow_edges WHERE target_user_id = :targetUserId")
    suspend fun getFollowerEdgesForUser(targetUserId: String): List<FollowEdgeEntity>

    @Query("SELECT * FROM telefam_follow_edges")
    suspend fun getAllFollowEdges(): List<FollowEdgeEntity>

    @Query("SELECT * FROM telefam_follow_edges")
    fun observeAllFollowEdges(): Flow<List<FollowEdgeEntity>>

    @Query("SELECT * FROM telefam_follow_edges WHERE follower_user_id = :followerUserId AND target_user_id = :targetUserId LIMIT 1")
    suspend fun getFollowEdge(followerUserId: String, targetUserId: String): FollowEdgeEntity?

    @Query("SELECT * FROM telefam_follow_edges WHERE idempotency_key = :idempotencyKey LIMIT 1")
    suspend fun getFollowEdgeByIdempotencyKey(idempotencyKey: String): FollowEdgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowEdge(edge: FollowEdgeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFollowEdgesIgnore(edges: List<FollowEdgeEntity>)

    @Query("DELETE FROM telefam_follow_edges WHERE follower_user_id = :followerUserId AND target_user_id = :targetUserId")
    suspend fun deleteFollowEdge(followerUserId: String, targetUserId: String)

    @Query("DELETE FROM telefam_follow_edges WHERE (follower_user_id = :userA AND target_user_id = :userB) OR (follower_user_id = :userB AND target_user_id = :userA)")
    suspend fun deleteAllFollowEdgesBetween(userA: String, userB: String)

    @Query("SELECT COUNT(*) FROM telefam_follow_edges WHERE target_user_id = :userId")
    suspend fun countFollowers(userId: String): Long

    @Query("SELECT COUNT(*) FROM telefam_follow_edges WHERE target_user_id = :userId")
    fun observeFollowersCount(userId: String): Flow<Long>

    @Query("SELECT COUNT(*) FROM telefam_follow_edges WHERE follower_user_id = :userId")
    suspend fun countFollowing(userId: String): Long

    @Query("SELECT COUNT(*) FROM telefam_follow_edges WHERE follower_user_id = :userId")
    fun observeFollowingCount(userId: String): Flow<Long>

    // --- User Subscriptions (Future Subscribers Support) ---
    @Query("SELECT * FROM telefam_user_subscriptions WHERE creator_user_id = :creatorUserId ORDER BY created_at_ms DESC")
    suspend fun getSubscribersForCreator(creatorUserId: String): List<UserSubscriptionEntity>

    @Query("SELECT COUNT(*) FROM telefam_user_subscriptions WHERE creator_user_id = :creatorUserId")
    suspend fun countSubscribers(creatorUserId: String): Long

    @Query("SELECT COUNT(*) FROM telefam_user_subscriptions WHERE creator_user_id = :creatorUserId")
    fun observeSubscribersCount(creatorUserId: String): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubscriptionsIgnore(subscriptions: List<UserSubscriptionEntity>)

    @Query("DELETE FROM telefam_user_subscriptions WHERE idempotency_key LIKE 'seed_sub_%'")
    suspend fun deleteAllSeededSubscriptions()

    // --- Contact Hashes (Phone & Email SHA-256 Discovery) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContactHashes(hashes: List<ContactHashEntity>)

    @Query("SELECT * FROM telefam_contact_hashes WHERE owner_user_id = :ownerUserId")
    suspend fun getContactHashesForOwner(ownerUserId: String): List<ContactHashEntity>

    @Query("UPDATE telefam_contact_hashes SET is_pending_sync = 0 WHERE owner_user_id = :ownerUserId")
    suspend fun markContactHashesSynced(ownerUserId: String)

    // --- User Blocks ---
    @Query("SELECT * FROM telefam_user_blocks WHERE blocker_user_id = :blockerUserId ORDER BY created_at_ms DESC")
    suspend fun getBlockedUsers(blockerUserId: String): List<UserBlockEntity>

    @Query("SELECT * FROM telefam_user_blocks WHERE blocker_user_id = :blockerUserId ORDER BY created_at_ms DESC")
    fun observeBlockedUsers(blockerUserId: String): Flow<List<UserBlockEntity>>

    @Query("SELECT * FROM telefam_user_blocks WHERE blocker_user_id = :blockerUserId AND blocked_user_id = :blockedUserId LIMIT 1")
    suspend fun getBlockRecord(blockerUserId: String, blockedUserId: String): UserBlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserBlock(block: UserBlockEntity)

    @Query("DELETE FROM telefam_user_blocks WHERE blocker_user_id = :blockerUserId AND blocked_user_id = :blockedUserId")
    suspend fun deleteUserBlock(blockerUserId: String, blockedUserId: String)

    // --- Multi-Question User Reports ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserReport(report: UserReportEntity)

    @Query("SELECT * FROM telefam_user_reports WHERE idempotency_key = :idempotencyKey LIMIT 1")
    suspend fun getReportByIdempotencyKey(idempotencyKey: String): UserReportEntity?

    // --- User Posts (Views & Likes) ---
    @Query("SELECT * FROM telefam_user_posts WHERE user_id = :userId ORDER BY created_at_ms DESC")
    suspend fun getPostsForUser(userId: String): List<UserPostEntity>

    @Query("SELECT * FROM telefam_user_posts WHERE user_id = :userId ORDER BY created_at_ms DESC")
    fun observePostsForUser(userId: String): Flow<List<UserPostEntity>>

    @Query("SELECT COALESCE(SUM(likes_count), 0) FROM telefam_user_posts WHERE user_id = :userId")
    suspend fun getTotalLikesForUser(userId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserPosts(posts: List<UserPostEntity>)

    @Query("DELETE FROM telefam_user_posts WHERE post_id LIKE 'post_%' OR post_id LIKE 'owner_%'")
    suspend fun deleteAllSeededPosts()

    // --- Social Sync Queue (Silent Background Worker Outbox) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSocialSyncOperation(item: SocialSyncQueueEntity)

    @Query("SELECT * FROM telefam_social_sync_queue ORDER BY created_at_ms ASC")
    suspend fun getPendingSocialSyncOperations(): List<SocialSyncQueueEntity>

    @Query("DELETE FROM telefam_social_sync_queue WHERE Operation_id = :operationId")
    suspend fun deleteSocialSyncOperation(operationId: String)

    @Query("UPDATE telefam_social_sync_queue SET retry_count = retry_count + 1 WHERE Operation_id = :operationId")
    suspend fun incrementSocialSyncRetryCount(operationId: String)

    // --- Account Settings, Security, 2FA, Passkey, Notifications & Content Preferences (RLS) ---
    @Query("SELECT * FROM telefam_account_security_settings WHERE owner_user_id = :ownerUserId LIMIT 1")
    suspend fun getAccountSettingsAndSecurity(ownerUserId: String): AccountSettingsAndSecurityEntity?

    @Query("SELECT * FROM telefam_account_security_settings WHERE owner_user_id = :ownerUserId LIMIT 1")
    fun observeAccountSettingsAndSecurity(ownerUserId: String): Flow<AccountSettingsAndSecurityEntity?>

    @Query("SELECT * FROM telefam_account_security_settings WHERE is_pending_sync = 1 ORDER BY updated_at_ms ASC")
    suspend fun getPendingSyncAccountSettings(): List<AccountSettingsAndSecurityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccountSettingsAndSecurity(entity: AccountSettingsAndSecurityEntity)

    @Query("UPDATE telefam_account_security_settings SET is_pending_sync = 0, last_synced_at_ms = :syncedAtMs WHERE owner_user_id = :ownerUserId")
    suspend fun markAccountSettingsSynced(ownerUserId: String, syncedAtMs: Long)

    @Query("SELECT * FROM telefam_account_security_settings WHERE passkey_enabled = 1 ORDER BY updated_at_ms DESC LIMIT 1")
    suspend fun getLatestPasskeyEnabledSettings(): AccountSettingsAndSecurityEntity?

    @Query("DELETE FROM telefam_trusted_devices WHERE user_id = :userId AND (device_record_id LIKE '%_secondary_%' OR os_description LIKE '%iPhone%')")
    suspend fun deleteLegacyFakeDevicesForUser(userId: String)

    // --- Connected Apps (RLS) ---
    @Query("SELECT * FROM telefam_connected_apps WHERE owner_user_id = :ownerUserId AND is_revoked = 0 ORDER BY last_used_at_ms DESC")
    suspend fun getConnectedAppsForUser(ownerUserId: String): List<ConnectedAppSessionEntity>

    @Query("SELECT * FROM telefam_connected_apps WHERE owner_user_id = :ownerUserId AND is_revoked = 0 ORDER BY last_used_at_ms DESC")
    fun observeConnectedAppsForUser(ownerUserId: String): Flow<List<ConnectedAppSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnectedApps(apps: List<ConnectedAppSessionEntity>)

    @Query("UPDATE telefam_connected_apps SET is_revoked = 1, is_pending_sync = 1 WHERE connection_id = :connectionId AND owner_user_id = :ownerUserId")
    suspend fun revokeConnectedApp(connectionId: String, ownerUserId: String)

    // --- Problem Reports & Support Tickets (RLS) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProblemSupportTicket(ticket: ProblemSupportTicketEntity)

    @Query("SELECT * FROM telefam_problem_support_tickets WHERE owner_user_id = :ownerUserId ORDER BY created_at_ms DESC")
    suspend fun getProblemSupportTicketsForUser(ownerUserId: String): List<ProblemSupportTicketEntity>

    @Query("SELECT * FROM telefam_problem_support_tickets WHERE owner_user_id = :ownerUserId ORDER BY created_at_ms DESC")
    fun observeProblemSupportTicketsForUser(ownerUserId: String): Flow<List<ProblemSupportTicketEntity>>

    @Query("UPDATE telefam_problem_support_tickets SET is_pending_sync = 0 WHERE ticket_id = :ticketId")
    suspend fun markProblemSupportTicketSynced(ticketId: String)

    @Query("SELECT * FROM telefam_problem_support_tickets WHERE ticket_id = :ticketId AND owner_user_id = :ownerUserId LIMIT 1")
    suspend fun getProblemSupportTicketById(ticketId: String, ownerUserId: String): ProblemSupportTicketEntity?

    @Query("UPDATE telefam_problem_support_tickets SET status = :status, user_follow_up_reply = :followUpReply, support_response_note = :supportNote, is_pending_sync = 1, updated_at_ms = :updatedAtMs WHERE ticket_id = :ticketId AND owner_user_id = :ownerUserId")
    suspend fun updateSupportTicketFollowUp(
        ticketId: String,
        ownerUserId: String,
        status: String,
        followUpReply: String,
        supportNote: String,
        updatedAtMs: Long
    )

    // --- Quick Login Accounts ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuickLoginAccount(account: QuickLoginAccountEntity)

    @Query("SELECT * FROM telefam_quick_login_accounts ORDER BY last_logged_out_at_ms DESC")
    suspend fun getQuickLoginAccounts(): List<QuickLoginAccountEntity>

    @Query("DELETE FROM telefam_quick_login_accounts WHERE user_id = :userId")
    suspend fun deleteQuickLoginAccount(userId: String)
}
