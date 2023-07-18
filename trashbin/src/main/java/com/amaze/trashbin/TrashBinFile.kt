/**
 * Copyright 2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
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
package com.amaze.trashbin

import android.os.Build
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * @param fileName name of file
 * @param isDirectory whether file is directory
 * @param path original path of file
 * @param sizeBytes size of file
 * @param deleteTime time of deletion, provide custom or initialized by default implementation to current time
 */
data class TrashBinFile(
    val fileName: String,
    val isDirectory: Boolean,
    val path: String,
    val sizeBytes: Long,
    var deleteTime: Long? = null
) {

    init {
        if (deleteTime == null) {
            deleteTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            } else {
                System.currentTimeMillis() / 1000
            }
        }
    }

    fun getDeletedPath(config: TrashBinConfig): String {
        return config.basePath + File.separator + TrashBinConfig.TRASH_BIN_DIR +
            File.separator + fileName
    }
}
