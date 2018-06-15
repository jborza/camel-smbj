/*
 * Copyright [2018] [Juraj Borza]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jborza.camel.component.smbj

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.connection.Connection
import spock.lang.Ignore
import spock.lang.Specification

class ConnectionCacheSpec extends Specification {
    static final HOST = "localhost"
    static final PORT = 445

    def "getConnection connects if no cached connection exists"() {
        given:
        def mockClient = Mock(SMBClient)
        def cache = new ConnectionCache(mockClient)
        when:
        def conn = cache.getConnection(HOST, PORT)
        then:
        1 * mockClient.connect(HOST, PORT)
    }

    def "getConnection doesn't reconnect when a cached existing connection exists"() {
        given:
        def mockClient = Mock(SMBClient)
        def cache = new ConnectionCache(mockClient)
        def conn1 = cache.getConnection(HOST, PORT)
        when:
        def conn2 = cache.getConnection(HOST, PORT)
        then:
        1 * mockClient.connect(HOST, PORT)
    }

    @Ignore("Needs investigation why does the mock claim it was invoked only once")
    def "getConnection reconnects if a cached connection is closed"() {
        given:
        def mockClient = Mock(SMBClient)
        //a mock of connection that is closed
        def mockConn = Mock(Connection)
        mockConn.isConnected() >> false
        def cache = new ConnectionCache(mockClient)
        mockClient.connect(_, _) >> mockConn
        def conn1 = cache.getConnection(HOST, PORT)
        when:
        def conn2 = cache.getConnection(HOST, PORT)
        then:
        2 * mockClient.connect(_, _)
    }

    def "getConnections returns connections"() {
        given:
        def mockClient = Mock(SMBClient)
        def cache = new ConnectionCache(mockClient)
        cache.getConnection(HOST, PORT)
        cache.getConnection("different_host", PORT)
        when:
        def connections = cache.getConnections()
        then:
        connections.size() == 2
    }
}