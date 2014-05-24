package filesystem.util

/**
 * This class is used to compare two directory hierarchies, a "master" and a "clone".
 * It will remove all the files from clone that is in master and where the file content is identical.
 * After all the identical files have been removed, all the empty folders in clone will be removed
 */
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

    public void doCleanup() {

    }

    CompareAndCleanup(String mastersRoot, String clonesRoot, String[] folders) {
        this.mastersRoot = mastersRoot
        this.clonesRoot = clonesRoot
        this.folders = folders
    }

}
