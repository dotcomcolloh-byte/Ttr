package com.example

import com.telefam.company.app.data.TelefamFcmTokenRegisterPayload
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmRegistrationTest {
    @Test
    fun `fcm registration payload carries matching token hash and idempotency key`() {
        val token = "fcm-token-for-telefam-test-device-123456"
        val tokenHash = TelefamCryptoAndTelemetry.sha256Hex(token.toByteArray())
        val payload = TelefamFcmTokenRegisterPayload(
            userId = "user-123",
            fcmToken = token,
            fcmTokenSha256 = tokenHash,
            deviceFingerprint = "device-fingerprint-123",
            appVersion = "1.0.0",
            platform = "ANDROID",
            e2eeWakeOnlyEnabled = true,
            idempotencyKey = "idempotency-123"
        )

        assertEquals(token, payload.fcmToken)
        assertEquals(tokenHash, payload.fcmTokenSha256)
        assertEquals("ANDROID", payload.platform)
        assertTrue(payload.e2eeWakeOnlyEnabled)
        assertEquals("idempotency-123", payload.idempotencyKey)
    }
}
