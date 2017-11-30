import com.github.jborza.camel.component.smbj.SmbConfiguration
import spock.lang.Specification

class SmbConfigurationSpec extends Specification{
    def "should correctly process domain, user and password"(){
        given:
        def uri = new URI("smb2://mydomain;user:password@127.0.0.1")
        when:
        def cfg = new SmbConfiguration(uri)
        then:
        cfg.getDomain()=="mydomain"
        cfg.getUsername()=="user"
        cfg.getPassword()=="password"
    }

    def "should correctly process user and password without domain"(){
        given:
        def uri = new URI("smb2://user:password@127.0.0.1")
        when:
        def cfg = new SmbConfiguration(uri)
        then:
        cfg.getDomain()==null
        cfg.getUsername()=="user"
        cfg.getPassword()=="password"
    }

}