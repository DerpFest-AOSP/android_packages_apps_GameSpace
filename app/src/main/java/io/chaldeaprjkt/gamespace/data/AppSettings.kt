/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: Chaldeaprjkt
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-FileCopyrightText: risingOS Android Project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.chaldeaprjkt.gamespace.data

import android.app.Service
import android.content.Context
import android.view.WindowManager
import androidx.preference.PreferenceManager
import io.chaldeaprjkt.gamespace.utils.dp
import io.chaldeaprjkt.gamespace.utils.statusbarHeight
import javax.inject.Inject

class AppSettings @Inject constructor(private val context: Context) {

    private val db by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private val wm by lazy { context.getSystemService(Service.WINDOW_SERVICE) as WindowManager }

    var x
        get() = db.getInt("offset_x", wm.maximumWindowMetrics.bounds.width() / 2)
        set(point) = db.edit().putInt("offset_x", point).apply()

    var y
        get() = db.getInt("offset_y", context.statusbarHeight + 8.dp)
        set(point) = db.edit().putInt("offset_y", point).apply()

    var showFps
        get() = db.getBoolean("show_fps", false)
        set(point) = db.edit().putBoolean("show_fps", point).apply()

    var noHeadsUp
        get() = db.getBoolean(KEY_HEADS_UP_DISABLE, true)
        set(it) = db.edit().putBoolean(KEY_HEADS_UP_DISABLE, it).apply()

    var noAutoBrightness
        get() = db.getBoolean(KEY_AUTO_BRIGHTNESS_DISABLE, true)
        set(it) = db.edit().putBoolean(KEY_AUTO_BRIGHTNESS_DISABLE, it).apply()

    var noThreeScreenshot
        get() = db.getBoolean(KEY_3SCREENSHOT_DISABLE, false)
        set(it) = db.edit().putBoolean(KEY_3SCREENSHOT_DISABLE, it).apply()

    var stayAwake
        get() = db.getBoolean(KEY_STAY_AWAKE, false)
        set(value) = db.edit().putBoolean(KEY_STAY_AWAKE, value).apply()

    var danmakuNotification
        get() = db.getBoolean(KEY_DANMAKU_NOTIFICATION_MODE, true)
        set(value) = db.edit().putBoolean(KEY_DANMAKU_NOTIFICATION_MODE, value).apply()

    var callsMode: Int
        get() = db.getString(KEY_CALLS_MODE, "0")?.toInt() ?: 0
        set(value) = db.edit().putString(KEY_CALLS_MODE, value.toString()).apply()

    var callsDelay: Int
        get() = db.getInt(KEY_CALLS_DELAY, 0)
        set(value) = db.edit().putInt(KEY_CALLS_DELAY, value).apply()

    var ringerMode: Int
        get() = db.getString(KEY_RINGER_MODE, "2")?.toInt() ?: 2
        set(value) = db.edit().putString(KEY_RINGER_MODE, value.toString()).apply()

    var menuOpacity: Int
        get() = db.getInt(KEY_MENU_OPACITY, 100)
        set(value) = db.edit().putInt(KEY_MENU_OPACITY, value).apply()

    var doubleTaptoSleep
        get() = db.getBoolean(KEY_DOUBLE_TAP_TO_SLEEP,true)
        set(value) = db.edit().putBoolean(KEY_DOUBLE_TAP_TO_SLEEP,value).apply()

    var lockGesture
        get() = db.getBoolean(KEY_LOCK_GESTURE, false)
        set(value) = db.edit().putBoolean(KEY_LOCK_GESTURE, value).apply()

    var edgeCutout
        get() = db.getBoolean(KEY_EDGE_CUTOUT, false)
        set(value) = db.edit().putBoolean(KEY_EDGE_CUTOUT, value).apply()

    var autoGameDetect
        get() = db.getBoolean(KEY_AUTO_GAME_DETECT, true)
        set(value) = db.edit().putBoolean(KEY_AUTO_GAME_DETECT, value).apply()

    var noPulseBassHaptics
        get() = db.getBoolean(KEY_PULSE_BASS_DISABLE, true)
        set(it) = db.edit().putBoolean(KEY_PULSE_BASS_DISABLE, it).apply()

    companion object {
        const val KEY_HEADS_UP_DISABLE = "gamespace_heads_up_disabled"
        const val KEY_AUTO_BRIGHTNESS_DISABLE = "gamespace_auto_brightness_disabled"
        const val KEY_3SCREENSHOT_DISABLE = "gamespace_tfgesture_disabled"
        const val KEY_STAY_AWAKE = "gamespace_stay_awake"
        const val KEY_CALLS_MODE = "gamespace_calls_mode"
        const val KEY_CALLS_DELAY = "gamespace_calls_delay"
        const val KEY_DANMAKU_NOTIFICATION_MODE = "gamespace_danmaku_notification_mode"
        const val KEY_RINGER_MODE = "gamespace_ringer_mode"
        const val KEY_MENU_OPACITY = "gamespace_menu_opacity"
        const val KEY_DOUBLE_TAP_TO_SLEEP = "double_tap_sleep_gesture"
        const val KEY_LOCK_GESTURE = "gamespace_lock_gesture"
        const val KEY_EDGE_CUTOUT = "gamespace_edge_cutout"
        const val KEY_AUTO_GAME_DETECT = "gamespace_auto_game_detect"
        const val KEY_PULSE_BASS_DISABLE = "gamespace_pulse_bass_haptics_disabled"
    }
}
