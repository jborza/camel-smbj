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
