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

import java.io.File

/**
 * path to store trash files and metadata. eg /storage/ID/.demo/TrashBinFiles
 * /storage/ID/.demo/metadata.json
 * triggerCleanupAutomatically - library automatically triggers a cleanup after every delete / move operation
 * setting this false means you're responsible to trigger the cleanup at your own discretion
 */
data class TrashBinConfig(
    val basePath: String,
    val retentionDays: Int,
    val retentionBytes: Long,
    val retentionNumOfFiles: Int,
    val deleteRogueFiles: Boolean,
    val triggerCleanupAutomatically: Boolean
) {

    companion object {
        const val RETENTION_DAYS_INFINITE = -1
        const val RETENTION_BYTES_INFINITE = -1L
        const val RETENTION_NUM_OF_FILES = -1
        const val TRASH_BIN_CAPACITY_INVALID = -1
        const val TRASH_BIN_DIR = "TrashBinFiles"
        const val TRASH_BIN_META_FILE = "metadata.json"
    }

    fun getTrashBinFilesDirectory(): String {
        val baseDir = File(basePath)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        val directory = File(basePath, TRASH_BIN_DIR)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return basePath + File.separator + TRASH_BIN_DIR
    }

    fun getMetaDataFilePath(): String {
        val file = File(basePath, TRASH_BIN_META_FILE)
        if (!file.exists()) {
            file.createNewFile()
        }
        return basePath + File.separator + TRASH_BIN_META_FILE
    }
}
