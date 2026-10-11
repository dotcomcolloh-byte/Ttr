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
            val pendingChatOutbox = dao.getPendingChatOutboxItems()
            if (pendingPrivacyList.isEmpty() &&
                pendingProfilesList.isEmpty() &&
                pendingAccountSettingsList.isEmpty() &&
                pendingSocialQueue.isEmpty() &&
                pendingChatOutbox.isEmpty()
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

            // 3. Sync pending 1:1 Chat Outbox items (E2EE ciphertext envelopes, edits, unsends, reactions, pins, favourites, disappearing, receipts)
            for (item in pendingChatOutbox) {
                val synced = processSingleChatOutboxItem(dao, item, clientDeviceFp)
                if (!synced) {
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

        suspend fun processSingleChatOutboxItem(
            dao: TelefamDao,
            item: ChatOutboxEntity,
            clientDeviceFp: String
        ): Boolean {
            return runCatching {
                val json = JSONObject(item.payloadJson)
                val isTargetBlocked = dao.isEitherBlocked(item.ownerUserId, item.targetUserId) > 0
                val targetChat = dao.getChatById(item.chatId)
                val isTargetDeleted = targetChat?.peerIsDeleted == true

                val actionResp: TelefamChatActionResponse = when (item.actionType) {
                    "PUBLISH_PREKEYS" -> {
                        val opkArr = json.optJSONArray("oneTimePreKeys") ?: JSONArray()
                        val opks = buildList<TelefamOneTimePreKeyDto> {
                            for (i in 0 until opkArr.length()) {
                                val o = opkArr.getJSONObject(i)
                                add(
                                    TelefamOneTimePreKeyDto(
                                        preKeyId = o.optInt("prekeyId", i + 1),
                                        publicKeyBase64 = o.optString("publicKeyBase64", "")
                                    )
                                )
                            }
                        }
                        TelefamNetworkClient.api.uploadPreKeyBundle(
                            rlsUserId = item.ownerUserId,
                            payload = TelefamPreKeyUploadPayload(
                                userId = item.ownerUserId,
                                registrationId = json.optInt("registrationId", 1),
                                identityPublicKeyBase64 = json.optString("identityPublicKeyBase64", ""),
                                signedPreKeyId = json.optInt("signedPreKeyId", 1),
                                signedPreKeyPublicBase64 = json.optString("signedPreKeyPublicBase64", ""),
                                signedPreKeySignatureBase64 = json.optString("signedPreKeySignatureBase64", ""),
                                oneTimePreKeys = opks,
                                clientTimestampMs = item.createdAtMs
                            )
                        )
                    }
                    "SEND", "FORWARD" -> {
                        val attachmentBytes = json.optLong("attachmentSizeBytes", 0L)
                        if (attachmentBytes > TelefamNetworkClient.MAX_BACKEND_FILE_SIZE_BYTES) {
                            TelefamChatActionResponse(
                                synced = false,
                                status = "PAYLOAD_TOO_LARGE",
                                errorCode = "FILE_TOO_LARGE_100MB_LIMIT"
                            )
                        } else if (isTargetDeleted) {
                            TelefamChatActionResponse(synced = false, status = "USER_NOT_FOUND", errorCode = "USER_NOT_FOUND")
                        } else if (isTargetBlocked) {
                            TelefamChatActionResponse(synced = false, status = "BLOCKED", errorCode = "BLOCKED")
                        } else {
                            TelefamNetworkClient.api.sendEncryptedChatMessage(
                                rlsUserId = item.ownerUserId,
                                idempotencyKey = item.idempotencyKey,
                                payload = TelefamSendChatMessagePayload(
                                    messageId = item.messageId ?: item.outboxId,
                                    chatId = item.chatId,
                                    senderUserId = item.ownerUserId,
                                    recipientUserId = item.targetUserId,
                                    messageType = json.optString("messageType", "TEXT"),
                                    ciphertextBase64 = json.optString("ciphertextBase64", ""),
                                    e2eeHeaderJson = json.optString("ratchetHeaderJson", "{}"),
                                    replyToMessageId = json.optString("replyToMessageId").takeIf { it.isNotBlank() },
                                    isForwarded = json.optBoolean("isForwarded", item.actionType == "FORWARD"),
                                    isViewOnce = json.optBoolean("isViewOnce", false),
                                    expiresAtMs = json.optLong("expiresAtMs", 0L).takeIf { it > 0L },
                                    attachmentSizeBytes = attachmentBytes,
                                    contentCompression = "deflate-lvl9",
                                    idempotencyKey = item.idempotencyKey,
                                    clientTimestampMs = item.createdAtMs
                                )
                            )
                        }
                    }
                    "EDIT" -> {
                        TelefamNetworkClient.api.editEncryptedChatMessage(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamEditMessagePayload(
                                messageId = item.messageId ?: item.outboxId,
                                chatId = item.chatId,
                                senderUserId = item.ownerUserId,
                                recipientUserId = item.targetUserId,
                                newCiphertextBase64 = json.optString("ciphertextBase64", ""),
                                newE2eeHeaderJson = json.optString("ratchetHeaderJson", "{}"),
                                editedAtMs = item.createdAtMs,
                                idempotencyKey = item.idempotencyKey
                            )
                        )
                    }
                    "UNSEND" -> {
                        TelefamNetworkClient.api.unsendChatMessage(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamUnsendMessagePayload(
                                messageId = item.messageId ?: item.outboxId,
                                chatId = item.chatId,
                                senderUserId = item.ownerUserId,
                                recipientUserId = item.targetUserId,
                                idempotencyKey = item.idempotencyKey,
                                clientTimestampMs = item.createdAtMs
                            )
                        )
                    }
                    "DELETE" -> {
                        TelefamNetworkClient.api.deleteChatMessageForOwner(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamDeleteMessagePayload(
                                messageId = item.messageId ?: item.outboxId,
                                chatId = item.chatId,
                                ownerUserId = item.ownerUserId,
                                idempotencyKey = item.idempotencyKey,
                                clientTimestampMs = item.createdAtMs
                            )
                        )
                    }
                    "VIEW_ONCE_CONSUMED" -> {
                        TelefamNetworkClient.api.consumeViewOnceMessage(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamViewOnceConsumedPayload(
                                messageId = item.messageId ?: item.outboxId,
                                chatId = item.chatId,
                                consumerUserId = item.ownerUserId,
                                senderUserId = item.targetUserId,
                                idempotencyKey = item.idempotencyKey,
                                consumedAtMs = item.createdAtMs
                            )
                        )
                    }
                    "PIN" -> {
                        TelefamNetworkClient.api.pinChatMessage(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamPinMessagePayload(
                                chatId = item.chatId,
                                messageId = item.messageId ?: item.outboxId,
                                userId = item.ownerUserId,
                                peerUserId = item.targetUserId,
                                pinned = json.optBoolean("pinned", true),
                                idempotencyKey = item.idempotencyKey,
                                timestampMs = item.createdAtMs
                            )
                        )
                    }
                    "FAVOURITE" -> {
                        TelefamNetworkClient.api.favouriteChatMessage(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamFavouriteMessagePayload(
                                chatId = item.chatId,
                                messageId = item.messageId ?: item.outboxId,
                                userId = item.ownerUserId,
                                favourited = json.optBoolean("favourited", true),
                                idempotencyKey = item.idempotencyKey,
                                timestampMs = item.createdAtMs
                            )
                        )
                    }
                    "REACT" -> {
                        val key = json.optString("reactionKey")
                        TelefamNetworkClient.api.reactToChatMessage(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamMessageReactionPayload(
                                chatId = item.chatId,
                                messageId = item.messageId ?: item.outboxId,
                                senderUserId = item.ownerUserId,
                                recipientUserId = item.targetUserId,
                                reactionCode = if (key == "REMOVE" || key.isBlank()) null else key,
                                encryptedReactionBase64 = json.optString("ciphertextBase64", ""),
                                e2eeHeaderJson = json.optString("ratchetHeaderJson", "{}"),
                                idempotencyKey = item.idempotencyKey,
                                timestampMs = item.createdAtMs
                            )
                        )
                    }
                    "DISAPPEARING" -> {
                        TelefamNetworkClient.api.updateDisappearingSetting(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamDisappearingSettingPayload(
                                chatId = item.chatId,
                                userId = item.ownerUserId,
                                peerUserId = item.targetUserId,
                                durationSeconds = json.optLong("durationSeconds", 0L),
                                idempotencyKey = item.idempotencyKey,
                                updatedAtMs = item.createdAtMs
                            )
                        )
                    }
                    "CLEAR_CHAT" -> {
                        TelefamNetworkClient.api.clearChatHistory(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamClearChatPayload(
                                chatId = item.chatId,
                                ownerUserId = item.ownerUserId,
                                peerUserId = item.targetUserId,
                                idempotencyKey = item.idempotencyKey,
                                clearedAtMs = item.createdAtMs
                            )
                        )
                    }
                    "POLL_VOTE" -> {
                        val selArr = json.optJSONArray("selectedOptionIds") ?: JSONArray()
                        val selectedIds = buildList {
                            for (i in 0 until selArr.length()) add(selArr.optString(i))
                        }
                        TelefamNetworkClient.api.syncPollVote(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamPollVotePayload(
                                chatId = item.chatId,
                                messageId = item.messageId ?: item.outboxId,
                                pollId = json.optString("pollId", ""),
                                voterUserId = item.ownerUserId,
                                recipientUserId = item.targetUserId,
                                selectedOptionIds = selectedIds,
                                ciphertextBase64 = json.optString("ciphertextBase64", ""),
                                e2eeHeaderJson = json.optString("ratchetHeaderJson", "{}"),
                                idempotencyKey = item.idempotencyKey,
                                votedAtMs = item.createdAtMs
                            )
                        )
                    }
                    "EVENT_RSVP" -> {
                        TelefamNetworkClient.api.syncEventRsvp(
                            rlsUserId = item.ownerUserId,
                            idempotencyKey = item.idempotencyKey,
                            payload = TelefamEventRsvpPayload(
                                chatId = item.chatId,
                                messageId = item.messageId ?: item.outboxId,
                                eventId = json.optString("eventId", ""),
                                responderUserId = item.ownerUserId,
                                recipientUserId = item.targetUserId,
                                rsvpStatus = json.optString("rsvpStatus", "GOING"),
                                ciphertextBase64 = json.optString("ciphertextBase64", ""),
                                e2eeHeaderJson = json.optString("ratchetHeaderJson", "{}"),
                                idempotencyKey = item.idempotencyKey,
                                updatedAtMs = item.createdAtMs
                            )
                        )
                    }
                    else -> {
                        TelefamNetworkClient.api.reportMessageSeen(
                            rlsUserId = item.ownerUserId,
                            payload = TelefamDeliveryReceiptPayload(
                                messageId = item.messageId ?: item.outboxId,
                                chatId = item.chatId,
                                recipientUserId = item.ownerUserId,
                                status = "SEEN",
                                timestampMs = item.createdAtMs
                            )
                        )
                    }
                }

                val termCode = actionResp.errorCode
                    ?: actionResp.status.takeIf { it == "BLOCKED" || it == "USER_NOT_FOUND" }

                if (termCode != null) {
                    // Terminal server rejection (BLOCKED or USER_NOT_FOUND): stop retrying and update message/chat state
                    dao.markChatOutboxTerminalRejected(item.outboxId, termCode)
                    if (item.messageId != null) {
                        val rejectedDeliveryStatus = if (termCode == "USER_NOT_FOUND") {
                            "REJECTED_USER_NOT_FOUND"
                        } else {
                            "REJECTED_BLOCKED"
                        }
                        dao.updateMessageDeliveryStatus(
                            messageId = item.messageId,
                            status = rejectedDeliveryStatus,
                            deliveredAtMs = null,
                            seenAtMs = null
                        )
                        val chat = dao.getChatById(item.chatId)
                        if (chat != null && chat.lastMessageId == item.messageId) {
                            dao.upsertChat(
                                chat.copy(
                                    lastMessageDeliveryStatus = rejectedDeliveryStatus,
                                    peerIsDeleted = if (termCode == "USER_NOT_FOUND") true else chat.peerIsDeleted
                                )
                            )
                        }
                    }
                    true
                } else if (actionResp.synced) {
                    dao.deleteChatOutboxItem(item.outboxId)
                    if (item.actionType in listOf("SEND", "FORWARD", "EDIT") && item.messageId != null) {
                        val now = System.currentTimeMillis()
                        val msg = dao.getMessageById(item.messageId)
                        if (msg != null && msg.deliveryStatus == "SENDING") {
                            // Advance from SENDING -> SENT -> DELIVERED
                            dao.updateMessageDeliveryStatus(
                                messageId = item.messageId,
                                status = "DELIVERED",
                                deliveredAtMs = now,
                                seenAtMs = msg.seenAtMs
                            )
                            dao.upsertDeliveryReceipt(
                                DeliveryReceiptEntity(
                                    messageId = item.messageId,
                                    chatId = item.chatId,
                                    recipientUserId = item.targetUserId,
                                    receiptType = "DELIVERED",
                                    timestampMs = now
                                )
                            )
                            val chat = dao.getChatById(item.chatId)
                            if (chat != null && chat.lastMessageId == item.messageId) {
                                dao.upsertChat(
                                    chat.copy(lastMessageDeliveryStatus = "DELIVERED")
                                )
                            }
                        }
                    }
                    true
                } else {
                    dao.incrementChatOutboxRetryCount(item.outboxId)
                    false
                }
            }.getOrElse {
                dao.incrementChatOutboxRetryCount(item.outboxId)
                false
            }
        }

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
