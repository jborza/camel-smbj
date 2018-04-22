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

import com.hierynomus.smbj.SMBClient;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.*;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;

import java.io.*;
import java.util.List;

public class SmbOperations implements GenericFileOperations<SmbFile>, SmbShareFactory {
    private final SMBClient client;

    private SmbClient smbClient;
    private GenericFileEndpoint<SmbFile> endpoint;

    public SmbOperations(SMBClient client) {
        this.client = client;
        this.smbClient = new SmbClient(this);
    }

    public void setSmbClient(SmbClient smbClient) {
        this.smbClient = smbClient;
    }

    @Override
    public void setEndpoint(GenericFileEndpoint<SmbFile> genericFileEndpoint) {
        this.endpoint = genericFileEndpoint;
    }

    @Override
    public boolean deleteFile(String name) throws GenericFileOperationFailedException {
        try {
            smbClient.deleteFile(name);
            return true;
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot delete file: " + name, e);
        }
    }


    @Override
    public boolean buildDirectory(String directory, boolean absolute) throws GenericFileOperationFailedException {
        try{
            return smbClient.mkdirs(directory);
        }
        catch(IOException e){
            throw new GenericFileOperationFailedException("Could not build directory: "+directory,e);
        }
    }

    @Override
    public boolean existsFile(String name) throws GenericFileOperationFailedException {
        try {
            return smbClient.fileExists(name);
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot determine if file: " + name + " exists", e);
        }
    }

    @Override
    public boolean renameFile(String from, String to) throws GenericFileOperationFailedException {
        try {
            smbClient.renameFile(from, to);
            return true;
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot rename file: " + from + " to:" + to, e);
        }
    }

    @Override
    public boolean retrieveFile(String name, Exchange exchange) throws GenericFileOperationFailedException {
        if (ObjectHelper.isNotEmpty(endpoint.getLocalWorkDirectory())) {
            // local work directory is configured so we should store file content as files in this local directory
            return retrieveFileToFileInLocalWorkDirectory(name, exchange);
        } else {
            // store file content directory as stream on the body
            return retrieveFileToStreamInBody(name, exchange);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean retrieveFileToFileInLocalWorkDirectory(String name, Exchange exchange) throws GenericFileOperationFailedException {
        File temp;

        File local = new File(endpoint.getLocalWorkDirectory());
        local.mkdirs();
        OutputStream os;
        GenericFile<SmbFile> file = (GenericFile<SmbFile>) exchange.getProperty(FileComponent.FILE_EXCHANGE_FILE);
        ObjectHelper.notNull(file, "Exchange should have the " + FileComponent.FILE_EXCHANGE_FILE + " set");

        // use relative filename in local work directory
        String relativeName = file.getRelativeFilePath();
        temp = new File(local, relativeName + ".inprogress");
        local = new File(local, relativeName);

        // delete any existing files
        cleanupLocalFile(temp);
        cleanupLocalFile(local);

        // create new temp local work file
        createNewTempLocalFile(temp);

        // output stream points to the temp file in the local work directory
        try {
            os = new FileOutputStream(temp);
        } catch (FileNotFoundException e1) {
            throw new GenericFileOperationFailedException("Local work file not found: " + temp, e1);
        }

        // set header with the path to the local work file
        exchange.getIn().setHeader(Exchange.FILE_LOCAL_WORK_PATH, local.getPath());

        try {
            // store the File reference as the message body
            file.setBody(local);
            smbClient.retrieveFile(name, os);
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot retrieve file: " + name, e);
        } catch (Exception e) {
            throw new GenericFileOperationFailedException("Cannot retrieve file: " + name, e);
        } finally {
            IOHelper.close(os, "retrieve: " + name);
        }

        renameTempToTargetFile(temp, local);

        return true;
    }

    private void createNewTempLocalFile(File temp) {
        try {
            if (!temp.createNewFile()) {
                throw new GenericFileOperationFailedException("Cannot create new local work file: " + temp);
            }
        } catch (IOException e1) {
            throw new GenericFileOperationFailedException("Cannot create new local work file: " + temp, e1);
        }
    }

    private void cleanupLocalFile(File file) {
        if (file.exists() && (!FileUtil.deleteFile(file))) {
            throw new GenericFileOperationFailedException("Cannot delete existing local work file: " + file);
        }
    }

    private void renameTempToTargetFile(File temp, File local) {
        try {
            if (!FileUtil.renameFile(temp, local, true)) {
                throw new GenericFileOperationFailedException("Cannot rename local work file from: " + temp + " to: " + local);
            }
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot rename local work file from: " + temp + " to: " + local, e);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean retrieveFileToStreamInBody(String name, Exchange exchange) throws GenericFileOperationFailedException {
        OutputStream os = new ByteArrayOutputStream();
        GenericFile<SmbFile> target = (GenericFile<SmbFile>) exchange.getProperty(FileComponent.FILE_EXCHANGE_FILE);
        ObjectHelper.notNull(target, "Exchange should have the " + FileComponent.FILE_EXCHANGE_FILE + " set");
        target.setBody(os);
        try {
            smbClient.retrieveFile(name, os);
            return true;
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot retrieve file: " + name, e);
        }
    }

    @Override
    public void releaseRetreivedFileResources(Exchange exchange) throws GenericFileOperationFailedException {
        //intentionally left blank
    }

    @Override
    public String getCurrentDirectory() throws GenericFileOperationFailedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changeCurrentDirectory(String path) throws GenericFileOperationFailedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changeToParentDirectory() throws GenericFileOperationFailedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SmbFile> listFiles() throws GenericFileOperationFailedException {
        //not implemented - use listFiles(String path)
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SmbFile> listFiles(String path) throws GenericFileOperationFailedException {
        try {
            return smbClient.listFiles(path);
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Could not get files " + e.getMessage(), e);
        }
    }

    private String getPath(String pathEnd) {
        String path = getConfiguration().getSmbHostPath() + pathEnd;
        return path.replace('\\', '/');
    }

    @Override
    public boolean storeFile(String name, Exchange exchange) {
        InputStream inputStream = null;
        try {
            inputStream = exchange.getIn().getMandatoryBody(InputStream.class);
            smbClient.storeFile(name, inputStream);
            return true;
        } catch (Exception e) {
            String storeName = getPath(name);
            throw new GenericFileOperationFailedException("Cannot store file " + storeName, e);
        } finally {
            IOHelper.close(inputStream, "store: " + name);
        }
    }

    private boolean isDfs() {
        return ((SmbEndpoint) endpoint).isDfs();
    }

    private SmbConfiguration getConfiguration() {
        return ((SmbConfiguration) endpoint.getConfiguration());
    }

    @Override
    public SmbShare makeSmbShare() {
        return new SmbShare(client, getConfiguration(), isDfs(), endpoint.getBufferSize());
    }
}