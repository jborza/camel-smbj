package com.github.jborza.camel.component.smbj;

import com.github.jborza.camel.component.smbj.smbj.DfsPathResolveException;
import com.github.jborza.camel.component.smbj.smbj.DfsResolutionResult;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.messages.SMB2Echo;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.paths.PathResolveException;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
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
import java.nio.file.Path;
import java.nio.file.Paths;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean existsFile(String name) throws GenericFileOperationFailedException {
        SmbConfiguration config = ((SmbConfiguration) endpoint.getConfiguration());
        String actualPath = SmbPathUtils.removeShareName(name, config.getShare(), true);
        Session session = connectSession(config.getHost());
        SmbPath targetPath = new SmbPath(config.getHost(), config.getShare(), actualPath);
        DfsResolutionResult dfsResolutionResult = resolvePath(session, targetPath);
        DiskShare share = dfsResolutionResult.getDiskShare();
        actualPath = dfsResolutionResult.getSmbPath().getPath();
        return share.fileExists(actualPath);
    }

    @Override
    public boolean renameFile(String s, String s1) throws GenericFileOperationFailedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean buildDirectory(String directory, boolean absolute) throws GenericFileOperationFailedException {
        if(isDfs()) {
            throw new UnsupportedOperationException();
        }
        else{
            SmbConfiguration config = ((SmbConfiguration) endpoint.getConfiguration());
            Session session = connectSession(config.getHost());
            DiskShare share = (DiskShare) session.connectShare(config.getShare());
            //strip share name from the beginning of directory
            String shareName = config.getShare();
            String directoryNormalized = directory.replaceFirst("^" + shareName, "");

            Path path = Paths.get(directoryNormalized);
            int len = path.getNameCount();
            for (int i = 0; i < len; i++) {
                Path partialPath = path.subpath(0, i + 1);
                String pathAsString = SmbPathUtils.convertToBackslashes(partialPath.toString());
                if (!share.folderExists(pathAsString))
                    share.mkdir(pathAsString);
            }
            return true;
        }
    }

    @Override
    public boolean retrieveFile(String name, Exchange exchange) throws GenericFileOperationFailedException {
        OutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            GenericFile<SmbFile> target = (GenericFile<SmbFile>) exchange.getProperty(FileComponent.FILE_EXCHANGE_FILE);
            ObjectHelper.notNull(target, "Exchange should have the " + FileComponent.FILE_EXCHANGE_FILE + " set");
            target.setBody(os);
            //SMB part
            SmbConfiguration config = ((SmbConfiguration) endpoint.getConfiguration());
            String path = name;
            path = SmbPathUtils.convertToBackslashes(path);
            String actualPath = SmbPathUtils.removeShareName(path, config.getShare(), true);
            Session session = connectSession(config.getHost());
            SmbPath targetPath = new SmbPath(config.getHost(), config.getShare(), actualPath);
            DfsResolutionResult dfsResolutionResult = resolvePath(session, targetPath);
            DiskShare share = dfsResolutionResult.getDiskShare();
            actualPath = dfsResolutionResult.getSmbPath().getPath();
            //NB https://msdn.microsoft.com/en-us/library/cc246502.aspx - SMB2 CREATE Request
            // ShareAccess.ALL means that other opens are allowed to read, but not write or delete the file
            File f = share.openFile(actualPath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
            InputStream is = f.getInputStream();
            IOUtils.copy(is, os, endpoint.getBufferSize());
            return true;
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot retrieve file: " + name, e);
        } catch (Exception e) {
            throw new GenericFileOperationFailedException("Cannot retrieve file: " + name, e);
        } finally {
            IOHelper.close(os, "retrieve: " + name);
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
        List<SmbFile> files = new ArrayList<>();
        SmbConfiguration config = ((SmbConfiguration) endpoint.getConfiguration());
        String actualPath = SmbPathUtils.removeShareName(path, config.getShare(), true);
        try {
            Session session = connectSession(config.getHost());
            SmbPath targetPath = new SmbPath(config.getHost(), config.getShare(), actualPath);
            DfsResolutionResult dfsResolutionResult = resolvePath(session, targetPath);
            actualPath = dfsResolutionResult.getSmbPath().getPath();

            for (FileIdBothDirectoryInformation f : dfsResolutionResult.getDiskShare().list(actualPath)) {
                boolean isDirectory = isDirectory(f);
                if (isDirectory) {
                    //skip special directories . and ..
                    if (f.getFileName().equals(CURRENT_DIRECTORY) || f.getFileName().equals(PARENT_DIRECTORY))
                        continue;
                }
                files.add(new SmbFile(isDirectory, f.getFileName(), f.getEndOfFile(), getLastModified(f)));
            }
        } catch (Exception e) {
            throw new GenericFileOperationFailedException("Could not get files " + e.getMessage(), e);
        }
        return files;
    }

    private static boolean isDirectory(FileIdBothDirectoryInformation info) {
        return (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_DIRECTORY) == SmbConstants.FILE_ATTRIBUTE_DIRECTORY;
    }

    private static long getLastModified(FileIdBothDirectoryInformation info) {
        return info.getLastWriteTime().toEpochMillis();
    }

    private String getPath(String pathEnd) {
        String path = ((SmbConfiguration) endpoint.getConfiguration()).getSmbHostPath() + pathEnd;
        return path.replace('\\', '/');
    }

    //see https://github.com/apache/camel/blob/master/components/camel-ftp/src/main/java/org/apache/camel/component/file/remote/SftpOperations.java - doStoreFile
    @Override
    public boolean storeFile(String name, Exchange exchange) {
        String storeName = getPath(name);
        InputStream inputStream = null;
        try {
            inputStream = exchange.getIn().getMandatoryBody(InputStream.class);

            String actualPath = name;
            SmbConfiguration config = ((SmbConfiguration) endpoint.getConfiguration());
            Session session = connectSession(config.getHost());
            SmbPath targetPath = new SmbPath(config.getHost(), config.getShare(), actualPath);
            DfsResolutionResult dfsResolutionResult = resolvePath(session, targetPath);
            actualPath = dfsResolutionResult.getSmbPath().getPath();
            File file = dfsResolutionResult.getDiskShare().openFile(actualPath, EnumSet.of(AccessMask.GENERIC_ALL), null, SMB2ShareAccess.ALL, FILE_CREATE, null);

            OutputStream outputStream = file.getOutputStream();

            IOUtils.copy(inputStream, outputStream, endpoint.getBufferSize());
            outputStream.close();
            return true;
        } catch (Exception e) {
            throw new GenericFileOperationFailedException("Cannot store file " + storeName, e);
        } finally {
            IOHelper.close(inputStream, "store: " + storeName);
        }
    }

    private DfsResolutionResult resolvePath(Session session, SmbPath path) {
        if (isDfs()) {
            SmbPath resolvedPath = resolve(session, path);
            DiskShare share = getDfsShare(session, resolvedPath);
            return new DfsResolutionResult(share, resolvedPath);
        } else {
            DiskShare share = (DiskShare) session.connectShare(path.getShareName());
            return new DfsResolutionResult(share, path);
        }
    }

    private boolean isDfs() {
        return ((SmbEndpoint) endpoint).isDfs();
    }

    private SmbPath resolve(Session session, SmbPath path) {
        try {
            SMB2Echo responsePacket = new SMB2Echo();
            responsePacket.getHeader().setStatus(NtStatus.STATUS_PATH_NOT_COVERED);
            return client.getPathResolver().resolve(session, responsePacket, path);
        } catch (PathResolveException e) {
            throw new DfsPathResolveException(e);
        }
    }

    private DiskShare getDfsShare(Session session, SmbPath resolvedPath) {
        if (isOnSameHost(session, resolvedPath))
            return (DiskShare) session.connectShare(resolvedPath.getShareName());
        else {
            Session newSession = connectSession(resolvedPath.getHostname());
            return (DiskShare) newSession.connectShare(resolvedPath.getShareName());
        }
    }

    private boolean isOnSameHost(Session session, SmbPath path) {
        return session.getConnection().getRemoteHostname().equals(path.getHostname());
    }

    private Session connectSession(String host) {
        try {
            Connection connection = client.connect(host);
            return connection.authenticate(getAuthenticationContext());
        } catch (IOException e) {
            //TODO bad code
            throw new RuntimeException(e);
        }
    }

    private AuthenticationContext getAuthenticationContext() {
        if (authenticationContext == null) {
            SmbConfiguration config = ((SmbConfiguration) endpoint.getConfiguration());

            String domain = config.getDomain();
            String username = config.getUsername();
            String password = config.getPassword();
            authenticationContext = new AuthenticationContext(username, password.toCharArray(), domain);
        }
        return authenticationContext;
    }
}