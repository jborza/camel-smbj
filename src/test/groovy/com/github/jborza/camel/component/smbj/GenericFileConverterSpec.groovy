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

package com.github.jborza.camel.component.smbj

import spock.lang.Specification

class GenericFileConverterSpec extends Specification {
    def "should correctly convert to GenericFile"() {
        expect:
        def conv = new GenericFileConverter()
        def smbFile = new SmbFile(isDirectory, fileName, fileLength, lastModified, isArchive, isHidden, isReadonly, isSystem)
        def currentRelativePath = ""
        def path = "share/directory"
        def file = conv.asGenericFile(path, smbFile, "share/directory/", currentRelativePath)
        file.getFile() == smbFile
        file.getFileName() == fileName
        file.getFileNameOnly() == fileName
        file.getRelativeFilePath() == fileName
        file.isDirectory() == isDirectory
        file.getFileLength() == fileLength
        file.lastModified == lastModified
        file.getExtendedAttributes().get("dos:archive") == isArchive
        file.getExtendedAttributes().get("dos:hidden") == isHidden
        file.getExtendedAttributes().get("dos:readonly") == isReadonly
        file.getExtendedAttributes().get("dos:system") == isSystem

        where:
        isDirectory | fileName   | fileLength    | lastModified   | isArchive | isHidden | isReadonly | isSystem
        true        | "subdir"   | 12345678910L  | 1261207195000L | true      | true     | true       | true
        false       | "file.txt" | 1234L         | 1261207195000L | true      | false    | false      | false
        false       | "file.txt" | 1234L         | 1261207195000L | false     | true     | false      | true
    }
}
