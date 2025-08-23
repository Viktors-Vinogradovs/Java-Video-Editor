package com.videoeditor.model.transfer;

import java.awt.datatransfer.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileTransferable implements Transferable {
    private final List<File> fileList;

    public FileTransferable(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        this.fileList = new ArrayList<>();
        this.fileList.add(file);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.javaFileListFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (isDataFlavorSupported(flavor)) {
            return fileList;
        }
        throw new UnsupportedFlavorException(flavor);
    }
}