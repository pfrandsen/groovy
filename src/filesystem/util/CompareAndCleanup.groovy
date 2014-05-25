package filesystem.util

import groovy.util.logging.*

import java.security.MessageDigest
import java.util.logging.Level

/**
 * This class is used to compare two directory hierarchies, a "master" and a "clone".
 * It will remove all the files from clone that is in master and where the file content is identical.
 * After all the identical files have been removed, all the empty folders in clone will be removed.
 */
@Log
class CompareAndCleanup {

    private String mastersRoot = ""
    private String clonesRoot = ""
    private String[] folders = []
    private int filesDeleted = 0
    private int foldersDeleted = 0
    private int deleteFailures = 0
    private int cloneFileCount = 0
    private int cloneFolderCount = 0

    CompareAndCleanup(String mastersRoot, String clonesRoot, String[] folders) {
        this.mastersRoot = mastersRoot
        this.clonesRoot = clonesRoot
        this.folders = folders
    }

    int getFilesDeleted() {
        return filesDeleted
    }

    int getFoldersDeleted() {
        return foldersDeleted
    }

    int getDeleteFailures() {
        return deleteFailures
    }

    int getCloneFileCount() {
        return cloneFileCount
    }

    int getCloneFolderCount() {
        return cloneFolderCount
    }

    private static boolean filesEqual(File master, File clone) {
        int MB = 1024*1024
        def masterDigest = MessageDigest.getInstance("SHA1")
        def cloneDigest = MessageDigest.getInstance("SHA1")

        master.eachByte(MB) { byte[] buf, int bytesRead ->
            masterDigest.update(buf, 0, bytesRead);
        }
        clone.eachByte(MB) { byte[] buf, int bytesRead ->
            cloneDigest.update(buf, 0, bytesRead);
        }
        if (MessageDigest.isEqual(masterDigest.digest(), cloneDigest.digest())) {
            return true
        }
        return false
    }

    private boolean checkRootFolders() {
        if (! new File(mastersRoot).isDirectory()) {
            log.log(Level.SEVERE, "Master root folder '${mastersRoot}' does not exist.")
            return false
        }
        if (! new File(clonesRoot).isDirectory()) {
            log.log(Level.SEVERE, "Clone root folder '${clonesRoot}' does not exist.")
            return false
        }
        return true
    }

    private static boolean isDirectory(def path) {
        return new File(path).isDirectory()
    }

    private boolean checkFolders() {
        if (!checkRootFolders()) {
            return false
        }
        def check = []
        folders.each { folder ->
            check.add(mastersRoot + File.separator + folder)
            check.add(clonesRoot + File.separator + folder)
        }
        check.each {path ->
            if (! new File(path).isDirectory()) {
                log.log(Level.WARNING, "'${path}' does not exist or is not a folder.")
            }
        }
        return true
    }

    private void removeIfEmptyRecursive(File folder) {
        folder.eachDir() { dir ->
            removeIfEmptyRecursive(dir)
        }
        String[] files = folder.list()
        if (files != null && files.length == 0) {
            log.info("Deleting '${folder}'")
            if (!folder.delete()) {
                log.log(Level.WARNING, "Could not delete '${folder}'")
                deleteFailures++
            } else {
                foldersDeleted++
            }
        }
    }

    private void cleanToDelta(String master, String branch) {
        int masterLength = master.length()
        new File(master).eachFileRecurse() { file ->
            String masterPath = file.getPath()
            String branchPath = branch + masterPath.substring(masterLength)
            File branchFile = new File(branchPath)
            if (file.isDirectory()) {
                if (!branchFile.isDirectory()) {
                    if (branchFile.exists()) {
                        log.log(Level.WARNING, "Wrong filetype, expected folder but found file '${branchFile}'")
                    } else {
                        log.log(Level.WARNING, "Folder not found '${branchFile}'")
                    }
                }
            } else {
                if (branchFile.exists()) {
                    if (branchFile.isDirectory()) {
                        log.log(Level.WARNING, "Wrong filetype, expected file but found folder '${branchFile}'")
                    } else {
                        if (filesEqual(file, branchFile)) {
                            log.info("Deleting '${branchPath}'")
                            if (!branchFile.delete()) {
                                log.log(Level.WARNING, "Could not delete '${branchFile}'")
                                deleteFailures++
                            } else {
                                filesDeleted++
                            }
                        } else {
                            log.info("Keeping '${branchPath}'")
                        }
                    }
                } else {
                    log.log(Level.WARNING, "File not found '${branchFile}'")
                }
            }
        }
        removeIfEmptyRecursive(new File(branch))
    }

    public void doCleanup() {
        if (!checkFolders()) {
            log.log(Level.SEVERE, "Invalid root folder(s). No cleanup done.")
            return
        }
        folders.each { folder ->
            String master = mastersRoot + File.separator + folder
            String clone = clonesRoot + File.separator + folder
            // count files and folders in clone (for statistics)
            if (isDirectory(clone)) {
                cloneFolderCount++
                new File(clone).eachFileRecurse() { file ->
                    file.isDirectory() ? cloneFolderCount++ : cloneFileCount++
                }
            }
            if (isDirectory(master) && isDirectory(clone)) {
                log.info("Checking clone '${clone}' against master '${master}'")
                cleanToDelta(master, clone)
            } else {
                log.log(Level.WARNING, "Skipping ${folder} - master or clone does not exist (${master}, ${clone})")
            }
        }
    }

}
