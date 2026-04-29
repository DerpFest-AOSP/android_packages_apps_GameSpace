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
package io.chaldeaprjkt.gamespace.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment

import dagger.hilt.android.AndroidEntryPoint

import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.data.GameOptimizationManager
import io.chaldeaprjkt.gamespace.preferences.AppListPreferences
import io.chaldeaprjkt.gamespace.preferences.QuickStartAppPreference
import io.chaldeaprjkt.gamespace.preferences.QuickStartAppPreferenceDialogFragment
import io.chaldeaprjkt.gamespace.preferences.appselector.AppSelectorActivity
import javax.inject.Inject

@AndroidEntryPoint(SettingsBasePreferenceFragment::class)
class SettingsFragment : Hilt_SettingsFragment(), Preference.OnPreferenceChangeListener {
    @Inject
    lateinit var settings: SystemSettings

    @Inject
    lateinit var gameOptimization: GameOptimizationManager

    private val TAG = "GameSettingsFragment"

    private var apps: AppListPreferences? = null

    private val selectorResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            apps?.useSelectorResult(it)
        }

    private val perAppResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            apps?.usePerAppResult(it)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Game Optimization preferences
        findPreference<SwitchPreferenceCompat>("game_memory_management")?.apply {
            isChecked = gameOptimization.isMemoryManagementEnabled
            onPreferenceChangeListener = this@SettingsFragment
        }

        findPreference<SwitchPreferenceCompat>("game_cache_management")?.apply {
            isChecked = gameOptimization.isCacheManagementEnabled
            onPreferenceChangeListener = this@SettingsFragment
        }

        apps = findPreference(Settings.System.GAMESPACE_GAME_LIST)
        apps?.onRegisteredAppClick {
            perAppResult.launch(Intent(context, PerAppSettingsActivity::class.java).apply {
                putExtra(PerAppSettingsActivity.EXTRA_PACKAGE, it)
            })
        }

        findPreference<Preference>(AppListPreferences.KEY_ADD_GAME)
            ?.setOnPreferenceClickListener {
                selectorResult.launch(Intent(context, AppSelectorActivity::class.java))
                return@setOnPreferenceClickListener true
            }

        findPreference<SwitchPreferenceCompat>(Settings.System.GAMESPACE_SUPPRESS_FULLSCREEN_INTENT)?.apply {
            isChecked = settings.suppressFullscreenIntent
            onPreferenceChangeListener = this@SettingsFragment
        }

        findPreference<SwitchPreferenceCompat>(AppSettings.KEY_EDGE_CUTOUT)?.apply {
            try {
                context?.resources?.getString(com.android.internal.R.string.config_edge_cutout_overlay_package)?.let {
                    context?.packageManager?.getPackageInfo(it, 0)
                }
                    ?.let {
                        isVisible = true
                    } ?: run {
                        isVisible = false
                    }
            } catch (e: PackageManager.NameNotFoundException) {
                isVisible = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        apps?.updateAppList()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            "game_memory_management" -> {
                gameOptimization.isMemoryManagementEnabled = newValue as Boolean
                return true
            }
            "game_cache_management" -> {
                gameOptimization.isCacheManagementEnabled = newValue as Boolean
                return true
            }
            Settings.System.GAMESPACE_SUPPRESS_FULLSCREEN_INTENT -> {
                settings.suppressFullscreenIntent = newValue as Boolean
                return true
            }
        }
        return false
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is QuickStartAppPreference) {
            val dialogFragment = QuickStartAppPreferenceDialogFragment.newInstance(preference.key)
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, "QuickStartAppPreferenceDialogFragment")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}
