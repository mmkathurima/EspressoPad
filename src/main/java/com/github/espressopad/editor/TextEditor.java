package com.github.espressopad.editor;

import javafx.embed.swing.SwingNode;
import javafx.scene.control.Tab;
import jdk.jshell.JShell;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.io.IOException;

public class TextEditor {
    private final RSyntaxTextArea codeArea;
    private final Tab tab;
    private JShell shell;

    public RSyntaxTextArea getCodeArea() {
        return this.codeArea;
    }

    public Tab getTab() {
        return this.tab;
    }

    public JShell getShell() {
        return this.shell;
    }

    public TextEditor(Tab tab) {
        this.tab = tab;
        this.codeArea = new RSyntaxTextArea();
        this.codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        this.codeArea.setMarkOccurrences(true);
        this.codeArea.setCloseCurlyBraces(true);
        this.codeArea.setAnimateBracketMatching(true);
        this.codeArea.setCloseMarkupTags(true);
        this.codeArea.setInsertPairedCharacters(true);
        this.codeArea.setAutoIndentEnabled(true);
        this.codeArea.setCodeFoldingEnabled(true);
        RTextScrollPane scrollPane = new RTextScrollPane(this.codeArea);
        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                swingNode.setContent(scrollPane);
            }
        });
        tab.setContent(swingNode);
        this.codeArea.setPreferredSize(new Dimension(600, 500));
        tab.getTabPane().getStylesheets().add(this.getClass().getResource("editor.css").toExternalForm());
    }

    public void stop() throws IOException {
        this.shell.close();
        this.shell.stop();
    }
}
