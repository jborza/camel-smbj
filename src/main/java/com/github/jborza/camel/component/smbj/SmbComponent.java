/*
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
        String encodedUri = urlEncodeSpace(uri);
        SmbConfiguration config = new SmbConfiguration(new URI(encodedUri));
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
