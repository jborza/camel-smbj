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

import java.nio.file.Paths

class SmbPathUtilsSpec extends Specification {
    def "should correctly remove share with windows separator"() {
        expect:
        result == SmbPathUtils.removeShareName(path, share, true)

        where:
        path                           | share   | result
        "share"                        | "share" | ""
        "share\\file.ext"              | "share" | "file.ext"
        "share\\dir\\file.ext"         | "share" | "dir\\file.ext"
        "share\\dir\\subdir\\file.ext" | "share" | "dir\\subdir\\file.ext"
    }

    def "should not remove share name on mismatch"() {
        expect:
        result == SmbPathUtils.removeShareName(path, share, true)

        where:
        path              | share | result
        "share\\file.ext" | "sha" | "share\\file.ext"
    }

    def "should work with native path separator"() {
        given:
        def share = "sharename"
        def path = Paths.get(share, "dir", "subdir").toString()

        when:
        def result = SmbPathUtils.removeShareName(path, share, false)
        then:
        result == Paths.get("dir", "subdir").toString()
        !result.startsWith(share)
    }

    def "should convert to backslashes"() {
        expect:
        result == SmbPathUtils.convertToBackslashes(path)

        where:
        path                       | result
        "dir"                      | "dir"
        "dir/subdir"               | "dir\\subdir"
        "dir/subdir/sub2/file.txt" | "dir\\subdir\\sub2\\file.txt"
        "dir\\subdir\\a"           | "dir\\subdir\\a"
    }
}