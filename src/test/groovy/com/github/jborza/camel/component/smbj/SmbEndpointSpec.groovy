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

import org.apache.camel.component.file.strategy.GenericFileNoOpProcessStrategy
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.language.SimpleExpression
import org.apache.camel.spi.ExchangeFactory
import org.apache.camel.spi.Language
import spock.lang.Specification

class SmbEndpointSpec extends Specification {
    def "should return correct metadata"() {
        when:
        def uri = "smb2://user@server.example.com/sharename?password=secret"
        def component = Mock(SmbComponent)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)
        endpoint.setCamelContext(createCamelContext())

        then:
        endpoint.getScheme() == "smb2"
        endpoint.getFileSeparator() == '/'
        endpoint.isAbsolute()
        !endpoint.isSingleton()
    }

    def "should create operations"() {
        when:
        def uri = "smb2://user@server.example.com/sharename?password=secret"
        def component = Mock(SmbComponent)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)
        endpoint.setCamelContext(createCamelContext())

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
        endpoint.setCamelContext(createCamelContext())

        //Camel usually sets this from URL
        endpoint.setDfs(expectedDfs)
        endpoint.isDfs() == expectedDfs

        where:
        expectedDfs << [true, false]
    }

    def "createProducer returns SmbProducer"() {
        given:
        def uri = "smb2://server/share?dfs=true"
        def component = Mock(SmbComponent)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)
        endpoint.setCamelContext(createCamelContext())

        when:
        def producer = endpoint.createProducer()
        then:
        producer in SmbProducer
    }


    def "createConsumer returns SmbConsumer"() {
        given:
        def context = createCamelContext()
        def uri = "smb2://server/share?dfs=true"
        def component = Mock(SmbComponent)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)
        endpoint.setCamelContext(context)
        endpoint.setProcessStrategy(new GenericFileNoOpProcessStrategy<SmbFile>())
        when:
        def consumer = endpoint.createConsumer(null)
        then:
        consumer in SmbConsumer
    }

    def createCamelContext() {
        def context = Mock(DefaultCamelContext)
        context.resolveLanguage(_) >> {

            Language lang = Mock(Language)
            lang.createExpression(_ as String) >> {
                return Mock(SimpleExpression)
            }
            return lang
        }
        context.adapt(_) >> {
            return context;
        }
        context.getExchangeFactory() >> {
            return Mock(ExchangeFactory)
        }
        return context
    }

    def "createConsumer throws exception if both delete and move are enabled"() {
        given:
        def context = createCamelContext()
        def uri = "smb2://server/share?delete=true&move=true"
        def component = new SmbComponent(context)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)
        endpoint.setCamelContext(context)
        //Camel usually sets this from URL
        endpoint.setDelete(true)
        endpoint.setMove(".moved")
        when:
        def consumer = endpoint.createConsumer(null)
        then:
        thrown(IllegalArgumentException)
    }

    def "when idempotent=true an idempotent repository should be set up"() {
        given:
        def uri = "smb2://server/share?idempotent=true"
        def component = Mock(SmbComponent)
        def config = new SmbConfiguration(new URI(uri))
        def endpoint = new SmbEndpoint(uri, component, config)
        endpoint.setCamelContext(createCamelContext())
        endpoint.setProcessStrategy(new GenericFileNoOpProcessStrategy<SmbFile>())
        //Camel usually sets this from URL
        endpoint.setIdempotent(Boolean.TRUE)
        def consumer = endpoint.createConsumer(null)
        when:
        def repo = endpoint.getIdempotentRepository()
        then:
        repo != null
    }
}
