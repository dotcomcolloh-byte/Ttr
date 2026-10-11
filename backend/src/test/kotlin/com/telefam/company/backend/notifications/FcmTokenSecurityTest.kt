package com.telefam.company.backend.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FcmTokenSecurityTest {
    private val token = "fcm-token-for-telefam-test-device-123456"
    private val hash = FcmTokenSecurity.sha256Hex(token)

    @Test
    fun `valid registration payload passes and hash is deterministic`() {
        FcmTokenSecurity.validateRegistration(
            headerUserId = "user-123",
            payloadUserId = "user-123",
            fcmToken = token,
            fcmTokenSha256 = hash,
            deviceFingerprint = "device-fingerprint-123",
            appVersion = "1.0.0",
            platform = "ANDROID",
            idempotencyKey = "idempotency-123"
        )
        assertEquals(hash, FcmTokenSecurity.sha256Hex(token))
    }

    @Test
    fun `registration rejects a mismatched token hash`() {
        assertFailsWith<IllegalArgumentException> {
            FcmTokenSecurity.validateRegistration(
                headerUserId = "user-123",
                payloadUserId = "user-123",
                fcmToken = token,
                fcmTokenSha256 = FcmTokenSecurity.sha256Hex("different-token"),
                deviceFingerprint = "device-fingerprint-123",
                appVersion = "1.0.0",
                platform = "ANDROID",
                idempotencyKey = "idempotency-123"
            )
        }
    }

    @Test
    fun `registration rejects a user header that does not match payload`() {
        assertFailsWith<IllegalArgumentException> {
            FcmTokenSecurity.validateRegistration(
                headerUserId = "user-a",
                payloadUserId = "user-b",
                fcmToken = token,
                fcmTokenSha256 = hash,
                deviceFingerprint = "device-fingerprint-123",
                appVersion = "1.0.0",
                platform = "ANDROID",
                idempotencyKey = "idempotency-123"
            )
        }
    }

    @Test
    fun `revocation rejects malformed token hashes`() {
        assertFailsWith<IllegalArgumentException> {
            FcmTokenSecurity.validateRevocation(
                headerUserId = "user-123",
                payloadUserId = "user-123",
                fcmTokenSha256 = "not-a-sha256",
                deviceFingerprint = "device-fingerprint-123",
                idempotencyKey = "idempotency-123"
            )
        }
    }
}
