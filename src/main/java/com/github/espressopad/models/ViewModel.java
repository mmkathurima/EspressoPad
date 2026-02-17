package com.github.espressopad.models;

import com.github.espressopad.views.components.StatusBar;
import com.github.espressopad.views.components.TextEditor;

import javax.swing.JTextPane;
import javax.swing.JPanel;
import java.nio.file.Path;

public class ViewModel {
    private JPanel tab;
    private TextEditor textEditor;
    private JTextPane resultView;
    private StatusBar statusBar;
    private Path backingFile = null;
    private String title;

    public ViewModel() {
        this(null, null, null, null, null);
    }

    private ViewModel(JPanel tab, TextEditor textEditor, JTextPane resultView, StatusBar statusBar, String title) {
        this.setTextEditor(textEditor);
        this.setResultView(resultView);
        this.setStatusBar(statusBar);
        this.setTitle(title);
        this.setTab(tab);
    }

    public JPanel getTab() {
        return this.tab;
    }

    public void setTab(JPanel tab) {
        if (tab == null)
            tab = new JPanel();
        this.tab = tab;
    }

    public TextEditor getTextEditor() {
        return this.textEditor;
    }

    public void setTextEditor(TextEditor textEditor) {
        if (textEditor == null)
            textEditor = new TextEditor(this);
        this.textEditor = textEditor;
    }

    public JTextPane getResultView() {
        return this.resultView;
    }

    public void setResultView(JTextPane resultView) {
        if (resultView == null)
            resultView = new JTextPane();
        this.resultView = resultView;
        this.resultView.setEditable(false);
        this.resultView.setContentType("text/html");
    }

    public StatusBar getStatusBar() {
        return this.statusBar;
    }

    public void setStatusBar(StatusBar statusBar) {
        if (statusBar == null)
            statusBar = new StatusBar();
        this.statusBar = statusBar;
    }

    public Path getBackingFile() {
        return this.backingFile;
    }

    public void setBackingFile(Path backingFile) {
        this.backingFile = backingFile;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
