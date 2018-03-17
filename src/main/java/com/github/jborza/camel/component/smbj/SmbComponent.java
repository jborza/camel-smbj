package com.github.jborza.camel.component.smbj;

import org.apache.camel.CamelContext;
import org.apache.camel.component.file.GenericFileComponent;
import org.apache.camel.component.file.GenericFileEndpoint;

import java.net.URI;
import java.util.Map;

public class SmbComponent extends GenericFileComponent<SmbFile> {

    public SmbComponent() {

    }

    public SmbComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected SmbEndpoint buildFileEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        uri = urlEncodeSpace(uri);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        return new SmbEndpoint(uri, this, config);
    }

    @Override
    protected void afterPropertiesSet(GenericFileEndpoint<SmbFile> genericFileEndpoint) throws Exception {
        //empty on purpose
    }

    private String urlEncodeSpace(String input) {
        return input.replace(" ", "%20");
    }
}
