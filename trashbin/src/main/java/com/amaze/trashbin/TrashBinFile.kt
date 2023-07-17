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
data class TrashBinFile(val fileName: String, val isDirectory: Boolean, val path: String, val sizeBytes: Long,
                        var deleteTime: Long? = null) {

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
        return config.basePath + File.separator + TrashBinConfig.TRASH_BIN_DIR + File.separator + fileName
    }
}