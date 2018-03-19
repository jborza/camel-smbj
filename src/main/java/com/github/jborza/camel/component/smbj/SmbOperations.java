package com.github.jborza.camel.component.smbj;

import com.github.jborza.camel.component.smbj.dfs.DfsResolutionResult;
import com.github.jborza.camel.component.smbj.dfs.DfsResolver;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.connection.Connection;
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
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(name);
            share.getShare().rm(share.getPath());
            return true;
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot delete file: " + name, e);
        }
    }

    @Override
    public boolean existsFile(String name) throws GenericFileOperationFailedException {
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(name);
            return share.getShare().fileExists(share.getPath());
        } catch (IOException e) {
            throw new GenericFileOperationFailedException("Cannot determine if file: " + name + " exists", e);
        }
    }

    @Override
    public boolean renameFile(String from, String to) throws GenericFileOperationFailedException {
        throw new UnsupportedOperationException();
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
        try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
            share.connect(name);
            //NB https://msdn.microsoft.com/en-us/library/cc246502.aspx - SMB2 CREATE Request
            // ShareAccess.ALL means that other opens are allowed to read, but not write or delete the file
            File f = share.getShare().openFile(share.getPath(), EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
            InputStream is = f.getInputStream();
            IOUtils.copy(is, os, endpoint.getBufferSize());
            return true;
        } catch (IOException e) {
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
        String path = getConfiguration().getSmbHostPath() + pathEnd;
        return path.replace('\\', '/');
    }

    @Override
    public boolean storeFile(String name, Exchange exchange) {
        String storeName = getPath(name);
        InputStream inputStream = null;

        try {
            inputStream = exchange.getIn().getMandatoryBody(InputStream.class);

            try (SmbShare share = new SmbShare(client, getConfiguration(), isDfs())) {
                share.connect(name);
                File file = share.getShare().openFile(share.getPath(), EnumSet.of(AccessMask.GENERIC_ALL), null, SMB2ShareAccess.ALL, FILE_CREATE, null);
                OutputStream outputStream = file.getOutputStream();

                IOUtils.copy(inputStream, outputStream, endpoint.getBufferSize());
                outputStream.close();
                return true;
            }
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

    class SmbShare implements AutoCloseable {
        private final SMBClient client;
        private final SmbConfiguration config;
        private final boolean dfs;

        private Session session;
        private String path;
        private DfsResolutionResult resolutionResult;
        private DiskShare share;

        public SmbShare(SMBClient client, SmbConfiguration config, boolean dfs) {
            this.client = client;
            this.config = config;
            this.dfs = dfs;
        }

        public void connect(String targetPath) {
            String actualPath = SmbPathUtils.removeShareName(targetPath, config.getShare(), true);
            session = connectSession(config.getHost());
            SmbPath targetSmbPath = new SmbPath(config.getHost(), config.getShare(), actualPath);
            resolutionResult = resolvePath(session, targetSmbPath);
            path = resolutionResult.getSmbPath().getPath();
            share = resolutionResult.getDiskShare();
        }

        @Override
        public void close() throws IOException {
            if (share != null)
                share.close();
            if (session != null)
                session.close();
        }

        public DiskShare getShare() {
            return share;
        }

        public String getPath() {
            return path;
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
            String domain = config.getDomain();
            String username = config.getUsername();
            String password = config.getPassword();
            return new AuthenticationContext(username, password.toCharArray(), domain);
        }

        private boolean isDfs() {
            return dfs;
        }

        private DfsResolutionResult resolvePath(Session session, SmbPath path) {
            if (isDfs()) {
                return connectDfsShare(session, path);
            } else {
                return connectNonDfsShare(session, path);
            }
        }

        private DfsResolutionResult connectDfsShare(Session session, SmbPath path) {
            DfsResolver resolver = new DfsResolver();
            SmbPath resolvedPath = resolver.resolve(client, session, path);
            DiskShare share = getDfsShare(session, resolvedPath);
            return new DfsResolutionResult(share, resolvedPath);
        }

        private DfsResolutionResult connectNonDfsShare(Session session, SmbPath path) {
            DiskShare share = (DiskShare) session.connectShare(path.getShareName());
            return new DfsResolutionResult(share, path);
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
    }
}