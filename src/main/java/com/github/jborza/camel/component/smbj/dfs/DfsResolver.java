package com.github.jborza.camel.component.smbj.dfs;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb2.messages.SMB2Echo;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.paths.PathResolveException;
import com.hierynomus.smbj.session.Session;

public class DfsResolver {
    public SmbPath resolve(SMBClient client, Session session, SmbPath path) {
        try {
            SMB2Echo responsePacket = new SMB2Echo();
            responsePacket.getHeader().setStatus(NtStatus.STATUS_PATH_NOT_COVERED);
            return client.getPathResolver().resolve(session, responsePacket, path);
        } catch (PathResolveException e) {
            throw new DfsPathResolveException(e);
        }
    }
}
