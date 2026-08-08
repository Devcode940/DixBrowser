package com.example.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserNavigationTest {
    @Test
    fun rejectsNonHttpSchemes() {
        assertFalse(isAllowed("javascript:alert(1)", httpsOnly = false))
        assertFalse(isAllowed("file:///sdcard/test.html", httpsOnly = false))
        assertFalse(isAllowed("intent://example.com", httpsOnly = false))
        assertFalse(isAllowed("data:text/html,test", httpsOnly = false))
    }

    @Test
    fun acceptsHttpWhenHttpsOnlyDisabled() {
        assertTrue(isAllowed("http://example.com", httpsOnly = false))
        assertTrue(isAllowed("https://example.com", httpsOnly = false))
    }

    @Test
    fun rejectsHttpWhenHttpsOnlyEnabled() {
        assertFalse(isAllowed("http://example.com", httpsOnly = true))
        assertTrue(isAllowed("https://example.com", httpsOnly = true))
    }

    private fun isAllowed(url: String, httpsOnly: Boolean): Boolean {
        val scheme = android.net.Uri.parse(url).scheme?.lowercase() ?: return false
        return (scheme == "https" || scheme == "http") && (!httpsOnly || scheme == "https")
    }
}
