package com.bbbbiu.biu.gui.choose;

import java.io.File;

public interface OnChoosingListener {
    void onFileChosen(String filePath);

    void onFileDismissed(String filePath);
}
