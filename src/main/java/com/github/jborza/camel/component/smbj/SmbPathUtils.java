package com.github.jborza.camel.component.smbj;

import java.io.File;
import java.util.regex.Pattern;

public class SmbPathUtils {
    public static String convertToBackslashes(String path) {
        return path.replace('/', '\\');
    }

    public static String removeShareName(String path, String shareName, boolean forceWindowsSeparator) {
        String separator = forceWindowsSeparator ? "\\" : File.separator;
        return removeShareName(path, shareName, separator);
    }

    private static String removeShareName(String path, String shareName, String separator) {
        String sharePathElementPattern = "^" + shareName + Pattern.quote(java.io.File.separator);
        return path.replaceFirst(sharePathElementPattern, "");
    }
}
