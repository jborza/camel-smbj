/**
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

import org.apache.camel.Exchange;
import org.apache.camel.component.file.*;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.ObjectHelper;

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
        log.debug("processExchange() target: {}", target);
        if (shouldWriteToTempFile()) {
            processWithTempFile(exchange, target);
        } else {
            writeFile(exchange, target);
        }
        // store the name we really used in the header, so end-users can retrieve it
        exchange.getIn().setHeader(Exchange.FILE_NAME_PRODUCED, target);
    }

    /**
     * Check if we should write to a temporary name and then afterwards rename to the real target
     * This occurs when either tempFileName or tempPrefix options are set
     */
    private boolean shouldWriteToTempFile() {
        return ObjectHelper.isNotEmpty(endpoint.getTempFileName());
    }

    private void processWithTempFile(Exchange exchange, String target) {
        // compute temporary name with the temp prefix
        String tempTarget = createTempFileName(exchange, target);
        log.debug("Writing using tempNameFile: ", tempTarget);
        cleanupExistingTempFile(tempTarget);
        writeFile(exchange, tempTarget);
        deleteExistingTargetFileOnOverride(target);
        renameTempToTargetFile(tempTarget, target);
    }

    private void renameTempToTargetFile(String tempTarget, String target) {
        log.debug("Renaming file: {} to : {}", tempTarget, target);
        if (!operations.renameFile(tempTarget, target))
            throw new GenericFileOperationFailedException("Cannot rename file from: " + tempTarget + " to:" + target);
    }

    /**
     * When overriding the target and eagerDeleteTargetFile=false we delete target file at this latest point,
     * so the temp file can be renamed with success as the existing target file have been deleted
     * If eagerDeleteTargetFile=true it would have been deleted by SmbOperations.storeFile
     *
     * @param target target file name
     */
    private void deleteExistingTargetFileOnOverride(String target) {
        if (!endpoint.isEagerDeleteTargetFile() && operations.existsFile(target) && endpoint.getFileExist() == GenericFileExist.Override) {
            log.debug("Deleting existing file: ", target);
            if (!operations.deleteFile(target)) {
                throw new GenericFileOperationFailedException("Cannot delete target file: " + target);
            }
        }
    }

    private void cleanupExistingTempFile(String tempTarget) {
        if (operations.existsFile(tempTarget)) {
            log.debug("Deleting existing temp file: {}", tempTarget);
            if (!operations.deleteFile(tempTarget)) {
                throw new GenericFileOperationFailedException("Cannot delete temp file: " + tempTarget);
            }
        }
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

        if (endpoint.isAutoCreate()) {
            autoCreateFolder(fileNameWithoutShare);
        }
        // upload
        if (log.isDebugEnabled()) {
            log.debug("About to write [" + fileName + "] to [" + getEndpoint() + "] from exchange [" + exchange + "]");
        }
        operations.storeFile(fileNameWithoutShare, exchange);

        if (log.isDebugEnabled()) {
            log.debug("Wrote [" + fileName + "] to [" + getEndpoint() + "]");
        }
    }

    private void autoCreateFolder(String fileNameWithoutShare) {
        java.io.File file = new java.io.File(fileNameWithoutShare);
        String parentDirectory = file.getParent();
        boolean absolute = FileUtil.isAbsolute(file);
        if (parentDirectory != null && !operations.buildDirectory(parentDirectory, absolute)) {
            log.warn("Cannot build directory [" + parentDirectory + "] (could be because of denied permissions)");
        }
    }

    @Override
    public SmbEndpoint getEndpoint() {
        return (SmbEndpoint) super.getEndpoint();
    }
}
