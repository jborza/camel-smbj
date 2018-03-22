package com.github.jborza.camel.component.smbj;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;

public final class FileDirectoryAttributes {
    public static boolean isDirectory(FileIdBothDirectoryInformation info) {
        return (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_DIRECTORY) == SmbConstants.FILE_ATTRIBUTE_DIRECTORY;
    }

    public static long getLastModified(FileIdBothDirectoryInformation info) {
        return info.getLastWriteTime().toEpochMillis();
    }
}
