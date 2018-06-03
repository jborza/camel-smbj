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

package com.github.jborza.camel.component.smbj;

public final class SmbConstants {
    //see [MS-FSCC].pdf 2.6 File Attributes.
    public final static long FILE_ATTRIBUTE_DIRECTORY = 0x10L;
    public final static long FILE_ATTRIBUTE_READONLY = 0x1L;
    public final static long FILE_ATTRIBUTE_HIDDEN = 0x2L;
    public final static long FILE_ATTRIBUTE_ARCHIVE = 0x20L;
    public final static long FILE_ATTRIBUTE_SYSTEM = 0x4L;
    public final static String PARENT_DIRECTORY = "..";
    public final static String CURRENT_DIRECTORY = ".";
}
