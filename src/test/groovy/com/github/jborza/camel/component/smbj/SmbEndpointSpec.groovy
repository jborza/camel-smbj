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

class SmbEndpointSpec extends Specification {
    def "should return correct metadata"() {
        when:
        def uri = "smb2://user@server.example.com/sharename?password=secret"
        def component = Mock(SmbComponent)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)

        then:
        endpoint.getScheme() == "smb2"
        endpoint.getFileSeparator() == "/"
        endpoint.isAbsolute() == true
        endpoint.isSingleton() == false
    }

    def "should create operations"() {
        when:
        def uri = "smb2://user@server.example.com/sharename?password=secret"
        def component = Mock(SmbComponent)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)
        def operations = endpoint.createSmbOperations()

        then:
        operations != null
    }

    def "dfs attribute is set up"() {
        expect:
        def uri = "smb2://server/share?dfs=true"
        def component = Mock(SmbComponent)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)
        endpoint.setDfs(expectedDfs)
        endpoint.isDfs() == expectedDfs

        where:
        expectedDfs << [true, false]
    }
}
