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
import java.time.LocalDateTime
import java.time.ZoneOffset

data class TrashBinMetadata(
    var config: TrashBinConfig,
    var totalSize: Long,
    var files: List<TrashBinFile>
) {

    /**
     * Returns percent of trash bin memory used
     */
    fun getCapacity(): Int {
        val numOfFiles = files.size
        val totalBytes = totalSize
        var capacityNumOfFiles = 0
        var capacityBytes = 0
        if (config.retentionNumOfFiles != TrashBinConfig.RETENTION_NUM_OF_FILES) {
            capacityNumOfFiles = (numOfFiles / config.retentionNumOfFiles) * 100
        }
        if (config.retentionBytes != TrashBinConfig.RETENTION_BYTES_INFINITE) {
            capacityBytes = ((totalBytes / config.retentionBytes) * 100).toInt()
        }
        return if (capacityBytes > capacityNumOfFiles) {
            capacityBytes
        } else if (capacityNumOfFiles > capacityBytes) {
            capacityBytes
        } else {
            TrashBinConfig.TRASH_BIN_CAPACITY_INVALID
        }
    }

    fun getFilesWithDeletionCriteria(): List<TrashBinFile> {
        var totalBytes = totalSize
        var numOfFiles = files.size
        return files.sortedByDescending { it.deleteTime }.filter {
            file ->
            if (config.retentionNumOfFiles != TrashBinConfig.RETENTION_NUM_OF_FILES &&
                numOfFiles > config.retentionNumOfFiles
            ) {
                numOfFiles--
                return@filter true
            }
            if (config.retentionBytes != TrashBinConfig.RETENTION_BYTES_INFINITE &&
                totalBytes > config.retentionBytes
            ) {
                totalBytes -= file.sizeBytes
                return@filter true
            }
            if (config.retentionDays != TrashBinConfig.RETENTION_DAYS_INFINITE) {
                return@filter if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalDateTime.ofEpochSecond(
                        file.deleteTime
                            ?: (System.currentTimeMillis() / 1000),
                        0, ZoneOffset.UTC
                    ).plusDays(
                        config.retentionDays.toLong()
                    ).isBefore(LocalDateTime.now())
                } else {
                    val secondsToAdd: Long = (config.retentionDays * 24 * 60 * 60).toLong()
                    val newEpochSeconds: Long = (
                        file.deleteTime
                            ?: (System.currentTimeMillis() / 1000)
                        ) + secondsToAdd
                    newEpochSeconds < System.currentTimeMillis() / 1000
                }
            }
            return@filter false
        }
    }
}
