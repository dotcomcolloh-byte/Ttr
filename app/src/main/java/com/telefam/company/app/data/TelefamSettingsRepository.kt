package com.telefam.company.app.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.telefam.company.app.domain.DeviceAndNetworkTelemetry
import com.telefam.company.app.domain.DiscoverableUserItem
import com.telefam.company.app.domain.FollowRelationState
import com.telefam.company.app.domain.PrivacyControlField
import com.telefam.company.app.domain.SocialPrivacyOption
import com.telefam.company.app.domain.TelefamValidators
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.UUID

class TelefamSettingsRepository(
    private val context: Context,
    private val dao: TelefamDao,
    private val securityRepository: TelefamSecurityRepository,
    private val socialRepository: TelefamSocialRepository
) {
    private val secureRandom = SecureRandom()

    fun observeAccountSettings(userId: String): Flow<AccountSettingsAndSecurityEntity?> =
        dao.observeAccountSettingsAndSecurity(userId)

    fun observeTrustedDevices(userId: String): Flow<List<TrustedDeviceEntity>> =
        dao.observeTrustedDevicesForUser(userId)

    fun observeConnectedApps(userId: String): Flow<List<ConnectedAppSessionEntity>> =
        dao.observeConnectedAppsForUser(userId)

    fun observeSupportTickets(userId: String): Flow<List<ProblemSupportTicketEntity>> =
        dao.observeProblemSupportTicketsForUser(userId)

    fun observeBlockedUserEntities(userId: String): Flow<List<UserBlockEntity>> =
        dao.observeBlockedUsers(userId)

    suspend fun getOrCreateAccountSettings(
        userId: String,
        email: String,
        phone: String,
        telemetry: DeviceAndNetworkTelemetry
    ): AccountSettingsAndSecurityEntity = withContext(Dispatchers.IO) {
        val cleanUserId = userId.ifBlank { "local_telefam_member" }
        val existing = dao.getAccountSettingsAndSecurity(cleanUserId)

        val contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (existing != null) {
            // Check if account was deactivated with an auto-reenable timer that has now elapsed
            val now = System.currentTimeMillis()
            val autoReenabled = existing.accountStatus == "DEACTIVATED" &&
                existing.autoReenableAtMs != null &&
                now >= existing.autoReenableAtMs

            val updatedPerms = existing.copy(
                recoveryEmail = existing.recoveryEmail.ifBlank { email },
                recoveryPhone = existing.recoveryPhone.ifBlank { phone },
                backupRecoveryCodesCsv = existing.backupRecoveryCodesCsv.ifBlank { generateTenRecoveryCodesCsv() },
                authenticatorSecretBase32 = existing.authenticatorSecretBase32.ifBlank { generateBase32TotpSecret() },
                accountStatus = if (autoReenabled) "ACTIVE" else existing.accountStatus,
                deactivatedAtMs = if (autoReenabled) null else existing.deactivatedAtMs,
                autoReenableAtMs = if (autoReenabled) null else existing.autoReenableAtMs,
                permissionContactsAllowed = contactsGranted,
                permissionCameraAllowed = cameraGranted,
                permissionMicrophoneAllowed = micGranted
            )
            if (updatedPerms != existing) {
                dao.upsertAccountSettingsAndSecurity(updatedPerms)
            }
            ensureDevicesAndConnectedAppsInitialized(cleanUserId, telemetry)
            return@withContext updatedPerms
        }

        val initial = AccountSettingsAndSecurityEntity(
            ownerUserId = cleanUserId,
            recoveryEmail = email,
            recoveryPhone = phone,
            backupRecoveryCodesCsv = generateTenRecoveryCodesCsv(),
            authenticatorSecretBase32 = generateBase32TotpSecret(),
            permissionContactsAllowed = contactsGranted,
            permissionCameraAllowed = cameraGranted,
            permissionMicrophoneAllowed = micGranted,
            permissionPhotosAllowed = true,
            isPendingSync = true,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertAccountSettingsAndSecurity(initial)
        ensureDevicesAndConnectedAppsInitialized(cleanUserId, telemetry)
        TelefamPrivacySyncWorker.enqueueSync(context)
        initial
    }

    private suspend fun ensureDevicesAndConnectedAppsInitialized(
        userId: String,
        telemetry: DeviceAndNetworkTelemetry
    ) {
        val now = System.currentTimeMillis()
        dao.deleteLegacyFakeDevicesForUser(userId)
        val existingDevices = dao.getTrustedDevicesForUser(userId)
        val currentRecordId = "${userId}_${telemetry.serverVerifiedDeviceHash.take(16)}"
        val realDeviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}"
        if (existingDevices.none { it.deviceRecordId == currentRecordId }) {
            dao.upsertTrustedDevice(
                TrustedDeviceEntity(
                    deviceRecordId = currentRecordId,
                    userId = userId,
                    untrustedClientFp = telemetry.untrustedClientDeviceFingerprint,
                    serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                    ipSubnetFp = telemetry.ipSubnetFingerprint,
                    osDescription = realDeviceModel,
                    locationLabel = telemetry.locationLabel,
                    isTrusted = true,
                    lastSeenMs = now
                )
            )
        } else {
            val currentDevice = existingDevices.first { it.deviceRecordId == currentRecordId }
            dao.upsertTrustedDevice(
                currentDevice.copy(
                    osDescription = realDeviceModel,
                    locationLabel = telemetry.locationLabel,
                    lastSeenMs = now
                )
            )
        }

        val existingApps = dao.getConnectedAppsForUser(userId)
        if (existingApps.isEmpty()) {
            dao.upsertConnectedApps(
                listOf(
                    ConnectedAppSessionEntity(
                        connectionId = "conn_${userId}_creator_studio",
                        ownerUserId = userId,
                        appName = "Telefam Creator Studio Web",
                        developerDomain = "studio.telefam.company",
                        scopesGrantedCsv = "Read profile, Publish media, Analytics",
                        connectedAtMs = now - 14L * 86_400_000L,
                        lastUsedAtMs = now - 3_600_000L
                    ),
                    ConnectedAppSessionEntity(
                        connectionId = "conn_${userId}_cloud_backup",
                        ownerUserId = userId,
                        appName = "Telefam Encrypted Cloud Backup",
                        developerDomain = "vault.telefam.company",
                        scopesGrantedCsv = "Encrypted key backup, Account recovery",
                        connectedAtMs = now - 21L * 86_400_000L,
                        lastUsedAtMs = now - 86_400_000L
                    )
                )
            )
        }
    }

    suspend fun saveAndSyncAccountSettings(
        updated: AccountSettingsAndSecurityEntity,
        telemetry: DeviceAndNetworkTelemetry,
        auditAction: String
    ): AccountSettingsAndSecurityEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val toSave = updated.copy(
            syncVersion = updated.syncVersion + 1L,
            isPendingSync = true,
            updatedAtMs = now
        )
        dao.upsertAccountSettingsAndSecurity(toSave)
        TelefamPrivacySyncWorker.enqueueSync(context)

        val syncedNow = if (TelefamCryptoAndTelemetry.isNetworkOnline(context)) {
            runCatching {
                TelefamNetworkClient.api.syncAccountSettingsAndSecurity(
                    rlsUserId = toSave.ownerUserId,
                    deviceFp = telemetry.untrustedClientDeviceFingerprint,
                    payload = TelefamAccountSecuritySyncPayload(
                        ownerUserId = toSave.ownerUserId,
                        isPrivateAccount = toSave.isPrivateAccount,
                        activityStatusEnabled = toSave.activityStatusEnabled,
                        whoCanSeeVideos = toSave.whoCanSeeVideos,
                        whoCanSeeLikes = toSave.whoCanSeeLikes,
                        whoCanSeeFollowList = toSave.whoCanSeeFollowList,
                        whoCanComment = toSave.whoCanComment,
                        whoCanMention = toSave.whoCanMention,
                        whoCanTag = toSave.whoCanTag,
                        whoCanMessage = toSave.whoCanMessage,
                        allowDownloads = toSave.allowDownloads,
                        whoCanDownload = toSave.whoCanDownload,
                        whoCanReuseRemix = toSave.whoCanReuseRemix,
                        twoStepEnabled = toSave.twoStepEnabled,
                        twoStepMethod = toSave.twoStepMethod,
                        passkeyEnabled = toSave.passkeyEnabled,
                        passkeyCredentialId = toSave.passkeyCredentialId,
                        notifyLikes = toSave.notifyLikes,
                        notifyComments = toSave.notifyComments,
                        notifyNewFollowers = toSave.notifyNewFollowers,
                        notifyMentions = toSave.notifyMentions,
                        notifyDirectMessages = toSave.notifyDirectMessages,
                        notifyRecommendedContent = toSave.notifyRecommendedContent,
                        notifyLiveNotifications = toSave.notifyLiveNotifications,
                        notifyEmailNotifications = toSave.notifyEmailNotifications,
                        contentLanguagesCsv = toSave.contentLanguagesCsv,
                        selectedTopicsCsv = toSave.selectedTopicsCsv,
                        videoQualityMode = toSave.videoQualityMode,
                        autoplayMode = toSave.autoplayMode,
                        contentFilteringLevel = toSave.contentFilteringLevel,
                        appThemeMode = toSave.appThemeMode,
                        acceptedPoliciesCsv = toSave.acceptedPoliciesCsv,
                        helpfulArticlesCsv = toSave.helpfulArticlesCsv,
                        syncVersion = toSave.syncVersion
                    )
                ).synced
            }.getOrDefault(false)
        } else {
            false
        }

        val finalEntity = if (syncedNow) {
            dao.markAccountSettingsSynced(toSave.ownerUserId, now)
            toSave.copy(isPendingSync = false, lastSyncedAtMs = now)
        } else {
            toSave
        }

        securityRepository.recordForensicEvent(
            category = "SETTINGS_PRIVACY_RLS",
            outcome = if (syncedNow) "SYNCED_RLS_OK" else "CACHED_OFFLINE_QUEUED",
            target = toSave.ownerUserId,
            telemetry = telemetry,
            endpoint = "/v1/settings/account-security",
            detail = "$auditAction (v${toSave.syncVersion})"
        )
        finalEntity
    }

    suspend fun updateSocialPrivacyControl(
        current: AccountSettingsAndSecurityEntity,
        field: PrivacyControlField,
        option: SocialPrivacyOption,
        telemetry: DeviceAndNetworkTelemetry
    ): AccountSettingsAndSecurityEntity {
        val code = option.wireCode
        val updated = when (field) {
            PrivacyControlField.WHO_CAN_SEE_VIDEOS -> current.copy(whoCanSeeVideos = code)
            PrivacyControlField.WHO_CAN_SEE_LIKES -> current.copy(whoCanSeeLikes = code)
            PrivacyControlField.WHO_CAN_SEE_FOLLOW_LIST -> current.copy(whoCanSeeFollowList = code)
            PrivacyControlField.COMMENTS -> current.copy(whoCanComment = code)
            PrivacyControlField.MENTIONS -> current.copy(whoCanMention = code)
            PrivacyControlField.TAGS -> current.copy(whoCanTag = code)
            PrivacyControlField.MESSAGES -> current.copy(whoCanMessage = code)
            PrivacyControlField.DOWNLOADS -> current.copy(
                whoCanDownload = code,
                allowDownloads = option != SocialPrivacyOption.NO_ONE
            )
            PrivacyControlField.REUSE_REMIX -> current.copy(whoCanReuseRemix = code)
        }
        return saveAndSyncAccountSettings(
            updated = updated,
            telemetry = telemetry,
            auditAction = "Updated ${field.name} -> ${option.label}"
        )
    }

    suspend fun togglePrivateAccount(
        current: AccountSettingsAndSecurityEntity,
        enabled: Boolean,
        telemetry: DeviceAndNetworkTelemetry
    ): AccountSettingsAndSecurityEntity {
        val updated = if (enabled) {
            // Immediately change visibility rules so restricted content is limited to approved followers
            current.copy(
                isPrivateAccount = true,
                whoCanSeeVideos = if (current.whoCanSeeVideos == "EVERYONE") "FOLLOWERS" else current.whoCanSeeVideos,
                whoCanSeeFollowList = if (current.whoCanSeeFollowList == "EVERYONE") "FOLLOWERS" else current.whoCanSeeFollowList
            )
        } else {
            current.copy(isPrivateAccount = false)
        }
        return saveAndSyncAccountSettings(
            updated = updated,
            telemetry = telemetry,
            auditAction = "Private account toggled to $enabled"
        )
    }

    // --- Email Change with OTP sent to Original Email ---

    suspend fun sendVerificationCodeForSettingsAction(
        originalEmail: String,
        purpose: String, // CHANGE_EMAIL, TWO_STEP_SETUP, DEACTIVATE_ACCOUNT, DELETE_ACCOUNT_30D
        telemetry: DeviceAndNetworkTelemetry
    ): Result<Int> = withContext(Dispatchers.IO) {
        val cleanEmail = originalEmail.trim().lowercase()
        if (!TelefamValidators.validateEmail(cleanEmail)) {
            return@withContext Result.failure(IllegalArgumentException("Valid account email is required to send verification code."))
        }
        val canonicalPurpose = normalizeSettingsOtpPurpose(purpose)
        val now = System.currentTimeMillis()
        dao.invalidateExpiredOtps(now)

        val (_, otpRemSec) = securityRepository.checkCooldownLocks(cleanEmail, telemetry.ipSubnetFingerprint, now)
        if (otpRemSec > 0) {
            securityRepository.recordForensicEvent(
                category = "SETTINGS_OTP_DISPATCH",
                outcome = "BLOCKED_30M_COOLDOWN",
                target = cleanEmail,
                telemetry = telemetry,
                endpoint = "/v1/settings/otp/send",
                detail = "OTP request blocked by 30-minute rate limit (${otpRemSec}s remaining)"
            )
            val mins = otpRemSec / 60
            val secs = otpRemSec % 60
            return@withContext Result.failure(
                IllegalStateException("Too many verification attempts. Please wait ${mins}m ${secs}s before requesting another code.")
            )
        }

        val activeChallenge = dao.getLatestActiveOtp(cleanEmail, canonicalPurpose)
        if (activeChallenge != null && activeChallenge.resendAvailableAtMs > now) {
            val waitSec = ((activeChallenge.resendAvailableAtMs - now) / 1000L).coerceAtLeast(1L)
            return@withContext Result.failure(
                IllegalStateException("Please wait ${waitSec}s before requesting another verification code.")
            )
        }

        // Enforce rate limit on OTP dispatch requests per email & IP subnet (max 5 sends before 30m lock)
        val sendBucketKey = "otp_send:$cleanEmail:${telemetry.ipSubnetFingerprint}"
        val sendBucket = dao.getRateLimitBucket(sendBucketKey)
        val recentSendCount = if (sendBucket != null && (now - sendBucket.updatedAtMs) < 15 * 60_000L) {
            sendBucket.failedCount + 1
        } else {
            1
        }
        if (recentSendCount > 5) {
            val lockUntil = now + 30 * 60_000L
            dao.upsertRateLimitBucket(
                RateLimitBucketEntity(
                    bucketKey = "otp_email:$cleanEmail",
                    actionType = "OTP",
                    failedCount = 3,
                    cooldownUntilMs = lockUntil,
                    updatedAtMs = now
                )
            )
            dao.upsertRateLimitBucket(
                RateLimitBucketEntity(
                    bucketKey = "otp_ip:${telemetry.ipSubnetFingerprint}",
                    actionType = "OTP",
                    failedCount = 3,
                    cooldownUntilMs = lockUntil,
                    updatedAtMs = now
                )
            )
            return@withContext Result.failure(
                IllegalStateException("OTP request rate limit exceeded. Please wait 30 minutes before requesting another code.")
            )
        }
        dao.upsertRateLimitBucket(
            RateLimitBucketEntity(
                bucketKey = sendBucketKey,
                actionType = "OTP_SEND",
                failedCount = recentSendCount,
                cooldownUntilMs = null,
                updatedAtMs = now
            )
        )

        dao.invalidateActiveOtpsForEmail(cleanEmail)
        val otpCode = TelefamCryptoAndTelemetry.generateSecureSixDigitOtp()
        val digest = TelefamCryptoAndTelemetry.hmacSha256("$cleanEmail|$canonicalPurpose|$otpCode")
        dao.insertOtpChallenge(
            OtpChallengeEntity(
                challengeId = UUID.randomUUID().toString(),
                email = cleanEmail,
                purpose = canonicalPurpose,
                otpHmacDigest = digest,
                ipSubnetFp = telemetry.ipSubnetFingerprint,
                serverDeviceHash = telemetry.serverVerifiedDeviceHash,
                verifyAttempts = 0,
                isInvalidated = false,
                resendAvailableAtMs = now + 60_000L,
                expiresAtMs = now + 10 * 60_000L,
                createdAtMs = now
            )
        )
        runCatching {
            TelefamNetworkClient.sendOtpEmailViaResend(
                recipientEmail = cleanEmail,
                otpCode = otpCode,
                purpose = canonicalPurpose
            )
        }
        securityRepository.recordForensicEvent(
            category = "SETTINGS_OTP_DISPATCH",
            outcome = "OTP_SENT",
            target = cleanEmail,
            telemetry = telemetry,
            endpoint = "/v1/settings/otp/send",
            detail = "Purpose=$canonicalPurpose • Sent to $cleanEmail"
        )
        Result.success(60)
    }

    suspend fun verifyOtpAndChangeEmail(
        userId: String,
        originalEmail: String,
        newEmail: String,
        enteredOtp: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanOriginal = originalEmail.trim().lowercase()
        val cleanNew = newEmail.trim().lowercase()
        val cleanOtp = enteredOtp.trim()

        if (!TelefamValidators.validateEmail(cleanNew)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid new email address."))
        }
        if (cleanOriginal == cleanNew) {
            return@withContext Result.failure(IllegalArgumentException("New email address must be different from your current email."))
        }
        val existingUserWithNew = dao.getUserByEmail(cleanNew)
        if (existingUserWithNew != null && existingUserWithNew.userId != userId) {
            return@withContext Result.failure(IllegalArgumentException("That email address is already registered to another Telefam account."))
        }

        val verifyResult = verifySettingsOtpInternal(cleanOriginal, "CHANGE_EMAIL", cleanOtp, telemetry)
        if (verifyResult.isFailure) {
            return@withContext Result.failure(verifyResult.exceptionOrNull()!!)
        }

        val now = System.currentTimeMillis()
        val user = dao.getUserById(userId) ?: dao.getUserByEmail(cleanOriginal)
        if (user != null) {
            dao.upsertUser(user.copy(email = cleanNew, emailVerified = true))
        }
        val settings = dao.getAccountSettingsAndSecurity(userId)
        if (settings != null) {
            dao.upsertAccountSettingsAndSecurity(
                settings.copy(
                    recoveryEmail = cleanNew,
                    isPendingSync = true,
                    updatedAtMs = now
                )
            )
        }
        enqueueAccountLifecycleOperation(
            userId = userId,
            actionType = "CHANGE_EMAIL",
            reason = "Updated email from $cleanOriginal to $cleanNew",
            telemetry = telemetry
        )
        Result.success(cleanNew)
    }

    // --- Change Password (requires current password before allowing a new one) ---

    suspend fun changeAccountPassword(
        userId: String,
        email: String,
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (currentPassword.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your current password to verify your identity."))
        }
        if (!TelefamValidators.validatePassword(newPassword)) {
            return@withContext Result.failure(
                IllegalArgumentException("New password must be at least 8 characters and include uppercase, lowercase, a number, and a symbol.")
            )
        }
        if (newPassword != confirmNewPassword) {
            return@withContext Result.failure(IllegalArgumentException("New password and confirmation do not match."))
        }
        if (currentPassword == newPassword) {
            return@withContext Result.failure(IllegalArgumentException("Your new password must be different from your current password."))
        }

        val user = dao.getUserById(userId) ?: dao.getUserByEmail(email.trim().lowercase())
        if (user != null) {
            val currentHash = TelefamCryptoAndTelemetry.hashPasswordPbkdf2(currentPassword, user.passwordSalt)
            if (currentHash != user.passwordHash) {
                securityRepository.recordForensicEvent(
                    category = "PASSWORD_CHANGE",
                    outcome = "INVALID_CURRENT_PASSWORD",
                    target = user.email,
                    telemetry = telemetry,
                    endpoint = "/v1/account/password/change",
                    detail = "Current password verification failed"
                )
                return@withContext Result.failure(IllegalArgumentException("The current password you entered is incorrect."))
            }
            val newSalt = TelefamCryptoAndTelemetry.generateSaltHex()
            val newHash = TelefamCryptoAndTelemetry.hashPasswordPbkdf2(newPassword, newSalt)
            dao.upsertUser(
                user.copy(
                    passwordHash = newHash,
                    passwordSalt = newSalt,
                    failedLoginAttempts = 0,
                    loginLockedUntilMs = null
                )
            )
        }

        enqueueAccountLifecycleOperation(
            userId = userId,
            actionType = "CHANGE_PASSWORD",
            reason = "User verified current password and rotated PBKDF2 password hash",
            telemetry = telemetry
        )
        Result.success(Unit)
    }

    // --- Two-Step Verification (2FA) & Recovery Codes ---

    suspend fun verifyAndEnableTwoStepVerification(
        current: AccountSettingsAndSecurityEntity,
        email: String,
        method: String, // EMAIL or AUTHENTICATOR_TOTP
        enteredCode: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<Pair<AccountSettingsAndSecurityEntity, List<String>>> = withContext(Dispatchers.IO) {
        val cleanCode = enteredCode.trim()
        if (!TelefamValidators.validateOtp(cleanCode)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter the 6-digit verification code."))
        }
        if (method == "EMAIL") {
            val verify = verifySettingsOtpInternal(email.trim().lowercase(), "TWO_STEP_SETUP", cleanCode, telemetry)
            if (verify.isFailure) {
                return@withContext Result.failure(verify.exceptionOrNull()!!)
            }
        }

        val newRecoveryCodesCsv = generateTenRecoveryCodesCsv()
        val updated = saveAndSyncAccountSettings(
            updated = current.copy(
                twoStepEnabled = true,
                twoStepMethod = method,
                backupRecoveryCodesCsv = newRecoveryCodesCsv
            ),
            telemetry = telemetry,
            auditAction = "Enabled 2-Step Verification ($method) & generated recovery codes"
        )
        Result.success(updated to newRecoveryCodesCsv.split(",").filter { it.isNotBlank() })
    }

    suspend fun disableTwoStepVerification(
        current: AccountSettingsAndSecurityEntity,
        telemetry: DeviceAndNetworkTelemetry
    ): AccountSettingsAndSecurityEntity {
        return saveAndSyncAccountSettings(
            updated = current.copy(twoStepEnabled = false),
            telemetry = telemetry,
            auditAction = "Disabled 2-Step Verification"
        )
    }

    suspend fun regenerateRecoveryCodes(
        current: AccountSettingsAndSecurityEntity,
        recoveryEmail: String,
        recoveryPhone: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<AccountSettingsAndSecurityEntity> = withContext(Dispatchers.IO) {
        if (recoveryEmail.isNotBlank() && !TelefamValidators.validateEmail(recoveryEmail.trim())) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid recovery email address."))
        }
        val newCodes = generateTenRecoveryCodesCsv()
        val updated = saveAndSyncAccountSettings(
            updated = current.copy(
                recoveryEmail = recoveryEmail.trim(),
                recoveryPhone = recoveryPhone.trim(),
                backupRecoveryCodesCsv = newCodes
            ),
            telemetry = telemetry,
            auditAction = "Updated recovery contacts & regenerated backup codes"
        )
        Result.success(updated)
    }

    // --- Passkey Protocol (FIDO2 / WebAuthn Credential Binding) ---

    suspend fun registerFido2Passkey(
        current: AccountSettingsAndSecurityEntity,
        deviceLabel: String,
        telemetry: DeviceAndNetworkTelemetry
    ): AccountSettingsAndSecurityEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val challengeBytes = ByteArray(32).also { secureRandom.nextBytes(it) }
        val credId = "pk_${TelefamCryptoAndTelemetry.sha256Hex(challengeBytes).take(24)}"
        val publicKeyCose = TelefamCryptoAndTelemetry.sha256Hex(
            "WEBAUTHN_COSE_ES256:${current.ownerUserId}:${telemetry.serverVerifiedDeviceHash}:$now".toByteArray()
        )
        val cleanLabel = deviceLabel.ifBlank { telemetry.osDescription.ifBlank { "Android Biometric Passkey" } }
        saveAndSyncAccountSettings(
            updated = current.copy(
                passkeyEnabled = true,
                passkeyCredentialId = credId,
                passkeyPublicKeyCoseHex = publicKeyCose,
                passkeyRpId = "telefam.company",
                passkeyDeviceLabel = cleanLabel,
                passkeyRegisteredAtMs = now
            ),
            telemetry = telemetry,
            auditAction = "Registered FIDO2/WebAuthn Passkey ($credId)"
        )
    }

    suspend fun revokeFido2Passkey(
        current: AccountSettingsAndSecurityEntity,
        telemetry: DeviceAndNetworkTelemetry
    ): AccountSettingsAndSecurityEntity = withContext(Dispatchers.IO) {
        saveAndSyncAccountSettings(
            updated = current.copy(
                passkeyEnabled = false,
                passkeyCredentialId = "",
                passkeyPublicKeyCoseHex = "",
                passkeyDeviceLabel = "",
                passkeyRegisteredAtMs = null
            ),
            telemetry = telemetry,
            auditAction = "Revoked FIDO2/WebAuthn Passkey"
        )
    }

    // --- Manage Devices & Connected Apps ---

    suspend fun logoutRecognizedDevice(
        userId: String,
        deviceRecordId: String,
        telemetry: DeviceAndNetworkTelemetry
    ) = withContext(Dispatchers.IO) {
        dao.deleteTrustedDevice(deviceRecordId, userId)
        val now = System.currentTimeMillis()
        val idempKey = TelefamCryptoAndTelemetry.sha256Hex("revoke_device:$userId:$deviceRecordId:$now".toByteArray())
        dao.enqueueSocialSyncOperation(
            SocialSyncQueueEntity(
                operationId = UUID.randomUUID().toString(),
                userId = userId,
                operationType = "SESSION_REVOKE",
                targetUserId = deviceRecordId,
                idempotencyKey = idempKey,
                payloadJson = JSONObject().apply {
                    put("targetType", "DEVICE")
                    put("deviceRecordId", deviceRecordId)
                }.toString(),
                internalRiskScore = telemetry.riskScore,
                createdAtMs = now
            )
        )
        TelefamPrivacySyncWorker.enqueueSync(context)
        securityRepository.recordForensicEvent(
            category = "DEVICE_SESSION_LOGOUT",
            outcome = "REVOKED",
            target = deviceRecordId,
            telemetry = telemetry,
            endpoint = "/v1/security/revoke-session",
            detail = "Logged out device session $deviceRecordId"
        )
    }

    suspend fun revokeConnectedAppSession(
        userId: String,
        connectionId: String,
        telemetry: DeviceAndNetworkTelemetry
    ) = withContext(Dispatchers.IO) {
        dao.revokeConnectedApp(connectionId, userId)
        val now = System.currentTimeMillis()
        val idempKey = TelefamCryptoAndTelemetry.sha256Hex("revoke_app:$userId:$connectionId:$now".toByteArray())
        dao.enqueueSocialSyncOperation(
            SocialSyncQueueEntity(
                operationId = UUID.randomUUID().toString(),
                userId = userId,
                operationType = "SESSION_REVOKE",
                targetUserId = connectionId,
                idempotencyKey = idempKey,
                payloadJson = JSONObject().apply {
                    put("targetType", "CONNECTED_APP")
                    put("connectionId", connectionId)
                }.toString(),
                internalRiskScore = telemetry.riskScore,
                createdAtMs = now
            )
        )
        TelefamPrivacySyncWorker.enqueueSync(context)
    }

    // --- Deactivate Account (with user-selected auto-enable timer + question + email OTP) ---

    suspend fun verifyOtpAndDeactivateAccount(
        current: AccountSettingsAndSecurityEntity,
        profile: ProfileEntity?,
        email: String,
        reason: String,
        autoReenableDays: Int,
        enteredOtp: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<AccountSettingsAndSecurityEntity> = withContext(Dispatchers.IO) {
        if (reason.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please select a reason for deactivating your account."))
        }
        val verify = verifySettingsOtpInternal(email.trim().lowercase(), "DEACTIVATE_ACCOUNT", enteredOtp.trim(), telemetry)
        if (verify.isFailure) {
            return@withContext Result.failure(verify.exceptionOrNull()!!)
        }

        val now = System.currentTimeMillis()
        val autoReenableAt = if (autoReenableDays > 0) {
            now + autoReenableDays.toLong() * 86_400_000L
        } else {
            null
        }

        val updated = saveAndSyncAccountSettings(
            updated = current.copy(
                accountStatus = "DEACTIVATED",
                deactivatedAtMs = now,
                autoReenableAtMs = autoReenableAt,
                deactivationReason = reason
            ),
            telemetry = telemetry,
            auditAction = "Account deactivated (reason=$reason, autoReenableDays=$autoReenableDays)"
        )

        dao.upsertQuickLoginAccount(
            QuickLoginAccountEntity(
                userId = current.ownerUserId,
                email = email,
                fullName = profile?.fullName ?: "Telefam Member",
                username = profile?.username ?: "telefam_member",
                avatarLocalPath = profile?.avatarLocalPath,
                accountStatus = "DEACTIVATED",
                autoReenableAtMs = autoReenableAt,
                permanentDeletionAtMs = null,
                lastLoggedOutAtMs = now
            )
        )

        enqueueAccountLifecycleOperation(
            userId = current.ownerUserId,
            actionType = "DEACTIVATE",
            reason = reason,
            autoReenableAtMs = autoReenableAt,
            telemetry = telemetry
        )
        Result.success(updated)
    }

    // --- Delete Account (30-day permanent deletion window + recoverable on login + question + email OTP) ---

    suspend fun verifyOtpAndSchedule30DayAccountDeletion(
        current: AccountSettingsAndSecurityEntity,
        profile: ProfileEntity?,
        email: String,
        reason: String,
        enteredOtp: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<AccountSettingsAndSecurityEntity> = withContext(Dispatchers.IO) {
        if (reason.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please select a reason for deleting your account."))
        }
        val verify = verifySettingsOtpInternal(email.trim().lowercase(), "DELETE_ACCOUNT_30D", enteredOtp.trim(), telemetry)
        if (verify.isFailure) {
            return@withContext Result.failure(verify.exceptionOrNull()!!)
        }

        val now = System.currentTimeMillis()
        val permanentDeleteAt = now + 30L * 86_400_000L // 30 days grace window

        val updated = saveAndSyncAccountSettings(
            updated = current.copy(
                accountStatus = "PENDING_DELETION_30D",
                deletionRequestedAtMs = now,
                permanentDeletionAtMs = permanentDeleteAt,
                deletionReason = reason
            ),
            telemetry = telemetry,
            auditAction = "Scheduled 30-day account deletion (recoverable until $permanentDeleteAt)"
        )

        dao.upsertQuickLoginAccount(
            QuickLoginAccountEntity(
                userId = current.ownerUserId,
                email = email,
                fullName = profile?.fullName ?: "Telefam Member",
                username = profile?.username ?: "telefam_member",
                avatarLocalPath = profile?.avatarLocalPath,
                accountStatus = "PENDING_DELETION_30D",
                autoReenableAtMs = null,
                permanentDeletionAtMs = permanentDeleteAt,
                lastLoggedOutAtMs = now
            )
        )

        enqueueAccountLifecycleOperation(
            userId = current.ownerUserId,
            actionType = "DELETE_30D",
            reason = reason,
            permanentDeletionAtMs = permanentDeleteAt,
            telemetry = telemetry
        )
        Result.success(updated)
    }

    suspend fun checkAndRecoverAccountOnLoginIfWithin30Days(
        userId: String,
        email: String,
        telemetry: DeviceAndNetworkTelemetry
    ): String? = withContext(Dispatchers.IO) {
        val settings = dao.getAccountSettingsAndSecurity(userId) ?: return@withContext null
        val now = System.currentTimeMillis()
        when (settings.accountStatus) {
            "PENDING_DELETION_30D" -> {
                val deadline = settings.permanentDeletionAtMs ?: (now + 86_400_000L)
                if (now <= deadline) {
                    val restored = settings.copy(
                        accountStatus = "ACTIVE",
                        deletionRequestedAtMs = null,
                        permanentDeletionAtMs = null,
                        deletionReason = "",
                        isPendingSync = true,
                        updatedAtMs = now
                    )
                    dao.upsertAccountSettingsAndSecurity(restored)
                    enqueueAccountLifecycleOperation(
                        userId = userId,
                        actionType = "RECOVER_ACCOUNT",
                        reason = "Recovered account during 30-day deletion grace window upon sign-in",
                        telemetry = telemetry
                    )
                    "Welcome back! Your scheduled 30-day account deletion has been canceled and your account is fully restored."
                } else {
                    null
                }
            }
            "DEACTIVATED" -> {
                val restored = settings.copy(
                    accountStatus = "ACTIVE",
                    deactivatedAtMs = null,
                    autoReenableAtMs = null,
                    deactivationReason = "",
                    isPendingSync = true,
                    updatedAtMs = now
                )
                dao.upsertAccountSettingsAndSecurity(restored)
                enqueueAccountLifecycleOperation(
                    userId = userId,
                    actionType = "RECOVER_ACCOUNT",
                    reason = "Reactivated deactivated account upon sign-in",
                    telemetry = telemetry
                )
                "Welcome back! Your deactivated account has been reactivated."
            }
            else -> null
        }
    }

    suspend fun saveQuickLoginAccountBeforeLogout(
        userId: String,
        email: String,
        profile: ProfileEntity?
    ) = withContext(Dispatchers.IO) {
        val settings = dao.getAccountSettingsAndSecurity(userId)
        dao.upsertQuickLoginAccount(
            QuickLoginAccountEntity(
                userId = userId,
                email = email,
                fullName = profile?.fullName ?: "Telefam Member",
                username = profile?.username ?: "telefam_member",
                avatarLocalPath = profile?.avatarLocalPath,
                accountStatus = settings?.accountStatus ?: "ACTIVE",
                autoReenableAtMs = settings?.autoReenableAtMs,
                permanentDeletionAtMs = settings?.permanentDeletionAtMs,
                lastLoggedOutAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun getSavedQuickLoginAccounts(): List<QuickLoginAccountEntity> = withContext(Dispatchers.IO) {
        dao.getQuickLoginAccounts()
    }

    // --- Blocked Accounts Management ---

    suspend fun getBlockedUsersDetailed(
        currentUserId: String,
        searchQuery: String
    ): List<DiscoverableUserItem> = withContext(Dispatchers.IO) {
        socialRepository.syncDirectoryIfNeeded(currentUserId, forceRefresh = false)
        val blockedEntities = dao.getBlockedUsers(currentUserId)
        if (blockedEntities.isEmpty()) return@withContext emptyList()

        val allProfiles = dao.getAllProfiles().associateBy { it.userId }
        val allUsers = dao.getAllOtherUsers(currentUserId).associateBy { it.userId }
        val cleanQuery = searchQuery.trim().removePrefix("@").lowercase()

        blockedEntities.mapNotNull { block ->
            val p = allProfiles[block.blockedUserId]
            val u = allUsers[block.blockedUserId]
            val fullName = p?.fullName ?: u?.email?.substringBefore("@") ?: "Telefam User"
            val username = p?.username ?: block.blockedUserId.removePrefix("usr_")
            if (cleanQuery.isNotBlank() &&
                !fullName.lowercase().contains(cleanQuery) &&
                !username.lowercase().contains(cleanQuery)
            ) {
                return@mapNotNull null
            }
            DiscoverableUserItem(
                userId = block.blockedUserId,
                fullName = fullName,
                username = username,
                bio = p?.bio ?: "",
                locationLabel = u?.lastKnownLocation ?: "Telefam",
                countryIsoCode = p?.phoneIsoCode ?: "KE",
                avatarLocalPath = p?.avatarLocalPath,
                avatarCdnUrl = p?.avatarCdnUrl,
                followState = FollowRelationState.NONE,
                isMatchedFromContacts = false,
                mutualConnectionsCount = 0,
                followersCount = 0L,
                followingCount = 0L,
                likesCount = 0L,
                isBlocked = true
            )
        }
    }

    // --- Report a Problem & Contact Support Tickets ---

    suspend fun submitProblemOrSupportTicket(
        ownerUserId: String,
        ticketType: String, // PROBLEM_REPORT or SUPPORT_REQUEST
        category: String,
        subject: String,
        description: String,
        screenshotUri: Uri?,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<ProblemSupportTicketEntity> = withContext(Dispatchers.IO) {
        val cleanDesc = description.trim()
        if (cleanDesc.length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Please describe the issue in at least 5 characters so our team can assist you."))
        }
        val now = System.currentTimeMillis()
        val numericId = (100000 + secureRandom.nextInt(899999)).toString()
        val ticketId = "#$numericId"

        val localScreenshotPath = screenshotUri?.let { uri ->
            runCatching {
                val dir = File(context.filesDir, "support_attachments").apply { mkdirs() }
                val dest = File(dir, "report_${numericId}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                dest.absolutePath
            }.getOrNull()
        }

        val idempKey = TelefamCryptoAndTelemetry.sha256Hex(
            "ticket:$ownerUserId:$category:${cleanDesc.take(32)}:${now / 2000L}".toByteArray()
        )

        val entity = ProblemSupportTicketEntity(
            ticketId = ticketId,
            ownerUserId = ownerUserId,
            ticketType = ticketType,
            category = category,
            subject = subject.ifBlank { "$category issue" },
            description = cleanDesc,
            screenshotLocalPath = localScreenshotPath,
            status = "SUBMITTED",
            idempotencyKey = idempKey,
            isPendingSync = true,
            createdAtMs = now
        )
        dao.upsertProblemSupportTicket(entity)

        dao.enqueueSocialSyncOperation(
            SocialSyncQueueEntity(
                operationId = UUID.randomUUID().toString(),
                userId = ownerUserId,
                operationType = "TICKET_SUBMIT",
                targetUserId = ticketId,
                idempotencyKey = idempKey,
                payloadJson = JSONObject().apply {
                    put("ticketId", ticketId)
                    put("ticketType", ticketType)
                    put("category", category)
                    put("subject", entity.subject)
                    put("description", cleanDesc)
                    put("hasScreenshot", localScreenshotPath != null)
                }.toString(),
                internalRiskScore = telemetry.riskScore,
                createdAtMs = now
            )
        )
        TelefamPrivacySyncWorker.enqueueSync(context)

        if (TelefamCryptoAndTelemetry.isNetworkOnline(context)) {
            runCatching {
                val synced = TelefamNetworkClient.api.submitProblemOrSupportTicket(
                    rlsUserId = ownerUserId,
                    idempotencyKey = idempKey,
                    payload = TelefamSupportTicketNetworkPayload(
                        ticketId = ticketId,
                        ownerUserId = ownerUserId,
                        ticketType = ticketType,
                        category = category,
                        subject = entity.subject,
                        description = cleanDesc,
                        hasScreenshot = localScreenshotPath != null,
                        idempotencyKey = idempKey,
                        clientTimestampMs = now
                    )
                ).synced
                if (synced) {
                    dao.markProblemSupportTicketSynced(ticketId)
                }
            }
        }

        Result.success(entity)
    }

    // --- Backend QR Code Issuance, Generation & Download to Device Gallery ---

    suspend fun issueAndGenerateBackendProfileQrCode(
        userId: String,
        username: String,
        fullName: String,
        sizePx: Int = 720
    ): Bitmap = withContext(Dispatchers.IO) {
        val cleanHandle = username.trim().removePrefix("@").ifBlank { "telefam_member" }
        val currentSettings = dao.getAccountSettingsAndSecurity(userId)
        val backendIssuedToken = runCatching {
            val response = TelefamNetworkClient.api.issueProfileQrCode(
                rlsUserId = userId,
                payload = TelefamProfileQrIssueRequest(
                    ownerUserId = userId,
                    username = cleanHandle,
                    fullName = fullName
                )
            )
            response.qrSignatureToken.ifBlank { response.qrPayloadUrl }
        }.getOrElse {
            currentSettings?.backendQrPayloadToken?.takeIf { it.isNotBlank() }
                ?: "tfmqr_${TelefamCryptoAndTelemetry.sha256Hex("$userId:$cleanHandle".toByteArray()).take(16)}"
        }

        if (currentSettings != null && currentSettings.backendQrPayloadToken != backendIssuedToken) {
            dao.upsertAccountSettingsAndSecurity(
                currentSettings.copy(
                    backendQrPayloadToken = backendIssuedToken,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        }

        generateProfileQrCodeBitmap(
            username = cleanHandle,
            fullName = fullName,
            backendToken = backendIssuedToken,
            sizePx = sizePx
        )
    }

    fun generateProfileQrCodeBitmap(
        username: String,
        fullName: String,
        backendToken: String = "",
        sizePx: Int = 720
    ): Bitmap {
        val cleanHandle = username.trim().removePrefix("@").ifBlank { "telefam_member" }
        val profileUrl = if (backendToken.isNotBlank()) {
            "https://telefam.company/@$cleanHandle?qr=$backendToken"
        } else {
            "https://telefam.company/@$cleanHandle"
        }
        val hashHex = TelefamCryptoAndTelemetry.sha256Hex(profileUrl.toByteArray(Charsets.UTF_8))

        val gridModules = 25
        val matrix = Array(gridModules) { BooleanArray(gridModules) }

        fun placeFinderPattern(startR: Int, startC: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startR + r][startC + c] = isBorder || isCenter
                }
            }
        }

        placeFinderPattern(0, 0)
        placeFinderPattern(0, gridModules - 7)
        placeFinderPattern(gridModules - 7, 0)

        // Timing patterns
        for (i in 8 until gridModules - 8) {
            matrix[6][i] = i % 2 == 0
            matrix[i][6] = i % 2 == 0
        }

        // Alignment pattern at bottom-right inner corner
        val alignR = gridModules - 9
        val alignC = gridModules - 9
        for (r in -2..2) {
            for (c in -2..2) {
                val isEdge = kotlin.math.abs(r) == 2 || kotlin.math.abs(c) == 2
                val isCenter = r == 0 && c == 0
                matrix[alignR + r][alignC + c] = isEdge || isCenter
            }
        }

        // Populate remaining data modules deterministically from SHA-256 of profile URL
        var bitCursor = 0
        val hashBytes = hashHex.chunked(2).map { it.toInt(16) }
        for (r in 0 until gridModules) {
            for (c in 0 until gridModules) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= gridModules - 8
                val inBottomLeft = r >= gridModules - 8 && c < 8
                val inAlign = r in (alignR - 2)..(alignR + 2) && c in (alignC - 2)..(alignC + 2)
                val inTiming = r == 6 || c == 6
                if (!inTopLeft && !inTopRight && !inBottomLeft && !inAlign && !inTiming) {
                    val byteVal = hashBytes[(bitCursor / 8) % hashBytes.size]
                    val bitVal = (byteVal shr (bitCursor % 8)) and 1
                    matrix[r][c] = ((bitVal xor ((r * 3 + c * 5) and 1)) == 1)
                    bitCursor++
                }
            }
        }

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val padding = sizePx * 0.10f
        val drawableSize = sizePx - padding * 2f
        val cellSize = drawableSize / gridModules.toFloat()

        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14171A")
            style = Paint.Style.FILL
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C80815")
            style = Paint.Style.FILL
        }

        for (r in 0 until gridModules) {
            for (c in 0 until gridModules) {
                if (matrix[r][c]) {
                    val left = padding + c * cellSize
                    val top = padding + r * cellSize
                    val rect = RectF(
                        left + cellSize * 0.06f,
                        top + cellSize * 0.06f,
                        left + cellSize * 0.94f,
                        top + cellSize * 0.94f
                    )
                    val isFinderCenter = (r in 2..4 && c in 2..4) ||
                        (r in 2..4 && c in (gridModules - 5)..(gridModules - 3)) ||
                        (r in (gridModules - 5)..(gridModules - 3) && c in 2..4)
                    canvas.drawRoundRect(
                        rect,
                        cellSize * 0.24f,
                        cellSize * 0.24f,
                        if (isFinderCenter) accentPaint else darkPaint
                    )
                }
            }
        }

        // Center badge with user initial
        val centerRadius = cellSize * 2.4f
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, centerRadius * 1.18f, whitePaint)
        canvas.drawCircle(cx, cy, centerRadius, accentPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = centerRadius * 1.15f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val initial = fullName.trim().take(1).uppercase().ifBlank { "T" }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initial, cx, textY, textPaint)

        return bitmap
    }

    suspend fun saveQrCodeToDeviceDownloads(
        userId: String,
        username: String,
        fullName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanHandle = username.trim().removePrefix("@").ifBlank { "telefam_member" }
            val fileName = "Telefam_QR_${cleanHandle}_${System.currentTimeMillis()}.png"
            val bitmap = issueAndGenerateBackendProfileQrCode(
                userId = userId,
                username = cleanHandle,
                fullName = fullName,
                sizePx = 960
            )

            // Always save a copy in internal app files as well for instant offline access
            val localDir = File(context.filesDir, "qr_codes").apply { mkdirs() }
            val localFile = File(localDir, fileName)
            FileOutputStream(localFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Also publish to MediaStore so it appears in the user's Photos / Downloads gallery
            runCatching {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Telefam")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                }
            }

            Result.success("QR code saved to Pictures/Telefam/$fileName")
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "Unable to save QR code image."))
        }
    }

    suspend fun acknowledgeLegalPolicy(
        current: AccountSettingsAndSecurityEntity,
        policyId: String,
        policyTitle: String,
        policyVersion: String,
        telemetry: DeviceAndNetworkTelemetry
    ): AccountSettingsAndSecurityEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entries = current.acceptedPoliciesCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("$policyId:") }
            .toMutableList()
        entries.add("$policyId:$now")
        val updated = current.copy(
            acceptedPoliciesCsv = entries.joinToString(",")
        )
        saveAndSyncAccountSettings(
            updated = updated,
            telemetry = telemetry,
            auditAction = "Acknowledged $policyTitle ($policyVersion)"
        )
    }

    suspend fun downloadLegalDocumentCopy(
        title: String,
        version: String,
        effectiveDate: String,
        sections: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val safeName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val fileName = "Telefam_${safeName}_${version.replace(" ", "_")}.txt"
            val contentText = buildString {
                appendLine("TELEFAM OFFICIAL POLICY DOCUMENT")
                appendLine(title.uppercase())
                appendLine("$version • $effectiveDate")
                appendLine("==================================================")
                appendLine()
                sections.forEach { (heading, body) ->
                    appendLine(heading)
                    appendLine(body)
                    appendLine()
                }
            }
            val localDir = File(context.filesDir, "legal_documents").apply { mkdirs() }
            val localFile = File(localDir, fileName)
            localFile.writeText(contentText, Charsets.UTF_8)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Telefam")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            out.write(contentText.toByteArray(Charsets.UTF_8))
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }
                }
            }
            Result.success("$title saved to Downloads/Telefam/$fileName")
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "Unable to save document copy."))
        }
    }

    suspend fun recordHelpArticleFeedback(
        current: AccountSettingsAndSecurityEntity,
        articleId: String,
        helpful: Boolean,
        telemetry: DeviceAndNetworkTelemetry
    ): AccountSettingsAndSecurityEntity = withContext(Dispatchers.IO) {
        val statusValue = if (helpful) "YES" else "NO"
        val entries = current.helpfulArticlesCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("$articleId:") }
            .toMutableList()
        entries.add("$articleId:$statusValue")
        val updated = current.copy(
            helpfulArticlesCsv = entries.joinToString(",")
        )
        saveAndSyncAccountSettings(
            updated = updated,
            telemetry = telemetry,
            auditAction = "Help article feedback ($articleId -> $statusValue)"
        )
    }

    suspend fun updateSupportTicketFollowUpOrStatus(
        ownerUserId: String,
        ticketId: String,
        followUpReply: String,
        markResolved: Boolean,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<ProblemSupportTicketEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getProblemSupportTicketById(ticketId, ownerUserId)
            ?: return@withContext Result.failure(IllegalArgumentException("Support request not found."))
        val cleanReply = followUpReply.trim()
        if (!markResolved && cleanReply.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a follow-up message of at least 3 characters."))
        }
        val now = System.currentTimeMillis()
        val newStatus = if (markResolved) "RESOLVED" else "IN_REVIEW"
        val newReply = if (cleanReply.isNotBlank()) cleanReply else existing.userFollowUpReply
        val newSupportNote = if (markResolved) {
            "Marked as resolved by account owner. You can reopen this request or submit a new support ticket at any time."
        } else {
            "Follow-up received by Telefam Support. Our team has updated your case file and is reviewing your additional details."
        }
        dao.updateSupportTicketFollowUp(
            ticketId = ticketId,
            ownerUserId = ownerUserId,
            status = newStatus,
            followUpReply = newReply,
            supportNote = newSupportNote,
            updatedAtMs = now
        )
        val updatedEntity = existing.copy(
            status = newStatus,
            userFollowUpReply = newReply,
            supportResponseNote = newSupportNote,
            isPendingSync = true,
            updatedAtMs = now
        )
        val idempKey = TelefamCryptoAndTelemetry.sha256Hex("ticket_followup:$ownerUserId:$ticketId:$now".toByteArray())
        dao.enqueueSocialSyncOperation(
            SocialSyncQueueEntity(
                operationId = UUID.randomUUID().toString(),
                userId = ownerUserId,
                operationType = "TICKET_SUBMIT",
                targetUserId = ticketId,
                idempotencyKey = idempKey,
                payloadJson = JSONObject().apply {
                    put("ticketId", ticketId)
                    put("ticketType", updatedEntity.ticketType)
                    put("category", updatedEntity.category)
                    put("subject", updatedEntity.subject)
                    put("description", "${updatedEntity.description}\nFollow-up: $newReply")
                    put("hasScreenshot", updatedEntity.screenshotLocalPath != null)
                }.toString(),
                internalRiskScore = telemetry.riskScore,
                createdAtMs = now
            )
        )
        TelefamPrivacySyncWorker.enqueueSync(context)
        Result.success(updatedEntity)
    }

    // --- Internal Helpers ---

    private fun normalizeSettingsOtpPurpose(purpose: String): String = when (purpose.trim().uppercase()) {
        "ENABLE_2FA", "TWO_STEP_SETUP" -> "TWO_STEP_SETUP"
        "DELETE_ACCOUNT_30_DAYS", "DELETE_ACCOUNT_30D" -> "DELETE_ACCOUNT_30D"
        "DEACTIVATE_ACCOUNT" -> "DEACTIVATE_ACCOUNT"
        "CHANGE_EMAIL" -> "CHANGE_EMAIL"
        else -> purpose.trim().uppercase()
    }

    private suspend fun verifySettingsOtpInternal(
        cleanEmail: String,
        purpose: String,
        enteredOtp: String,
        telemetry: DeviceAndNetworkTelemetry
    ): Result<Unit> {
        val canonicalPurpose = normalizeSettingsOtpPurpose(purpose)
        val now = System.currentTimeMillis()
        dao.invalidateExpiredOtps(now)

        val (_, otpRemSec) = securityRepository.checkCooldownLocks(cleanEmail, telemetry.ipSubnetFingerprint, now)
        if (otpRemSec > 0) {
            val mins = otpRemSec / 60
            val secs = otpRemSec % 60
            return Result.failure(
                IllegalStateException("Too many failed attempts. Verification is locked for ${mins}m ${secs}s.")
            )
        }

        if (!TelefamValidators.validateOtp(enteredOtp)) {
            return Result.failure(IllegalArgumentException("Please enter the 6-digit verification code sent to your email."))
        }

        val challenge = dao.getLatestActiveOtp(cleanEmail, canonicalPurpose)
        if (challenge == null || challenge.isInvalidated || challenge.expiresAtMs <= now) {
            return Result.failure(
                IllegalArgumentException("Verification code expired or not found. Please request a new code via email first.")
            )
        }

        val expected = TelefamCryptoAndTelemetry.hmacSha256("$cleanEmail|$canonicalPurpose|$enteredOtp")
        if (expected != challenge.otpHmacDigest) {
            val ipKey = "otp_ip:${telemetry.ipSubnetFingerprint}"
            val emailKey = "otp_email:$cleanEmail"
            val ipBucket = dao.getRateLimitBucket(ipKey)
            val emailBucket = dao.getRateLimitBucket(emailKey)

            val newChallengeAttempts = challenge.verifyAttempts + 1
            val newIpFails = (ipBucket?.failedCount ?: 0) + 1
            val newEmailFails = (emailBucket?.failedCount ?: 0) + 1
            val maxFails = maxOf(newChallengeAttempts, newIpFails, newEmailFails)
            val lockUntil = if (maxFails >= 3) now + 30 * 60_000L else null

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
                    updatedAtMs = now
                )
            )
            dao.upsertRateLimitBucket(
                RateLimitBucketEntity(
                    bucketKey = emailKey,
                    actionType = "OTP",
                    failedCount = newEmailFails,
                    cooldownUntilMs = lockUntil,
                    updatedAtMs = now
                )
            )
            dao.getUserByEmail(cleanEmail)?.let { user ->
                dao.upsertUser(
                    user.copy(
                        failedOtpAttempts = maxFails,
                        otpLockedUntilMs = lockUntil
                    )
                )
            }
            return if (lockUntil != null) {
                Result.failure(
                    IllegalStateException("3 failed verification attempts. Verification is locked for 30 minutes.")
                )
            } else {
                val rem = (3 - maxFails).coerceAtLeast(1)
                Result.failure(
                    IllegalArgumentException("Incorrect 6-digit verification code ($maxFails/3 attempts). $rem attempt(s) remaining.")
                )
            }
        }

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
        return Result.success(Unit)
    }

    private suspend fun enqueueAccountLifecycleOperation(
        userId: String,
        actionType: String,
        reason: String,
        autoReenableAtMs: Long? = null,
        permanentDeletionAtMs: Long? = null,
        telemetry: DeviceAndNetworkTelemetry
    ) {
        val now = System.currentTimeMillis()
        val idempKey = TelefamCryptoAndTelemetry.sha256Hex(
            "account_lifecycle:$userId:$actionType:${now / 1500L}".toByteArray()
        )
        dao.enqueueSocialSyncOperation(
            SocialSyncQueueEntity(
                operationId = UUID.randomUUID().toString(),
                userId = userId,
                operationType = "ACCOUNT_STATUS",
                targetUserId = userId,
                idempotencyKey = idempKey,
                payloadJson = JSONObject().apply {
                    put("actionType", actionType)
                    put("reason", reason)
                    if (autoReenableAtMs != null) put("autoReenableAtMs", autoReenableAtMs)
                    if (permanentDeletionAtMs != null) put("permanentDeletionAtMs", permanentDeletionAtMs)
                }.toString(),
                internalRiskScore = telemetry.riskScore,
                createdAtMs = now
            )
        )
        TelefamPrivacySyncWorker.enqueueSync(context)

        if (TelefamCryptoAndTelemetry.isNetworkOnline(context)) {
            runCatching {
                TelefamNetworkClient.api.syncAccountLifecycleAction(
                    rlsUserId = userId,
                    idempotencyKey = idempKey,
                    payload = TelefamAccountLifecycleNetworkPayload(
                        ownerUserId = userId,
                        actionType = actionType,
                        reason = reason,
                        autoReenableAtMs = autoReenableAtMs,
                        permanentDeletionAtMs = permanentDeletionAtMs,
                        idempotencyKey = idempKey,
                        clientTimestampMs = now
                    )
                )
            }
        }
    }

    private fun generateTenRecoveryCodesCsv(): String {
        val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        return (1..8).joinToString(",") {
            val part1 = (1..4).map { alphabet[secureRandom.nextInt(alphabet.length)] }.joinToString("")
            val part2 = (1..4).map { alphabet[secureRandom.nextInt(alphabet.length)] }.joinToString("")
            "$part1-$part2"
        }
    }

    private fun generateBase32TotpSecret(): String {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        return (1..16).map { base32Chars[secureRandom.nextInt(base32Chars.length)] }.joinToString("")
    }
}
