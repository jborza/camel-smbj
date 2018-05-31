/**
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

public final class SmbFile {
    private final boolean isDirectory;
    private final boolean isArchive;
    private final boolean isHidden;
    private final boolean isReadOnly;
    private final boolean isSystem;
    private final String fileName;
    private final long fileLength;
    private final long lastModified;

    public SmbFile(boolean isDirectory, String fileName, long fileLength, long lastModified, boolean isArchive, boolean isHidden, boolean isReadOnly, boolean isSystem) {
        this.isDirectory = isDirectory;
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.lastModified = lastModified;
        this.isArchive = isArchive;
        this.isHidden = isHidden;
        this.isReadOnly = isReadOnly;
        this.isSystem = isSystem;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isArchive(){
        return isArchive;
    }

    public boolean isHidden(){
        return isHidden;
    }

    public boolean isReadOnly(){
        return isReadOnly;
    }

    public boolean isSystem(){
        return isSystem;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    /**
     * see @{@link java.io.File#lastModified()}
     * @return Last modified time, measured in milliseconds since the epoch
     */
    public long getLastModified() {
        return lastModified;
    }
}
