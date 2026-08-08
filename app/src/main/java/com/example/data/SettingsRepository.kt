package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persistent browser settings with security-safe defaults. */
class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("browser_settings", Context.MODE_PRIVATE)

    private val _isNightMode = MutableStateFlow(prefs.getBoolean("night_mode", false))
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    private val _adBlockEnabled = MutableStateFlow(prefs.getBoolean("ad_block", true))
    val adBlockEnabled: StateFlow<Boolean> = _adBlockEnabled.asStateFlow()

    private val _searchEngine = MutableStateFlow(prefs.getString("search_engine", "DuckDuckGo") ?: "DuckDuckGo")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _defaultDesktopMode = MutableStateFlow(prefs.getBoolean("default_desktop", false))
    val defaultDesktopMode: StateFlow<Boolean> = _defaultDesktopMode.asStateFlow()

    private val _javaScriptEnabled = MutableStateFlow(prefs.getBoolean("javascript_enabled", true))
    val javaScriptEnabled: StateFlow<Boolean> = _javaScriptEnabled.asStateFlow()

    private val _thirdPartyCookiesEnabled = MutableStateFlow(prefs.getBoolean("third_party_cookies", false))
    val thirdPartyCookiesEnabled: StateFlow<Boolean> = _thirdPartyCookiesEnabled.asStateFlow()

    private val _urlBarPosition = MutableStateFlow(prefs.getString("url_bar_position", "Bottom") ?: "Bottom")
    val urlBarPosition: StateFlow<String> = _urlBarPosition.asStateFlow()

    private val _showShieldIcon = MutableStateFlow(prefs.getBoolean("show_shield_icon", true))
    val showShieldIcon: StateFlow<Boolean> = _showShieldIcon.asStateFlow()

    private val _showTranslateIcon = MutableStateFlow(prefs.getBoolean("show_translate_icon", true))
    val showTranslateIcon: StateFlow<Boolean> = _showTranslateIcon.asStateFlow()

    private val _showPasswordsIcon = MutableStateFlow(prefs.getBoolean("show_passwords_icon", true))
    val showPasswordsIcon: StateFlow<Boolean> = _showPasswordsIcon.asStateFlow()

    private val _showMediaSnifferIcon = MutableStateFlow(prefs.getBoolean("show_media_sniffer_icon", true))
    val showMediaSnifferIcon: StateFlow<Boolean> = _showMediaSnifferIcon.asStateFlow()

    private val _showHomeIcon = MutableStateFlow(prefs.getBoolean("show_home_icon", true))
    val showHomeIcon: StateFlow<Boolean> = _showHomeIcon.asStateFlow()

    private val _showTabsIcon = MutableStateFlow(prefs.getBoolean("show_tabs_icon", true))
    val showTabsIcon: StateFlow<Boolean> = _showTabsIcon.asStateFlow()

    private val _fingerprintProtectionEnabled = MutableStateFlow(prefs.getBoolean("fingerprint_protection", true))
    val fingerprintProtectionEnabled: StateFlow<Boolean> = _fingerprintProtectionEnabled.asStateFlow()

    private val _defaultTargetLanguage = MutableStateFlow(prefs.getString("target_language", "es") ?: "es")
    val defaultTargetLanguage: StateFlow<String> = _defaultTargetLanguage.asStateFlow()

    private val _dnsProvider = MutableStateFlow(prefs.getString("dns_provider", "Cloudflare 1.1.1.1") ?: "Cloudflare 1.1.1.1")
    val dnsProvider: StateFlow<String> = _dnsProvider.asStateFlow()

    private val _customDnsUrl = MutableStateFlow(prefs.getString("custom_dns_url", "https://cloudflare-dns.com/dns-query") ?: "https://cloudflare-dns.com/dns-query")
    val customDnsUrl: StateFlow<String> = _customDnsUrl.asStateFlow()

    private val _adBlockLevel = MutableStateFlow(prefs.getString("ad_block_level", "Aggressive") ?: "Aggressive")
    val adBlockLevel: StateFlow<String> = _adBlockLevel.asStateFlow()

    private val defaultFilters = setOf("EasyList Standard", "EasyPrivacy Trackers", "Fanboy Annoyances", "Anti-Adblock Defeater", "Social Media Widgets")
    private val _enabledFilterLists = MutableStateFlow(prefs.getStringSet("enabled_filter_lists", defaultFilters) ?: defaultFilters)
    val enabledFilterLists: StateFlow<Set<String>> = _enabledFilterLists.asStateFlow()

    private val _httpsOnlyMode = MutableStateFlow(prefs.getBoolean("https_only_mode", true))
    val httpsOnlyMode: StateFlow<Boolean> = _httpsOnlyMode.asStateFlow()

    private val _webRtcLeakProtection = MutableStateFlow(prefs.getBoolean("webrtc_leak_protection", true))
    val webRtcLeakProtection: StateFlow<Boolean> = _webRtcLeakProtection.asStateFlow()

    private val _antiPhishingEnabled = MutableStateFlow(prefs.getBoolean("anti_phishing", true))
    val antiPhishingEnabled: StateFlow<Boolean> = _antiPhishingEnabled.asStateFlow()

    private val _menuLayoutMode = MutableStateFlow(prefs.getString("menu_layout_mode", "Grid") ?: "Grid")
    val menuLayoutMode: StateFlow<String> = _menuLayoutMode.asStateFlow()

    private val _userAgentPreset = MutableStateFlow(prefs.getString("user_agent_preset", "Default Mobile") ?: "Default Mobile")
    val userAgentPreset: StateFlow<String> = _userAgentPreset.asStateFlow()

    private val _textZoomPercent = MutableStateFlow(prefs.getInt("text_zoom_percent", 100))
    val textZoomPercent: StateFlow<Int> = _textZoomPercent.asStateFlow()

    private val _selectedNewsCategory = MutableStateFlow(prefs.getString("news_category", "Technology") ?: "Technology")
    val selectedNewsCategory: StateFlow<String> = _selectedNewsCategory.asStateFlow()

    // WHY: The previous in-process Incognito toggle was not a genuine private
    // profile. It is removed so users cannot mistake it for isolated browsing.
    val allMenuItems = listOf(
        "Bookmarks", "History", "Downloads", "Share", "Desktop", "Night Mode",
        "AdBlock", "Privacy Shield", "DNS Settings", "TV Casting", "News Feed",
        "Clear Data", "Translate", "Save Offline", "Offline Pages", "Settings", "Exit"
    )

    private val _menuItems = MutableStateFlow(
        (prefs.getStringSet("menu_items", allMenuItems.toSet()) ?: allMenuItems.toSet()) - "Incognito"
    )
    val menuItems: StateFlow<Set<String>> = _menuItems.asStateFlow()

    init {
        // WHY: Migrate existing installations so the obsolete fake-private entry
        // cannot remain visible after upgrade.
        prefs.edit().apply {
            remove("incognito")
            putStringSet("menu_items", _menuItems.value)
        }.apply()
    }

    fun setNightMode(enabled: Boolean) { prefs.edit().putBoolean("night_mode", enabled).apply(); _isNightMode.value = enabled }
    fun setAdBlockEnabled(enabled: Boolean) { prefs.edit().putBoolean("ad_block", enabled).apply(); _adBlockEnabled.value = enabled }
    fun setSearchEngine(engine: String) { prefs.edit().putString("search_engine", engine).apply(); _searchEngine.value = engine }
    fun setDefaultDesktopMode(enabled: Boolean) { prefs.edit().putBoolean("default_desktop", enabled).apply(); _defaultDesktopMode.value = enabled }
    fun setJavaScriptEnabled(enabled: Boolean) { prefs.edit().putBoolean("javascript_enabled", enabled).apply(); _javaScriptEnabled.value = enabled }
    fun setThirdPartyCookiesEnabled(enabled: Boolean) { prefs.edit().putBoolean("third_party_cookies", enabled).apply(); _thirdPartyCookiesEnabled.value = enabled }
    fun setUrlBarPosition(position: String) { prefs.edit().putString("url_bar_position", position).apply(); _urlBarPosition.value = position }
    fun setShowShieldIcon(show: Boolean) { prefs.edit().putBoolean("show_shield_icon", show).apply(); _showShieldIcon.value = show }
    fun setShowTranslateIcon(show: Boolean) { prefs.edit().putBoolean("show_translate_icon", show).apply(); _showTranslateIcon.value = show }
    fun setShowPasswordsIcon(show: Boolean) { prefs.edit().putBoolean("show_passwords_icon", show).apply(); _showPasswordsIcon.value = show }
    fun setShowMediaSnifferIcon(show: Boolean) { prefs.edit().putBoolean("show_media_sniffer_icon", show).apply(); _showMediaSnifferIcon.value = show }
    fun setShowHomeIcon(show: Boolean) { prefs.edit().putBoolean("show_home_icon", show).apply(); _showHomeIcon.value = show }
    fun setShowTabsIcon(show: Boolean) { prefs.edit().putBoolean("show_tabs_icon", show).apply(); _showTabsIcon.value = show }
    fun setFingerprintProtectionEnabled(enabled: Boolean) { prefs.edit().putBoolean("fingerprint_protection", enabled).apply(); _fingerprintProtectionEnabled.value = enabled }
    fun setDefaultTargetLanguage(langCode: String) { prefs.edit().putString("target_language", langCode).apply(); _defaultTargetLanguage.value = langCode }
    fun setDnsProvider(provider: String) { prefs.edit().putString("dns_provider", provider).apply(); _dnsProvider.value = provider }
    fun setCustomDnsUrl(url: String) { prefs.edit().putString("custom_dns_url", url).apply(); _customDnsUrl.value = url }
    fun setAdBlockLevel(level: String) { prefs.edit().putString("ad_block_level", level).apply(); _adBlockLevel.value = level }
    fun toggleFilterList(filterName: String, enabled: Boolean) { val current = _enabledFilterLists.value.toMutableSet(); if (enabled) current.add(filterName) else current.remove(filterName); prefs.edit().putStringSet("enabled_filter_lists", current).apply(); _enabledFilterLists.value = current }
    fun setHttpsOnlyMode(enabled: Boolean) { prefs.edit().putBoolean("https_only_mode", enabled).apply(); _httpsOnlyMode.value = enabled }
    fun setWebRtcLeakProtection(enabled: Boolean) { prefs.edit().putBoolean("webrtc_leak_protection", enabled).apply(); _webRtcLeakProtection.value = enabled }
    fun setAntiPhishingEnabled(enabled: Boolean) { prefs.edit().putBoolean("anti_phishing", enabled).apply(); _antiPhishingEnabled.value = enabled }
    fun setMenuLayoutMode(mode: String) { prefs.edit().putString("menu_layout_mode", mode).apply(); _menuLayoutMode.value = mode }
    fun setUserAgentPreset(preset: String) { prefs.edit().putString("user_agent_preset", preset).apply(); _userAgentPreset.value = preset }
    fun setTextZoomPercent(percent: Int) { prefs.edit().putInt("text_zoom_percent", percent).apply(); _textZoomPercent.value = percent }
    fun setSelectedNewsCategory(category: String) { prefs.edit().putString("news_category", category).apply(); _selectedNewsCategory.value = category }

    fun toggleMenuItem(item: String, enabled: Boolean) {
        if (item == "Incognito") return
        val current = _menuItems.value.toMutableSet()
        if (enabled) current.add(item) else current.remove(item)
        prefs.edit().putStringSet("menu_items", current).apply()
        _menuItems.value = current
    }
}
