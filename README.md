Amaze Trash Bin library
---
Library responsible to manage files deleted, from filesystem, keep it in for some time and delete 
once the provided configurations are met

Usage
---
`
implementation 'com.github.TeamAmaze:AmazeTrashBin:x.y.z'
`

Proguard
---
`
-keep class com.amaze.trashbin.** { *; }
`

Overview
---
Helper methods to 
- move files to bin
- restore files from bin
- empty bin
- delete permanently
- list files in bin
- trigger cleanup
- trigger deletion of rogue files (not present in metadata)

Library maintains metadata as you call either methods in an external directory.  
Make sure the directory is not in app's cache as it might get removed as user re-installs the app.

Documentation
---
**TrashBin Constructor**
- Trashbin config - mandatory (see below)
- deletePermanentlyCallback (optional) - way to delete files, required by cleanup and retention job
- listTrashBinFilesCallback (optional) - way to list files in a directory, required by rogue job to list files in trashbin and delete any rogue data

**trash bin config.**
Deleted files will be at baseDir/TrashBinFiles/  
Suggested to keep baseDir as a directory starting with a dot (.) so that it's hidden
- baseStorageDir // warning: avoid using app cache directory as base storage dir because if user uninstall your app they'll lose all their files 
- retentionDays - days to keep the files in the deleted directory
- retentionBytes - bytes to keep till we start deleting the oldest files
- retentionNumOfFiles - num of files to keep till we start deleting the oldest files
- deleteRogueFiles - whether to delete files which aren't recorded in the metadata

**trash bin metadata**
- located at baseDir / metadata.json
- trashbin config
- total size of files in trash
- list of files

**trashBinFile**
defines a file structure in the bin. All the contracts require you to enclose file in a trashBinFile object.  
You'll be returned a list of path files that you should utilize to show list to user and to allow various operations to be performed.
- fileName - name of file (can be utilized to show to user in list of files in the bin)
- isDirectory - whether the file is a directory
- path - original path of file where it can be restored
- sizeBytes - size of file
- deleteTime - time at which file was deleted

Note: allowing to rename files in recycle bin is discouraged. It'll break the file and will be considered as rogue file 
(volunteer to be deleted in next cleanup cycle)

Library initializes by creating an object of TrashBin providing it a trashBinConfig object
As soon as it's initialized it creates the necessary directories / config files


### License:

    Copyright 2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
    Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
