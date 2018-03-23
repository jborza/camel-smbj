package com.github.jborza.camel.component.smbj;

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

    public List<SmbFile> listFiles(String path) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            return share.listFiles(path);
        }
    }

    public void storeFile(String name, InputStream inputStream) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.storeFile(name, inputStream);
        }
    }

    public void retrieveFile(String name, OutputStream os) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.retrieveFile(name, os);
        } finally {
            IOHelper.close(os, "retrieve: " + name);
        }
    }

    public boolean fileExists(String name) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            return share.fileExists(name);
        }
    }

    public void deleteFile(String name) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.deleteFile(name);
        }
    }

    public void renameFile(String from, String to) throws IOException {
        try (SmbShare share = makeSmbShare()) {
            share.rename(from, to);
        }
    }
}
