package com.github.jborza.camel.component.smbj;

import com.github.jborza.camel.component.smbj.dfs.DfsResolutionResult;
import com.github.jborza.camel.component.smbj.dfs.DfsResolver;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import java.io.IOException;

public class SmbShare implements AutoCloseable {
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

    private boolean isDfs() {
        return dfs;
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
