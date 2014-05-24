package filesystem.util

import groovy.util.logging.*

import java.security.MessageDigest
import java.util.logging.Level

/**
 * This class is used to compare two directory hierarchies, a "master" and a "clone".
 * It will remove all the files from clone that is in master and where the file content is identical.
 * After all the identical files have been removed, all the empty folders in clone will be removed
 */
@Log
class CompareAndCleanup {

    private String mastersRoot = ""
    private String clonesRoot = ""
    private String[] folders = []
    private int filesDeleted = 0
    private int foldersDeleted = 0

    int getFilesDeleted() {
        return filesDeleted
    }

    int getFoldersDeleted() {
        return foldersDeleted
    }

    private boolean filesEqual(File master, File clone) {
        int MB = 1024*1024
        def masterDigest = MessageDigest.getInstance("SHA1")
        def branchDigest = MessageDigest.getInstance("SHA1")

        master.eachByte(MB) { byte[] buf, int bytesRead ->
            masterDigest.update(buf, 0, bytesRead);
        }
        clone.eachByte(MB) { byte[] buf, int bytesRead ->
            branchDigest.update(buf, 0, bytesRead);
        }
        if (MessageDigest.isEqual(masterDigest.digest(), branchDigest.digest())) {
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

    public void doCleanup() {
        if (!checkFolders()) {
            log.log(Level.SEVERE, "Invalid root folder(s). No cleanup done.")
            return
        }

    }

    private void removeIfEmptyRecursive(File folder) {
        folder.eachDir() { dir ->
            removeIfEmptyRecursive(dir)
        }
        String[] files = folder.list()
        if (files != null && files.length == 0) {
            println "LOG INFO: Deleting '${folder}'"
            if (!folder.delete()) {
                println "LOG ERROR: Could not delete '${folder}'"
            }
        }
    }

    CompareAndCleanup(String mastersRoot, String clonesRoot, String[] folders) {
        this.mastersRoot = mastersRoot
        this.clonesRoot = clonesRoot
        this.folders = folders
    }

}
