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

import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProducer;
import org.apache.camel.util.FileUtil;

public class SmbProducer extends GenericFileProducer<SmbFile> {

    protected SmbProducer(GenericFileEndpoint<SmbFile> endpoint, GenericFileOperations<SmbFile> operations) {
        super(endpoint, operations);
    }

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
