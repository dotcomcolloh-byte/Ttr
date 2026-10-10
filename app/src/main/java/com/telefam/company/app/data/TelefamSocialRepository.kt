package com.telefam.company.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.R
import com.telefam.company.app.domain.ContactsFilterMode
import com.telefam.company.app.domain.DeviceAndNetworkTelemetry
import com.telefam.company.app.domain.DiscoverableUserItem
import com.telefam.company.app.domain.FollowListType
import com.telefam.company.app.domain.FollowRelationState
import com.telefam.company.app.domain.OwnerProfileCooldownState
import com.telefam.company.app.domain.OwnerProfileSubTab
import com.telefam.company.app.domain.PostsFilterOption
import com.telefam.company.app.domain.ReportLocationOption
import com.telefam.company.app.domain.ReportReasonOption
import com.telefam.company.app.domain.ReportUrgencyOption
import com.telefam.company.app.domain.TelefamCountries
import com.telefam.company.app.domain.UserPostItem
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ContactsDiscoverySnapshot(
    val items: List<DiscoverableUserItem>,
    val totalMatchingCount: Int,
    val hasMorePages: Boolean,
    val currentPage: Int,
    val hasGrantedContactsPermission: Boolean,
    val syncedContactsHashesCount: Int,
    val matchedFromContactsCount: Int,
    val lastDailyRefreshAtMs: Long,
    val isOffline: Boolean
)

data class PublicProfileSnapshot(
    val profile: DiscoverableUserItem,
    val posts: List<UserPostItem>,
    val isOffline: Boolean
)

data class OwnerProfileSnapshot(
    val followersCount: Long,
    val followingCount: Long,
    val subscribersCount: Long,
    val posts: List<UserPostItem>,
    val cooldownState: OwnerProfileCooldownState,
    val isOffline: Boolean
)

class TelefamSocialRepository(
    private val context: Context,
    private val dao: TelefamDao
) {
    private val prefs by lazy {
        context.getSharedPreferences("telefam_social_discovery_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val DAILY_REFRESH_INTERVAL_MS = 24L * 60 * 60 * 1000L
        private const val FOLLOW_RATE_WINDOW_MS = 60_000L
        private const val MAX_FOLLOW_ACTIONS_PER_MINUTE = 20
        private const val FOLLOW_COOLDOWN_MS = 60_000L
        const val DEFAULT_PAGE_SIZE = 6
    }

    private fun sha256String(value: String): String {
        return TelefamCryptoAndTelemetry.sha256Hex(value.toByteArray(Charsets.UTF_8))
    }

    fun hasReadContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLastDailyRefreshMs(userId: String): Long {
        return prefs.getLong("last_daily_refresh_$userId", 0L)
    }

    private fun setLastDailyRefreshMs(userId: String, timestampMs: Long) {
        prefs.edit().putLong("last_daily_refresh_$userId", timestampMs).apply()
    }

    /**
     * Synchronizes the registered Telefam users directory into the local Room database
     * and refreshes daily (every 24 hours) or when explicitly requested.
     * Preserves all existing local follow/unfollow/block states.
     */
    suspend fun syncDirectoryIfNeeded(
        currentUserId: String,
        forceRefresh: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
        val now = System.currentTimeMillis()
        val lastRefresh = getLastDailyRefreshMs(cleanUserId)
        val existingOtherProfiles = dao.getAllOtherProfiles(cleanUserId)
        val needsDailyRefresh = (now - lastRefresh) >= DAILY_REFRESH_INTERVAL_MS

        if (!forceRefresh && !needsDailyRefresh && existingOtherProfiles.isNotEmpty()) {
            return@withContext TelefamCryptoAndTelemetry.isNetworkOnline(context)
        }

        val isOnline = TelefamCryptoAndTelemetry.isNetworkOnline(context)
        // Fetch from backend API (or local edge cache if initial Room table is empty)
        if (isOnline || existingOtherProfiles.isEmpty()) {
            runCatching {
                val response = TelefamNetworkClient.api.discoverRegisteredUsers(
                    rlsUserId = cleanUserId,
                    query = "",
                    page = 1,
                    pageSize = 50
                )
                persistRemoteDirectoryUsers(cleanUserId, response.users, now)
                setLastDailyRefreshMs(cleanUserId, now)
            }
        }

        if (isOnline) {
            TelefamPrivacySyncWorker.enqueueSync(context)
        }
        isOnline
    }

    private suspend fun persistRemoteDirectoryUsers(
        currentUserId: String,
        remoteUsers: List<RemoteDirectoryUserDto>,
        nowMs: Long
    ) {
        val seedEdges = mutableListOf<FollowEdgeEntity>()
        val seedPosts = mutableListOf<UserPostEntity>()
        val initKey = "dir_graph_initialized_$currentUserId"
        val isFirstGraphInit = !prefs.getBoolean(initKey, false)

        for (dto in remoteUsers) {
            if (dto.userId == currentUserId) continue

            val existingUser = dao.getUserById(dto.userId)
            if (existingUser == null) {
                val salt = TelefamCryptoAndTelemetry.generateSaltHex()
                dao.upsertUser(
                    UserEntity(
                        userId = dto.userId,
                        email = dto.email.lowercase(),
                        passwordHash = sha256String("telefam_dir_${dto.userId}_$salt"),
                        passwordSalt = salt,
                        emailVerified = true,
                        lastKnownLocation = dto.locationLabel,
                        createdAtMs = nowMs - 86_400_000L
                    )
                )
            }

            val existingProfile = dao.getProfileByUserId(dto.userId)
            if (existingProfile == null) {
                dao.upsertProfile(
                    ProfileEntity(
                        userId = dto.userId,
                        fullName = dto.fullName,
                        username = dto.username.lowercase(),
                        phoneCountryCode = dto.phoneCountryCode,
                        phoneIsoCode = dto.phoneIsoCode,
                        phoneNumber = dto.phoneNumber,
                        gender = dto.gender,
                        dateOfBirth = dto.dateOfBirth,
                        bio = dto.bio,
                        updatedAtMs = nowMs
                    )
                )
            }

            // Seed inter-user social graph edges so mutual connections & followers/following counts are realistic
            for (friendId in dto.mutualSeedUserIds) {
                if (friendId != dto.userId) {
                    seedEdges.add(
                        FollowEdgeEntity(
                            followerUserId = dto.userId,
                            targetUserId = friendId,
                            idempotencyKey = "seed_edge_${dto.userId}_$friendId",
                            internalRiskScore = 0,
                            isPendingSync = false,
                            createdAtMs = nowMs - 172_800_000L
                        )
                    )
                    seedEdges.add(
                        FollowEdgeEntity(
                            followerUserId = friendId,
                            targetUserId = dto.userId,
                            idempotencyKey = "seed_edge_${friendId}_${dto.userId}",
                            internalRiskScore = 0,
                            isPendingSync = false,
                            createdAtMs = nowMs - 172_800_000L
                        )
                    )
                }
            }

            // Only seed incoming "Follow Back" edges to currentUserId on first initialization
            // so if currentUserId blocks or changes relationships, it isn't re-added.
            if (isFirstGraphInit && dto.followsCurrentUserByDefault) {
                seedEdges.add(
                    FollowEdgeEntity(
                        followerUserId = dto.userId,
                        targetUserId = currentUserId,
                        idempotencyKey = "seed_inbound_${dto.userId}_$currentUserId",
                        internalRiskScore = 0,
                        isPendingSync = false,
                        createdAtMs = nowMs - 86_400_000L
                    )
                )
            }

            for (post in dto.posts) {
                seedPosts.add(
                    UserPostEntity(
                        postId = post.postId,
                        userId = dto.userId,
                        mediaThemeKey = post.mediaThemeKey,
                        caption = post.caption,
                        viewsCount = post.viewsCount,
                        likesCount = post.likesCount,
                        createdAtMs = post.createdAtMs
                    )
                )
            }
        }

        if (seedEdges.isNotEmpty()) {
            dao.insertFollowEdgesIgnore(seedEdges)
        }
        dao.deleteAllSeededPosts()
        dao.deleteAllSeededSubscriptions()
        ensureOwnerMediaAndSubscribersInitialized(currentUserId, nowMs)
        if (isFirstGraphInit) {
            prefs.edit().putBoolean(initKey, true).apply()
        }
    }

    suspend fun ensureOwnerMediaAndSubscribersInitialized(
        currentUserId: String,
        nowMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        // Ensure any legacy demo posts or demo subscriber rows are purged so counts & grids are 100% real
        dao.deleteAllSeededPosts()
        dao.deleteAllSeededSubscriptions()
    }

    fun observeAllFollowEdges(): Flow<List<FollowEdgeEntity>> {
        return dao.observeAllFollowEdges()
    }

    fun observeProfile(userId: String): Flow<ProfileEntity?> {
        return dao.observeProfileByUserId(userId.ifBlank { "local_telefam_member" })
    }

    fun observeFollowersCount(userId: String): Flow<Long> {
        return dao.observeFollowersCount(userId.ifBlank { "local_telefam_member" })
    }

    fun observeFollowingCount(userId: String): Flow<Long> {
        return dao.observeFollowingCount(userId.ifBlank { "local_telefam_member" })
    }

    fun observeSubscribersCount(userId: String): Flow<Long> {
        return dao.observeSubscribersCount(userId.ifBlank { "local_telefam_member" })
    }

    fun observeOwnerPostsByTab(userId: String, subTab: OwnerProfileSubTab): Flow<List<UserPostItem>> {
        val cleanUserId = userId.ifBlank { "local_telefam_member" }
        return dao.observePostsForUser(cleanUserId).map { list ->
            list.filter { it.postCategory == subTab.name }
                .sortedByDescending { it.createdAtMs }
                .map { entity ->
                    UserPostItem(
                        postId = entity.postId,
                        userId = entity.userId,
                        mediaDrawableRes = resolvePostDrawableRes(entity.mediaThemeKey),
                        caption = entity.caption,
                        durationLabel = entity.durationLabel,
                        subTabCategory = runCatching { OwnerProfileSubTab.valueOf(entity.postCategory) }
                            .getOrDefault(OwnerProfileSubTab.POSTS),
                        viewsCount = entity.viewsCount,
                        likesCount = entity.likesCount,
                        createdAtMs = entity.createdAtMs
                    )
                }
        }
    }

    suspend fun getOwnerProfileSnapshot(
        currentUserId: String,
        subTab: OwnerProfileSubTab
    ): OwnerProfileSnapshot = withContext(Dispatchers.IO) {
        val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
        ensureOwnerMediaAndSubscribersInitialized(cleanUserId)
        val profile = dao.getProfileByUserId(cleanUserId)
        val followersCount = dao.countFollowers(cleanUserId)
        val followingCount = dao.countFollowing(cleanUserId)
        val subscribersCount = dao.countSubscribers(cleanUserId)
        val rawPosts = dao.getPostsForUser(cleanUserId).filter { it.postCategory == subTab.name }
        val mappedPosts = rawPosts.map { entity ->
            UserPostItem(
                postId = entity.postId,
                userId = entity.userId,
                mediaDrawableRes = resolvePostDrawableRes(entity.mediaThemeKey),
                caption = entity.caption,
                durationLabel = entity.durationLabel,
                subTabCategory = runCatching { OwnerProfileSubTab.valueOf(entity.postCategory) }
                    .getOrDefault(OwnerProfileSubTab.POSTS),
                viewsCount = entity.viewsCount,
                likesCount = entity.likesCount,
                createdAtMs = entity.createdAtMs
            )
        }
        val cooldown = OwnerProfileCooldownState.compute(
            lastFullNameChangedAtMs = profile?.lastFullNameChangedAtMs,
            lastUsernameChangedAtMs = profile?.lastUsernameChangedAtMs
        )
        OwnerProfileSnapshot(
            followersCount = followersCount,
            followingCount = followingCount,
            subscribersCount = subscribersCount,
            posts = mappedPosts,
            cooldownState = cooldown,
            isOffline = !TelefamCryptoAndTelemetry.isNetworkOnline(context)
        )
    }

    /**
     * Reads the device's phone numbers and emails from Android ContactsContract (once READ_CONTACTS
     * is granted), normalizes and hashes them with SHA-256 on-device, matches them against registered
     * Telefam users in Room, and queues an encrypted background upload to `/v1/contacts/upload-hashes`.
     */
    suspend fun syncDeviceContactsAndMergeSuggestions(
        currentUserId: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
            if (!hasReadContactsPermission()) {
                throw SecurityException("Contacts permission is required to find friends from your address book.")
            }

            val phoneHashes = linkedSetOf<String>()
            val emailHashes = linkedSetOf<String>()
            val resolver = context.contentResolver

            // 1. Read and SHA-256 hash phone numbers
            runCatching {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        if (numIdx >= 0) {
                            val rawPhone = cursor.getString(numIdx) ?: continue
                            val digits = rawPhone.filter { it.isDigit() }
                            if (digits.length >= 7) {
                                phoneHashes.add(sha256String(digits))
                                phoneHashes.add(sha256String("+$digits"))
                                if (digits.length >= 9) {
                                    phoneHashes.add(sha256String(digits.takeLast(9)))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Read and SHA-256 hash email addresses
            runCatching {
                resolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    while (cursor.moveToNext()) {
                        if (emailIdx >= 0) {
                            val rawEmail = cursor.getString(emailIdx)?.trim()?.lowercase() ?: continue
                            if (rawEmail.contains("@")) {
                                emailHashes.add(sha256String(rawEmail))
                            }
                        }
                    }
                }
            }

            // 3. Match hashes against registered Telefam users & profiles
            val allUsers = dao.getAllOtherUsers(cleanUserId).associateBy { it.userId }
            val allProfiles = dao.getAllOtherProfiles(cleanUserId)

            val userHashLookup = mutableMapOf<String, String>() // sha256 -> userId
            for (profile in allProfiles) {
                val u = allUsers[profile.userId]
                if (u != null && u.email.isNotBlank()) {
                    userHashLookup[sha256String(u.email.trim().lowercase())] = profile.userId
                }
                val cleanDigits = profile.phoneNumber.filter { it.isDigit() }
                val ccDigits = profile.phoneCountryCode.filter { it.isDigit() }
                if (cleanDigits.isNotBlank()) {
                    userHashLookup[sha256String(cleanDigits)] = profile.userId
                    userHashLookup[sha256String("$ccDigits$cleanDigits")] = profile.userId
                    userHashLookup[sha256String("+$ccDigits$cleanDigits")] = profile.userId
                    if (cleanDigits.length >= 9) {
                        userHashLookup[sha256String(cleanDigits.takeLast(9))] = profile.userId
                    }
                }
            }

            val now = System.currentTimeMillis()
            val entities = mutableListOf<ContactHashEntity>()
            for (hash in phoneHashes) {
                entities.add(
                    ContactHashEntity(
                        ownerUserId = cleanUserId,
                        contactHashSha256 = hash,
                        hashType = "PHONE",
                        matchedUserId = userHashLookup[hash],
                        isPendingSync = true,
                        createdAtMs = now
                    )
                )
            }
            for (hash in emailHashes) {
                entities.add(
                    ContactHashEntity(
                        ownerUserId = cleanUserId,
                        contactHashSha256 = hash,
                        hashType = "EMAIL",
                        matchedUserId = userHashLookup[hash],
                        isPendingSync = true,
                        createdAtMs = now
                    )
                )
            }

            if (entities.isNotEmpty()) {
                dao.upsertContactHashes(entities)
            }

            // 4. Queue background upload to backend (/v1/contacts/upload-hashes)
            val idempotencyKey = sha256String(
                "contacts_upload:$cleanUserId:${phoneHashes.size}:${emailHashes.size}:${now / 60_000L}"
            )
            val payloadJson = JSONObject().apply {
                put("phoneHashes", JSONArray(phoneHashes.take(500)))
                put("emailHashes", JSONArray(emailHashes.take(500)))
            }.toString()

            dao.enqueueSocialSyncOperation(
                SocialSyncQueueEntity(
                    operationId = UUID.randomUUID().toString(),
                    userId = cleanUserId,
                    operationType = "UPLOAD_CONTACTS",
                    targetUserId = "",
                    idempotencyKey = idempotencyKey,
                    payloadJson = payloadJson,
                    internalRiskScore = telemetry.riskScore,
                    createdAtMs = now
                )
            )
            TelefamPrivacySyncWorker.enqueueSync(context)

            if (TelefamCryptoAndTelemetry.isNetworkOnline(context)) {
                runCatching {
                    val synced = TelefamNetworkClient.api.uploadContactHashes(
                        rlsUserId = cleanUserId,
                        deviceFp = telemetry.untrustedClientDeviceFingerprint,
                        payload = TelefamContactHashesUploadPayload(
                            userId = cleanUserId,
                            phoneHashesSha256 = phoneHashes.take(500),
                            emailHashesSha256 = emailHashes.take(500),
                            clientTimestampMs = now
                        )
                    ).synced
                    if (synced) {
                        dao.markContactHashesSynced(cleanUserId)
                    }
                }
            }

            entities.size
        }
    }

    /**
     * Queries the local encrypted Room database for discoverable registered users, supporting:
     * - Contacts hash matches + 2nd-degree mutual connections graph ranking
     * - Automatic fallback to other registered users if 0 contacts match
     * - Keyword search by full name, @username, bio, or location/country
     * - Filter mode (`SUGGESTED` vs `ALL`)
     * - Pagination (`page`, `pageSize`) for smooth lazy loading
     */
    suspend fun queryDiscoverableUsers(
        currentUserId: String,
        searchQuery: String,
        filterMode: ContactsFilterMode,
        page: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): ContactsDiscoverySnapshot = withContext(Dispatchers.IO) {
        val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
        val isOnline = TelefamCryptoAndTelemetry.isNetworkOnline(context)

        val currentProfile = dao.getProfileByUserId(cleanUserId)
        val allOtherProfiles = dao.getAllOtherProfiles(cleanUserId)
        val allUsersById = dao.getAllOtherUsers(cleanUserId).associateBy { it.userId }
        val blockedIds = dao.getBlockedUsers(cleanUserId).map { it.blockedUserId }.toSet()

        val allEdges = dao.getAllFollowEdges()
        val myFollowingIds = allEdges.asSequence()
            .filter { it.followerUserId == cleanUserId }
            .map { it.targetUserId }
            .toSet()
        val myFollowerIds = allEdges.asSequence()
            .filter { it.targetUserId == cleanUserId }
            .map { it.followerUserId }
            .toSet()

        // Precompute followers & following counts per user from graph
        val followersCountMap = mutableMapOf<String, Long>()
        val followingCountMap = mutableMapOf<String, Long>()
        val userConnectionsMap = mutableMapOf<String, MutableSet<String>>()
        for (edge in allEdges) {
            followersCountMap[edge.targetUserId] = (followersCountMap[edge.targetUserId] ?: 0L) + 1L
            followingCountMap[edge.followerUserId] = (followingCountMap[edge.followerUserId] ?: 0L) + 1L
            userConnectionsMap.getOrPut(edge.followerUserId) { mutableSetOf() }.add(edge.targetUserId)
            userConnectionsMap.getOrPut(edge.targetUserId) { mutableSetOf() }.add(edge.followerUserId)
        }

        val myNetworkPeers = myFollowingIds union myFollowerIds
        val contactHashes = dao.getContactHashesForOwner(cleanUserId)
        val matchedContactUserIds = contactHashes.mapNotNull { it.matchedUserId }.toSet()

        val normalizedQuery = searchQuery.trim().removePrefix("@").lowercase()

        val fullList = allOtherProfiles
            .filter { it.userId !in blockedIds }
            .map { profile ->
                val userRecord = allUsersById[profile.userId]
                val country = TelefamCountries.findByIso(profile.phoneIsoCode)
                val locationLabel = userRecord?.lastKnownLocation?.takeIf { it.isNotBlank() }
                    ?: country?.name
                    ?: "Global"

                val peerConnections = userConnectionsMap[profile.userId].orEmpty() - cleanUserId
                val graphOverlap = (peerConnections intersect myNetworkPeers).size
                val mutualCount = if (graphOverlap > 0) {
                    graphOverlap
                } else {
                    // Social graph 2nd-degree connectedness score within the community
                    peerConnections.size
                }

                val iFollowThem = profile.userId in myFollowingIds
                val theyFollowMe = profile.userId in myFollowerIds
                val followState = FollowRelationState.resolve(
                    iFollowThem = iFollowThem,
                    theyFollowMe = theyFollowMe
                )

                DiscoverableUserItem(
                    userId = profile.userId,
                    fullName = profile.fullName,
                    username = profile.username,
                    bio = profile.bio,
                    locationLabel = locationLabel,
                    countryIsoCode = profile.phoneIsoCode,
                    avatarLocalPath = profile.avatarLocalPath,
                    avatarCdnUrl = profile.avatarCdnUrl,
                    mutualConnectionsCount = mutualCount,
                    isMatchedFromContacts = profile.userId in matchedContactUserIds,
                    followState = followState,
                    followersCount = followersCountMap[profile.userId] ?: 0L,
                    followingCount = followingCountMap[profile.userId] ?: 0L,
                    likesCount = dao.getTotalLikesForUser(profile.userId),
                    isBlocked = false
                )
            }
            .filter { item ->
                if (normalizedQuery.isEmpty()) {
                    true
                } else {
                    val countryName = TelefamCountries.findByIso(item.countryIsoCode)?.name?.lowercase().orEmpty()
                    item.fullName.lowercase().contains(normalizedQuery) ||
                        item.username.lowercase().contains(normalizedQuery) ||
                        item.bio.lowercase().contains(normalizedQuery) ||
                        item.locationLabel.lowercase().contains(normalizedQuery) ||
                        countryName.contains(normalizedQuery)
                }
            }
            .toList()

        // Sort according to filterMode:
        // In SUGGESTED mode:
        // 1. Prioritize users matched from uploaded device contact hashes
        // 2. Prioritize users who already follow me (FOLLOW_BACK) or aren't followed yet
        // 3. Sort by mutual connections count DESC, same country as current user, then name
        // Note: If no contacts matched, all other registered users are shown and refreshed daily!
        val sortedList = when (filterMode) {
            ContactsFilterMode.SUGGESTED -> {
                fullList
                    .filter { !it.followState.isFollowing }
                    .ifEmpty { fullList }
                    .sortedWith(
                        compareByDescending<DiscoverableUserItem> { it.isMatchedFromContacts }
                            .thenByDescending { it.followState == FollowRelationState.FOLLOW_BACK }
                            .thenByDescending {
                                currentProfile != null &&
                                    it.countryIsoCode.equals(currentProfile.phoneIsoCode, ignoreCase = true)
                            }
                            .thenByDescending { it.mutualConnectionsCount }
                            .thenBy { it.fullName.lowercase() }
                    )
            }
            ContactsFilterMode.ALL -> {
                fullList.sortedWith(
                    compareByDescending<DiscoverableUserItem> { it.followState == FollowRelationState.FRIENDS }
                        .thenByDescending { it.followState == FollowRelationState.FOLLOW_BACK }
                        .thenByDescending { it.followState == FollowRelationState.FOLLOWING }
                        .thenBy { it.fullName.lowercase() }
                )
            }
            ContactsFilterMode.FOLLOWERS -> {
                fullList
                    .filter { it.userId in myFollowerIds }
                    .sortedWith(
                        compareByDescending<DiscoverableUserItem> { it.followState == FollowRelationState.FOLLOW_BACK }
                            .thenBy { it.fullName.lowercase() }
                    )
            }
            ContactsFilterMode.FOLLOWING -> {
                fullList
                    .filter { it.userId in myFollowingIds }
                    .sortedWith(
                        compareByDescending<DiscoverableUserItem> { it.followState == FollowRelationState.FRIENDS }
                            .thenBy { it.fullName.lowercase() }
                    )
            }
            ContactsFilterMode.FRIENDS -> {
                fullList
                    .filter { it.followState == FollowRelationState.FRIENDS }
                    .sortedBy { it.fullName.lowercase() }
            }
        }

        val safePage = page.coerceAtLeast(1)
        val limit = safePage * pageSize
        val pagedItems = sortedList.take(limit)

        ContactsDiscoverySnapshot(
            items = pagedItems,
            totalMatchingCount = sortedList.size,
            hasMorePages = sortedList.size > pagedItems.size,
            currentPage = safePage,
            hasGrantedContactsPermission = hasReadContactsPermission(),
            syncedContactsHashesCount = contactHashes.size,
            matchedFromContactsCount = matchedContactUserIds.size,
            lastDailyRefreshAtMs = getLastDailyRefreshMs(cleanUserId).takeIf { it > 0L } ?: System.currentTimeMillis(),
            isOffline = !isOnline
        )
    }

    /**
     * Toggles Follow / Unfollow between `currentUserId` and `targetUserId` with:
     * 1. Strict Idempotency Key verification (prevents duplicate mutations on retry/double-tap)
     * 2. Per-user Follow Velocity Rate Limiting (`telefam_rate_limits`)
     * 3. Hidden Internal Risk Score computation & logging (stored in Room & sent to backend, never shown in UI)
     * 4. Offline-first Room persistence + silent WorkManager background sync retry
     */
    suspend fun toggleFollowUser(
        currentUserId: String,
        targetUserId: String,
        telemetry: DeviceAndNetworkTelemetry,
        explicitIdempotencyKey: String? = null
    ): Result<FollowRelationState> = withContext(Dispatchers.IO) {
        val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
        if (cleanUserId == targetUserId) {
            return@withContext Result.failure(IllegalArgumentException("You cannot follow your own account."))
        }

        val now = System.currentTimeMillis()

        // 1. Check Follow Rate Limit Bucket
        val rateBucketKey = "follow_rate:$cleanUserId"
        val existingBucket = dao.getRateLimitBucket(rateBucketKey)
        if (existingBucket?.cooldownUntilMs != null && existingBucket.cooldownUntilMs > now) {
            val waitSec = ((existingBucket.cooldownUntilMs - now) / 1000L).coerceAtLeast(1L)
            return@withContext Result.failure(
                IllegalStateException("Action rate limit reached. Please wait ${waitSec}s before following more people.")
            )
        }

        val withinWindow = existingBucket != null && (now - existingBucket.updatedAtMs) <= FOLLOW_RATE_WINDOW_MS
        val updatedCount = if (withinWindow) existingBucket!!.failedCount + 1 else 1
        if (updatedCount > MAX_FOLLOW_ACTIONS_PER_MINUTE) {
            dao.upsertRateLimitBucket(
                RateLimitBucketEntity(
                    bucketKey = rateBucketKey,
                    actionType = "FOLLOW_VELOCITY",
                    failedCount = updatedCount,
                    cooldownUntilMs = now + FOLLOW_COOLDOWN_MS,
                    updatedAtMs = now
                )
            )
            return@withContext Result.failure(
                IllegalStateException("You are following people too quickly. Please wait a moment and try again.")
            )
        }

        dao.upsertRateLimitBucket(
            RateLimitBucketEntity(
                bucketKey = rateBucketKey,
                actionType = "FOLLOW_VELOCITY",
                failedCount = updatedCount,
                cooldownUntilMs = null,
                updatedAtMs = if (withinWindow) existingBucket!!.updatedAtMs else now
            )
        )

        // 2. Compute hidden internal behavioural risk score (never displayed in UI)
        val velocityRisk = (updatedCount * 3).coerceAtMost(45)
        val internalRiskScore = (telemetry.riskScore + velocityRisk).coerceIn(0, 100)

        // 3. Determine current relationship & desired action
        val existingMyEdge = dao.getFollowEdge(cleanUserId, targetUserId)
        val theyFollowMe = dao.getFollowEdge(targetUserId, cleanUserId) != null
        val isCurrentlyFollowing = existingMyEdge != null
        val targetAction = if (isCurrentlyFollowing) "UNFOLLOW" else "FOLLOW"

        val idempotencyKey = explicitIdempotencyKey ?: sha256String(
            "$cleanUserId:$targetUserId:$targetAction:${now / 1500L}"
        )

        // Idempotency guard: if this exact follow idempotency key already exists, return current state
        if (targetAction == "FOLLOW") {
            val duplicateEdge = dao.getFollowEdgeByIdempotencyKey(idempotencyKey)
            if (duplicateEdge != null) {
                return@withContext Result.success(
                    FollowRelationState.resolve(iFollowThem = true, theyFollowMe = theyFollowMe)
                )
            }
        }

        // 4. Commit to local encrypted Room database immediately
        val newRelationState = if (targetAction == "FOLLOW") {
            dao.upsertFollowEdge(
                FollowEdgeEntity(
                    followerUserId = cleanUserId,
                    targetUserId = targetUserId,
                    idempotencyKey = idempotencyKey,
                    internalRiskScore = internalRiskScore,
                    isPendingSync = true,
                    createdAtMs = now
                )
            )
            FollowRelationState.resolve(iFollowThem = true, theyFollowMe = theyFollowMe)
        } else {
            dao.deleteFollowEdge(cleanUserId, targetUserId)
            FollowRelationState.resolve(iFollowThem = false, theyFollowMe = theyFollowMe)
        }

        // 5. Enqueue in durable outbox for silent background retry & log internal audit
        val payloadJson = JSONObject().apply {
            put("followerUserId", cleanUserId)
            put("targetUserId", targetUserId)
            put("action", targetAction)
            put("idempotencyKey", idempotencyKey)
            put("internalRiskScore", internalRiskScore)
        }.toString()

        dao.enqueueSocialSyncOperation(
            SocialSyncQueueEntity(
                operationId = UUID.randomUUID().toString(),
                userId = cleanUserId,
                operationType = targetAction,
                targetUserId = targetUserId,
                idempotencyKey = idempotencyKey,
                payloadJson = payloadJson,
                internalRiskScore = internalRiskScore,
                createdAtMs = now
            )
        )

        dao.insertForensicAuditLog(
            ForensicAuditLogEntity(
                auditId = UUID.randomUUID().toString(),
                timestampMs = now,
                eventCategory = "SOCIAL_$targetAction",
                eventOutcome = "SUCCESS_IDEMPOTENT",
                targetIdentifier = targetUserId,
                clientIp = telemetry.clientIpAddress,
                ipSubnetFp = telemetry.ipSubnetFingerprint,
                untrustedClientFp = telemetry.untrustedClientDeviceFingerprint,
                serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                locationLabel = telemetry.locationLabel,
                riskScore = internalRiskScore,
                riskSignalsCsv = telemetry.riskSignals.joinToString(", "),
                endpointPath = "/v1/social/follow",
                forensicDetail = "Idempotent $targetAction -> ${newRelationState.name} (key=${idempotencyKey.take(12)})"
            )
        )

        TelefamPrivacySyncWorker.enqueueSync(context)
        Result.success(newRelationState)
    }

    /**
     * Loads the full public profile, relationship status, follower/following/likes counts,
     * location/country, and sorted posts for `targetUserId`.
     */
    suspend fun getPublicUserProfile(
        currentUserId: String,
        targetUserId: String,
        postsFilter: PostsFilterOption
    ): PublicProfileSnapshot? = withContext(Dispatchers.IO) {
        val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
        val profile = dao.getProfileByUserId(targetUserId) ?: return@withContext null
        val userRecord = dao.getUserById(targetUserId)
        val country = TelefamCountries.findByIso(profile.phoneIsoCode)
        val locationLabel = userRecord?.lastKnownLocation?.takeIf { it.isNotBlank() }
            ?: country?.name
            ?: "Global"

        val iFollowThem = dao.getFollowEdge(cleanUserId, targetUserId) != null
        val theyFollowMe = dao.getFollowEdge(targetUserId, cleanUserId) != null
        val isBlocked = dao.getBlockRecord(cleanUserId, targetUserId) != null

        val followersCount = dao.countFollowers(targetUserId)
        val followingCount = dao.countFollowing(targetUserId)
        val rawPosts = dao.getPostsForUser(targetUserId)
        val totalLikes = rawPosts.sumOf { it.likesCount }

        val sortedPosts = when (postsFilter) {
            PostsFilterOption.ALL_POSTS -> rawPosts.sortedByDescending { it.createdAtMs }
            PostsFilterOption.MOST_VIEWED -> rawPosts.sortedByDescending { it.viewsCount }
            PostsFilterOption.LATEST -> rawPosts.sortedByDescending { it.createdAtMs }
        }.map { entity ->
            UserPostItem(
                postId = entity.postId,
                userId = entity.userId,
                mediaDrawableRes = resolvePostDrawableRes(entity.mediaThemeKey),
                caption = entity.caption,
                durationLabel = entity.durationLabel,
                subTabCategory = runCatching { OwnerProfileSubTab.valueOf(entity.postCategory) }
                    .getOrDefault(OwnerProfileSubTab.POSTS),
                viewsCount = entity.viewsCount,
                likesCount = entity.likesCount,
                createdAtMs = entity.createdAtMs
            )
        }

        val discoverable = DiscoverableUserItem(
            userId = profile.userId,
            fullName = profile.fullName,
            username = profile.username,
            bio = profile.bio,
            locationLabel = locationLabel,
            countryIsoCode = profile.phoneIsoCode,
            avatarLocalPath = profile.avatarLocalPath,
            avatarCdnUrl = profile.avatarCdnUrl,
            mutualConnectionsCount = 0,
            isMatchedFromContacts = false,
            followState = FollowRelationState.resolve(iFollowThem = iFollowThem, theyFollowMe = theyFollowMe),
            followersCount = followersCount,
            followingCount = followingCount,
            likesCount = totalLikes,
            isBlocked = isBlocked
        )

        PublicProfileSnapshot(
            profile = discoverable,
            posts = sortedPosts,
            isOffline = !TelefamCryptoAndTelemetry.isNetworkOnline(context)
        )
    }

    /**
     * Returns paginated Followers or Following users for `profileUserId`,
     * along with each user's relationship state relative to `currentUserId`
     * so `currentUserId` can follow/follow-back/unfollow directly inside the list.
     */
    suspend fun getPaginatedFollowList(
        currentUserId: String,
        profileUserId: String,
        listType: FollowListType,
        page: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Pair<List<DiscoverableUserItem>, Boolean> = withContext(Dispatchers.IO) {
        val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
        val targetIds = when (listType) {
            FollowListType.FOLLOWING -> dao.getFollowingEdgesForUser(profileUserId).map { it.targetUserId }
            FollowListType.FOLLOWERS -> dao.getFollowerEdgesForUser(profileUserId).map { it.followerUserId }
            FollowListType.SUBSCRIBERS -> dao.getSubscribersForCreator(profileUserId).map { it.subscriberUserId }
        }.distinct()

        val blockedByMe = dao.getBlockedUsers(cleanUserId).map { it.blockedUserId }.toSet()
        val myFollowingIds = dao.getFollowingEdgesForUser(cleanUserId).map { it.targetUserId }.toSet()
        val myFollowerIds = dao.getFollowerEdgesForUser(cleanUserId).map { it.followerUserId }.toSet()
        val allUsersById = dao.getAllOtherUsers("").associateBy { it.userId }

        val items = targetIds
            .filter { it !in blockedByMe }
            .mapNotNull { uid ->
                val p = dao.getProfileByUserId(uid) ?: return@mapNotNull null
                val u = allUsersById[uid]
                val country = TelefamCountries.findByIso(p.phoneIsoCode)
                val locationLabel = u?.lastKnownLocation?.takeIf { it.isNotBlank() }
                    ?: country?.name
                    ?: "Global"

                val followState = if (uid == cleanUserId) {
                    FollowRelationState.FRIENDS
                } else {
                    FollowRelationState.resolve(
                        iFollowThem = uid in myFollowingIds,
                        theyFollowMe = uid in myFollowerIds
                    )
                }

                DiscoverableUserItem(
                    userId = p.userId,
                    fullName = p.fullName,
                    username = p.username,
                    bio = p.bio,
                    locationLabel = locationLabel,
                    countryIsoCode = p.phoneIsoCode,
                    avatarLocalPath = p.avatarLocalPath,
                    avatarCdnUrl = p.avatarCdnUrl,
                    followState = followState
                )
            }

        val safePage = page.coerceAtLeast(1)
        val limit = safePage * pageSize
        val paged = items.take(limit)
        val hasMore = items.size > paged.size
        paged to hasMore
    }

    /**
     * Blocks or unblocks a user, persists to local encrypted Room (`telefam_user_blocks`),
     * severs any follow edges between the users when blocking, and syncs silently in background.
     */
    suspend fun setBlockUserState(
        currentUserId: String,
        targetUserId: String,
        block: Boolean,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
            val now = System.currentTimeMillis()
            val action = if (block) "BLOCK" else "UNBLOCK"
            val idempotencyKey = sha256String(
                "$cleanUserId:$targetUserId:$action:${now / 2000L}"
            )

            if (block) {
                dao.upsertUserBlock(
                    UserBlockEntity(
                        blockerUserId = cleanUserId,
                        blockedUserId = targetUserId,
                        idempotencyKey = idempotencyKey,
                        isPendingSync = true,
                        createdAtMs = now
                    )
                )
                dao.deleteAllFollowEdgesBetween(cleanUserId, targetUserId)
            } else {
                dao.deleteUserBlock(cleanUserId, targetUserId)
            }

            val payloadJson = JSONObject().apply {
                put("blockerUserId", cleanUserId)
                put("blockedUserId", targetUserId)
                put("action", action)
                put("idempotencyKey", idempotencyKey)
            }.toString()

            dao.enqueueSocialSyncOperation(
                SocialSyncQueueEntity(
                    operationId = UUID.randomUUID().toString(),
                    userId = cleanUserId,
                    operationType = action,
                    targetUserId = targetUserId,
                    idempotencyKey = idempotencyKey,
                    payloadJson = payloadJson,
                    internalRiskScore = telemetry.riskScore,
                    createdAtMs = now
                )
            )
            TelefamPrivacySyncWorker.enqueueSync(context)
            block
        }
    }

    /**
     * Submits a multi-question profile report, stores it in local encrypted Room (`telefam_user_reports`),
     * optionally blocks/mutes if `HIDE_AND_MUTE` urgency is chosen, and syncs silently in the background.
     */
    suspend fun submitProfileReport(
        currentUserId: String,
        reportedUserId: String,
        reason: ReportReasonOption,
        location: ReportLocationOption,
        urgency: ReportUrgencyOption,
        additionalNotes: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUserId = currentUserId.ifBlank { "local_telefam_member" }
            val now = System.currentTimeMillis()
            val cleanNotes = additionalNotes.trim().take(300)
            val idempotencyKey = sha256String(
                "report:$cleanUserId:$reportedUserId:${reason.wireCode}:${location.wireCode}:${now / 5000L}"
            )

            val existing = dao.getReportByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                return@runCatching existing.reportId
            }

            val reportId = UUID.randomUUID().toString()
            val entity = UserReportEntity(
                reportId = reportId,
                reporterUserId = cleanUserId,
                reportedUserId = reportedUserId,
                idempotencyKey = idempotencyKey,
                reasonCode = reason.wireCode,
                locationCode = location.wireCode,
                urgencyCode = urgency.wireCode,
                additionalNotes = cleanNotes,
                internalRiskScore = telemetry.riskScore,
                isPendingSync = true,
                createdAtMs = now
            )
            dao.insertUserReport(entity)

            if (urgency == ReportUrgencyOption.HIDE_AND_MUTE) {
                setBlockUserState(
                    currentUserId = cleanUserId,
                    targetUserId = reportedUserId,
                    block = true,
                    telemetry = telemetry
                )
            }

            val payloadJson = JSONObject().apply {
                put("reportId", reportId)
                put("reporterUserId", cleanUserId)
                put("reportedUserId", reportedUserId)
                put("reasonCode", reason.wireCode)
                put("locationCode", location.wireCode)
                put("urgencyCode", urgency.wireCode)
                put("additionalNotes", cleanNotes)
            }.toString()

            dao.enqueueSocialSyncOperation(
                SocialSyncQueueEntity(
                    operationId = UUID.randomUUID().toString(),
                    userId = cleanUserId,
                    operationType = "REPORT",
                    targetUserId = reportedUserId,
                    idempotencyKey = idempotencyKey,
                    payloadJson = payloadJson,
                    internalRiskScore = telemetry.riskScore,
                    createdAtMs = now
                )
            )
            TelefamPrivacySyncWorker.enqueueSync(context)
            reportId
        }
    }

    private fun resolvePostDrawableRes(themeKey: String): Int = when (themeKey) {
        "crimson_silk" -> R.drawable.bg_chat_theme_crimson_silk_1791535312731
        "midnight_aurora" -> R.drawable.bg_chat_theme_aurora_cosmos_1791535334698
        "sahara_dunes" -> R.drawable.bg_chat_theme_sahara_twilight_1791535356607
        "ocean_bioluminescence" -> R.drawable.bg_chat_theme_alpine_mist_1791535368092
        "emerald_canopy" -> R.drawable.bg_chat_theme_emerald_botanical_1791535344448
        "amethyst_nebula" -> R.drawable.bg_chat_theme_savannah_sunset_1791535323502
        else -> R.drawable.bg_chat_theme_crimson_silk_1791535312731
    }
}
