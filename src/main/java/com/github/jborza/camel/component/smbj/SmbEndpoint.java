package com.github.jborza.camel.component.smbj;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.share.File;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.UriEndpoint;

@UriEndpoint(scheme = "smb2", title = "SMBJ", syntax = "smb3://user@server.example.com/sharename?password=secret&localWorkDirectory=/tmp", consumerClass = SmbConsumer.class)
public class SmbEndpoint extends GenericFileEndpoint<File> {
    private boolean download = true;

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
//
//        if (isDelete() && getMove() != null) {
//            throw new IllegalArgumentException("You cannot set both delete=true and move options");
//        }
//
//        // if noop=true then idempotent should also be configured
//        if (isNoop() && !isIdempotentSet()) {
//            log.info("Endpoint is configured with noop=true so forcing endpoint to be idempotent as well");
//            setIdempotent(true);
//        }
//
//        // if idempotent and no repository set then create a default one
//        if (isIdempotentSet() && isIdempotent() && idempotentRepository == null) {
//            log.info("Using default memory based idempotent repository with cache max size: " + DEFAULT_IDEMPOTENT_CACHE_SIZE);
//            idempotentRepository = MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_IDEMPOTENT_CACHE_SIZE);
//        }
//
//        consumer.setMaxMessagesPerPoll(getMaxMessagesPerPoll());
//        consumer.setEagerLimitMaxMessagesPerPoll(isEagerMaxMessagesPerPoll());
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public GenericFileProducer<File> createProducer() throws Exception {
        return new SmbProducer(this, createSmbOperations());
    }

    @Override
    public Exchange createExchange(GenericFile<File> file) {
        Exchange answer = new DefaultExchange(this);
        if (file != null) {
            file.bindToExchange(answer);
        }
        return answer;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public SmbOperations createSmbOperations() {
//        DefaultSmbClient client = new DefaultSmbClient();
//        if (((SmbConfiguration)this.configuration).getSmbApiFactory() != null) {
//            client.setSmbApiFactory(((SmbConfiguration)this.configuration).getSmbApiFactory());
//        }
        SmbConfig config = SmbConfig
                .builder()
                .withMultiProtocolNegotiate(true)
                .withSigningRequired(true).build();

        SMBClient client = new SMBClient(config);
        SmbOperations operations = new SmbOperations(client);
        operations.setEndpoint(this);
        return operations;
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

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    @Override
    protected String createDoneFileName(String fileName) {
        return super.createDoneFileName(fileName);
    }
}