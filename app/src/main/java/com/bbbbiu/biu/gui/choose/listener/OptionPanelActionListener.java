package com.bbbbiu.biu.gui.choose.listener;

import java.io.File;

public interface OptionPanelActionListener {
    void onOptionToggleClicked(File file);

    void onOptionItemClicked(File file);

    void onOptionModifyContent();

    void onOptionDeleteFile(File file);
}
