package com.bbbbiu.biu.gui.fragments;

import java.io.File;

/**
 * Created by finalize on 3/13/16.
 */
public interface OnFileChoosingListener {
    void onFileFirstChosen();

    void onFileAllDismissed();

    void onFileChosen(File file);

    void onFileDismissed(File file);
}
