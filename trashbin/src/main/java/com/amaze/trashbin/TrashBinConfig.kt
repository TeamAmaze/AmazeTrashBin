package com.amaze.trashbin

import android.os.Build
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * path to store trash files and metadata. eg /storage/ID/.demo/TrashBinFiles
 * /storage/ID/.demo/metadata.json
 * triggerCleanupAutomatically - library automatically triggers a cleanup after every delete / move operation
 * setting this false means you're responsible to trigger the cleanup at your own discretion
 */
data class TrashBinConfig(val basePath: String, val retentionDays: Int, val retentionBytes: Long,
                          val retentionNumOfFiles: Int,
                          val deleteRogueFiles: Boolean, val triggerCleanupAutomatically: Boolean) {

    companion object {
        const val RETENTION_DAYS_INFINITE = -1
        const val RETENTION_BYTES_INFINITE = -1L
        const val RETENTION_NUM_OF_FILES = -1
        const val TRASH_BIN_CAPACITY_INVALID = -1
        const val TRASH_BIN_DIR = "TrashBinFiles"
        const val TRASH_BIN_META_FILE = "metadata.json"
    }

    fun getTrashBinFilesDirectory(): String {
        val directory = File(basePath + File.separator + TRASH_BIN_DIR)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return basePath + File.separator + TRASH_BIN_DIR
    }

    fun getMetaDataFilePath(): String {
        val file = File(basePath + File.separator + TRASH_BIN_META_FILE)
        if (!file.exists()) {
            file.createNewFile()
        }
        return basePath + File.separator + TRASH_BIN_META_FILE
    }
}

data class TrashBinMetadata(var config: TrashBinConfig, var totalSize: Long, var files: List<TrashBinFile>) {

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
        return files.sortedBy { it.deleteTime }.filter {
            file ->
            if (config.retentionNumOfFiles != TrashBinConfig.RETENTION_NUM_OF_FILES && numOfFiles > config.retentionNumOfFiles) {
                numOfFiles--
                return@filter true
            }
            if (config.retentionBytes != TrashBinConfig.RETENTION_BYTES_INFINITE && totalBytes > config.retentionBytes) {
                totalBytes -= file.sizeBytes
                return@filter true
            }
            if (config.retentionDays != TrashBinConfig.RETENTION_DAYS_INFINITE) {
                return@filter if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalDateTime.ofEpochSecond(file.deleteTime ?: (System.currentTimeMillis()/1000), 0, ZoneOffset.UTC).plusDays(
                        config.retentionDays.toLong()
                    ).isBefore(LocalDateTime.now())
                } else {
                    val secondsToAdd: Long = (config.retentionDays * 24 * 60 * 60).toLong()
                    val newEpochSeconds: Long = (file.deleteTime ?: (System.currentTimeMillis() / 1000)) + secondsToAdd
                    newEpochSeconds < System.currentTimeMillis()/1000
                }
            }
            return@filter false
        }
    }
}