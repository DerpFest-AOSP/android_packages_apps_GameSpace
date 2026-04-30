/*
 * Copyright (C) 2021 Chaldeaprjkt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chaldeaprjkt.gamespace.preferences

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.GameConfig
import io.chaldeaprjkt.gamespace.data.UserGame
import io.chaldeaprjkt.gamespace.settings.PerAppSettingsFragment
import io.chaldeaprjkt.gamespace.utils.GameModeUtils.Companion.describeGameMode
import io.chaldeaprjkt.gamespace.utils.di.ServiceViewEntryPoint
import io.chaldeaprjkt.gamespace.utils.entryPointOf


class AppListPreferences @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    PreferenceCategory(context, attrs), Preference.OnPreferenceClickListener {

    private val apps = mutableListOf<UserGame>()
    private val systemSettings by lazy {
        context.entryPointOf<ServiceViewEntryPoint>().systemSettings()
    }

    private val gameModeUtils by lazy {
        context.entryPointOf<ServiceViewEntryPoint>().gameModeUtils()
    }

    private val appSettings by lazy {
        context.entryPointOf<ServiceViewEntryPoint>().appSettings()
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val autoDetectPref by lazy {
        SwitchPreferenceCompat(context, null).apply {
            key = AppSettings.KEY_AUTO_GAME_DETECT
            title = context.getString(R.string.auto_game_detect_title)
            summary = context.getString(R.string.auto_game_detect_summary)
            setDefaultValue(true)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                // Persist runs after this returns; refresh once prefs match the new toggle.
                mainHandler.post { updateAppList() }
                true
            }
        }
    }

    private lateinit var registeredAppClickAction: (String) -> Unit

    init {
        isOrderingAsAdded = false
    }

    private val makeAddPref by lazy {
        Preference(context).apply {
            title = context.getString(R.string.game_list_add_title)
            key = KEY_ADD_GAME
            setIcon(R.drawable.ic_add)
            isPersistent = false
            onPreferenceClickListener = this@AppListPreferences
        }
    }

    private fun getAppInfo(packageName: String): ApplicationInfo? = try {
        val flags = PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        context.packageManager.getApplicationInfo(packageName, flags)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun launchGame(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // No-op
        }
    }

    private fun buildGamePref(game: UserGame): Preference {
        val info = getAppInfo(game.packageName)
        val pm = context.packageManager

        return object : Preference(context) {
            override fun onBindViewHolder(holder: PreferenceViewHolder) {
                super.onBindViewHolder(holder)

                val canLaunch = pm.getLaunchIntentForPackage(game.packageName) != null
                holder.findViewById(R.id.launch_icon)?.let { play ->
                    play.visibility = if (canLaunch) View.VISIBLE else View.GONE
                    play.setOnClickListener { launchGame(game.packageName) }
                }

                holder.findViewById(R.id.settings_icon)?.setOnClickListener {
                    if (::registeredAppClickAction.isInitialized) {
                        registeredAppClickAction(game.packageName)
                    }
                }
            }
        }.apply {
            key = game.packageName
            title = info?.loadLabel(pm)
            summary = context.describeGameMode(game.mode)
            icon = info?.loadIcon(pm)
            layoutResource = R.layout.library_item
            isPersistent = false
        }
    }

    fun updateAppList() {
        val deniedGames = PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet("gamespace_denied_list", emptySet()) ?: emptySet()
        val userGames = systemSettings.userGames?.toMutableList() ?: mutableListOf()
        val autoDetectedGames = if (appSettings.autoGameDetect) {
            context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.category == ApplicationInfo.CATEGORY_GAME && it.packageName !in deniedGames }
                .map { UserGame(it.packageName) }
        } else {
            emptyList()
        }

        val allGames = (userGames + autoDetectedGames).distinctBy { it.packageName }
        systemSettings.userGames = allGames

        apps.clear()
        if (!systemSettings.userGames.isNullOrEmpty()) {
            apps.addAll(allGames)
        }

        removeAll()
        addPreference(autoDetectPref)
        addPreference(makeAddPref)
        apps.filter { getAppInfo(it.packageName) != null }
            .map(::buildGamePref)
            .sortedBy { it.title.toString().lowercase() }
            .forEach(::addPreference)
    }

    private fun registerApp(packageName: String) {
        val deniedGames = PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet("gamespace_denied_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        deniedGames.remove(packageName)
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet("gamespace_denied_list", deniedGames).apply()

        if (!apps.any { it.packageName == packageName }) {
            apps.add(UserGame(packageName))
        }
        systemSettings.userGames = apps
        gameModeUtils.setIntervention(packageName, GameConfig.ModeBuilder.build())
        updateAppList()
    }

    private fun unregisterApp(packageName: String) {
        val deniedGames = PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet("gamespace_denied_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        getAppInfo(packageName)?.takeIf { it.category == ApplicationInfo.CATEGORY_GAME }?.let {
            deniedGames.add(it.packageName)
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet("gamespace_denied_list", deniedGames).apply()
        apps.removeIf { it.packageName == packageName }
        systemSettings.userGames = apps
        gameModeUtils.setIntervention(packageName, null)
        updateAppList()
    }

    override fun onAttached() {
        super.onAttached()
        updateAppList()
    }

    override fun onPreferenceClick(preference: Preference): Boolean = true

    fun onRegisteredAppClick(action: (String) -> Unit) {
        registeredAppClickAction = action
    }

    fun usePerAppResult(result: ActivityResult?) {
        result?.takeIf { it.resultCode == Activity.RESULT_OK }
            ?.data?.getStringExtra(PerAppSettingsFragment.PREF_UNREGISTER)
            ?.let { unregisterApp(it) }
    }

    fun useSelectorResult(result: ActivityResult?) {
        result?.takeIf { it.resultCode == Activity.RESULT_OK }
            ?.data?.getStringExtra(EXTRA_APP)
            ?.let { registerApp(it) }
    }

    companion object {
        const val KEY_ADD_GAME = "add_game"
        const val EXTRA_APP = "selected_app"
    }
}
