/*
 *  Copyright [2018] [Juraj Borza]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jborza.camel.component.smbj;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;

public final class FileDirectoryAttributes {
    public static final String DOS_ARCHIVE = "dos:archive";
    public static final String DOS_HIDDEN = "dos:hidden";
    public static final String DOS_READONLY = "dos:readonly";
    public static final String DOS_SYSTEM = "dos:system";

    public static boolean isDirectory(FileIdBothDirectoryInformation info) {
        return (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_DIRECTORY) == SmbConstants.FILE_ATTRIBUTE_DIRECTORY;
    }

    public static long getLastModified(FileIdBothDirectoryInformation info) {
        return info.getLastWriteTime().toEpochMillis();
    }

    public static boolean isArchive(FileIdBothDirectoryInformation info) {
        return (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_ARCHIVE) == SmbConstants.FILE_ATTRIBUTE_ARCHIVE;
    }

    public static boolean isHidden(FileIdBothDirectoryInformation info) {
        return (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_HIDDEN) == SmbConstants.FILE_ATTRIBUTE_HIDDEN;
    }

    public static boolean isReadOnly(FileIdBothDirectoryInformation info) {
        return (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_READONLY) == SmbConstants.FILE_ATTRIBUTE_READONLY;
    }

    public static boolean isSystem(FileIdBothDirectoryInformation info) {
        return (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_SYSTEM) == SmbConstants.FILE_ATTRIBUTE_SYSTEM;
    }
}
