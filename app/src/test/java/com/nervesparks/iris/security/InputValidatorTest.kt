package com.nervesparks.iris.security

import org.junit.Assert.*
import org.junit.Test

class InputValidatorTest {

    @Test
    fun apiKeyValidation() {
        assertTrue(InputValidator.isValidApiKey("valid_api_key_1234567890"))
        assertFalse(InputValidator.isValidApiKey("short"))
    }

    @Test
    fun huggingFaceTokenValidation() {
        val valid = "hf_" + "a".repeat(40)
        assertTrue(InputValidator.isValidHuggingFaceToken(valid))
        assertFalse(InputValidator.isValidHuggingFaceToken("hf_short"))
    }

    @Test
    fun usernameValidation() {
        assertTrue(InputValidator.isValidUsername("user_name-1"))
        assertFalse(InputValidator.isValidUsername("!"))
    }

    @Test
    fun urlValidation() {
        assertTrue(InputValidator.isValidUrl("https://example.com"))
        assertFalse(InputValidator.isValidUrl("not a url"))
    }

    @Test
    fun emailValidation() {
        assertTrue(InputValidator.isValidEmail("test@example.com"))
        assertFalse(InputValidator.isValidEmail("bad-email"))
    }

    @Test
    fun sanitizeTextInputRemovesUnsafeCharactersAndLimitsLength() {
        val dangerous = "<script>alert(\"x\")</script>" + "a".repeat(20000)
        val sanitized = InputValidator.sanitizeTextInput(dangerous)
        assertFalse(sanitized.contains("<"))
        assertFalse(sanitized.contains(">"))
        assertTrue(sanitized.length <= 10000)
    }

    @Test
    fun sanitizeModelNameAllowsSafeCharacters() {
        val name = "model@name!"
        val sanitized = InputValidator.sanitizeModelName(name)
        assertEquals("modelname", sanitized)
    }

    @Test
    fun numericValidation() {
        assertTrue(InputValidator.isValidTemperature(1.0f))
        assertFalse(InputValidator.isValidTemperature(-0.1f))
        assertTrue(InputValidator.isValidTopP(0.5f))
        assertFalse(InputValidator.isValidTopP(1.5f))
        assertTrue(InputValidator.isValidTopK(50))
        assertFalse(InputValidator.isValidTopK(0))
        assertTrue(InputValidator.isValidMaxTokens(1000))
        assertFalse(InputValidator.isValidMaxTokens(50000))
        assertTrue(InputValidator.isValidContextLength(1024))
        assertFalse(InputValidator.isValidContextLength(100))
    }

    @Test
    fun filePathValidation() {
        assertTrue(InputValidator.isValidFilePath("model.gguf"))
        assertFalse(InputValidator.isValidFilePath("../etc/passwd"))
    }

    @Test
    fun validateApiKeyReturnsProperResult() {
        val invalid = InputValidator.validateApiKey("short")
        assertFalse(invalid.isValid)
        assertEquals("Invalid API key format", invalid.errorMessage)
        val key = "valid_api_key_1234567890"
        val valid = InputValidator.validateApiKey(key)
        assertTrue(valid.isValid)
        assertEquals(key, valid.sanitizedValue)
    }

    @Test
    fun validateHuggingFaceTokenReturnsProperResult() {
        val invalid = InputValidator.validateHuggingFaceToken("hf_short")
        assertFalse(invalid.isValid)
        assertEquals("Invalid HuggingFace token format", invalid.errorMessage)
        val token = "hf_" + "a".repeat(40)
        val valid = InputValidator.validateHuggingFaceToken(token)
        assertTrue(valid.isValid)
        assertEquals(token, valid.sanitizedValue)
    }

    @Test
    fun validateUsernameReturnsProperResult() {
        val invalid = InputValidator.validateUsername("!")
        assertFalse(invalid.isValid)
        assertEquals("Username must be 3-50 alphanumeric characters", invalid.errorMessage)
        val name = "user_name-1"
        val valid = InputValidator.validateUsername(name)
        assertTrue(valid.isValid)
        assertEquals(name, valid.sanitizedValue)
    }
}

