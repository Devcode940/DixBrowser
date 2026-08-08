package com.example.security

import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewSecurityPolicyTest {
    @Test
    fun allowsHttps() {
        assertTrue(
            WebViewSecurityPolicy.isSafeNavigation(
                Uri.parse("https://example.com/path"),
                httpsOnly = true
            )
        )
    }

    @Test
    fun blocksHttpWhenHttpsOnly() {
        assertFalse(
            WebViewSecurityPolicy.isSafeNavigation(
                Uri.parse("http://example.com"),
                httpsOnly = true
            )
        )
    }

    @Test
    fun blocksDangerousSchemes() {
        listOf("file:///tmp/a", "content://authority/a", "javascript:alert(1)", "data:text/html,test")
            .forEach { value ->
                assertFalse(
                    value,
                    WebViewSecurityPolicy.isSafeNavigation(Uri.parse(value), httpsOnly = false)
                )
            }
    }

    @Test
    fun blocksMissingHost() {
        assertFalse(
            WebViewSecurityPolicy.isSafeNavigation(Uri.parse("https:///path"), httpsOnly = false)
        )
    }
}
