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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hierynomus.smbj.SmbConfig;

import java.util.concurrent.ConcurrentHashMap;

@UriEndpoint(scheme = "smb2", title = "SMBJ", syntax = "smb2://user@server.example.com/sharename?password=secret&localWorkDirectory=/tmp", consumerClass = SmbConsumer.class)
public class SmbEndpoint extends GenericFileEndpoint<SmbFile> {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @UriParam(description = "You can use this special option to provide RAW in password as an " +
            "alternative to placing it directly to URI")
    protected String password;

    @UriParam
    protected boolean dfs;

    public SmbEndpoint(String uri, SmbComponent smbComponent, SmbConfiguration configuration) {
        super(uri, smbComponent);
        this.configuration = configuration;
    }

    @Override
    public SmbConfiguration getConfiguration() {
        if (password != null){
            ((SmbConfiguration) configuration).setPassword(password);
        }
        return (SmbConfiguration)configuration;
    }

    @Override
    public SmbConsumer createConsumer(Processor processor) throws Exception {
        createProcessStrategyIfNotSet();
        SmbConsumer consumer = new SmbConsumer(this, processor, createSmbOperations(), getProcessStrategy());
        if (isDelete() && getMove() != null) {
            throw new IllegalArgumentException("You cannot set both delete=true and move options");
        }

        // if noop=true then idempotent should also be configured
        if (isNoop() && !isIdempotentSet()) {
            log.info("Endpoint is configured with noop=true so forcing endpoint to be idempotent as well");
            setIdempotent(true);
        }

        // if idempotent and no repository set then create a new one
        if (isIdempotentSet() && isIdempotent() && idempotentRepository == null) {
            log.info("Using default memory based idempotent repository with cache max size: " + DEFAULT_IDEMPOTENT_CACHE_SIZE);
            setIdempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_IDEMPOTENT_CACHE_SIZE));
        }

        consumer.setMaxMessagesPerPoll(getMaxMessagesPerPoll());
        consumer.setEagerLimitMaxMessagesPerPoll(isEagerMaxMessagesPerPoll());
        configureConsumer(consumer);
        return consumer;
    }

    private void createProcessStrategyIfNotSet() {
        if(getProcessStrategy() == null)
            setProcessStrategy(createGenericFileStrategy());
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Exchange createExchange(GenericFile<SmbFile> file) {
        Exchange answer = new DefaultExchange(this);
        if (file != null) {
            file.bindToExchange(answer);
        }
        return answer;
    }

    @Override
    public Exchange createExchange() {
        Exchange exchange = super.createExchange();
        if (exchange.getProperties() == null && DefaultExchange.class.isAssignableFrom(exchange.getClass())){
            DefaultExchange def = (DefaultExchange) exchange;
            def.setProperties(new ConcurrentHashMap<>());
        }
        return exchange;
    }

    public SmbOperations createSmbOperations() {
        SmbConfig config = createSmbConfig();
        SmbOperations operations = new SmbOperations(config);
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
    protected GenericFileProcessStrategy<SmbFile> createGenericFileStrategy() {
        return new SmbFileProcessStrategy().createGenericFileProcessStrategy(this.getCamelContext(), this.getParamsAsMap());
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}