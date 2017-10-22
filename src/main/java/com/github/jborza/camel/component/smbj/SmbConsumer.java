package com.github.jborza.camel.component.smbj;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.share.File;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileConsumer;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;

import java.util.List;

public class SmbConsumer extends GenericFileConsumer<File> {

    private String endpointPath;
    private String currentRelativePath = "";

    public SmbConsumer(GenericFileEndpoint<File> endpoint, Processor processor, GenericFileOperations<File> operations) {
        super(endpoint, processor, operations);
        SmbConfiguration config = (SmbConfiguration) endpoint.getConfiguration();
        this.endpointPath = config.getShare() + "\\" + config.getPath();
    }

    private SmbOperations getOperations() {
        return (SmbOperations) operations;
    }

    @Override
    protected boolean pollDirectory(String fileName, List<GenericFile<File>> fileList, int depth) {
        if (log.isTraceEnabled()) {
            log.trace("pollDirectory() running. My delay is [" + this.getDelay() + "] and my strategy is [" + this.getPollStrategy().getClass().toString() + "]");
            log.trace("pollDirectory() fileName[" + fileName + "]");
        }
        depth++;

        SmbOperations ops = (SmbOperations) operations;
        boolean currentFileIsDirectory = false;

        //TODO doing 1 level just now
        List<FileIdBothDirectoryInformation> smbFiles = getOperations().listFilesSpecial(fileName);
        for (FileIdBothDirectoryInformation f : smbFiles) {
            if (!canPollMoreFiles(fileList)) {
                return false;
            }
            GenericFile<File> gf = asGenericFile(f);
            if (gf.isDirectory()) {
                if (endpoint.isRecursive() && depth < endpoint.getMaxDepth()) {
                    //recursive scan of the subdirectory
                    pollDirectory(gf.getFileName(), fileList, depth);
                }
            }
            else{
                if(depth < endpoint.getMinDepth())
                    continue;
                //if (isValidFile(gf, false, smbFiles)) {
                //      fileList.add(gf);
                //}
                fileList.add(gf);
            }
        }
        return true;

//        List<DiskEntry> smbFiles;
//        boolean currentFileIsDir = false;
//        smbFiles = operations.listFiles(fileName);
//        for (DiskEntry smbFile : smbFiles) {
//            if (!canPollMoreFiles(fileList)) {
//                return false;
//            }
//            try {
//                if (smbFile.isDirectory()) {
//                    currentFileIsDir = true;
//                } else {
//                    currentFileIsDir = false;
//                }
//            } catch (SmbException e1) {
//                throw ObjectHelper.wrapRuntimeCamelException(e1);
//            }
//            if (currentFileIsDir) {
//                if (endpoint.isRecursive()) {
//                    currentRelativePath = smbFile.getName().split("/")[0] + "/";
//                    int nextDepth = depth++;
//                    pollDirectory(fileName + "/" + smbFile.getName(), fileList, nextDepth);
//                } else {
//                    currentRelativePath = "";
//                }
//            } else {
//                try {
//                    GenericFile<DiskEntry> genericFile = asGenericFile(fileName, smbFile);
//                    if (isValidFile(genericFile, false, smbFiles)) {
//                        fileList.add(asGenericFile(fileName, smbFile));
//                    }
//                } catch (IOException e) {
//                    throw ObjectHelper.wrapRuntimeCamelException(e);
//                }
//            }
//        }
//        return true;
    }

    @Override
    protected void updateFileHeaders(GenericFile<File> genericFile, Message message) {
        //TODO which headers?
    }

    private GenericFile<File> asGenericFile(FileIdBothDirectoryInformation info) {
        GenericFile<File> f = new GenericFile<File>();
        f.setAbsoluteFilePath(endpointPath + "/" + info.getFileName());
        f.setAbsolute(true);
        f.setEndpointPath(endpointPath);
        f.setFileNameOnly(info.getFileName());
        f.setFileLength(info.getEndOfFile());
        //INFO not setting setFile
        f.setLastModified(info.getLastWriteTime().toEpochMillis());
        f.setFileName(currentRelativePath + info.getFileName());
        f.setRelativeFilePath(info.getFileName());
        boolean isDirectory = (info.getFileAttributes() & SmbConstants.FILE_ATTRIBUTE_DIRECTORY) == SmbConstants.FILE_ATTRIBUTE_DIRECTORY;
        f.setDirectory(isDirectory);
        return f;
    }

    @Override
    protected boolean isMatched(GenericFile<File> file, String doneFileName, List<File> files) {
        return true;
    }
}
