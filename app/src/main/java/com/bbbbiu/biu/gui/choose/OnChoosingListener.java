package com.bbbbiu.biu.gui.choose;

import java.io.File;

public interface OnChoosingListener {
    void onFileChosen(File file);

    void onFileDismissed(File file);
}
