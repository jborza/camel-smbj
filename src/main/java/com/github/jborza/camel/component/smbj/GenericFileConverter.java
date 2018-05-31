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

import org.apache.camel.component.file.GenericFile;

import java.util.HashMap;
import java.util.Map;

public class GenericFileConverter {
    public GenericFile<SmbFile> asGenericFile(String path, SmbFile info, String endpointPath, String currentRelativePath) {
        GenericFile<SmbFile> f = new GenericFile<>();
        f.setAbsoluteFilePath(path + f.getFileSeparator() + info.getFileName());
        f.setAbsolute(true);
        f.setEndpointPath(endpointPath);
        f.setFileNameOnly(info.getFileName());
        f.setFileLength(info.getFileLength());
        f.setFile(info);
        f.setLastModified(info.getLastModified());
        f.setFileName(currentRelativePath + info.getFileName());
        f.setRelativeFilePath(info.getFileName());
        f.setDirectory(info.isDirectory());
        f.setExtendedAttributes(getExtendedAttributes(info));
        return f;
    }

    private Map<String,Object> getExtendedAttributes(SmbFile info){
        Map<String,Object> attrs = new HashMap<>();
        attrs.put(FileDirectoryAttributes.DOS_ARCHIVE,info.isArchive());
        attrs.put(FileDirectoryAttributes.DOS_HIDDEN,info.isHidden());
        attrs.put(FileDirectoryAttributes.DOS_READONLY,info.isReadOnly());
        attrs.put(FileDirectoryAttributes.DOS_SYSTEM,info.isSystem());
        return attrs;
    }
}
