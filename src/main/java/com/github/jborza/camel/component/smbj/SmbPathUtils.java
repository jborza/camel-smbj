/**
 * Copyright [2018] [Juraj Borza]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jborza.camel.component.smbj;

import java.io.File;
import java.util.regex.Pattern;

public final class SmbPathUtils {

    public static String convertToBackslashes(String path) {
        return path.replace('/', '\\');
    }

    public static String removeShareName(String path, String shareName, boolean forceWindowsSeparator) {
        String separator = forceWindowsSeparator ? "\\" : File.separator;
        return removeShareName(path, shareName, separator);
    }

    private static String removeShareName(String path, String shareName, String separator) {
        if (path.equals(shareName))
            return "";
        String sharePathElementPattern = "^" + shareName + Pattern.quote(separator);
        return path.replaceFirst(sharePathElementPattern, "");
    }
}
