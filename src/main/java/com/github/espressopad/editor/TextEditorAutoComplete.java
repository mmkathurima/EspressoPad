package com.github.espressopad.editor;

import com.github.espressopad.EspressoPadController;
import com.github.espressopad.xml.HtmlHandler;
import com.github.espressopad.xml.XmlHandler;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.fxmisc.richtext.model.TwoDimensional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextEditorAutoComplete {
    private final TextEditor textEditor;
    private String currentLine;
    private Popup autoCompletePopup;
    private ListView<String> autocomplete;
    private List<String> keyphrases;
    private TwoDimensional.Position caretPos;
    private List<SnippetEvent> snippetEvents;
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
        return this.textEditor.shell;
    }

    public Popup getAutoCompletePopup() {
        return this.autoCompletePopup;
    }


    private String findCommonPrefix(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        int index = 0;

        while (index < minLength && s1.charAt(index) == s2.charAt(index))
            index++;

        return s1.substring(0, index);
    }

    private void tabAutoCompletion(String currentText) {
        if (currentText == null || this.keyphrases == null) return;
        if (currentText.contains("."))
            currentText = currentText.substring(currentText.lastIndexOf(".") + 1);
        else if (currentText.contains(" "))
            currentText = currentLine.substring(currentText.lastIndexOf(" ") + 1);

        String finalCurrentText = currentText;
        List<String> suggestions = this.keyphrases.stream().filter(x -> x.startsWith(finalCurrentText))
                .collect(Collectors.toList());

        if (!suggestions.isEmpty()) {
            String suggestion = suggestions.stream()
                    .filter(x -> x.equals(this.autocomplete.getSelectionModel().getSelectedItem()))
                    .findFirst().orElse(suggestions.get(0));
            String prefix = this.findCommonPrefix(currentText, suggestion);
            this.textEditor.getCodeArea().insertText(this.textEditor.getCodeArea().getCaretPosition(),
                    suggestion.substring(prefix.length()));
        }
        //this.codeArea.appendText(currentText);
    }

    private void showAutoCompletePopup() {
        this.keyphrases = this.textEditor.shell.sourceCodeAnalysis()
                .completionSuggestions(currentLine, currentLine.length(), new int[1])
                .stream().map(SourceCodeAnalysis.Suggestion::continuation)
                .collect(Collectors.toList());
        if (this.autoCompletePopup != null)
            this.autoCompletePopup.hide();
        if (!this.keyphrases.isEmpty()) {
            this.autocomplete.getItems().setAll(this.keyphrases);
            this.autoCompletePopup = new Popup();
            this.autocomplete.setMaxHeight(80);
            this.autoCompletePopup.getContent().add(this.autocomplete);
            this.autoCompletePopup.show(
                    this.textEditor.getCodeArea(),
                    this.textEditor.getCodeArea().getCaretBounds().get().getMaxX(),
                    this.textEditor.getCodeArea().getCaretBounds().get().getMaxY());
        }
        this.textEditor.getCodeArea().requestFocus();
    }

    private void showDocumentation() {
        try (JShell docShell = JShell.create()) {
            this.addArtifactsAndImports(docShell);
            this.getShell().imports().map(Snippet::source).forEach(docShell::eval);
            this.caretPos = textEditor.getCodeArea().offsetToPosition(textEditor.getCodeArea().getCaretPosition(),
                    TwoDimensional.Bias.Forward);
            this.currentLine = textEditor.getCodeArea().getText(caretPos.getMajor()).substring(0, caretPos.getMinor());

            List<SourceCodeAnalysis.Documentation> docs = docShell.sourceCodeAnalysis()
                    .documentation(this.currentLine, this.currentLine.length(), true);

            if (!docs.isEmpty())
                this.controller.getDocumentationView().getEngine().loadContent(docs.stream()
                        .map(doc -> String.format("<div><code>%s</code><br><br>%s<hr/></div>", doc.signature(),
                                HtmlHandler.convertJavaDoc(doc.javadoc())))
                        .collect(Collectors.joining()));
        }
    }

    protected void initAutoCompleteEvents() {
        this.autocomplete = new ListView<>();
        this.autocomplete.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case TAB:
                    case ENTER:
                        tabAutoCompletion(currentLine);
                    case UP:
                    case DOWN:
                    case LEFT:
                    case RIGHT:
                        autocomplete.requestFocus();
                        break;
                }
            }
        });

        this.autocomplete.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2 && event.getTarget() instanceof Text) {
                    tabAutoCompletion(currentLine);
                    autocomplete.requestFocus();
                }
            }
        });

        this.textEditor.shell = JShell.create();
        this.addArtifactsAndImports(this.textEditor.shell);
        this.textEditor.getCodeArea().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                caretPos = textEditor.getCodeArea().offsetToPosition(textEditor.getCodeArea().getCaretPosition(),
                        TwoDimensional.Bias.Forward);
                currentLine = textEditor.getCodeArea().getText(caretPos.getMajor()).substring(0, caretPos.getMinor());
                Path shelfDir = controller.getHomePath().resolve("shelf");

                if (!Objects.equals(newValue, oldValue))
                    textEditor.getTab().setText(String.format("*%s", textEditor.getTab().getText()
                            .replaceFirst("^\\*", "")));

                try {
                    if (!controller.getSavedOpenFiles().containsKey(textEditor) && shelvedFileName == null
                            && savedFile == null) {
                        if (!shelfDir.toFile().exists())
                            Files.createDirectory(shelfDir);
                        shelvedFileName = UUID.randomUUID().toString();
                        Files.writeString(shelfDir.resolve(shelvedFileName), textEditor.getCodeArea().getText(),
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } else if (shelvedFileName != null && !shelvedFileName.isBlank())
                        Files.writeString(shelfDir.resolve(shelvedFileName), textEditor.getCodeArea().getText(),
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    savedFile = null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (!currentLine.isBlank() && (currentLine.charAt(currentLine.length() - 1) == '('
                        || currentLine.charAt(currentLine.length() - 1) == '.' ||
                        currentLine.charAt(currentLine.length() - 1) == ' ')) {
                    showDocumentation();
                } else if (caretPos.getMinor() > 0 && !currentLine.isBlank() &&
                        currentLine.charAt(currentLine.length() - 1) != '{' &&
                        currentLine.charAt(currentLine.length() - 1) != '}') {
                    showAutoCompletePopup();
                    //showDocumentation();
                } else if (autoCompletePopup != null)
                    autoCompletePopup.hide();
            }
        });
        this.textEditor.getCodeArea().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case ENTER:
                        caretPos = textEditor.getCodeArea().offsetToPosition(textEditor.getCodeArea().getCaretPosition(),
                                TwoDimensional.Bias.Forward);
                        if (caretPos.getMinor() == 0) {
                            String prevLine = textEditor.getCodeArea().getText(
                                    caretPos.getMajor() - ((caretPos.getMajor() > 0) ? 1 : 0));
                            SourceCodeAnalysis.CompletionInfo completionInfo = textEditor.shell
                                    .sourceCodeAnalysis()
                                    .analyzeCompletion(prevLine);
                            switch (completionInfo.completeness()) {
                                case UNKNOWN:
                                case DEFINITELY_INCOMPLETE:
                                case EMPTY:
                                    return;
                                case COMPLETE_WITH_SEMI:
                                case COMPLETE:
                                    snippetEvents = textEditor.shell.eval(prevLine);
                                    break;
                            }
                        }
                        break;
                    case SPACE:
                        if (event.isControlDown())
                            showAutoCompletePopup();
                        //showDocumentationPopup();
                        break;
                }
            }
        });
        this.textEditor.getCodeArea().addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (Stream.of(KeyCode.SHIFT, KeyCode.ALT, KeyCode.CONTROL, KeyCode.TAB, KeyCode.CAPS, KeyCode.BACK_SPACE,
                        KeyCode.DELETE, KeyCode.ENTER).noneMatch(x -> event.getCode().equals(x)) &&
                        !event.getCode().isArrowKey()) {
                    int cursorPosition = textEditor.getCodeArea().getCaretPosition();
                    if (cursorPosition > 0) {
                        char bracket = textEditor.getCodeArea().getText(cursorPosition - 1, cursorPosition).charAt(0);
                        //PunctuationComplete.onPunctuationComplete(codeTextArea, bracket, cursorPosition);
                        switch (bracket) {
                            case '{':
                                textEditor.getCodeArea().insertText(cursorPosition, "}");
                                textEditor.getCodeArea().moveTo(cursorPosition);
                                break;
                            case '[':
                                textEditor.getCodeArea().insertText(cursorPosition, "]");
                                textEditor.getCodeArea().moveTo(cursorPosition);
                                break;
                            case '(':
                                textEditor.getCodeArea().insertText(cursorPosition, ")");
                                textEditor.getCodeArea().moveTo(cursorPosition);
                                break;
                            case '\'':
                                textEditor.getCodeArea().insertText(cursorPosition, "'");
                                textEditor.getCodeArea().moveTo(cursorPosition);
                                break;
                            case '\"':
                                textEditor.getCodeArea().insertText(cursorPosition, "\"");
                                textEditor.getCodeArea().moveTo(cursorPosition);
                                break;
                        }
                    }
                }
            }
        });
        this.textEditor.getCodeArea().addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (autoCompletePopup != null && autoCompletePopup.isShowing())
                    autoCompletePopup.hide();
                if (event.getTarget() instanceof Text) {
                    try {
                        int line = Integer.parseInt(((Text) event.getTarget()).getText()) - 1;
                        textEditor.getCodeArea().selectRange(line, 0, line,
                                textEditor.getCodeArea().getParagraph(line).getText().length());
                    } catch (NumberFormatException e) {
                    }
                }
            }
        });
        this.textEditor.getCodeArea().addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (!textEditor.getCodeArea().getText().isBlank())
                    showDocumentation();
            }
        });
    }

    private void addArtifactsAndImports(JShell shell) {
        XmlHandler handler = new XmlHandler();
        if (handler.getArtifactFile().exists()) {
            for (String s : handler.parseArtifactXml())
                shell.addToClasspath(s);
        }
        Path dumpFile = controller.getHomePath().resolve("libs").resolve("dump.jar");
        shell.addToClasspath(dumpFile.toString());
        if (handler.getImportsFile().exists())
            shell.eval(handler.parseImportXml()
                    .stream()
                    .map(imports -> String.format("import %s;", imports))
                    .collect(Collectors.joining()));
    }
}
