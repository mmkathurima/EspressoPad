package com.github.espressopad.editor;

import com.github.espressopad.controllers.EspressoPadController;
import com.github.espressopad.xml.XmlHandler;
import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TextEditorAutoComplete {
    private final TextEditor textEditor;
    private final EspressoPadController controller;
    private String shelvedFileName = null;
    private File savedFile = null;

    public TextEditorAutoComplete(TextEditor textEditor, EspressoPadController controller) {
        this.textEditor = textEditor;
        this.controller = controller;
        this.initAutoCompleteEvents();
    }

    public TextEditorAutoComplete(TextEditor textEditor, String shelvedFileName, EspressoPadController controller) {
        this(textEditor, controller);
        this.shelvedFileName = shelvedFileName;
    }

    public TextEditorAutoComplete(TextEditor textEditor, File savedFile, EspressoPadController controller) {
        this(textEditor, controller);
        this.savedFile = savedFile;
    }

    public JShell getShell() {
        return this.textEditor.getShell();
    }

    protected void initAutoCompleteEvents() {
        try (JShell shell = JShell.builder().out(null).err(null).in(null).build()) {
            RSyntaxTextArea textArea = this.textEditor.getCodeArea();
            String currentLine;
            try {
                int start = textArea.getLineStartOffset(textArea.getCaretLineNumber());
                int end = textArea.getLineEndOffset(textArea.getCaretLineNumber());
                currentLine = textArea.getText(start, Math.abs(end - start));
            } catch (BadLocationException e) {
                currentLine = "";
            }
            DefaultCompletionProvider provider = new DefaultCompletionProvider();
            List<Completion> completions = new ArrayList<>();
            List<SourceCodeAnalysis.Documentation> documentations;
            String completion, signature = null, description = null;
            for (SourceCodeAnalysis.Suggestion suggestion : shell.sourceCodeAnalysis()
                    .completionSuggestions(currentLine, currentLine.length(), new int[1])) {
                completion = currentLine + suggestion.continuation();
                documentations = shell.sourceCodeAnalysis()
                        .documentation(completion, completion.length(), true);
                for (SourceCodeAnalysis.Documentation documentation : documentations) {
                    signature = documentation.signature();
                    description = documentation.javadoc();
                }
                BasicCompletion basicCompletion = new BasicCompletion(
                        provider, suggestion.continuation(), signature, description
                );
                completions.add(basicCompletion);
            }
            provider.addCompletions(completions);
            AutoCompletion autoCompletion = new AutoCompletion(provider);
            autoCompletion.setShowDescWindow(true);
            autoCompletion.setParameterAssistanceEnabled(true);
            autoCompletion.setAutoCompleteEnabled(true);
            autoCompletion.setAutoActivationEnabled(true);
            autoCompletion.setAutoCompleteSingleChoices(true);
            autoCompletion.install(textArea);
        }
    }

    private void onChange(DocumentEvent event) {
        Path shelfDir = controller.getHomePath().resolve("shelf");
        if (!controller.getSavedOpenFiles().containsKey(textEditor))
            textEditor.getTab().setText(String.format("*%s", textEditor.getTab().getText()
                    .replaceFirst("^\\*", "")));

        try {
            if (!controller.getSavedOpenFiles().containsKey(textEditor)
                    && shelvedFileName == null && savedFile == null) {
                if (!shelfDir.toFile().exists())
                    Files.createDirectory(shelfDir);
                shelvedFileName = UUID.randomUUID().toString();
                Files.writeString(shelfDir.resolve(shelvedFileName),
                        textEditor.getCodeArea().getText(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else if (shelvedFileName != null && !shelvedFileName.isBlank())
                Files.writeString(shelfDir.resolve(shelvedFileName),
                        textEditor.getCodeArea().getText(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            else if (controller.getSavedOpenFiles().containsKey(textEditor))
                Files.writeString(controller.getSavedOpenFiles().get(textEditor).toPath(),
                        textEditor.getCodeArea().getText(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            savedFile = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addArtifactsAndImports(JShell shell) {
        XmlHandler handler = new XmlHandler();
        if (handler.getArtifactFile().exists()) {
            for (String s : handler.parseArtifactXml())
                shell.addToClasspath(s);
        }
        shell.addToClasspath(controller.getDumpFile().toString());
        if (handler.getImportsFile().exists())
            shell.eval(handler.parseImportXml()
                    .stream()
                    .map(imports -> String.format("import %s;", imports))
                    .collect(Collectors.joining()));
    }
}