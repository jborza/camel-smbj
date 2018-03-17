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
