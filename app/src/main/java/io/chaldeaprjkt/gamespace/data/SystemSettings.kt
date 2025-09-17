/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: Chaldeaprjkt
 * SPDX-License-Identifier: Apache-2.0
 */
package io.chaldeaprjkt.gamespace.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import javax.inject.Inject

import lineageos.health.HealthInterface
import lineageos.providers.LineageSettings
import vendor.lineage.health.FastChargeMode

class SystemSettings @Inject constructor(
    context: Context,
    private val gameModeUtils: GameModeUtils
) {
    private val TAG = "GameSpaceSystemSettings"

    private val resolver = context.contentResolver
    private val healthInterface: HealthInterface? by lazy {
        try {
            HealthInterface.getInstance(context)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Fast charge HAL unavailable", e)
            null
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var edgeCutoutRunnable: Runnable? = null

    var headsup
        get() = Settings.Global.getInt(
            resolver, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, 1) == 1
        set(it) {
            Settings.Global.putInt(
                resolver, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED,
                it.toInt()
            )
        }

    var autoBrightness
        get() =
            Settings.System.getIntForUser(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                UserHandle.USER_CURRENT
            ) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        set(auto) {
            Settings.System.putIntForUser(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                UserHandle.USER_CURRENT
            )
        }

    var statusbarBrightness
        get() =
            Settings.System.getIntForUser(
                resolver, Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0,
                UserHandle.USER_CURRENT
            ) == 1
        set(value) {
            Settings.System.putIntForUser(
                resolver, Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                if (value) 1 else 0, UserHandle.USER_CURRENT
            )
        }

    var threeScreenshot
        get() = Settings.System.getIntForUser(
            resolver, Settings.System.THREE_FINGER_GESTURE, 0,
            UserHandle.USER_CURRENT
        ) == 1
        set(it) {
            Settings.System.putIntForUser(
                resolver, Settings.System.THREE_FINGER_GESTURE,
                it.toInt(), UserHandle.USER_CURRENT
            )
        }

    var suppressFullscreenIntent
        get() = Settings.System.getIntForUser(
            resolver,
            Settings.System.GAMESPACE_SUPPRESS_FULLSCREEN_INTENT,
            0,
            UserHandle.USER_CURRENT
        ) == 1
        set(it) {
            Settings.System.putIntForUser(
                resolver,
                Settings.System.GAMESPACE_SUPPRESS_FULLSCREEN_INTENT,
                it.toInt(),
                UserHandle.USER_CURRENT
            )
        }

    var userGames
        get() =
            Settings.System.getStringForUser(
                resolver, Settings.System.GAMESPACE_GAME_LIST,
                UserHandle.USER_CURRENT
            )
                ?.split(";")
                ?.toList()?.filter { it.isNotEmpty() }
                ?.map { UserGame.fromSettings(it) } ?: emptyList()
        set(games) {
            Settings.System.putStringForUser(
                resolver,
                Settings.System.GAMESPACE_GAME_LIST,
                if (games.isEmpty()) "" else
                    games.joinToString(";") { it.toString() },
                UserHandle.USER_CURRENT
            )
            gameModeUtils.setupBatteryMode(games.isNotEmpty())
        }

    var doubleTapToSleep
        get() = LineageSettings.System.getIntForUser(
                resolver, LineageSettings.System.DOUBLE_TAP_SLEEP_GESTURE,1,
                UserHandle.USER_CURRENT
            )==1
            set(it){
                LineageSettings.System.putIntForUser(
                    resolver,LineageSettings.System.DOUBLE_TAP_SLEEP_GESTURE,
                    it.toInt(),UserHandle.USER_CURRENT
            )
        }

    var edgeCutout: Boolean
        get() {
            val value = Settings.Secure.getIntForUser(
                resolver, Settings.Secure.EDGE_CUTOUT, 0,
                UserHandle.USER_CURRENT
            ) == 1
            // If the edge cutout last update is executed successfully
            // then give the actual value.
            // Otherwise return false.
            // because we are updating setting with a delay.
            // So it may read the setting value before updating.
            // For example, closing and opening the game too quickly
            // will make gamespace consider edge cutout enabled in device by
            // default. In that case, it will enable the edge cutout system-wide
            // instead of only for the game.
            return edgeCutoutRunnable == null && value
        }
        set(value) {
            // Cancel if there is any previous runnable is waiting
            // to execute
            if (edgeCutoutRunnable != null) {
                handler.removeCallbacks(edgeCutoutRunnable!!)
                edgeCutoutRunnable = null
                return
            }
            edgeCutoutRunnable = Runnable {
                Settings.Secure.putIntForUser(
                    resolver, Settings.Secure.EDGE_CUTOUT,
                    value.toInt(), UserHandle.USER_CURRENT
                )
                edgeCutoutRunnable = null
            }
            handler.postDelayed(edgeCutoutRunnable!!, 1000)
        }

    var pulseHaptics
        get() =
            Settings.Secure.getIntForUser(
                resolver, Settings.Secure.PULSE_HAPTICS_ENABLED, 0,
                UserHandle.USER_CURRENT
            )
        set(value) {
            Settings.Secure.putIntForUser(
                resolver, Settings.Secure.PULSE_HAPTICS_ENABLED,
                value, UserHandle.USER_CURRENT
            )
        }

    val isFastChargeSupported: Boolean
        get() = try {
            healthInterface?.isFastChargeSupported() == true
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to read fast charge support", e)
            false
        }

    var fastChargeMode: Int
        get() {
            val health = healthInterface ?: return FastChargeMode.NONE
            return try {
                val mode = health.getFastChargeMode()
                if (mode == 0) FastChargeMode.NONE else mode
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to read fast charge mode", e)
                FastChargeMode.NONE
            }
        }
        set(value) {
            val health = healthInterface ?: return
            try {
                health.setFastChargeMode(value)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to set fast charge mode", e)
            }
        }

    var highTouchPollingRate
        get() = LineageSettings.System.getIntForUser(
                resolver, LineageSettings.System.HIGH_TOUCH_POLLING_RATE_ENABLE, 1,
                UserHandle.USER_CURRENT
            ) == 1
        set(it) {
            LineageSettings.System.putIntForUser(
                resolver, LineageSettings.System.HIGH_TOUCH_POLLING_RATE_ENABLE,
                it.toInt(), UserHandle.USER_CURRENT
            )
        }

    var displayAutoOutdoor
        get() = LineageSettings.System.getIntForUser(
                resolver, LineageSettings.System.DISPLAY_AUTO_OUTDOOR_MODE, 1,
                UserHandle.USER_CURRENT
            ) == 1
        set(it) {
            LineageSettings.System.putIntForUser(
                resolver, LineageSettings.System.DISPLAY_AUTO_OUTDOOR_MODE,
                it.toInt(), UserHandle.USER_CURRENT
            )
        }

    private fun Boolean.toInt() = if (this) 1 else 0
}
