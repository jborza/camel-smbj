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

import org.apache.camel.component.file.GenericFileConfiguration;
import org.apache.camel.util.Pair;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SmbConfiguration extends GenericFileConfiguration {

    private static final String DOMAIN_SEPARATOR = ";";
    private static final String USER_PASS_SEPARATOR = ":";
    private static final String PASSWORD_PARAM_NAME = "password";


    private final static String RAW_BEGIN = "RAW(";
    private String domain;
    private String username;
    private String password;
    private String host;
    private String path;

    private String share;
    private int port;

    public SmbConfiguration(URI uri) {
        configure(uri);
    }

    @Override
    public void configure(URI uri) {
        Optional<String> parametricPassword = Optional.empty();
        Map<String, Object> parameters = Collections.emptyMap();
        try {
            parameters = URISupport.parseParameters(uri);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        if (parameters.containsKey(PASSWORD_PARAM_NAME)){
            List<Pair<Integer>> passwordParamRaw = URISupport.scanRaw(parameters.get(PASSWORD_PARAM_NAME)
                                                                     .toString());
            if (passwordParamRaw.size() > 0){
                parametricPassword = Optional.of(parameters.get(PASSWORD_PARAM_NAME).toString().substring(passwordParamRaw.get(0).getLeft()+RAW_BEGIN.length(), passwordParamRaw.get(0).getRight()));
            }
            else {
                parametricPassword = Optional.of(parameters.get(PASSWORD_PARAM_NAME).toString());
            }
        }
        super.configure(uri);
        String userInfo = uri.getUserInfo();

        if (userInfo != null) {
            if (userInfo.contains(DOMAIN_SEPARATOR)) {
                setDomain(StringHelper.before(userInfo, DOMAIN_SEPARATOR));
                userInfo = StringHelper.after(userInfo, DOMAIN_SEPARATOR);
            }
            if (userInfo.contains(USER_PASS_SEPARATOR)) {
                setUsername(StringHelper.before(userInfo, USER_PASS_SEPARATOR));
                setPassword(StringHelper.after(userInfo, USER_PASS_SEPARATOR));
            } else {
                setUsername(userInfo);
            }
        }
        if (parametricPassword.isPresent()) {
            setPassword(parametricPassword.get());
        }
        setHost(uri.getHost());
        setPort(uri.getPort());
        setPath(uri.getPath());
        String[] segments = uri.getPath().split("/");
        if(segments.length > 1) //first one is "/"
            setShare(segments[1]);
        else
            setShare("");
        String path = uri.getPath().replace("/"+getShare()+"/","");
        if(!path.endsWith("/"))
            path = path + "/";
        setPath(path);
    }

    public String getSmbHostPath() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("smb://");
        buffer.append(getHost());
        if (!isDefaultPort()) {
            buffer.append(":").append(getPort());
        }
        buffer.append("/");
        return buffer.toString();
    }

    public boolean isDefaultPort() {
        return getPort() <= 0;
    }

    public String getShare() { return share; }

    public void setShare(String share) { this.share = share; }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}

