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

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.main.Main
import org.apache.commons.io.FileUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class FileToSmbSpecIT extends SmbSpecBase {
    static final HOST = "localhost"
    static final PORT = "4445"
    static final USER = "user"
    static final PASS = "pass"
    static final SHARE = "share"

    static final TEST_FILENAME = "file-to-smb.txt"
    static final NEW_CONTENT = "Hello, camel-smbj!"

    static final OUTPUT_DIR = "output"

    def getSmbUri() {
        return "smb2://${HOST}:${PORT}/${SHARE}?username=${USER}&password=${PASS}"
    }

    def getSambaRootDir() {
        if (isWindows())
            return "c:\\temp\\camel-smbj"
        else
            return "/tmp/camel-smbj"
    }

    def isWindows() {
        return (System.properties['os.name'].toLowerCase().contains('windows'))
    }

    def setup() {
        //clean samba target directory
        File directory = new File(getSambaRootDir())
        FileUtils.cleanDirectory(directory)
        //prepare file to copy
        File subDir = new File("to-smb")
        if (!subDir.exists())
            subDir.mkdir()
        //clean source directory
        FileUtils.cleanDirectory(subDir)
        File target = new File(Paths.get("to-smb", TEST_FILENAME).toString())
        FileUtils.writeStringToFile(target, NEW_CONTENT, StandardCharsets.UTF_8)
    }

    def "one file from file to smb root"() {
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb")
                        .to("smb2://localhost:4445/share/?username=user&password=pass")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get(getSambaRootDir(), TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == NEW_CONTENT
    }

    def "one file from file to smb subdirectory"() {
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb")
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == NEW_CONTENT
    }


    def "file to smb with doneFileName"() {
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb")
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&doneFileName=\${file:name}.done") //escaping ${} in Groovy with a backslash
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        File doneFile = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME + ".done").toString())
        doneFile.exists()
        doneFile.length() == 0
    }

    def "more files from file to smb subdirectory"() {
        given:
        for (def i = 0; i < 10; i++) {
            def name = "test" + i + ".txt"
            File target = new File(Paths.get("to-smb", name).toString())
            FileUtils.writeStringToFile(target, "data" + i, StandardCharsets.UTF_8)
        }
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb")
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File targetDirectory = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR).toString())
        //1 original file + 10 more
        targetDirectory.list().size() == 11
        File target1 = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target1.exists()
        FileUtils.readFileToString(target1, StandardCharsets.UTF_8) == NEW_CONTENT
        for (def i = 0; i < 10; i++) {
            File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, "test" + i + ".txt").toString())
            target.exists()
            FileUtils.readFileToString(target, StandardCharsets.UTF_8) == "data" + i
        }
    }

    def "file to smb with autoCreate=false should fail"() {
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&autoCreate=false")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()
        then:
        //directory not created
        File directory = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR).toString())
        !directory.exists()
        //file does not exist
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        !target.exists()
    }

    def "recursive file to smb with flatten=true should flatten"() {
        given:
        def additional_files = 5
        for (def i = 0; i < additional_files; i++) {
            File dir = new File(Paths.get("to-smb", "dir" + i).toString())
            dir.mkdirs()
            File src = new File(Paths.get("to-smb", "dir" + i, "file" + i).toString())
            FileUtils.writeStringToFile(src, "data" + i, StandardCharsets.UTF_8)
        }
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb?recursive=true")
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&flatten=true")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()
        then:
        File directory = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR).toString())
        directory.list().size() == additional_files + 1
        directory.list().sort() == ["file-to-smb.txt", "file0", "file1", "file2", "file3", "file4"]
    }

}