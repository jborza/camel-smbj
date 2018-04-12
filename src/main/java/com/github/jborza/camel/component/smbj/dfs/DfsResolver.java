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
