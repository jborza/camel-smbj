package com.github.jborza.camel.component.smbj;

import com.hierynomus.smbj.SMBClient;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.*;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        throw new UnsupportedOperationException();
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