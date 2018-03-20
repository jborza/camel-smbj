package com.github.jborza.camel.component.smbj;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.share.File;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.*;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.github.jborza.camel.component.smbj.SmbConstants.CURRENT_DIRECTORY;
import static com.github.jborza.camel.component.smbj.SmbConstants.PARENT_DIRECTORY;
import static com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_CREATE;

public class SmbOperations implements GenericFileOperations<SmbFile> {
    private final SMBClient client;
    private GenericFileEndpoint<SmbFile> endpoint;
    private AuthenticationContext authenticationContext;

    public SmbOperations(SMBClient client) {
        this.client = client;
    }

    @Override
    public void setEndpoint(GenericFileEndpoint<SmbFile> genericFileEndpoint) {
        this.endpoint = genericFileEndpoint;
    }

    @Override
    public boolean deleteFile(String name) throws GenericFileOperationFailedException {
        try {
            doDeleteFile(name);
            return true;
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot delete file: " + name, e);
        }
    }

    @Override
    public boolean existsFile(String name) throws GenericFileOperationFailedException {
        return doFileExists(name);
    }

    @Override
    public boolean renameFile(String from, String to) throws GenericFileOperationFailedException {
        try {
            doRenameFile(from, to);
            return true;
        }
        catch(IOException e){
            throw new GenericFileOperationFailedException("Cannot rename file: " + from + " to:" + to, e);
        }
    }

    @Override
    public boolean buildDirectory(String directory, boolean absolute) throws GenericFileOperationFailedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retrieveFile(String name, Exchange exchange) throws GenericFileOperationFailedException {
        OutputStream os = null;
        os = new ByteArrayOutputStream();
        GenericFile<SmbFile> target = (GenericFile<SmbFile>) exchange.getProperty(FileComponent.FILE_EXCHANGE_FILE);
        ObjectHelper.notNull(target, "Exchange should have the " + FileComponent.FILE_EXCHANGE_FILE + " set");
        target.setBody(os);
        try {
            doRetrieveFile(name, os);
            return true;
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot retrieve file: " + name, e);
        }
    }

    @Override
    public void releaseRetreivedFileResources(Exchange exchange) throws GenericFileOperationFailedException {

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
        try{
            return doListFiles(path);
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Could not get files " + e.getMessage(), e);
        }
    }

    private static boolean isDirectory(FileIdBothDirectoryInformation info) {
        return (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_DIRECTORY) == SmbConstants.FILE_ATTRIBUTE_DIRECTORY;
    }

    private static long getLastModified(FileIdBothDirectoryInformation info) {
        return info.getLastWriteTime().toEpochMillis();
    }

    private String getPath(String pathEnd) {
        String path = getConfiguration().getSmbHostPath() + pathEnd;
        return path.replace('\\', '/');
    }

    @Override
    public boolean storeFile(String name, Exchange exchange) {
        String storeName = getPath(name);
        InputStream inputStream = null;

        try {
            inputStream = exchange.getIn().getMandatoryBody(InputStream.class);
            return doStoreFile(name, inputStream);
        } catch (Exception e) {
            throw new GenericFileOperationFailedException("Cannot store file " + storeName, e);
        } finally {
            IOHelper.close(inputStream, "store: " + storeName);
        }
    }


    private boolean isDfs() {
        return ((SmbEndpoint) endpoint).isDfs();
    }

    private SmbConfiguration getConfiguration() {
        return ((SmbConfiguration) endpoint.getConfiguration());
    }

    private  List<SmbFile> doListFiles(String path) throws IOException {
        List<SmbFile> files = new ArrayList<>();
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(path);
            for (FileIdBothDirectoryInformation f : share.getShare().list(share.getPath())) {
                boolean isDirectory = isDirectory(f);
                if (isDirectory) {
                    //skip special directories . and ..
                    if (f.getFileName().equals(CURRENT_DIRECTORY) || f.getFileName().equals(PARENT_DIRECTORY))
                        continue;
                }
                files.add(new SmbFile(isDirectory, f.getFileName(), f.getEndOfFile(), getLastModified(f)));
            }
            return files;
        }
    }

    private boolean doStoreFile(String name, InputStream inputStream) throws IOException {
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(name);
            File file = share.getShare().openFile(share.getPath(), EnumSet.of(AccessMask.GENERIC_ALL), null, SMB2ShareAccess.ALL, FILE_CREATE, null);
            OutputStream outputStream = file.getOutputStream();

            IOUtils.copy(inputStream, outputStream, endpoint.getBufferSize());
            outputStream.close();
            return true;
        }
    }

    private void doRetrieveFile(String name, OutputStream os) throws IOException {
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(name);
            //NB https://msdn.microsoft.com/en-us/library/cc246502.aspx - SMB2 CREATE Request
            // ShareAccess.ALL means that other opens are allowed to read, but not write or delete the file
            File f = share.getShare().openFile(share.getPath(), EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
            InputStream is = f.getInputStream();
            IOUtils.copy(is, os, endpoint.getBufferSize());
        }
        finally {
            IOHelper.close(os, "retrieve: " + name);
        }
    }

    private boolean doFileExists(String name) {
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(name);
            return share.getShare().fileExists(share.getPath());
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot determine if file: " + name + " exists", e);
        }
    }

    private void doDeleteFile(String name) throws IOException{
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(name);
            share.getShare().rm(share.getPath());
        }
    }

    private void doRenameFile(String from, String to) throws IOException {
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(from);
            share.rename(from, to);
        }
    }
}