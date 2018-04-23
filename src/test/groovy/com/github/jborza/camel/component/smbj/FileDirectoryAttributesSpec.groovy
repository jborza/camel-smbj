package com.github.jborza.camel.component.smbj

import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import spock.lang.Specification

class FileDirectoryAttributesSpec extends Specification {
    def "isDirectory test"() {
        setup:
        def attrs = Mock(FileIdBothDirectoryInformation)
        attrs.getFileAttributes() >> attributes
        expect:
        FileDirectoryAttributes.isDirectory(attrs) == isDirectory
        where:
        isDirectory | attributes
        true        | SmbConstants.FILE_ATTRIBUTE_DIRECTORY
        false       | 0
    }

    def "getLastModified test"() {
          setup:
        def attrs = Mock(FileIdBothDirectoryInformation)
        attrs.getLastWriteTime() >> FileTime.ofEpochMillis(1261207195000L)
        expect:
        FileDirectoryAttributes.getLastModified(attrs) == 1261207195000L
    }
}
