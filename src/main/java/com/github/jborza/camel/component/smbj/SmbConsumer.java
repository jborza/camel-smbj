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
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.file.*;
import org.apache.camel.util.FileUtil;

import java.util.List;

public class SmbConsumer extends GenericFileConsumer<SmbFile> {

    private final String endpointPath;
    private final String currentRelativePath = "";
    private GenericFileConverter genericFileConverter;

    public SmbConsumer(GenericFileEndpoint<SmbFile> endpoint, Processor processor, GenericFileOperations<SmbFile> operations, GenericFileProcessStrategy<SmbFile> processStrategy) {
        super(endpoint, processor, operations, processStrategy);
        SmbConfiguration config = (SmbConfiguration) endpoint.getConfiguration();
        this.endpointPath = config.getShare() + "\\" + config.getPath();
        genericFileConverter = new GenericFileConverter();
    }

    @Override
    protected boolean pollDirectory(String fileName, List<GenericFile<SmbFile>> fileList, int depth) {
        int currentDepth = depth + 1;
        if (log.isTraceEnabled()) {
            log.trace("pollDirectory() running. My delay is [" + this.getDelay() + "] and my strategy is [" + this.getPollStrategy().getClass().toString() + "]");
            log.trace("pollDirectory() fileName[" + fileName + "]");
        }

        List<SmbFile> smbFiles = operations.listFiles(fileName);
        for (SmbFile smbFile : smbFiles) {
            //stop polling files if the limit is reached
            if (!canPollMoreFiles(fileList)) {
                return false;
            }
            GenericFile<SmbFile> gf = genericFileConverter.asGenericFile(fileName, smbFile, endpointPath, currentRelativePath);
            if (gf.isDirectory()) {
                if (endpoint.isRecursive() && currentDepth < endpoint.getMaxDepth()) {
                    //recursive scan of the subdirectory
                    String subDirName = fileName + "/" + gf.getFileName();
                    pollDirectory(subDirName, fileList, currentDepth);
                }
            } else {
                //conform to the minDepth parameter
                if (currentDepth < endpoint.getMinDepth())
                    continue;

                final boolean isDirectory = false; //we never return directories
                if (isValidFile(gf, isDirectory, smbFiles))
                    fileList.add(gf);
            }
        }
        return true;
    }

    @Override
    protected void updateFileHeaders(GenericFile<SmbFile> file, Message message) {
        //note: copied from FtpConsumer
        long length = file.getFile().getFileLength();
        long modified = file.getLastModified();
        file.setFileLength(length);
        file.setLastModified(modified);
        if (length >= 0) {
            message.setHeader(Exchange.FILE_LENGTH, length);
        }
        if (modified >= 0) {
            message.setHeader(Exchange.FILE_LAST_MODIFIED, modified);
        }
    }

    @Override
    protected boolean isMatched(GenericFile<SmbFile> file, String doneFileName, List<SmbFile> files) {
        String onlyName = FileUtil.stripPath(doneFileName);

        for (SmbFile f : files) {
            if (f.getFileName().equals(onlyName)) {
                return true;
            }
        }

        log.trace("Done file: {} does not exist", doneFileName);
        return false;
    }

    @Override
    protected void doStart() throws Exception {
        // turn off scheduler first, so autoCreate is handled before scheduler starts
        boolean startScheduler = isStartScheduler();
        setStartScheduler(false);
        try {
            super.doStart();
            if (endpoint.isAutoCreate()) {
                log.debug("Auto creating directory: {}", endpoint.getConfiguration().getDirectory());
                try {
                    operations.buildDirectory(endpoint.getConfiguration().getDirectory(), true);
                } catch (GenericFileOperationFailedException e) {
                    // log a WARN as we want to start the consumer.
                    log.warn("Error auto creating directory: " + endpoint.getConfiguration().getDirectory()
                            + " due " + e.getMessage() + ". This exception is ignored.", e);
                }
            }
        } finally {
            if (startScheduler) {
                setStartScheduler(true);
                startScheduler();
            }
        }
    }
}
