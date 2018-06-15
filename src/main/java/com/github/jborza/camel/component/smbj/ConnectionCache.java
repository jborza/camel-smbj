/*
 * Copyright [2018] [Juraj Borza]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jborza.camel.component.smbj;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.connection.Connection;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConnectionCache {
    private final SMBClient client;
    private Map<String, Connection> connections;

    public ConnectionCache(SMBClient client) {
        this.client = client;
        connections = new HashMap<>();
    }

    public Collection<Connection> getConnections() {
        return connections.values();
    }

    private String getKey(String host, int port) {
        return String.format("%s:%d", host, port);
    }

    public Connection getConnection(String host, int port) throws IOException {
        String key = getKey(host, port);
        Connection cachedConnection = connections.get(key);
        if (cachedConnection == null || !cachedConnection.isConnected()) {
            cachedConnection = client.connect(host, port);
            connections.put(key, cachedConnection);
        }
        return cachedConnection;
    }
}
