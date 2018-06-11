package com.jaisel.filepicker.loader;

import com.jaisel.filepicker.model.File;

import java.util.ArrayList;

public interface FileResultCallback {
    void onResult(ArrayList<File> files);
}
