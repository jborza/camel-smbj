package com.github.jborza.camel.component.smbj;

import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.FileUtil;

public class SmbProducer extends GenericFileProducer<SmbFile> {

    private String endpointPath;

    protected SmbProducer(GenericFileEndpoint<SmbFile> endpoint, GenericFileOperations<SmbFile> operations) {
        super(endpoint, operations);
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Exchange smbExchange = getEndpoint().createExchange(exchange);
        setEndpointPath(getEndpoint().getEndpointUri());
        processExchange(smbExchange);
        ExchangeHelper.copyResults(exchange, smbExchange);
    }

    protected void processExchange(Exchange exchange) {
        String target = createFileName(exchange);
        writeFile(exchange, target);
        // let's store the name we really used in the header, so end-users
        // can retrieve it
        exchange.getIn().setHeader(Exchange.FILE_NAME_PRODUCED, target);
    }

    //TODO maybe override createFileName to be compliant with options: flatten, etc
    //camel-jcifs looks at both Windows and Unix separators

    @Override
    public void writeFile(Exchange exchange, String fileName) throws GenericFileOperationFailedException {
        if (log.isDebugEnabled()) {
            log.debug("writeFile() fileName[" + fileName + "]");
        }
        //strip the share name, as it's a special part of the name for us
        String share = getEndpoint().getConfiguration().getShare();
        String fileNameWithoutShare = SmbPathUtils.removeShareName(fileName, share, false);

        if(endpoint.isAutoCreate()){
            java.io.File file = new java.io.File(fileNameWithoutShare);
            String parentDirectory = file.getParent();
            boolean absolute = FileUtil.isAbsolute(file);
            if (parentDirectory != null && !operations.buildDirectory(parentDirectory, absolute)) {
                log.warn("Cannot build directory [" + parentDirectory + "] (could be because of denied permissions)");
            }
        }
        // upload
        if (log.isDebugEnabled()) {
            log.debug("About to write [" + fileName + "] to [" + getEndpoint() + "] from exchange [" + exchange + "]");
        }
        boolean success = operations.storeFile(fileNameWithoutShare, exchange);
        if (!success) {
            throw new GenericFileOperationFailedException("Error writing file [" + fileName + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("Wrote [" + fileName + "] to [" + getEndpoint() + "]");
        }
    }

    @Override
    public SmbEndpoint getEndpoint() {
        return (SmbEndpoint) super.getEndpoint();
    }
}
