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

import spock.lang.Specification

class SmbSpecBase extends Specification {
    def DEFAULT_CAMEL_CONTEXT_DURATION = 6000

    static final HOST = "localhost"
    static final PORT = "4445"
    static final USER = "user"
    static final PASS = "pass"
    static final SHARE = "share"

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
        return (System.getProperty('os.name').toLowerCase().contains('windows'))
    }
}