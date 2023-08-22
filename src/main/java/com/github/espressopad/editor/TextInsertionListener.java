package com.github.espressopad.editor;

public interface TextInsertionListener {
    void codeInserted(int start, int end, String text);
}
