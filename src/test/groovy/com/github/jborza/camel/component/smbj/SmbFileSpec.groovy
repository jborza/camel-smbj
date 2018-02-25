package com.github.jborza.camel.component.smbj

import spock.lang.Specification

class SmbFileSpec extends Specification {
    def "should correctly process constructor"() {
        expect:
        def file = new SmbFile(isDirectory, fileName, fileLength, lastModified)
        file.isDirectory() == isDirectory
        file.getFileName() == fileName
        file.getFileLength() == fileLength
        file.lastModified == lastModified

        where:
        isDirectory | fileName   | fileLength   | lastModified
        true        | "file.fil" | 12345678910L | 1261207195000L
    }
}
