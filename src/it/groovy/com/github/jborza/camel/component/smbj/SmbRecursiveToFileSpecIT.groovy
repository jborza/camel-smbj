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

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class SmbRecursiveToFileSpecIT extends SmbSpecBase {
    final CONTENT = "Hello, SmbToFile content!"

    def setupSubDirectories() {
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
    }

    def "recursive smb folder to file simple"() {
        given:
        setupSubDirectories()
        when:
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

        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        new File(Paths.get("from-smb", "b", "testab.txt").toString()).exists()
        new File(Paths.get("from-smb", "b", "c1", "testabc1.txt").toString()).exists()
        new File(Paths.get("from-smb", "b", "c2", "testabc2.txt").toString()).exists()
    }

    def "recursive smb folder with flatten"() {
        given:
        setupSubDirectories()
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/a/?username=user&password=pass&recursive=true&flatten=true")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        new File(Paths.get("from-smb", "testab.txt").toString()).exists()
        new File(Paths.get("from-smb", "testabc1.txt").toString()).exists()
        new File(Paths.get("from-smb", "testabc2.txt").toString()).exists()
    }

    def "recursive smb folder to file with minDepth"() {
        given:
        setupSubDirectories()
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/a/?username=user&password=pass&recursive=true&minDepth=3")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        !new File(Paths.get("from-smb", "b", "testab.txt").toString()).exists()
        new File(Paths.get("from-smb", "b", "c1", "testabc1.txt").toString()).exists()
        new File(Paths.get("from-smb", "b", "c2", "testabc2.txt").toString()).exists()
    }

    def "recursive smb folder to file with maxDepth"() {
        given:
        setupSubDirectories()
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/a/?username=user&password=pass&recursive=true&maxDepth=2")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        new File(Paths.get("from-smb", "b", "testab.txt").toString()).exists()
        !new File(Paths.get("from-smb", "b", "c1", "testabc1.txt").toString()).exists()
        !new File(Paths.get("from-smb", "b", "c2", "testabc2.txt").toString()).exists()
    }
}