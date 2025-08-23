package com.nextgenapps.Mahallu.utils

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.nextgenapps.Mahallu.Profile.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    private val _isShowPayNow = MutableStateFlow(false)
    val isShowPayNow: StateFlow<Boolean> = _isShowPayNow

    private val _disabledOrgs = MutableStateFlow<List<String>>(emptyList())
    val disabledOrgs: StateFlow<List<String>> = _disabledOrgs

    private val _isAppBlocked = MutableStateFlow(false)
    val isAppBlocked: StateFlow<Boolean> = _isAppBlocked

    private val _isVersionBlocked = MutableStateFlow(false)
    val isVersionBlocked: StateFlow<Boolean> = _isVersionBlocked

    // ✅ New: organization blocked state
    private val _isOrganisationBlocked = MutableStateFlow(false)
    val isOrganisationBlocked: StateFlow<Boolean> = _isOrganisationBlocked

    init {
        // Observe _disabledOrgs to automatically update org blocked state
        viewModelScope.launch {
            _disabledOrgs.collect { disabledList ->
                val orgId = SessionManager.organizationId
                _isOrganisationBlocked.value = disabledList.contains(orgId)
            }
        }

        loadConfig()
    }

    fun loadConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // use 3600 in prod
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    applyConfig()
                } else {
                    Log.w("ConfigViewModel", "⚠️ Remote Config fetch failed", task.exception)
                }
            }
    }

    private fun applyConfig() {
        viewModelScope.launch {
            _isShowPayNow.value = remoteConfig.getBoolean("isShowPayNow")
            _isAppBlocked.value = remoteConfig.getBoolean("isAppBlocked")

            val minSupportedVersion = remoteConfig.getString("min_supported_version")
            _isVersionBlocked.value = isAppOutdated(minSupportedVersion)

            val disabledOrgsJson = remoteConfig.getString("disabled_orgs")
            val disabledList = parseDisabledOrgs(disabledOrgsJson)
            _disabledOrgs.value = disabledList

            // ✅ Update organization blocked state here
            val orgId = SessionManager.organizationId
            _isOrganisationBlocked.value = disabledList.contains(orgId)
        }
    }


    private fun parseDisabledOrgs(jsonString: String): List<String> {
        return try {
            val jsonArray = JSONArray(jsonString)
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isAppOutdated(minVersion: String): Boolean {
        if (minVersion.isEmpty()) return false
        val currentVersion = getAppVersion()
        return compareVersions(currentVersion, minVersion) < 0
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val v1Parts = v1.split(".").mapNotNull { it.toIntOrNull() }
        val v2Parts = v2.split(".").mapNotNull { it.toIntOrNull() }
        val count = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until count) {
            val a = v1Parts.getOrElse(i) { 0 }
            val b = v2Parts.getOrElse(i) { 0 }
            if (a < b) return -1
            if (a > b) return 1
        }
        return 0
    }
}


