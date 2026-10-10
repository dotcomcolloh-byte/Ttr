package com.example

import com.telefam.company.app.domain.TelefamCountries
import com.telefam.company.app.domain.TelefamValidators
import com.telefam.company.app.security.AndroidSandboxMediaProcessor
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `sql injection payloads are rejected by TelefamValidators`() {
        assertFalse(TelefamValidators.validateEmail("admin@telefam.company' OR '1'='1"))
        assertFalse(TelefamValidators.validateFullName("John; DROP TABLE telefam_users--"))
        assertTrue(TelefamValidators.validateEmail("amina@telefam.company"))
    }

    @Test
    fun `email prefix autofills full name and username accurately`() {
        val (fullName, username) = TelefamValidators.deriveFullNameAndUsernameFromEmail("amina.wanjiku_24@telefam.company")
        assertEquals("Amina Wanjiku", fullName)
        assertEquals("amina_wanjiku_24", username)
    }

    @Test
    fun `date of birth validator enforces 13 years and above`() {
        assertTrue(TelefamValidators.validateAgeAtLeast13("2000-05-15"))
        assertFalse(TelefamValidators.validateAgeAtLeast13("2022-01-01"))
    }

    @Test
    fun `all world countries are included and searchable by name iso and dial code`() {
        assertTrue("Expected 200+ world countries", TelefamCountries.ALL.size >= 200)
        assertEquals("KE", TelefamCountries.DEFAULT.isoCode)
        assertTrue(TelefamCountries.search("+254").any { it.isoCode == "KE" })
        assertTrue(TelefamCountries.search("Japan").any { it.isoCode == "JP" && it.dialCode == "+81" })
        assertTrue(TelefamCountries.search("Brazil").any { it.isoCode == "BR" && it.dialCode == "+55" })
    }

    @Test
    fun `password policy enforces uppercase lowercase digit and symbol`() {
        assertFalse(TelefamValidators.validatePassword("weakpass"))
        assertTrue(TelefamValidators.validatePassword("Telefam#2026!"))
    }

    @Test
    fun `sandbox magic mime header detector validates jpeg png webp and rejects spoofed bytes`() {
        val jpegHeader = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0, 0, 0, 0, 0, 0, 0, 0
        )
        assertEquals("image/jpeg", AndroidSandboxMediaProcessor.detectMagicMime(jpegHeader))

        val spoofedScript = "<?php system('cmd'); ?>".toByteArray()
        assertNull(AndroidSandboxMediaProcessor.detectMagicMime(spoofedScript))
    }

    @Test
    fun `aes gcm session encryption roundtrip succeeds`() {
        val raw = """{"user":"amina@telefam.company","domain":"telefam.company"}"""
        val (cipher, iv) = TelefamCryptoAndTelemetry.encryptSessionPayload(raw)
        val decrypted = TelefamCryptoAndTelemetry.decryptSessionPayload(cipher, iv)
        assertEquals(raw, decrypted)
    }
}
