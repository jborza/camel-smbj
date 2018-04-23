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

class SmbConfigurationSpec extends Specification {
    def "should correctly process domain, username and password"() {
        expect:
        def cfg = new SmbConfiguration(new URI(uri))
        cfg.getUsername() == username
        cfg.getPassword() == password
        cfg.getDomain() == domain

        where:
        uri                                       | username | password   | domain
        "smb2://mydomain;user:password@127.0.0.1" | "user"   | "password" | "mydomain"
        "smb2://user:password@127.0.0.1"          | "user"   | "password" | null
        "smb2://user@127.0.0.1"                   | "user"   | null       | null
    }

    def "should process port"() {
        expect:
        def cfg = new SmbConfiguration(new URI(uri))
        cfg.getPort() == port

        where:
        uri                                  | port
        "smb2://127.0.0.1"                   | -1
        "smb2://127.0.0.1:139"               | 139
        "smb2://user:password@127.0.0.1:139" | 139
        "smb2://a.computer.name:445"         | 445
    }

    def "should process hostname"() {
        expect:
        def cfg = new SmbConfiguration(new URI(uri))
        cfg.getHost() == host

        where:
        uri                                          | host
        "smb2://127.0.0.1"                           | "127.0.0.1"
        "smb2://a.computer.name:139"                 | "a.computer.name"
        "smb2://user:password@host:445/share/subdir" | "host"
    }

    def "should process share and path"() {
        //smb://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]]]][?options]
        expect:
        def cfg = new SmbConfiguration(new URI(uri))
        cfg.getPath() == path
        cfg.getShare() == share

        where:
        uri                             | share   | path
        "smb2://host/"                  | ""    | "/"
        "smb2://host/share/"            | "share" | "/"
        "smb2://host/share/dir/subdir/" | "share" | "dir/subdir/"
    }

    def "should process smb host path"() {
        expect:
        def cfg = new SmbConfiguration(new URI(uri))
        cfg.getSmbHostPath() == hostPath

        where:
        uri                                                                 | hostPath
        "smb2://host"                                                       | "smb://host/"
        "smb2://host:139/folder/"                                           | "smb://host:139/"
        "smb2://username:password@host:445/folder/"                         | "smb://host:445/"
        "smb2://domain;username:password@a.server.com:445/folder/subfolder" | "smb://a.server.com:445/"
    }

}