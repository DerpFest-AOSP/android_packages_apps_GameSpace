/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: Chaldeaprjkt
 * SPDX-FileCopyrightText: risingOS Android Project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.chaldeaprjkt.gamespace.data

import android.content.Context
import android.media.AudioManager
import com.google.gson.Gson
import javax.inject.Inject
import vendor.lineage.health.FastChargeMode

class GameSession @Inject constructor(
    private val context: Context,
    private val appSettings: AppSettings,
    private val systemSettings: SystemSettings,
    private val gson: Gson,
) {

    private val db by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private var state
        get() = db.getString(KEY_SAVED_SESSION, "")
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                try {
                    gson.fromJson(it, SessionState::class.java)
                } catch (e: RuntimeException) {
                    null
                }
            }
        set(value) = db.edit()
            .putString(KEY_SAVED_SESSION, value?.let {
                try {
                    gson.toJson(value)
                } catch (e: RuntimeException) {
                    ""
                }
            } ?: "")
            .apply()

    fun register(sessionName: String) {
        if (state?.packageName != sessionName) unregister()

        state = SessionState(
            packageName = sessionName,
            autoBrightness = systemSettings.autoBrightness,
            statusbarBrightness = systemSettings.statusbarBrightness,
            headsup = systemSettings.headsup,
            threeScreenshot = systemSettings.threeScreenshot,
            ringerMode = audioManager.ringerModeInternal,
            doubleTapToSleep = systemSettings.doubleTapToSleep,
            edgeCutout = systemSettings.edgeCutout,
            pulseHaptics = systemSettings.pulseHaptics,
            fastChargeMode = if (systemSettings.isFastChargeSupported) {
                systemSettings.fastChargeMode
            } else {
                null
            },
            highTouchPollingRate = systemSettings.highTouchPollingRate,
            displayAutoOutdoor = systemSettings.displayAutoOutdoor,
        )
        if (appSettings.noHeadsUp) {
            systemSettings.headsup = false
        }
        if (appSettings.noAutoBrightness) {
            systemSettings.autoBrightness = false
            systemSettings.statusbarBrightness = false
        }
        if (appSettings.danmakuNotification) {
            systemSettings.headsup = false
        }
        if (appSettings.noThreeScreenshot) {
            systemSettings.threeScreenshot = false
        }
        if (appSettings.doubleTaptoSleep){
            systemSettings.doubleTapToSleep = false
        }
        if (appSettings.edgeCutout) {
            systemSettings.edgeCutout = true
        }
        if (appSettings.ringerMode != 3) {
            audioManager.ringerModeInternal = appSettings.ringerMode
        }
        if (appSettings.noPulseBassHaptics) {
            systemSettings.pulseHaptics = 0
        }
        if (systemSettings.isFastChargeSupported) {
            val saved = state?.fastChargeMode ?: FastChargeMode.NONE
            systemSettings.fastChargeMode = if (appSettings.fastChargeEnabler) {
                if (saved == FastChargeMode.NONE) FastChargeMode.FAST_CHARGE else saved
            } else {
                FastChargeMode.NONE
            }
        }
        if (appSettings.highTouchPollingRate) {
            systemSettings.highTouchPollingRate = false
        }
        if (appSettings.displayAutoOutdoor) {
            systemSettings.displayAutoOutdoor = false
        }
    }

    fun unregister() {
        val orig = state?.copy() ?: return
        if (appSettings.noHeadsUp) {
            orig.headsup?.let { systemSettings.headsup = it }
        }
        if (appSettings.noAutoBrightness) {
            orig.autoBrightness?.let { systemSettings.autoBrightness = it }
            orig.statusbarBrightness?.let { systemSettings.statusbarBrightness = it }
        }
        if (appSettings.danmakuNotification) {
            orig.headsup?.let { systemSettings.headsup = it }
        }
        if (appSettings.noThreeScreenshot) {
            orig.threeScreenshot?.let { systemSettings.threeScreenshot = it }
        }
        if (appSettings.doubleTaptoSleep) {
            orig.doubleTapToSleep?.let{ systemSettings.doubleTapToSleep = it }
        }
        if (appSettings.edgeCutout) {
            orig.edgeCutout?.let { systemSettings.edgeCutout = it }
        }
        if (appSettings.ringerMode != 3) {
            audioManager.ringerModeInternal = orig.ringerMode
        }
        if (appSettings.noPulseBassHaptics) {
            systemSettings.pulseHaptics = orig.pulseHaptics
        }
        orig.fastChargeMode?.let { systemSettings.fastChargeMode = it }
        if (appSettings.highTouchPollingRate) {
            orig.highTouchPollingRate?.let { systemSettings.highTouchPollingRate = it }
        }
        if (appSettings.displayAutoOutdoor) {
            orig.displayAutoOutdoor?.let { systemSettings.displayAutoOutdoor = it }
        }
        state = null
    }

    fun finalize() {
        unregister()
    }

    companion object {
        const val PREFS_NAME = "persisted_session"
        const val KEY_SAVED_SESSION = "session"
    }
}
