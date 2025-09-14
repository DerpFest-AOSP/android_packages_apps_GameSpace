/*
 * Copyright (C) 2021 Chaldeaprjkt
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
package io.chaldeaprjkt.gamespace

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.HiltAndroidApp
import io.chaldeaprjkt.gamespace.receiver.PackageReceiver

@HiltAndroidApp(Application::class)
class GameSpace : Hilt_GameSpace() {
    private val packageReceiver = PackageReceiver()

    override fun onCreate() {
        super.onCreate()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, intentFilter)
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(packageReceiver)
    }
}