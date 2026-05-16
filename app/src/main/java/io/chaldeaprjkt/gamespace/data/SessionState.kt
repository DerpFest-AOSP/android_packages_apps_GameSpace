/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: Chaldeaprjkt
 * SPDX-License-Identifier: Apache-2.0
 */
package io.chaldeaprjkt.gamespace.data

import android.media.AudioManager
import androidx.annotation.Keep

@Keep
data class SessionState(
    var packageName: String,
    var autoBrightness: Boolean? = null,
    var statusbarBrightness: Boolean? = null,
    var headsup: Boolean? = null,
    var threeScreenshot: Boolean? = null,
    var ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
    var doubleTapToSleep: Boolean? = null,
    var edgeCutout: Boolean? = null,
    var pulseHaptics: Int = 0,
)
