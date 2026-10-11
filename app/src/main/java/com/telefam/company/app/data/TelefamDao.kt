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

    @Query("SELECT * FROM telefam_user_blocks WHERE blocker_user_id = :blockerUserId ORDER BY created_at_ms DESC")
    fun observeBlocksByUser(blockerUserId: String): Flow<List<UserBlockEntity>>

    @Query("SELECT * FROM telefam_user_blocks WHERE blocked_user_id = :blockedUserId ORDER BY created_at_ms DESC")
    fun observeBlocksAgainstUser(blockedUserId: String): Flow<List<UserBlockEntity>>

    @Query("SELECT COUNT(*) FROM telefam_user_blocks WHERE (blocker_user_id = :userA AND blocked_user_id = :userB) OR (blocker_user_id = :userB AND blocked_user_id = :userA)")
    suspend fun isEitherBlocked(userA: String, userB: String): Int

    @Query("SELECT * FROM telefam_profiles WHERE user_id != :excludeUserId ORDER BY updated_at_ms DESC")
    suspend fun getAllDiscoverableProfilesExcept(excludeUserId: String): List<ProfileEntity>

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

    // =========================================================================
    // 1:1 CHATS, MESSAGES, REACTIONS, PINS, FAVOURITES, DISAPPEARING, RECEIPTS,
    // DRAFTS, SIGNAL KEYS/SESSIONS, OUTBOX & KLIPY CACHE
    // =========================================================================

    @Query("SELECT * FROM telefam_chats WHERE owner_user_id = :ownerUserId ORDER BY last_message_at_ms DESC, updated_at_ms DESC")
    fun observeChatsForOwner(ownerUserId: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM telefam_chats WHERE owner_user_id = :ownerUserId ORDER BY last_message_at_ms DESC, updated_at_ms DESC")
    suspend fun getChatsForOwner(ownerUserId: String): List<ChatEntity>

    @Query("SELECT * FROM telefam_chats WHERE chat_id = :chatId LIMIT 1")
    fun observeChatById(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM telefam_chats WHERE chat_id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM telefam_chats WHERE owner_user_id = :ownerUserId AND peer_user_id = :peerUserId LIMIT 1")
    suspend fun getChatByOwnerAndPeer(ownerUserId: String, peerUserId: String): ChatEntity?

    @Query("SELECT * FROM telefam_chats WHERE owner_user_id = :ownerUserId AND peer_user_id = :peerUserId LIMIT 1")
    suspend fun getChatByPeer(ownerUserId: String, peerUserId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChat(chat: ChatEntity)

    @Query("UPDATE telefam_chats SET unread_count = 0 WHERE chat_id = :chatId")
    suspend fun clearChatUnreadCount(chatId: String)

    @Query("UPDATE telefam_chats SET is_peer_typing = :isTyping WHERE chat_id = :chatId")
    suspend fun updateChatPeerTyping(chatId: String, isTyping: Boolean)

    @Query("UPDATE telefam_chats SET is_peer_typing = :isTyping WHERE chat_id = :chatId")
    suspend fun updatePeerTypingState(chatId: String, isTyping: Boolean)

    @Query("UPDATE telefam_chats SET is_peer_online = :isOnline, peer_last_seen_ms = :lastSeenMs WHERE chat_id = :chatId")
    suspend fun updateChatPeerPresence(chatId: String, isOnline: Boolean, lastSeenMs: Long?)

    @Query("UPDATE telefam_chats SET is_peer_online = :isOnline, peer_last_seen_ms = COALESCE(:lastSeenMs, peer_last_seen_ms) WHERE chat_id = :chatId")
    suspend fun updatePeerPresenceState(chatId: String, isOnline: Boolean, lastSeenMs: Long?)

    @Query("UPDATE telefam_chats SET is_peer_account_deleted = :isDeleted WHERE chat_id = :chatId")
    suspend fun updateChatPeerAccountDeleted(chatId: String, isDeleted: Boolean)

    @Query("UPDATE telefam_chats SET is_e2ee_verified = :verified WHERE chat_id = :chatId")
    suspend fun updateChatE2eeVerified(chatId: String, verified: Boolean)

    // --- Messages ---
    @Query("SELECT * FROM telefam_chat_messages WHERE chat_id = :chatId AND (expires_at_ms IS NULL OR expires_at_ms > :nowMs) ORDER BY created_at_ms ASC")
    fun observeMessagesForChat(chatId: String, nowMs: Long = 0L): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM telefam_chat_messages WHERE chat_id = :chatId ORDER BY created_at_ms ASC")
    suspend fun getMessagesForChat(chatId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM telefam_chat_messages WHERE message_id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChatMessage(message: ChatMessageEntity)

    @Query("UPDATE telefam_chat_messages SET delivery_status = :status, delivered_at_ms = COALESCE(:deliveredAtMs, delivered_at_ms), seen_at_ms = COALESCE(:seenAtMs, seen_at_ms) WHERE message_id = :messageId")
    suspend fun updateMessageDeliveryStatus(messageId: String, status: String, deliveredAtMs: Long? = null, seenAtMs: Long? = null)

    @Query("UPDATE telefam_chat_messages SET delivery_status = 'SEEN', seen_at_ms = :seenAtMs WHERE chat_id = :chatId AND sender_user_id != :ownerUserId AND delivery_status != 'SEEN'")
    suspend fun markIncomingMessagesSeen(chatId: String, ownerUserId: String, seenAtMs: Long)

    @Query("UPDATE telefam_chat_messages SET decrypted_text = :newText, ciphertext_base64 = :newCiphertext, ratchet_header_json = :newHeader, is_edited = 1, edited_at_ms = :editedAtMs WHERE message_id = :messageId")
    suspend fun updateEditedMessage(messageId: String, newText: String, newCiphertext: String, newHeader: String, editedAtMs: Long)

    @Query("UPDATE telefam_chat_messages SET decrypted_text = :newText, ciphertext_base64 = :newCiphertext, ratchet_header_json = :newHeader, is_edited = 1, edited_at_ms = :editedAtMs WHERE message_id = :messageId")
    suspend fun updateMessageEdited(messageId: String, newText: String, newCiphertext: String, newHeader: String, editedAtMs: Long)

    @Query("UPDATE telefam_chat_messages SET is_view_once_consumed = 1, media_local_path = NULL, media_url = NULL, ciphertext_base64 = '' WHERE message_id = :messageId")
    suspend fun markViewOnceVoiceNoteConsumed(messageId: String)

    @Query("UPDATE telefam_chat_messages SET is_view_once_consumed = 1, media_local_path = NULL, media_url = NULL, ciphertext_base64 = '' WHERE message_id = :messageId")
    suspend fun markViewOnceConsumed(messageId: String)

    @Query("DELETE FROM telefam_chat_messages WHERE message_id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM telefam_chat_messages WHERE message_id = :messageId")
    suspend fun deleteMessageHard(messageId: String)

    @Query("DELETE FROM telefam_chat_messages WHERE message_id IN (:messageIds)")
    suspend fun deleteMessagesByIds(messageIds: List<String>)

    @Query("DELETE FROM telefam_chat_messages WHERE chat_id = :chatId")
    suspend fun deleteAllMessagesInChat(chatId: String)

    @Query("DELETE FROM telefam_chat_messages WHERE chat_id = :chatId")
    suspend fun deleteAllMessagesForChat(chatId: String)

    @Query("DELETE FROM telefam_chat_messages WHERE expires_at_ms IS NOT NULL AND expires_at_ms <= :nowMs")
    suspend fun purgeExpiredDisappearingMessages(nowMs: Long)

    @Query("DELETE FROM telefam_chat_messages WHERE expires_at_ms IS NOT NULL AND expires_at_ms <= :nowMs")
    suspend fun deleteExpiredMessages(nowMs: Long)

    @Query("SELECT * FROM telefam_chat_messages WHERE chat_id = :chatId ORDER BY created_at_ms DESC LIMIT 1")
    suspend fun getLatestMessageForChat(chatId: String): ChatMessageEntity?

    // --- Reactions ---
    @Query("SELECT * FROM telefam_message_reactions WHERE chat_id = :chatId ORDER BY created_at_ms ASC")
    fun observeReactionsForChat(chatId: String): Flow<List<MessageReactionEntity>>

    @Query("SELECT * FROM telefam_message_reactions WHERE message_id = :messageId AND reactor_user_id = :reactorUserId LIMIT 1")
    suspend fun getReaction(messageId: String, reactorUserId: String): MessageReactionEntity?

    @Query("SELECT * FROM telefam_message_reactions WHERE message_id = :messageId AND reactor_user_id = :reactorUserId LIMIT 1")
    suspend fun getReactionByUser(messageId: String, reactorUserId: String): MessageReactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReaction(reaction: MessageReactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessageReaction(reaction: MessageReactionEntity)

    @Query("DELETE FROM telefam_message_reactions WHERE message_id = :messageId AND reactor_user_id = :reactorUserId")
    suspend fun deleteReaction(messageId: String, reactorUserId: String)

    @Query("DELETE FROM telefam_message_reactions WHERE message_id = :messageId AND reactor_user_id = :reactorUserId")
    suspend fun deleteReactionByUser(messageId: String, reactorUserId: String)

    @Query("DELETE FROM telefam_message_reactions WHERE message_id = :messageId")
    suspend fun deleteAllReactionsForMessage(messageId: String)

    @Query("DELETE FROM telefam_message_reactions WHERE message_id = :messageId")
    suspend fun deleteReactionsForMessage(messageId: String)

    @Query("DELETE FROM telefam_message_reactions WHERE chat_id = :chatId")
    suspend fun deleteAllReactionsForChat(chatId: String)

    // --- Pins ---
    @Query("SELECT * FROM telefam_chat_pins WHERE chat_id = :chatId ORDER BY pinned_at_ms DESC")
    fun observePinsForChat(chatId: String): Flow<List<ChatPinEntity>>

    @Query("SELECT p.* FROM telefam_chat_pins p INNER JOIN telefam_chats c ON p.chat_id = c.chat_id WHERE c.owner_user_id = :ownerUserId")
    fun observeAllPinsForOwner(ownerUserId: String): Flow<List<ChatPinEntity>>

    @Query("SELECT * FROM telefam_chat_pins WHERE chat_id = :chatId AND message_id = :messageId LIMIT 1")
    suspend fun getPin(chatId: String, messageId: String): ChatPinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPin(pin: ChatPinEntity)

    @Query("DELETE FROM telefam_chat_pins WHERE chat_id = :chatId AND message_id = :messageId")
    suspend fun deletePin(chatId: String, messageId: String)

    @Query("DELETE FROM telefam_chat_pins WHERE message_id = :messageId")
    suspend fun deletePinsForMessage(messageId: String)

    @Query("DELETE FROM telefam_chat_pins WHERE chat_id = :chatId")
    suspend fun deleteAllPinsForChat(chatId: String)

    // --- Favourites ---
    @Query("SELECT * FROM telefam_chat_favourites WHERE owner_user_id = :ownerUserId AND chat_id = :chatId ORDER BY favourited_at_ms DESC")
    fun observeFavouritesForChat(ownerUserId: String, chatId: String): Flow<List<ChatFavouriteEntity>>

    @Query("SELECT * FROM telefam_chat_favourites WHERE owner_user_id = :ownerUserId AND message_id = :messageId LIMIT 1")
    suspend fun getFavourite(ownerUserId: String, messageId: String): ChatFavouriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavourite(favourite: ChatFavouriteEntity)

    @Query("DELETE FROM telefam_chat_favourites WHERE owner_user_id = :ownerUserId AND message_id = :messageId")
    suspend fun deleteFavourite(ownerUserId: String, messageId: String)

    @Query("DELETE FROM telefam_chat_favourites WHERE message_id = :messageId")
    suspend fun deleteFavouritesForMessage(messageId: String)

    @Query("DELETE FROM telefam_chat_favourites WHERE chat_id = :chatId")
    suspend fun deleteAllFavouritesForChat(chatId: String)

    // --- Disappearing Settings ---
    @Query("SELECT * FROM telefam_disappearing_settings WHERE chat_id = :chatId LIMIT 1")
    fun observeDisappearingSetting(chatId: String): Flow<DisappearingSettingEntity?>

    @Query("SELECT * FROM telefam_disappearing_settings WHERE chat_id = :chatId LIMIT 1")
    suspend fun getDisappearingSetting(chatId: String): DisappearingSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDisappearingSetting(setting: DisappearingSettingEntity)

    // --- Delivery Receipts ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeliveryReceipt(receipt: DeliveryReceiptEntity)

    @Query("SELECT * FROM telefam_delivery_receipts WHERE chat_id = :chatId")
    fun observeReceiptsForChat(chatId: String): Flow<List<DeliveryReceiptEntity>>

    @Query("DELETE FROM telefam_delivery_receipts WHERE chat_id = :chatId")
    suspend fun deleteAllReceiptsForChat(chatId: String)

    // --- Drafts ---
    @Query("SELECT * FROM telefam_chat_drafts WHERE owner_user_id = :ownerUserId")
    fun observeAllDraftsForOwner(ownerUserId: String): Flow<List<ChatDraftEntity>>

    @Query("SELECT * FROM telefam_chat_drafts WHERE chat_id = :chatId LIMIT 1")
    fun observeDraftForChat(chatId: String): Flow<ChatDraftEntity?>

    @Query("SELECT * FROM telefam_chat_drafts WHERE chat_id = :chatId LIMIT 1")
    suspend fun getDraftForChat(chatId: String): ChatDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChatDraft(draft: ChatDraftEntity)

    @Query("DELETE FROM telefam_chat_drafts WHERE chat_id = :chatId")
    suspend fun deleteChatDraft(chatId: String)

    // --- Signal Keys & Double Ratchet Sessions ---
    @Query("SELECT * FROM telefam_signal_identity_keys WHERE user_id = :userId LIMIT 1")
    suspend fun getSignalIdentityKey(userId: String): SignalIdentityKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSignalIdentityKey(entity: SignalIdentityKeyEntity)

    @Query("SELECT * FROM telefam_signal_signed_prekeys WHERE user_id = :userId ORDER BY created_at_ms DESC LIMIT 1")
    suspend fun getLatestSignalSignedPreKey(userId: String): SignalSignedPreKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSignalSignedPreKey(entity: SignalSignedPreKeyEntity)

    @Query("SELECT * FROM telefam_signal_one_time_prekeys WHERE user_id = :userId AND is_consumed = 0 ORDER BY prekey_id ASC")
    suspend fun getAvailableOneTimePreKeys(userId: String): List<SignalOneTimePreKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSignalOneTimePreKeys(entities: List<SignalOneTimePreKeyEntity>)

    @Query("UPDATE telefam_signal_one_time_prekeys SET is_consumed = 1 WHERE user_id = :userId AND prekey_id = :prekeyId")
    suspend fun markOneTimePreKeyConsumed(userId: String, prekeyId: Int)

    @Query("SELECT * FROM telefam_signal_ratchet_sessions WHERE owner_user_id = :ownerUserId AND peer_user_id = :peerUserId LIMIT 1")
    suspend fun getSignalRatchetSession(ownerUserId: String, peerUserId: String): SignalRatchetSessionEntity?

    @Query("SELECT * FROM telefam_signal_ratchet_sessions WHERE owner_user_id = :ownerUserId AND peer_user_id = :peerUserId LIMIT 1")
    fun observeSignalRatchetSession(ownerUserId: String, peerUserId: String): Flow<SignalRatchetSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSignalRatchetSession(session: SignalRatchetSessionEntity)

    // --- Chat Outbox (WorkManager + WebSocket Reliable Queue) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueChatOutbox(item: ChatOutboxEntity)

    @Query("SELECT * FROM telefam_chat_outbox WHERE status = 'PENDING' ORDER BY created_at_ms ASC")
    suspend fun getPendingChatOutboxItems(): List<ChatOutboxEntity>

    @Query("DELETE FROM telefam_chat_outbox WHERE outbox_id = :outboxId")
    suspend fun deleteChatOutboxItem(outboxId: String)

    @Query("UPDATE telefam_chat_outbox SET status = 'TERMINAL_REJECTED', terminal_error_code = :errorCode WHERE outbox_id = :outboxId")
    suspend fun markChatOutboxTerminalRejected(outboxId: String, errorCode: String)

    @Query("UPDATE telefam_chat_outbox SET retry_count = retry_count + 1 WHERE outbox_id = :outboxId")
    suspend fun incrementChatOutboxRetryCount(outboxId: String)

    // --- Klipy Cached Items & Recents (Offline-First) ---
    @Query("SELECT * FROM telefam_klipy_cached_items WHERE media_tab = :mediaTab AND LOWER(search_query) = LOWER(:query) ORDER BY page_Order ASC")
    suspend fun getCachedKlipyItems(mediaTab: String, query: String): List<KlipyCachedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedKlipyItems(items: List<KlipyCachedItemEntity>)

    @Query("SELECT * FROM telefam_klipy_recents WHERE owner_user_id = :ownerUserId AND media_tab = :mediaTab ORDER BY last_used_at_ms DESC LIMIT 24")
    fun observeKlipyRecents(ownerUserId: String, mediaTab: String): Flow<List<KlipyRecentItemEntity>>

    @Query("SELECT * FROM telefam_klipy_recents WHERE owner_user_id = :ownerUserId AND media_tab = :mediaTab ORDER BY last_used_at_ms DESC LIMIT 24")
    suspend fun getKlipyRecents(ownerUserId: String, mediaTab: String): List<KlipyRecentItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKlipyRecent(item: KlipyRecentItemEntity)
}
