/**
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
