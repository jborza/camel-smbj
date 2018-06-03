/**
 *  Copyright [2018] [Juraj Borza]
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.main.Main
import org.apache.commons.io.FileUtils
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class SmbToFileSpecIT extends Specification {
    static final HOST = "localhost"
    static final PORT = "4445"
    static final USER = "user"
    static final PASS = "pass"
    static final SHARE = "share"

    final CONTENT = "Hello, SmbToFile content!"

    def getSmbUri() {
        return "smb2://${HOST}:${PORT}/${SHARE}?username=${USER}&password=${PASS}"
    }

    def getTempDir() {
        if (isWindows())
            return "c:\\temp\\camel-smbj"
        else
            return "/tmp/camel-smbj"
    }

    def isWindows() {
        return (System.properties['os.name'].toLowerCase().contains('windows'))
    }

    def setup() {
        //clear samba target directory
        File directory = new File(getTempDir())
        FileUtils.cleanDirectory(directory)
        //clear up destination
        File targetDirectory = new File("from-smb")
        FileUtils.cleanDirectory(targetDirectory)
    }

    def setupDirectoryWithFile() {
        File subDir = new File(Paths.get(getTempDir(), "dir").toString())
        subDir.mkdir()
        File srcFile = new File(Paths.get(getTempDir(), "dir", "test.txt").toString())
        FileUtils.writeStringToFile(srcFile, CONTENT, StandardCharsets.UTF_8)
    }

    def "one file from smb directory to file"() {
        when:
        setupDirectoryWithFile()
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/dir/?username=user&password=pass")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(10000)
        camelContext.stop()

        then:
        File target = new File(Paths.get("from-smb", "test.txt").toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == CONTENT
    }

    def "one file from smb root to file"() {
        when:
        File srcFile = new File(Paths.get(getTempDir(), "test.txt").toString())
        FileUtils.writeStringToFile(srcFile, CONTENT, StandardCharsets.UTF_8)
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/?username=user&password=pass")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(10000)
        camelContext.stop()

        then:
        File target = new File(Paths.get("from-smb", "test.txt").toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == CONTENT
    }

    def "more files from smb directory to file"() {
        when:
        for (def i = 0; i < 10; i++) {
            File srcFile = new File(Paths.get(getTempDir(), "dir", "test" + i + ".txt").toString())
            FileUtils.writeStringToFile(srcFile, "data" + i, StandardCharsets.UTF_8)
        }
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/dir/?username=user&password=pass")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(10000)
        camelContext.stop()

        then:
        File targetDirectory = new File("from-smb")
        targetDirectory.list().size() == 10
        for (def i = 0; i < 10; i++) {
            File target = new File(Paths.get("from-smb", "test" + i + ".txt").toString())
            target.exists()
            String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
            content == "data" + i
        }
    }

    def "recursive smb folder to file"() {
        when:
        //prepare subfolders
        File subDir1 = new File(Paths.get(getTempDir(), "a", "b", "c1").toString())
        subDir1.mkdirs()
        File subDir2 = new File(Paths.get(getTempDir(), "a", "b", "c2").toString())
        subDir2.mkdirs()

        File srcFile1 = new File(Paths.get(getTempDir(), "a", "b", "c1", "testabc1.txt").toString())
        FileUtils.writeStringToFile(srcFile1, CONTENT, StandardCharsets.UTF_8)
        File srcFile2 = new File(Paths.get(getTempDir(), "a", "b", "c2", "testabc2.txt").toString())
        FileUtils.writeStringToFile(srcFile2, CONTENT, StandardCharsets.UTF_8)
        File srcFile3 = new File(Paths.get(getTempDir(), "a", "b", "testab.txt").toString())
        FileUtils.writeStringToFile(srcFile3, CONTENT, StandardCharsets.UTF_8)

        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/a/?username=user&password=pass&recursive=true")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(10000)
        camelContext.stop()

        then:
        new File("from-smb\\b\\testab.txt").exists()
        new File("from-smb\\b\\c1\\testabc1.txt").exists()
        new File("from-smb\\b\\c2\\testabc2.txt").exists()
    }
}
