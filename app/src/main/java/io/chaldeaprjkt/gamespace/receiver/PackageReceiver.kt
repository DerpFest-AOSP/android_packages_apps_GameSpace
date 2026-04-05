/*
 * Copyright (C) 2025 LibreMobileOS Foundation
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
package io.chaldeaprjkt.gamespace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.UserGame
import io.chaldeaprjkt.gamespace.utils.di.ServiceViewEntryPoint
import io.chaldeaprjkt.gamespace.utils.entryPointOf

class PackageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val systemSettings by lazy {
            context.entryPointOf<ServiceViewEntryPoint>().systemSettings()
        }
        val appSettings by lazy {
            context.entryPointOf<ServiceViewEntryPoint>().appSettings()
        }

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    return
                }
                if (!appSettings.autoGameDetect) {
                    return
                }
                val packageName = intent.data?.schemeSpecificPart ?: return
                try {
                    val flags = PackageManager.ApplicationInfoFlags.of(
                        PackageManager.GET_META_DATA.toLong()
                    )
                    val appInfo = context.packageManager.getApplicationInfo(packageName, flags)
                    if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                        val userGames = systemSettings.userGames.toMutableList()
                        userGames.add(UserGame(packageName))
                        systemSettings.userGames = userGames.distinctBy { it.packageName }
                        val appName = appInfo.loadLabel(context.packageManager)
                        val message = context.getString(R.string.game_added_to_library, appName)
                        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                        val layout = inflater.inflate(R.layout.game_added_toast, null)
                        val icon = layout.findViewById<ImageView>(R.id.toast_icon)
                        icon?.setImageDrawable(appInfo.loadIcon(context.packageManager))
                        val text = layout.findViewById<TextView>(R.id.toast_text)
                        text?.text = message
                        with (Toast(context)) {
                            duration = Toast.LENGTH_SHORT
                            view = layout
                            show()
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // nothing to do
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val packageName = intent.data?.schemeSpecificPart ?: return
                val userGames = systemSettings.userGames.toMutableList()
                if (userGames.removeIf { it.packageName == packageName }) {
                    systemSettings.userGames = userGames
                }
            }
        }
    }
}
