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
import io.chaldeaprjkt.gamespace.data.UserGame
import io.chaldeaprjkt.gamespace.utils.di.ServiceViewEntryPoint
import io.chaldeaprjkt.gamespace.utils.entryPointOf

class PackageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val systemSettings by lazy {
            context.entryPointOf<ServiceViewEntryPoint>().systemSettings()
        }

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
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
