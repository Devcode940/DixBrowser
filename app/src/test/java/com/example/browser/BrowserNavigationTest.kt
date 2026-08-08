package com.example.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserNavigationTest {
    @Test
    fun rejectsNonHttpSchemes() {
        assertFalse(BrowserNavigation.isAllowed("javascript:alert(1)", httpsOnly = false))
        assertFalse(BrowserNavigation.isAllowed("file:///sdcard/test.html", httpsOnly = false))
        assertFalse(BrowserNavigation.isAllowed("intent://example.com", httpsOnly = false))
        assertFalse(BrowserNavigation.isAllowed("data:text/html,test", httpsOnly = false))
    }

    @Test
    fun acceptsHttpWhenHttpsOnlyDisabled() {
        assertTrue(BrowserNavigation.isAllowed("http://example.com", httpsOnly = false))
        assertTrue(BrowserNavigation.isAllowed("https://example.com", httpsOnly = false))
    }

    @Test
    fun rejectsHttpWhenHttpsOnlyEnabled() {
        assertFalse(BrowserNavigation.isAllowed("http://example.com", httpsOnly = true))
        assertTrue(BrowserNavigation.isAllowed("https://example.com", httpsOnly = true))
    }

    @Test
    fun rejectsHostlessHttpUrls() {
        assertFalse(BrowserNavigation.isAllowed("https:", httpsOnly = false))
        assertFalse(BrowserNavigation.isAllowed("https:///path", httpsOnly = false))
    }
}
