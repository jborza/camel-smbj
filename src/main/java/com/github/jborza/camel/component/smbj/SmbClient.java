package com.github.jborza.camel.component.smbj;

import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.util.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SmbClient {
    private final SmbShareFactory smbShareFactory;

    public SmbClient(SmbShareFactory factory) {
        this.smbShareFactory = factory;
    }

    private SmbShare makeSmbShare() {
        return smbShareFactory.makeSmbShare();
    }

    public List<SmbFile> doListFiles(String path) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.connect(path);
            return share.listFiles();
        }
    }

    public void doStoreFile(String name, InputStream inputStream) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.connect(name);
            share.storeFile(inputStream);
        }
    }

    public void doRetrieveFile(String name, OutputStream os) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.connect(name);
            share.retrieveFile(os);
        } finally {
            IOHelper.close(os, "retrieve: " + name);
        }
    }

    public boolean doFileExists(String name) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.connect(name);
            return share.getShare().fileExists(share.getPath());
        }
    }

    public void doDeleteFile(String name) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.connect(name);
            share.getShare().rm(share.getPath());
        }
    }

    public void doRenameFile(String from, String to) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.connect(from);
            share.rename(from, to);
        }
    }
}
