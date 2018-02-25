package com.github.jborza.camel.component.smbj;

public final class SmbFile {
    private final boolean isDirectory;
    private final String fileName;
    private final long fileLength;
    private final long lastModified;

    public SmbFile(boolean isDirectory, String fileName, long fileLength, long lastModified) {
        this.isDirectory = isDirectory;
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.lastModified = lastModified;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    /**
     * Last modified time, measured in milliseconds since the epoch
     * see @{@link java.io.File#lastModified()}
     */
    public long getLastModified() {
        return lastModified;
    }
}
