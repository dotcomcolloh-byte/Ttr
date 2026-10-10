package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.telefam.company.app.data.AuthActionOutcome
import com.telefam.company.app.data.TelefamAppDatabase
import com.telefam.company.app.data.TelefamSecurityRepository
import com.telefam.company.app.data.TelefamSettingsRepository
import com.telefam.company.app.data.TelefamSocialRepository
import com.telefam.company.app.data.UsernameConflictException
import com.telefam.company.app.domain.AppThemeOption
import com.telefam.company.app.domain.DeviceAndNetworkTelemetry
import com.telefam.company.app.domain.PrivacyAudience
import com.telefam.company.app.domain.PrivacyRuleKey
import com.telefam.company.app.domain.RiskLevel
import com.telefam.company.app.domain.TelefamBubbleColors
import com.telefam.company.app.domain.TelefamChatThemes
import com.telefam.company.app.domain.TelefamSettingsCatalog
import com.telefam.company.app.security.AndroidSandboxMediaProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private fun sampleTelemetry(deviceHash: String = "server_dev_hash_primary"): DeviceAndNetworkTelemetry {
        return DeviceAndNetworkTelemetry(
            untrustedClientDeviceFingerprint = "untrusted_client_fp_1234567890",
            serverVerifiedDeviceHash = deviceHash,
            ipSubnetFingerprint = "ip_subnet_fp_ke_254",
            clientIpAddress = "197.248.16.42",
            osDescription = "Android 16 • Telefam Test Device",
            screenBucket = "COMPACT_PHONE",
            localeTag = "en-KE",
            locationLabel = "Kenya • Africa/Nairobi",
            latitude = -1.2921,
            longitude = 36.8219,
            riskScore = 10,
            riskLevel = RiskLevel.LOW,
            riskSignals = listOf("VERIFIED_TELEMETRY_NORMAL")
        )
    }

    @Test
    fun `verify username uniqueness suggestions optional photo and 13 plus age rule`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TelefamAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = TelefamSecurityRepository(context, db.telefamDao())
        val telemetry = sampleTelemetry()

        // 1. First user saves profile WITHOUT optional photo
        val firstSave = repo.saveProfileSetup(
            userId = "user_1",
            email = "amina.wanjiku@telefam.company",
            fullName = "Amina Wanjiku",
            username = "amina_wanjiku",
            phoneCountryCode = "+254",
            phoneIsoCode = "KE",
            phoneNumber = "712345678",
            gender = "Female",
            dateOfBirth = "1998-06-15",
            bio = "Building on Telefam",
            sanitizedAvatar = null, // Optional photo omitted
            telemetry = telemetry
        )
        assertTrue(firstSave.isSuccess)

        // 2. Second user attempting same username gets UsernameConflictException with unique suggestions
        val secondSaveCollision = repo.saveProfileSetup(
            userId = "user_2",
            email = "amina.wanjiku@telefam.company",
            fullName = "Amina Wanjiku",
            username = "amina_wanjiku",
            phoneCountryCode = "+254",
            phoneIsoCode = "KE",
            phoneNumber = "799887766",
            gender = "Female",
            dateOfBirth = "1999-04-20",
            bio = "Second Amina",
            sanitizedAvatar = null,
            telemetry = telemetry
        )
        assertTrue(secondSaveCollision.isFailure)
        val conflict = secondSaveCollision.exceptionOrNull() as? UsernameConflictException
        assertNotNull(conflict)
        assertTrue(conflict!!.suggestions.isNotEmpty())
        assertFalse(conflict.suggestions.contains("amina_wanjiku"))

        // 3. Under-13 Date of Birth is rejected
        val under13Save = repo.saveProfileSetup(
            userId = "user_3",
            email = "young@telefam.company",
            fullName = "Young User",
            username = "young_user_tf",
            phoneCountryCode = "+254",
            phoneIsoCode = "KE",
            phoneNumber = "711223344",
            gender = "Male",
            dateOfBirth = "2020-01-01",
            bio = "Too young",
            sanitizedAvatar = null,
            telemetry = telemetry
        )
        assertTrue(under13Save.isFailure)

        // 4. Photo fingerprinting computes 64-bit dHash and reuses existing asset on second upload
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val firstCrop = AndroidSandboxMediaProcessor.cropAndSanitizeInSandbox(
            context = context,
            sourceBitmap = bmp,
            scale = 1f,
            offsetXNormalized = 0f,
            offsetYNormalized = 0f,
            rotationDegrees = 0f,
            dao = db.telefamDao()
        ).getOrThrow()
        assertFalse(firstCrop.reusedFromFingerprint)
        assertEquals(16, firstCrop.perceptualDHash.length)

        val secondCropReused = AndroidSandboxMediaProcessor.cropAndSanitizeInSandbox(
            context = context,
            sourceBitmap = bmp,
            scale = 1f,
            offsetXNormalized = 0f,
            offsetYNormalized = 0f,
            rotationDegrees = 0f,
            dao = db.telefamDao()
        ).getOrThrow()
        assertTrue(secondCropReused.reusedFromFingerprint)

        // 5. Verify Encrypted Session Vault & Offline Launch to Home + Token Refresh Preservation
        val (offlineSession, offlineProfile) = repo.getActiveSessionWithTelemetry(
            screenWidthDp = 411,
            locationTriple = Triple("Kenya • Africa/Nairobi", -1.2921, 36.8219),
            simulateAnomaly = false
        )
        assertNotNull(offlineSession)
        assertNotNull(offlineProfile)
        assertTrue(offlineSession!!.hasCompletedProfile)
        assertEquals("amina_wanjiku", offlineProfile!!.username)
        assertTrue(offlineSession.refreshTokenHash.isNotBlank())

        // 6. Verify 6 Non-Human Chat Background Themes, Default Red Bubble Color First, and Offline-First Room Privacy Rules
        assertEquals(6, TelefamChatThemes.ALL.size)
        assertEquals("#C80815", TelefamBubbleColors.ALL.first().colorHex)
        val initialPrivacy = repo.getOrCreatePrivacySettings("user_1")
        assertEquals("ANYONE", initialPrivacy.messageRequests)
        assertEquals("#C80815", initialPrivacy.bubbleColorHex)
        assertEquals("crimson_silk", initialPrivacy.chatThemeId)

        val updatedRule = repo.updatePrivacyRuleOfflineFirst(
            userId = "user_1",
            ruleKey = PrivacyRuleKey.WHO_CAN_SCREENSHOT_CHATS,
            audience = PrivacyAudience.NOBODY,
            telemetry = telemetry
        )
        assertEquals("NOBODY", updatedRule.whoCanScreenshotChats)

        val updatedTheme = repo.updateChatBackgroundThemeOfflineFirst(
            userId = "user_1",
            themeId = "aurora_cosmos",
            telemetry = telemetry
        )
        assertEquals("aurora_cosmos", updatedTheme.chatThemeId)

        val updatedBubble = repo.updateMessageBubbleColorOfflineFirst(
            userId = "user_1",
            bubbleColorHex = "#1565C0",
            telemetry = telemetry
        )
        assertEquals("#1565C0", updatedBubble.bubbleColorHex)

        db.close()
    }

    @Test
    fun `verify settings integration passkey login 2FA real device qr and support flows`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TelefamAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.telefamDao()
        val securityRepo = TelefamSecurityRepository(context, dao)
        val socialRepo = TelefamSocialRepository(context, dao)
        val settingsRepo = TelefamSettingsRepository(context, dao, securityRepo, socialRepo)
        val telemetry = sampleTelemetry()

        // 1. Create account & profile
        val email = "enterprise.security@telefam.company"
        val signUpOutcome = securityRepo.initiateSignUpWithOtp(email, "StrongPass#2026", telemetry)
        assertTrue(signUpOutcome is AuthActionOutcome.OtpDispatched)
        val otpChallenge = dao.getLatestActiveOtp(email, "SIGNUP")
        assertNotNull(otpChallenge)

        // Complete profile for user
        val createdUser = dao.getUserByEmail(email)!!
        dao.upsertUser(createdUser.copy(emailVerified = true))
        val userId = createdUser.userId
        securityRepo.saveProfileSetup(
            userId = userId,
            email = email,
            fullName = "Enterprise Member",
            username = "enterprise_member",
            phoneCountryCode = "+254",
            phoneIsoCode = "KE",
            phoneNumber = "712000111",
            gender = "Prefer not to say",
            dateOfBirth = "1994-03-10",
            bio = "Enterprise account",
            sanitizedAvatar = null,
            telemetry = telemetry
        )

        // 2. Verify only real current device is registered (no fake devices)
        val initialSettings = settingsRepo.getOrCreateAccountSettings(
            userId = userId,
            email = email,
            phone = "+254712000111",
            telemetry = telemetry
        )
        val devices = dao.getTrustedDevicesForUser(userId)
        assertEquals(1, devices.size)
        assertEquals(telemetry.serverVerifiedDeviceHash, devices.first().serverDeviceHash)

        // 3. Register passkey and verify passkey sign-in works
        val passkeyUpdated = settingsRepo.registerFido2Passkey(
            current = initialSettings,
            deviceLabel = telemetry.osDescription,
            telemetry = telemetry
        )
        assertTrue(passkeyUpdated.passkeyEnabled)
        assertTrue(passkeyUpdated.passkeyCredentialId.startsWith("pk_"))

        val passkeyAuthOutcome = securityRepo.authenticateWithPasskey(email, telemetry)
        assertTrue(passkeyAuthOutcome is AuthActionOutcome.Authenticated)

        // 4. Verify backend QR code generation & token persistence
        val qrBitmap = settingsRepo.issueAndGenerateBackendProfileQrCode(
            userId = userId,
            username = "enterprise_member",
            fullName = "Enterprise Member",
            sizePx = 360
        )
        assertEquals(360, qrBitmap.width)
        val refreshedSettings = dao.getAccountSettingsAndSecurity(userId)!!
        assertTrue(refreshedSettings.backendQrPayloadToken.isNotBlank())

        // 5. Verify theme switching persistence (Light / Dark / System)
        val darkSaved = settingsRepo.saveAndSyncAccountSettings(
            updated = refreshedSettings.copy(appThemeMode = AppThemeOption.DARK.wireCode),
            telemetry = telemetry,
            auditAction = "Appearance -> DARK"
        )
        assertEquals("DARK", darkSaved.appThemeMode)

        // 6. Verify legal policy acknowledgement & help article feedback
        val doc = TelefamSettingsCatalog.LEGAL_DOCUMENTS.first()
        val afterPolicyAck = settingsRepo.acknowledgeLegalPolicy(
            current = darkSaved,
            policyId = doc.id,
            policyTitle = doc.title,
            policyVersion = doc.version,
            telemetry = telemetry
        )
        assertTrue(afterPolicyAck.acceptedPoliciesCsv.contains("${doc.id}:"))

        val article = TelefamSettingsCatalog.HELP_ARTICLES.first()
        val afterHelpFeedback = settingsRepo.recordHelpArticleFeedback(
            current = afterPolicyAck,
            articleId = article.id,
            helpful = true,
            telemetry = telemetry
        )
        assertTrue(afterHelpFeedback.helpfulArticlesCsv.contains("${article.id}:YES"))

        // 7. Verify support ticket submission, follow-up, and resolution
        val ticketResult = settingsRepo.submitProblemOrSupportTicket(
            ownerUserId = userId,
            ticketType = "SUPPORT_REQUEST",
            category = "Account",
            subject = "Verification inquiry",
            description = "Testing support ticket lifecycle and follow-up.",
            screenshotUri = null,
            telemetry = telemetry
        )
        assertTrue(ticketResult.isSuccess)
        val createdTicket = ticketResult.getOrThrow()

        val followUpResult = settingsRepo.updateSupportTicketFollowUpOrStatus(
            ownerUserId = userId,
            ticketId = createdTicket.ticketId,
            followUpReply = "Additional diagnostic details attached.",
            markResolved = false,
            telemetry = telemetry
        )
        assertTrue(followUpResult.isSuccess)
        assertEquals("IN_REVIEW", followUpResult.getOrThrow().status)

        val resolvedResult = settingsRepo.updateSupportTicketFollowUpOrStatus(
            ownerUserId = userId,
            ticketId = createdTicket.ticketId,
            followUpReply = "",
            markResolved = true,
            telemetry = telemetry
        )
        assertTrue(resolvedResult.isSuccess)
        assertEquals("RESOLVED", resolvedResult.getOrThrow().status)

        // 8. Verify Owner Menu Screen localized resources and Lucide SVG icons
        assertEquals("Menu", context.getString(R.string.owner_menu_title))
        assertEquals("Dashboard", context.getString(R.string.owner_menu_dashboard))
        assertEquals("Stars", context.getString(R.string.owner_menu_stars))
        assertEquals("Subscription", context.getString(R.string.owner_menu_subscription))
        assertEquals("Monetization", context.getString(R.string.owner_menu_monetization))
        assertEquals("Wallet", context.getString(R.string.owner_menu_wallet))
        assertEquals("Linked device", context.getString(R.string.owner_menu_linked_device))
        assertEquals("Create campaign", context.getString(R.string.owner_menu_create_campaign))
        assertEquals("Get Verified", context.getString(R.string.owner_menu_get_verified))
        assertEquals("Live Stream", context.getString(R.string.owner_menu_live_stream))
        assertEquals("Market", context.getString(R.string.owner_menu_market))
        assertEquals("Groups", context.getString(R.string.owner_menu_groups))
        assertEquals("Memories", context.getString(R.string.owner_menu_memories))
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.BarChart)
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.Star)
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.Crown)
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.DollarBadge)
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.Wallet)
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.LinkedDevices)
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.Megaphone)
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.BadgeCheck)
        assertNotNull(com.telefam.company.app.ui.screens.LucideOwnerMenuIcons.RadioLive)

        db.close()
    }
}
