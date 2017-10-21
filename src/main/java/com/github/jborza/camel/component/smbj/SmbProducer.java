package com.github.jborza.camel.component.smbj;

import com.hierynomus.smbj.share.File;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.util.ExchangeHelper;

public class SmbProducer extends GenericFileProducer<File> {

    private String endpointPath;

    protected SmbProducer(GenericFileEndpoint<File> endpoint, GenericFileOperations<File> operations) {
        super(endpoint, operations);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Exchange smbExchange = getEndpoint().createExchange(exchange);
        setEndpointPath(getEndpoint().getEndpointUri());
        processExchange(smbExchange);
        ExchangeHelper.copyResults(exchange, smbExchange);
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    protected void processExchange(Exchange exchange) throws Exception {
        String target = createFileName(exchange);
        writeFile(exchange, target);
        // lets store the name we really used in the header, so end-users
        // can retrieve it
        exchange.getIn().setHeader(Exchange.FILE_NAME_PRODUCED, target);
    }

    @Override
    public void writeFile(Exchange exchange, String fileName) throws GenericFileOperationFailedException {
        boolean success = operations.storeFile(fileName, exchange);
        if (!success) {
            throw new GenericFileOperationFailedException("Error writing file [" + fileName + "]");
        }
    }

        @Override
    public SmbEndpoint getEndpoint() {
        return (SmbEndpoint) super.getEndpoint();
    }
}
