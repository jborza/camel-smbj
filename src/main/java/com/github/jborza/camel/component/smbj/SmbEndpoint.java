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

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;

@UriEndpoint(scheme = "smb2", title = "SMBJ", syntax = "smb2://user@server.example.com/sharename?password=secret&localWorkDirectory=/tmp", consumerClass = SmbConsumer.class)
public class SmbEndpoint extends GenericFileEndpoint<SmbFile> {

    @UriParam
    protected boolean dfs;

    public SmbEndpoint(String uri, SmbComponent smbComponent, SmbConfiguration configuration) {
        super(uri, smbComponent);
        this.configuration = configuration;
    }

    @Override
    public SmbConfiguration getConfiguration() {
        return (SmbConfiguration)configuration;
    }

    @Override
    public SmbConsumer createConsumer(Processor processor) throws Exception {
        SmbConsumer consumer = new SmbConsumer(this, processor, createSmbOperations());
        if (isDelete() && getMove() != null) {
            throw new IllegalArgumentException("You cannot set both delete=true and move options");
        }

        // if noop=true then idempotent should also be configured
        if (isNoop() && !isIdempotentSet()) {
            log.info("Endpoint is configured with noop=true so forcing endpoint to be idempotent as well");
            setIdempotent(true);
        }
//
//        // if idempotent and no repository set then create a default one
//        if (isIdempotentSet() && isIdempotent() && idempotentRepository == null) {
//            log.info("Using default memory based idempotent repository with cache max size: " + DEFAULT_IDEMPOTENT_CACHE_SIZE);
//            idempotentRepository = MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_IDEMPOTENT_CACHE_SIZE);
//        }
//
        consumer.setMaxMessagesPerPoll(getMaxMessagesPerPoll());
        consumer.setEagerLimitMaxMessagesPerPoll(isEagerMaxMessagesPerPoll());
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public GenericFileProducer<SmbFile> createProducer() throws Exception {
        return new SmbProducer(this, createSmbOperations());
    }

    public boolean isDfs() {
        return dfs;
    }

    public void setDfs(boolean dfsEnabled) {
        this.dfs = dfsEnabled;
    }

    @Override
    public Exchange createExchange(GenericFile<SmbFile> file) {
        Exchange answer = new DefaultExchange(this);
        if (file != null) {
            file.bindToExchange(answer);
        }
        return answer;
    }

    public SmbOperations createSmbOperations() {
        SmbConfig config = createSmbConfig();

        SMBClient client = new SMBClient(config);
        SmbOperations operations = new SmbOperations(client);
        operations.setEndpoint(this);
        return operations;
    }

    private SmbConfig createSmbConfig() {
        return SmbConfig
                    .builder()
                    .withMultiProtocolNegotiate(true)
                    .withDfsEnabled(isDfs())
                    .build();
    }

    @Override
    public String getScheme() {
        return "smb2";
    }

    @Override
    public char getFileSeparator() {
        return '/';
    }

    @Override
    public boolean isAbsolute(String name) {
        return true;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}