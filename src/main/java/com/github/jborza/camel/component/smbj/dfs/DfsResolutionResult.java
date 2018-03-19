package com.github.jborza.camel.component.smbj.dfs;

import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.share.DiskShare;

public class DfsResolutionResult {
    private final DiskShare diskShare;
    private final SmbPath smbPath;

    public DfsResolutionResult(DiskShare diskShare, SmbPath smbPath) {
        this.diskShare = diskShare;
        this.smbPath = smbPath;
    }

    public DiskShare getDiskShare() {
        return diskShare;
    }

    public SmbPath getSmbPath() {
        return smbPath;
    }
}
