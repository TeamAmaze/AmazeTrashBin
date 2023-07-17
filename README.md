Amaze Trash Bin library
---
Library responsible to manage files deleted, from filesystem, keep it in for some time and delete 
once the provided configurations are met

*Notes*
trash bin config.
Deleted files will be at baseDir/TrashBinFiles/
Suggested to keep baseDir as a directory starting with a dot (.) so that it's hidden
- baseStorageDir / TrashBinFiles / (deleted files) // warning: avoid using app cache directory as base storage dir because if user uninstall your app they'll lose all their files 
- retentionDays - days to keep the files in the deleted directory
- retentionBytes - bytes to keep till we start deleting the oldest files
- deleteRogueFiles - whether to delete files which aren't recorded in the metadata

trash bin metadata
- located at baseDir / TrashBinFiles / metadata.json
struct:
- trashbin config
- total size of files in trash
- list of files

trashBinFile
defines a file structure in the bin. All the contracts require you to enclose file in a trashBinFile object.
You'll be returned a list of path files that you should utilize to show list to user and to allow various operations to be performed.
- fileName - name of file (can be utilized to show to user in list of files in the bin)
- isDirectory - whether the file is a directory
- path - original path of file where it can be restored
- sizeBytes - size of file
- deletedPath - deleted path of file
- deleteTime - time at which file was deleted

contract
- deletePermanently - deletes the files permanently
- moveToBin - deletes file temporarily
- triggerCleanup - triggers a cleanup manually based on the criteria defined in config
- emptyBin - deletes all files permanently
- restore - restore set of files back to their original location
Note: allowing to rename files in recycle bin is discouraged. It'll break the file and will be considered as rogue file 
(volunteer to be deleted in next cleanup cycle)
- 
Library initializes by creating an object of TrashBin providing it a trashBinConfig object
As soon as it's initalized it creates the necessary directories / config files