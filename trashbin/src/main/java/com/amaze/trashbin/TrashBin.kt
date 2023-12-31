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

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileReader
import java.io.FileWriter

typealias DeletePermanentlyCallback = (deletePath: String) -> Boolean
typealias MoveFilesCallback = (source: String, dest: String) -> Boolean
typealias ListTrashBinFilesCallback = (parentTrashBinPath: String) -> List<TrashBinFile>

/**
 * Class responsible to invoke trash bin functions. All the functions are NOT thread safe.
 * You're advised to call them in background threads.
 * @param trashConfig - configuration for trashbin
 * @param deletePermanentlySuperCallback - callback to invoke for cleanup automatically when any bin action is performed.
 * Pass null if you want to invoke cleanup manually through triggerCleanup. This will be executed in the same thread where your bin functions are executed.
 */
class TrashBin constructor(
    context: Context,
    doTriggerAutoCleanup: Boolean,
    var trashConfig: TrashBinConfig,
    var deletePermanentlySuperCallback: DeletePermanentlyCallback?,
    var listTrashBinFilesSuperCallback:
        ListTrashBinFilesCallback? = null
) {

    private var metadata: TrashBinMetadata? = null

    init {
        trashConfig.getTrashBinFilesDirectory()
        metadata = getTrashBinMetadata()
        if (deletePermanentlySuperCallback != null && doTriggerAutoCleanup &&
            trashConfig.getCleanupIntervalHours() != -1
        ) {
            // check for auto trigger criteria
            val sharedPreferences = context.getSharedPreferences(
                "${context.packageName}.com.amaze.trashbin",
                Context.MODE_PRIVATE
            )
            val lastCleanup = sharedPreferences.getLong(
                "com.amaze.trashbin.lastCleanup",
                0
            )
            val currentTime = System.currentTimeMillis()
            val hours = ((currentTime - lastCleanup) / (1000 * 60 * 60))
            Log.i(
                javaClass.simpleName,
                "auto cleanup pending minutes " +
                    "$hours and interval ${trashConfig.getCleanupIntervalHours()}"
            )
            if (hours >= trashConfig.getCleanupIntervalHours()) {
                Log.i(javaClass.simpleName, "triggering auto cleanup for trash bin")
                GlobalScope.launch(Dispatchers.IO) {
                    triggerCleanup(deletePermanentlySuperCallback!!)
                    sharedPreferences.edit().putLong(
                        "com.amaze.trashbin.lastCleanup",
                        currentTime
                    ).apply()
                }
            }
        }
    }

    fun deletePermanently(
        files: List<TrashBinFile>,
        deletePermanentlyCallback: DeletePermanentlyCallback,
        doTriggerCleanup: Boolean = true
    ): Boolean {
        if (files.isEmpty()) {
            Log.i(javaClass.simpleName, "Empty files list to delete permanently")
            return true
        }
        var totalSize = 0L
        val filesMetadata = ArrayList(getTrashBinMetadata().files)
        files.forEach {
            // try to find file in metadata
            var indexToRemove = -1
            for (i in filesMetadata.indices) {
                if (it.path == filesMetadata[i].path) {
                    indexToRemove = i
                    break
                }
            }
            if (indexToRemove != -1) {
                // found file in metadata, call delete with trash bin path
                val didDelete = deletePermanentlyCallback.invoke(it.getDeletedPath(trashConfig))
                if (didDelete) {
                    filesMetadata.removeAt(indexToRemove)
                    Log.w(
                        javaClass.simpleName,
                        "TrashBin: deleting file in trashbin " +
                            it.path
                    )
                }
            } else {
                // file not found in metadata, call delete on original file
                deletePermanentlyCallback.invoke(it.path)
                Log.w(javaClass.simpleName, "TrashBin: deleting original file " + it.path)
            }
        }

        filesMetadata.forEach {
            totalSize += it.sizeBytes
        }
        filesMetadata.sortedBy {
            trashBinFile ->
            trashBinFile.deleteTime?.times(-1)
        }
        writeMetadataAndTriggerCleanup(filesMetadata, totalSize, doTriggerCleanup)
        return true
    }

    fun moveToBin(
        files: List<TrashBinFile>,
        doTriggerCleanup: Boolean = true,
        moveFilesCallback: MoveFilesCallback
    ): Boolean {
        if (files.isEmpty()) {
            Log.i(javaClass.simpleName, "Empty files list to move to bin")
            return true
        }
        var totalSize: Long = metadata?.totalSize ?: 0L
        val filesMetadata = ArrayList(getTrashBinMetadata().files)
        files.forEach {
            val didMove = moveFilesCallback.invoke(it.path, it.getDeletedPath(trashConfig))
            if (didMove) {
                filesMetadata.add(it)
                totalSize += it.sizeBytes
            } else {
                Log.w(javaClass.simpleName, "Failed to move to bin " + it.path)
            }
        }
        filesMetadata.sortedByDescending {
            trashBinFile ->
            trashBinFile.deleteTime
        }
        writeMetadataAndTriggerCleanup(filesMetadata, totalSize, doTriggerCleanup)
        return true
    }

    fun restore(
        files: List<TrashBinFile>,
        doTriggerCleanup: Boolean = true,
        moveFilesCallback: MoveFilesCallback
    ): Boolean {
        if (files.isEmpty()) {
            Log.i(javaClass.simpleName, "Empty files list to restore")
            return true
        }
        var totalSize = 0L
        val filesMetadata = ArrayList(getTrashBinMetadata().files)
        files.forEach {
            val didMove = moveFilesCallback.invoke(it.getDeletedPath(trashConfig), it.path)
            if (didMove) {
                var indexToRemove = -1
                for (i in filesMetadata.indices) {
                    if (it.path == filesMetadata[i].path) {
                        indexToRemove = i
                        break
                    }
                }
                if (indexToRemove != -1) {
                    filesMetadata.removeAt(indexToRemove)
                }
            } else {
                Log.w(javaClass.simpleName, "Failed to restore from bin " + it.path)
            }
        }
        filesMetadata.forEach {
            totalSize += it.sizeBytes
        }
        filesMetadata.sortedBy {
            trashBinFile ->
            trashBinFile.deleteTime?.times(-1)
        }
        writeMetadataAndTriggerCleanup(filesMetadata, totalSize, doTriggerCleanup)
        return true
    }

    fun emptyBin(deletePermanentlyCallback: DeletePermanentlyCallback): Boolean {
        return deletePermanently(
            metadata?.files ?: emptyList(),
            deletePermanentlyCallback, true
        )
    }

    fun restoreBin(moveFilesCallback: MoveFilesCallback): Boolean {
        return restore(metadata?.files ?: emptyList(), true, moveFilesCallback)
    }

    fun listFilesInBin(): List<TrashBinFile> {
        return getTrashBinMetadata().files
    }

    fun getConfig(): TrashBinConfig {
        return trashConfig
    }

    fun setConfig(trashBinConfig: TrashBinConfig) {
        trashConfig = trashBinConfig
    }

    /**
     * Returns metadata info of trashbin, such as trash path
     * current size, Cleanup period etc
     */
    fun getTrashBinMetadata(): TrashBinMetadata {
        return metadata ?: loadMetaDataJSONFile()
    }

    private fun loadMetaDataJSONFile(): TrashBinMetadata {
        val metadataType = object : TypeToken<TrashBinMetadata?>() {}.type
        val reader = JsonReader(FileReader(trashConfig.getMetaDataFilePath()))

        try {
            val gson = Gson()
            reader.use {
                metadata = gson.fromJson(reader, metadataType)
                if (metadata == null) {
                    metadata = TrashBinMetadata(trashConfig, 0L, emptyList())
                } else {
                    metadata?.config = trashConfig
                }
                writeMetaDataJSONFile(metadata!!)
            }
        } catch (e: Exception) {
            Log.w(javaClass.simpleName, "Failed to load metadata", e)
            metadata = TrashBinMetadata(trashConfig, 0L, emptyList())
        }
        return metadata!!
    }

    /**
     * Do note this operation is not thread safe, you're supposed to execute this on your own accord
     * if you call this manually
     */
    fun triggerCleanup(deletePermanentlyCallback: DeletePermanentlyCallback): Boolean {
        val filesToDelete = metadata?.getFilesWithDeletionCriteria()
        if (!filesToDelete.isNullOrEmpty()) {
            deletePermanently(filesToDelete, deletePermanentlyCallback, false)
        }
        return true
    }

    /**
     * impacts performance, removes physical file if not present in metadata,
     * or removes from metadata if physical file not present
     */
    fun removeRogueFiles(
        files: List<TrashBinFile>,
        listTrashBinFilesCallback: ListTrashBinFilesCallback,
        deletePermanentlyCallback:
            DeletePermanentlyCallback
    ): Boolean {
        val physicalFilesList = listTrashBinFilesCallback
            .invoke(trashConfig.getTrashBinFilesDirectory())
        if (physicalFilesList.size > files.size) {
            for (i in physicalFilesList.indices) {
                var foundPhysicalFile = false
                for (j in files.indices) {
                    if (physicalFilesList[i].path == files[j].path) {
                        foundPhysicalFile = true
                        break
                    }
                }
                if (!foundPhysicalFile) {
                    deletePermanently(
                        listOf(physicalFilesList[i]), deletePermanentlyCallback,
                        false
                    )
                }
            }
        } else {
            val mutableMetaFiles = ArrayList(files)
            for (i in mutableMetaFiles.indices) {
                var foundFileMetadata = false
                for (j in physicalFilesList.indices) {
                    if (physicalFilesList[i].path == mutableMetaFiles[j].path) {
                        foundFileMetadata = true
                        break
                    }
                }
                if (!foundFileMetadata) {
                    mutableMetaFiles.removeAt(i)
                }
            }
            metadata?.files = mutableMetaFiles
            writeMetaDataJSONFile(metadata!!)
        }
        return true
    }

    fun writeMetadataAndTriggerCleanup(
        files: List<TrashBinFile>,
        totalSize: Long,
        doTriggerCleanup: Boolean = true
    ) {
        metadata?.config = trashConfig
        metadata?.files = files
        metadata?.totalSize = totalSize
        if (trashConfig.deleteRogueFiles && listTrashBinFilesSuperCallback != null &&
            deletePermanentlySuperCallback != null
        ) {
            // trigger rogue file deletion
            removeRogueFiles(
                files, listTrashBinFilesSuperCallback!!,
                deletePermanentlySuperCallback!!
            )
        } else {
            writeMetaDataJSONFile(metadata!!)
        }
        // trigger the Cleanup
        if (doTriggerCleanup && deletePermanentlySuperCallback != null) {
            triggerCleanup(deletePermanentlySuperCallback!!)
        }
    }

    private fun writeMetaDataJSONFile(meta: TrashBinMetadata) {
        FileWriter(trashConfig.getMetaDataFilePath()).use { writer ->
            val gson = GsonBuilder().serializeNulls().create()
            gson.toJson(meta, writer)
        }
    }
}
