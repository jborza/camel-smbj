import com.github.jborza.camel.component.smbj.SmbPathUtils
import spock.lang.Specification

import java.nio.file.Paths

class SmbPathUtilsSpec extends Specification {
    def "should correctly remove share with windows separator"() {
        expect:
        result == SmbPathUtils.removeShareName(path, share, true)

        where:
        path                           | share   | result
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
        result.startsWith(share) == false
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