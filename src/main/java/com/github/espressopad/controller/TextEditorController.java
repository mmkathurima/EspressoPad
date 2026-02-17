package com.github.espressopad.controller;

import com.github.abrarsyed.jastyle.ASFormatter;
import com.github.abrarsyed.jastyle.constants.EnumFormatStyle;
import com.github.abrarsyed.jastyle.constants.SourceMode;
import com.github.espressopad.models.ViewModel;
import com.github.espressopad.utils.Utilities;
import com.github.espressopad.views.components.StatusBar;
import com.github.espressopad.views.components.TextEditor;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class TextEditorController {
    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", Locale.getDefault());

    public void undo(TextEditor textEditor) {
        if (textEditor.canUndo())
            textEditor.undoLastAction();
    }

    public void redo(TextEditor textEditor) {
        if (textEditor.canRedo())
            textEditor.redoLastAction();
    }

    public void copy(TextEditor textEditor) {
        textEditor.copy();
    }

    public void cut(TextEditor textEditor) {
        textEditor.cut();
    }

    public void paste(TextEditor textEditor) {
        textEditor.paste();
    }

    public void selectAll(TextEditor textEditor) {
        textEditor.requestFocusInWindow();
        textEditor.selectAll();
    }

    public void findAction(ViewModel viewModel) {
        if (this.replaceDialog != null && this.replaceDialog.isVisible())
            this.replaceDialog.setVisible(false);

        Frame frame = JOptionPane.getFrameForComponent(viewModel.getTextEditor());
        this.findDialog = new FindDialog(frame, new TextEditorSearchListener(viewModel.getTextEditor(), viewModel.getStatusBar()));
        this.findDialog.setVisible(true);
    }

    public void replaceAction(ViewModel viewModel) {
        if (this.findDialog != null && this.findDialog.isVisible())
            this.findDialog.setVisible(false);
        Frame frame = JOptionPane.getFrameForComponent(viewModel.getTextEditor());
        this.replaceDialog = new ReplaceDialog(frame, new TextEditorSearchListener(viewModel.getTextEditor(), viewModel.getStatusBar()));
        this.replaceDialog.setVisible(true);
    }

    public void setupGoToLine(TextEditor textEditor) {
        Frame frame = JOptionPane.getFrameForComponent(textEditor);
        if (this.findDialog != null && this.findDialog.isVisible())
            this.findDialog.setVisible(false);

        if (this.replaceDialog != null && this.replaceDialog.isVisible())
            this.replaceDialog.setVisible(false);

        Object goTo = JOptionPane.showInputDialog(
                frame,
                this.resourceBundle.getString("go.to.line"),
                this.resourceBundle.getString("go.to.line"),
                JOptionPane.QUESTION_MESSAGE,
                UIManager.getIcon("OptionPane.questionIcon"),
                null,
                textEditor.getCaretLineNumber() + 1
        );
        if (goTo != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        int y = Integer.parseInt(String.valueOf(goTo)) - 1;
                        textEditor.getScrollPane().getVerticalScrollBar().setValue(textEditor.yForLine(y));
                        textEditor.setCaretPosition(textEditor.getLineStartOffset(y));
                        textEditor.requestFocusInWindow();
                    } catch (BadLocationException | NumberFormatException e) {
                        UIManager.getLookAndFeel().provideErrorFeedback(textEditor);
                        JOptionPane.showMessageDialog(
                                frame,
                                TextEditorController.this.resourceBundle.getString("invalid.line"),
                                TextEditorController.this.resourceBundle.getString("go.to.line"),
                                JOptionPane.ERROR_MESSAGE
                        );
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    public void duplicateSelectionAction(TextEditor textEditor) {
        try {
            if (textEditor.getSelectedText() == null) {
                int currentLineStart = textEditor.getLineStartOffsetOfCurrentLine();
                int currentLineEnd = textEditor.getLineEndOffsetOfCurrentLine();
                String currentText = textEditor.getText(currentLineStart, currentLineEnd - currentLineStart);
                if (Pattern.compile("^(.*)$", Pattern.MULTILINE).split(currentText).length == 0)
                    currentText = "\n" + currentText;
                textEditor.insert(currentText, currentLineEnd);
            } else textEditor.insert(String.format("\n%s", textEditor.getSelectedText()), textEditor.getSelectionEnd());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public void reformatSelectionAction(TextEditor textEditor) {
        ASFormatter formatter = new ASFormatter();
        formatter.setSourceStyle(SourceMode.JAVA);
        formatter.setFormattingStyle(EnumFormatStyle.JAVA);
        formatter.setSwitchIndent(true);
        formatter.setCaseIndent(true);
        formatter.setTabSpaceConversionMode(true);
        formatter.setLabelIndent(true);

        int start, end;
        if (textEditor.getSelectedText() == null) {
            start = 0;
            end = textEditor.getText().length() - 1;
        } else {
            start = textEditor.getSelectionStart();
            end = textEditor.getSelectionEnd();
        }

        try (StringReader stringReader = new StringReader(textEditor.getText(start, end - start));
             Reader reader = new BufferedReader(stringReader);
             Writer writer = new StringWriter()) {
            formatter.format(reader, writer);
            textEditor.replaceRange(writer.toString(), start, end);
        } catch (IOException | BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * NOTE: Never call this method
     */
    public Path saveFile(ViewModel viewModel) {
        try {
            Path backingFile = viewModel.getBackingFile();
            if (backingFile == null)
                backingFile = this.saveFileAs(viewModel);
            else
                try (BufferedWriter writer = Files.newBufferedWriter(backingFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write(viewModel.getTextEditor().getText());
                }
            viewModel.getTextEditor().setDirty(false);
            return backingFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * NOTE: Never call this method
     */
    public Path saveFileAs(ViewModel viewModel) {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(Utilities.validateDefaultDirectory().toFile());
            chooser.setFileFilter(new FileNameExtensionFilter(this.resourceBundle.getString("jsh.file"), "jsh"));
            if (chooser.showSaveDialog(viewModel.getTab()) == JFileChooser.APPROVE_OPTION) {
                Path selectedFile = chooser.getSelectedFile().toPath();
                String fileName = String.format("%s.jsh", selectedFile.getFileName());
                Path path = selectedFile.getParent().resolve(fileName);
                try (BufferedWriter writer = Files.newBufferedWriter(selectedFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write(viewModel.getTextEditor().getText());
                }
                viewModel.getTextEditor().setDirty(false);
                return path;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private class TextEditorSearchListener implements SearchListener {
        private final TextEditor textEditor;
        private final StatusBar statusBar;

        TextEditorSearchListener(TextEditor textEditor, StatusBar statusBar) {
            this.textEditor = textEditor;
            this.statusBar = statusBar;
        }

        @Override
        public void searchEvent(SearchEvent e) {
            SearchEvent.Type type = e.getType();
            SearchContext context = e.getSearchContext();
            SearchResult result;

            switch (type) {
                default: // Prevent FindBugs warning later
                case MARK_ALL:
                    result = SearchEngine.markAll(this.textEditor, context);
                    break;
                case FIND:
                    result = SearchEngine.find(this.textEditor, context);
                    if (!result.wasFound() || result.isWrapped())
                        UIManager.getLookAndFeel().provideErrorFeedback(this.textEditor);
                    break;
                case REPLACE:
                    result = SearchEngine.replace(this.textEditor, context);
                    if (!result.wasFound() || result.isWrapped())
                        UIManager.getLookAndFeel().provideErrorFeedback(this.textEditor);
                    break;
                case REPLACE_ALL:
                    result = SearchEngine.replaceAll(this.textEditor, context);
                    JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(this.textEditor),
                            String.format(TextEditorController.this.resourceBundle.getString("d.occurrences.replaced"), result.getCount())
                    );
                    break;
            }

            String text;
            if (result.wasFound())
                text = String.format(TextEditorController.this.resourceBundle.getString("text.found.occurrences.marked.d"), result.getMarkedCount());
            else if (type == SearchEvent.Type.MARK_ALL) {
                if (result.getMarkedCount() > 0)
                    text = String.format(TextEditorController.this.resourceBundle.getString("occurrences.marked.d"), result.getMarkedCount());
                else text = "";
            } else text = TextEditorController.this.resourceBundle.getString("text.not.found");
            this.statusBar.setFindOccurrencesLabel(text);
        }

        @Override
        public String getSelectedText() {
            return this.textEditor.getSelectedText();
        }
    }
}
