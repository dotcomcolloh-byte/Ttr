package com.telefam.company.app.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Offline-First Silent Background Retry Worker for:
 * 1. User Privacy, Chat Background Theme, and Message Bubble Colour settings (`telefam_user_privacy_settings`)
 * 2. Social Graph Outbox (`telefam_social_sync_queue`): Follow/Unfollow (with idempotency & internal risk score),
 *    Contact SHA-256 Hashes Upload, User Block/Unblock, and Multi-Question Profile Reports.
 */
class TelefamPrivacySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = TelefamAppDatabase.getInstance(applicationContext)
            val dao = db.telefamDao()
            val pendingPrivacyList = dao.getPendingSyncPrivacySettings()
            val pendingProfilesList = dao.getPendingSyncProfiles()
            val pendingAccountSettingsList = dao.getPendingSyncAccountSettings()
            val pendingSocialQueue = dao.getPendingSocialSyncOperations()
            if (pendingPrivacyList.isEmpty() &&
                pendingProfilesList.isEmpty() &&
                pendingAccountSettingsList.isEmpty() &&
                pendingSocialQueue.isEmpty()
            ) {
                return Result.success()
            }

            if (!TelefamCryptoAndTelemetry.isNetworkOnline(applicationContext)) {
                return Result.retry()
            }

            val clientDeviceFp = TelefamCryptoAndTelemetry.collectTelemetry(
                context = applicationContext,
                screenWidthDp = 411,
                knownServerDeviceHashes = emptySet(),
                lastKnownLocation = null,
                resolvedLocationLabel = "Kenya • Africa/Nairobi",
                latitude = null,
                longitude = null,
                failedLoginAttempts = 0,
                failedOtpAttempts = 0,
                simulateNewDeviceOrLocation = false
            ).untrustedClientDeviceFingerprint

            var allSucceeded = true

            // 1. Sync pending privacy & chat theme settings
            for (entity in pendingPrivacyList) {
                val synced = runCatching {
                    val response = TelefamNetworkClient.api.syncPrivacySettings(
                        rlsUserId = entity.userId,
                        deviceFp = clientDeviceFp,
                        payload = TelefamPrivacySettingsNetworkPayload(
                            userId = entity.userId,
                            messageRequests = entity.messageRequests,
                            whoCanCallMe = entity.whoCanCallMe,
                            whoCanScreenshotChats = entity.whoCanScreenshotChats,
                            whoCanShareChats = entity.whoCanShareChats,
                            whoCanCopyMessages = entity.whoCanCopyMessages,
                            whoCanDownloadMedia = entity.whoCanDownloadMedia,
                            chatThemeId = entity.chatThemeId,
                            bubbleColorHex = entity.bubbleColorHex,
                            readReceipts = entity.readReceipts,
                            typingIndicators = entity.typingIndicators,
                            onlineIndicators = entity.onlineIndicators,
                            syncVersion = entity.syncVersion
                        )
                    )
                    response.synced
                }.getOrDefault(false)

                if (synced) {
                    dao.markPrivacySettingsSynced(
                        userId = entity.userId,
                        syncedAtMs = System.currentTimeMillis()
                    )
                } else {
                    dao.upsertPrivacySettings(
                        entity.copy(
                            syncRetryCount = entity.syncRetryCount + 1,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    )
                    allSucceeded = false
                }
            }

            // 1b. Sync pending owner profile edits
            for (profile in pendingProfilesList) {
                val idempKey = TelefamCryptoAndTelemetry.sha256Hex(
                    "profile_sync:${profile.userId}:${profile.updatedAtMs}".toByteArray(Charsets.UTF_8)
                )
                val synced = runCatching {
                    TelefamNetworkClient.api.syncOwnerProfileUpdate(
                        rlsUserId = profile.userId,
                        idempotencyKey = idempKey,
                        deviceFp = clientDeviceFp,
                        payload = TelefamOwnerProfileUpdatePayload(
                            userId = profile.userId,
                            fullName = profile.fullName,
                            username = profile.username,
                            phoneCountryCode = profile.phoneCountryCode,
                            phoneIsoCode = profile.phoneIsoCode,
                            phoneNumber = profile.phoneNumber,
                            gender = profile.gender,
                            dateOfBirth = profile.dateOfBirth,
                            bio = profile.bio,
                            avatarCdnUrl = profile.avatarCdnUrl,
                            avatarSha256 = profile.avatarSha256,
                            avatarPerceptualDHash = profile.avatarPerceptualDHash,
                            lastFullNameChangedAtMs = profile.lastFullNameChangedAtMs,
                            lastUsernameChangedAtMs = profile.lastUsernameChangedAtMs,
                            idempotencyKey = idempKey,
                            clientTimestampMs = profile.updatedAtMs
                        )
                    ).synced
                }.getOrDefault(false)

                if (synced) {
                    dao.markProfileSynced(profile.userId, System.currentTimeMillis())
                } else {
                    allSucceeded = false
                }
            }

            // 1c. Sync pending Account, Security, Privacy, Notifications & Content Preferences (RLS)
            for (acc in pendingAccountSettingsList) {
                val synced = runCatching {
                    TelefamNetworkClient.api.syncAccountSettingsAndSecurity(
                        rlsUserId = acc.ownerUserId,
                        deviceFp = clientDeviceFp,
                        payload = TelefamAccountSecuritySyncPayload(
                            ownerUserId = acc.ownerUserId,
                            isPrivateAccount = acc.isPrivateAccount,
                            activityStatusEnabled = acc.activityStatusEnabled,
                            whoCanSeeVideos = acc.whoCanSeeVideos,
                            whoCanSeeLikes = acc.whoCanSeeLikes,
                            whoCanSeeFollowList = acc.whoCanSeeFollowList,
                            whoCanComment = acc.whoCanComment,
                            whoCanMention = acc.whoCanMention,
                            whoCanTag = acc.whoCanTag,
                            whoCanMessage = acc.whoCanMessage,
                            allowDownloads = acc.allowDownloads,
                            whoCanDownload = acc.whoCanDownload,
                            whoCanReuseRemix = acc.whoCanReuseRemix,
                            twoStepEnabled = acc.twoStepEnabled,
                            twoStepMethod = acc.twoStepMethod,
                            passkeyEnabled = acc.passkeyEnabled,
                            passkeyCredentialId = acc.passkeyCredentialId,
                            notifyLikes = acc.notifyLikes,
                            notifyComments = acc.notifyComments,
                            notifyNewFollowers = acc.notifyNewFollowers,
                            notifyMentions = acc.notifyMentions,
                            notifyDirectMessages = acc.notifyDirectMessages,
                            notifyRecommendedContent = acc.notifyRecommendedContent,
                            notifyLiveNotifications = acc.notifyLiveNotifications,
                            notifyEmailNotifications = acc.notifyEmailNotifications,
                            contentLanguagesCsv = acc.contentLanguagesCsv,
                            selectedTopicsCsv = acc.selectedTopicsCsv,
                            videoQualityMode = acc.videoQualityMode,
                            autoplayMode = acc.autoplayMode,
                            contentFilteringLevel = acc.contentFilteringLevel,
                            appThemeMode = acc.appThemeMode,
                            acceptedPoliciesCsv = acc.acceptedPoliciesCsv,
                            helpfulArticlesCsv = acc.helpfulArticlesCsv,
                            syncVersion = acc.syncVersion
                        )
                    ).synced
                }.getOrDefault(false)

                if (synced) {
                    dao.markAccountSettingsSynced(acc.ownerUserId, System.currentTimeMillis())
                } else {
                    allSucceeded = false
                }
            }

            // 2. Sync pending social graph outbox operations (Follow, Unfollow, Contact Hashes, Block, Report, Profile Update)
            for (op in pendingSocialQueue) {
                val opSuccess = runCatching {
                    val json = JSONObject(op.payloadJson)
                    when (op.operationType) {
                        "FOLLOW", "UNFOLLOW" -> {
                            TelefamNetworkClient.api.syncFollowAction(
                                rlsUserId = op.userId,
                                idempotencyKey = op.idempotencyKey,
                                internalRiskScore = op.internalRiskScore.toString(),
                                payload = TelefamFollowActionNetworkPayload(
                                    followerUserId = op.userId,
                                    targetUserId = op.targetUserId,
                                    action = op.operationType,
                                    idempotencyKey = op.idempotencyKey,
                                    internalRiskScore = op.internalRiskScore,
                                    clientTimestampMs = op.createdAtMs
                                )
                            ).synced
                        }
                        "UPLOAD_CONTACTS" -> {
                            val phoneArr = json.optJSONArray("phoneHashes") ?: JSONArray()
                            val emailArr = json.optJSONArray("emailHashes") ?: JSONArray()
                            val phones = buildList {
                                for (i in 0 until phoneArr.length()) add(phoneArr.getString(i))
                            }
                            val emails = buildList {
                                for (i in 0 until emailArr.length()) add(emailArr.getString(i))
                            }
                            val synced = TelefamNetworkClient.api.uploadContactHashes(
                                rlsUserId = op.userId,
                                deviceFp = clientDeviceFp,
                                payload = TelefamContactHashesUploadPayload(
                                    userId = op.userId,
                                    phoneHashesSha256 = phones,
                                    emailHashesSha256 = emails,
                                    clientTimestampMs = op.createdAtMs
                                )
                            ).synced
                            if (synced) {
                                dao.markContactHashesSynced(op.userId)
                            }
                            synced
                        }
                        "BLOCK", "UNBLOCK" -> {
                            TelefamNetworkClient.api.syncBlockAction(
                                rlsUserId = op.userId,
                                idempotencyKey = op.idempotencyKey,
                                payload = TelefamBlockActionNetworkPayload(
                                    blockerUserId = op.userId,
                                    blockedUserId = op.targetUserId,
                                    action = op.operationType,
                                    idempotencyKey = op.idempotencyKey,
                                    clientTimestampMs = op.createdAtMs
                                )
                            ).synced
                        }
                        "REPORT" -> {
                            TelefamNetworkClient.api.syncReportSubmission(
                                rlsUserId = op.userId,
                                idempotencyKey = op.idempotencyKey,
                                internalRiskScore = op.internalRiskScore.toString(),
                                payload = TelefamReportSubmissionNetworkPayload(
                                    reportId = json.optString("reportId", op.operationId),
                                    reporterUserId = op.userId,
                                    reportedUserId = op.targetUserId,
                                    idempotencyKey = op.idempotencyKey,
                                    reasonCode = json.optString("reasonCode", "SPAM_OR_BOT"),
                                    locationCode = json.optString("locationCode", "PROFILE_DETAILS"),
                                    urgencyCode = json.optString("urgencyCode", "STANDARD_REVIEW"),
                                    additionalNotes = json.optString("additionalNotes", ""),
                                    internalRiskScore = op.internalRiskScore,
                                    clientTimestampMs = op.createdAtMs
                                )
                            ).synced
                        }
                        "PROFILE_UPDATE" -> {
                            val synced = TelefamNetworkClient.api.syncOwnerProfileUpdate(
                                rlsUserId = op.userId,
                                idempotencyKey = op.idempotencyKey,
                                deviceFp = clientDeviceFp,
                                payload = TelefamOwnerProfileUpdatePayload(
                                    userId = op.userId,
                                    fullName = json.optString("fullName", ""),
                                    username = json.optString("username", ""),
                                    phoneCountryCode = json.optString("phoneCountryCode", "+254"),
                                    phoneIsoCode = json.optString("phoneIsoCode", "KE"),
                                    phoneNumber = json.optString("phoneNumber", ""),
                                    gender = json.optString("gender", ""),
                                    dateOfBirth = json.optString("dateOfBirth", ""),
                                    bio = json.optString("bio", ""),
                                    avatarCdnUrl = json.optString("avatarCdnUrl", "").ifBlank { null },
                                    avatarSha256 = json.optString("avatarSha256", "").ifBlank { null },
                                    avatarPerceptualDHash = json.optString("avatarPerceptualDHash", "").ifBlank { null },
                                    lastFullNameChangedAtMs = json.optLong("lastFullNameChangedAtMs", 0L).takeIf { it > 0L },
                                    lastUsernameChangedAtMs = json.optLong("lastUsernameChangedAtMs", 0L).takeIf { it > 0L },
                                    idempotencyKey = op.idempotencyKey,
                                    clientTimestampMs = op.createdAtMs
                                )
                            ).synced
                            if (synced) {
                                dao.markProfileSynced(op.userId, System.currentTimeMillis())
                            }
                            synced
                        }
                        "ACCOUNT_STATUS" -> {
                            TelefamNetworkClient.api.syncAccountLifecycleAction(
                                rlsUserId = op.userId,
                                idempotencyKey = op.idempotencyKey,
                                payload = TelefamAccountLifecycleNetworkPayload(
                                    ownerUserId = op.userId,
                                    actionType = json.optString("actionType", "UPDATE"),
                                    reason = json.optString("reason", ""),
                                    autoReenableAtMs = json.optLong("autoReenableAtMs", 0L).takeIf { it > 0L },
                                    permanentDeletionAtMs = json.optLong("permanentDeletionAtMs", 0L).takeIf { it > 0L },
                                    idempotencyKey = op.idempotencyKey,
                                    clientTimestampMs = op.createdAtMs
                                )
                            ).synced
                        }
                        "TICKET_SUBMIT" -> {
                            val ticketId = json.optString("ticketId", op.operationId)
                            val synced = TelefamNetworkClient.api.submitProblemOrSupportTicket(
                                rlsUserId = op.userId,
                                idempotencyKey = op.idempotencyKey,
                                payload = TelefamSupportTicketNetworkPayload(
                                    ticketId = ticketId,
                                    ownerUserId = op.userId,
                                    ticketType = json.optString("ticketType", "PROBLEM_REPORT"),
                                    category = json.optString("category", "Account"),
                                    subject = json.optString("subject", ""),
                                    description = json.optString("description", ""),
                                    hasScreenshot = json.optBoolean("hasScreenshot", false),
                                    idempotencyKey = op.idempotencyKey,
                                    clientTimestampMs = op.createdAtMs
                                )
                            ).synced
                            if (synced) {
                                dao.markProblemSupportTicketSynced(ticketId)
                            }
                            synced
                        }
                        "SESSION_REVOKE" -> {
                            TelefamNetworkClient.api.syncDeviceOrAppRevocation(
                                rlsUserId = op.userId,
                                idempotencyKey = op.idempotencyKey,
                                payload = TelefamSessionRevokeNetworkPayload(
                                    ownerUserId = op.userId,
                                    targetRecordId = op.targetUserId,
                                    targetType = json.optString("targetType", "DEVICE"),
                                    idempotencyKey = op.idempotencyKey,
                                    clientTimestampMs = op.createdAtMs
                                )
                            ).synced
                        }
                        else -> true
                    }
                }.getOrDefault(false)

                if (opSuccess) {
                    dao.deleteSocialSyncOperation(op.operationId)
                } else {
                    dao.incrementSocialSyncRetryCount(op.operationId)
                    allSucceeded = false
                }
            }

            if (allSucceeded) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "telefam_privacy_offline_sync_worker"

        fun enqueueSync(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<TelefamPrivacySyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(
                        UNIQUE_WORK_NAME,
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        workRequest
                    )
            }
        }
    }
}
