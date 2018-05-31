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

    def "attributes test"() {
        setup:
        def f = Mock(FileIdBothDirectoryInformation)
        f.getFileAttributes() >> fileAttributes
        expect:
        FileDirectoryAttributes.isArchive(f) == archive
        FileDirectoryAttributes.isHidden(f) == hidden
        FileDirectoryAttributes.isReadOnly(f) == readonly
        FileDirectoryAttributes.isSystem(f) == system
        where:
        fileAttributes          | archive   | hidden | readonly | system
        0	|	false	|	false	|	false	|	false
        4	|	false	|	false	|	false	|	true
        1	|	false	|	false	|	true	|	false
        5	|	false	|	false	|	true	|	true
        2	|	false	|	true	|	false	|	false
        6	|	false	|	true	|	false	|	true
        3	|	false	|	true	|	true	|	false
        7	|	false	|	true	|	true	|	true
        32	|	true	|	false	|	false	|	false
        36	|	true	|	false	|	false	|	true
        33	|	true	|	false	|	true	|	false
        37	|	true	|	false	|	true	|	true
        34	|	true	|	true	|	false	|	false
        38	|	true	|	true	|	false	|	true
        35	|	true	|	true	|	true	|	false
        39	|	true	|	true	|	true	|	true


    }
}
