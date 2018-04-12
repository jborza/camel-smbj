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

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import org.apache.camel.component.file.FileComponent
import org.apache.camel.component.file.GenericFile
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultExchange
import org.apache.camel.impl.DefaultMessage
import org.apache.commons.io.IOUtils
import spock.lang.Specification

import java.nio.charset.Charset

class SmbOperationsSpec extends Specification {

    def SmbOperations ops
    def SmbClient mockSmbClient

    def setup() {
        def config = SmbConfig.builder().build()
        def client = new SMBClient(config)
        ops = new SmbOperations(client)
        mockSmbClient = Mock(SmbClient)
        def endpoint = Mock(SmbEndpoint)
        ops.setEndpoint(endpoint)
        ops.setSmbClient(mockSmbClient)
    }

    def "listFiles should invoke listFiles on client"() {
        when:
        ops.listFiles("directory")
        then:
        1 * mockSmbClient.listFiles("directory")
    }

    def "deleteFile should invoke deleteFile on client"() {
        when:
        ops.deleteFile("path/to/file")
        then:
        1 * mockSmbClient.deleteFile("path/to/file")
    }

    def "existsFile test"() {
        when:
        ops.existsFile("path/to/some/file")
        then:
        1 * mockSmbClient.fileExists("path/to/some/file")
    }

    def "storeFile test with InputStream"() {
        given:
        def ctx = new DefaultCamelContext()
        def exchange = new DefaultExchange(ctx)
        def message = new DefaultMessage(ctx)
        def stream = IOUtils.toInputStream("Hello camel-smbj!", Charset.defaultCharset())
        message.setBody(stream)
        exchange.setIn(message)
        when:
        ops.storeFile("path/to/file", exchange)
        then:
        1 * mockSmbClient.storeFile("path/to/file", !null)
    }

    def "retrieveFile test"() {
        given:
        def ctx = new DefaultCamelContext()
        def exchange = new DefaultExchange(ctx)
        def gf = new GenericFile<SmbFile>()
        exchange.setProperty(FileComponent.FILE_EXCHANGE_FILE,gf)
        when:
        ops.retrieveFile("path/to/file",exchange)
        then:
        1 * mockSmbClient.retrieveFile("path/to/file", !null)
    }

    def "renameFile test"() {
        when:
        ops.renameFile("directory/source_file.ext", "directory/target_file.ext")
        then:
        1 * mockSmbClient.renameFile("directory/source_file.ext", "directory/target_file.ext")
    }
}