package com.github.espressopad.editor;

import com.github.espressopad.controllers.EspressoPadController;
import com.github.espressopad.xml.HtmlHandler;
import com.github.espressopad.xml.XmlHandler;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.stmt.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
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
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        return this.textEditor.getShell();
    }

    public Popup getAutoCompletePopup() {
        return this.autoCompletePopup;
    }


    private void tabAutoCompletion(String currentText) {
        if (currentText == null || this.keyphrases == null) return;

        final AtomicReference<String> finalCurrentText = new AtomicReference<>(currentText);
        Pattern pattern = Pattern.compile("\\b(\\w+)$");
        Matcher matcher = pattern.matcher(currentText);
        if (matcher.find())
            finalCurrentText.set(matcher.group(0));

        List<String> suggestions = this.keyphrases.stream()
                .filter(x -> x.equals(this.autocomplete.getSelectionModel().getSelectedItem()))
                .collect(Collectors.toList());

        suggestions.addAll(this.keyphrases.stream()
                .filter(x -> x.startsWith(finalCurrentText.get()))
                .collect(Collectors.toList()));

        if (!suggestions.isEmpty()) {
            String txt = finalCurrentText.get();
            String suggestion = suggestions.get(0);
            StringBuilder sb = new StringBuilder();

            if (txt.equals(currentText) && txt.contains("."))
                txt = txt.substring(txt.lastIndexOf('.'));

            for (int i = 0; i < suggestion.length(); i++) {
                if (i >= txt.length() || txt.charAt(i) != suggestion.charAt(i))
                    sb.append(suggestion.charAt(i));
            }
            this.textEditor.getCodeArea().insertText(this.textEditor.getCodeArea().getCaretPosition(), sb.toString());
        }
        //this.codeArea.appendText(currentText);
    }

    private void showAutoCompletePopup() {
        Bounds textBounds;
        this.keyphrases = this.textEditor.getShell()
                .sourceCodeAnalysis()
                .completionSuggestions(currentLine, currentLine.length(), new int[1])
                .stream()
                .map(SourceCodeAnalysis.Suggestion::continuation)
                .collect(Collectors.toList());
        if (this.autoCompletePopup != null)
            this.autoCompletePopup.hide();
        if (!this.keyphrases.isEmpty()) {
            this.autocomplete.getItems().setAll(this.keyphrases);
            this.autoCompletePopup = new Popup();
            this.autocomplete.setMaxHeight(80);
            this.autoCompletePopup.getContent().add(this.autocomplete);

            textBounds = this.textEditor.getCodeArea().getCaretBounds().get();
            this.autoCompletePopup.show(this.textEditor.getCodeArea(), textBounds.getMaxX(), textBounds.getMaxY());
            if (!this.autocomplete.getItems().isEmpty())
                this.autocomplete.getSelectionModel().select(0);
        }
        this.textEditor.getCodeArea().requestFocus();
    }

    private void showDocumentation() {
        try (JShell docShell = JShell.builder().out(null).err(null).in(null).build()) {
            this.addArtifactsAndImports(docShell);
            this.addSnippets(docShell);
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

        this.textEditor.setShell(JShell.builder().out(null).err(null).in(null).build());
        this.addArtifactsAndImports(this.textEditor.getShell());
        this.textEditor.getCodeArea().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                try {
                    Executors.newFixedThreadPool(10).submit(new Runnable() {
                        @Override
                        public void run() {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //getShell().snippets().forEach(getShell()::drop);
                                    addSnippets(getShell());
                                    caretPos = textEditor.getCodeArea()
                                            .offsetToPosition(textEditor.getCodeArea().getCaretPosition(),
                                            TwoDimensional.Bias.Forward);
                                    currentLine = textEditor.getCodeArea()
                                            .getText(caretPos.getMajor()).substring(0, caretPos.getMinor());
                                    Path shelfDir = controller.getHomePath().resolve("shelf");

                                    if (!Objects.equals(newValue, oldValue) &&
                                            !controller.getSavedOpenFiles().containsKey(textEditor))
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

                                    if (caretPos.getMinor() > 0 && !currentLine.isBlank() &&
                                            currentLine.charAt(currentLine.length() - 1) != '{' &&
                                            currentLine.charAt(currentLine.length() - 1) != '}') {
                                        showAutoCompletePopup();
                                        //showDocumentation();
                                    } else if (autoCompletePopup != null)
                                        autoCompletePopup.hide();
                                }
                            });

                        }
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
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
                            SourceCodeAnalysis.CompletionInfo completionInfo = textEditor.getShell()
                                    .sourceCodeAnalysis()
                                    .analyzeCompletion(prevLine);
                            switch (completionInfo.completeness()) {
                                case UNKNOWN:
                                case DEFINITELY_INCOMPLETE:
                                case EMPTY:
                                    return;
                                case COMPLETE_WITH_SEMI:
                                case COMPLETE:
                                    snippetEvents = textEditor.getShell().eval(prevLine);
                                    break;
                            }
                        }
                        break;
                    case SPACE:
                        if (event.isControlDown())
                            showAutoCompletePopup();
                        //showDocumentationPopup();
                        break;
                    case ESCAPE:
                        if (autoCompletePopup != null && autoCompletePopup.isShowing())
                            autoCompletePopup.hide();
                        break;
                }
            }
        });
        this.textEditor.getCodeArea().addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                try {
                    Executors.newFixedThreadPool(10).submit(new Runnable() {
                        @Override
                        public void run() {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (Stream.of(KeyCode.SHIFT, KeyCode.ALT, KeyCode.SHORTCUT, KeyCode.CONTROL,
                                                    KeyCode.TAB, KeyCode.CAPS, KeyCode.BACK_SPACE, KeyCode.DELETE,
                                                    KeyCode.ENTER, KeyCode.META)
                                            .noneMatch(x -> event.getCode().equals(x)) && !event.getCode().isArrowKey() &&
                                            !event.isShortcutDown() && !event.isAltDown() && !event.isMetaDown()) {
                                        int cursorPosition = textEditor.getCodeArea().getCaretPosition();
                                        if (cursorPosition > 0) {
                                            char bracket = textEditor.getCodeArea()
                                                    .getText(cursorPosition - 1, cursorPosition)
                                                    .charAt(0);
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
                        }
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        this.textEditor.getCodeArea().addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (autoCompletePopup != null && autoCompletePopup.isShowing())
                    autoCompletePopup.hide();
            }
        });
        this.textEditor.getCodeArea().caretPositionProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
                try {
                    Executors.newFixedThreadPool(10).submit(new Runnable() {
                        @Override
                        public void run() {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    caretPos = textEditor.getCodeArea()
                                            .offsetToPosition(textEditor.getCodeArea().getCaretPosition(),
                                                    TwoDimensional.Bias.Forward);
                                    currentLine = textEditor.getCodeArea()
                                            .getText(caretPos.getMajor())
                                            .substring(0, caretPos.getMinor());
                                    String word = getWord(textEditor.getCodeArea().getText(caretPos.getMajor()),
                                            caretPos.getMinor());
                                    if (controller.isFindReplaceVisible())
                                        controller.getSearchResults();
                                    if (Arrays.stream(TextEditorConstants.KEYWORDS).noneMatch(word::equals)) {
                                        List<Integer> matches = controller.indicesOf(textEditor.getCodeArea().getText(),
                                                word, false, true, true);
                                        for (int match : matches)
                                            textEditor.getCodeArea().setStyle(match, match + word.length(),
                                                    Collections.singletonList("matches"));
                                    }
                                    try {
                                        if (!textEditor.getCodeArea().getText().isBlank() &&
                                                (currentLine.charAt(currentLine.length() - 1) == '('
                                                || currentLine.charAt(currentLine.length() - 1) == '.' ||
                                                currentLine.charAt(currentLine.length() - 1) == ' '))
                                            showDocumentation();
                                    } catch (IndexOutOfBoundsException e) {
                                    }
                                }
                            });
                        }
                    }).get();
                    controller.resetHighlighting();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        });
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

    private void addSnippets(JShell shell) {
        SourceCodeAnalysis.CompletionInfo completion = shell.sourceCodeAnalysis()
                .analyzeCompletion(textEditor.getCodeArea().getText());
        while (completion.source() != null && !completion.source().isBlank()) {
            List<SnippetEvent> snippetEvents = shell.eval(completion.source());
            for (SnippetEvent snippetEvent : snippetEvents) {
                switch (snippetEvent.snippet().kind()) {
                    case TYPE_DECL:
                        //TODO 🤷‍
                        break;
                    case METHOD:
                        BodyDeclaration<?> body = StaticJavaParser.parseBodyDeclaration(snippetEvent.snippet().source());
                        body.asMethodDeclaration().getBody().ifPresent(x -> {
                            for (Statement st : x.getStatements())
                                shell.eval(st.toString());
                        });
                        break;
                    case STATEMENT:
                        Statement stmts = StaticJavaParser.parseStatement(snippetEvent.snippet().source());
                        if (stmts.isDoStmt()) {
                            DoStmt doStmt = stmts.asDoStmt();
                            for (Statement st : doStmt.getBody().asBlockStmt().getStatements())
                                shell.eval(st.toString());
                        } else if (stmts.isForEachStmt()) {
                            ForEachStmt forEachStmt = stmts.asForEachStmt();
                            for (Statement st : forEachStmt.getBody().asBlockStmt().getStatements())
                                shell.eval(st.toString());
                        } else if (stmts.isForStmt()) {
                            ForStmt forStmt = stmts.asForStmt();
                            for (Statement st : forStmt.getBody().asBlockStmt().getStatements())
                                shell.eval(st.toString());
                        } else if (stmts.isForStmt()) {
                            ForStmt forStmt = stmts.asForStmt();
                            for (Statement st : forStmt.getBody().asBlockStmt().getStatements())
                                shell.eval(st.toString());
                        } else if (stmts.isIfStmt()) {
                            IfStmt ifStmt = stmts.asIfStmt();
                            for (Statement st : ifStmt.getThenStmt().asBlockStmt().getStatements())
                                shell.eval(st.toString());
                        } else if (stmts.isSwitchStmt()) {
                            SwitchStmt switchStmt = stmts.asSwitchStmt();
                            for (SwitchEntry switchEntry : switchStmt.getEntries())
                                for (Statement st : switchEntry.getStatements())
                                    shell.eval(st.toString());
                        } else if (stmts.isSynchronizedStmt()) {
                            SynchronizedStmt syncStmt = stmts.asSynchronizedStmt();
                            for (Statement st : syncStmt.getBody().asBlockStmt().getStatements())
                                shell.eval(st.toString());
                        } else if (stmts.isTryStmt()) {
                            TryStmt tryStmt = stmts.asTryStmt();
                            for (Statement st : tryStmt.getTryBlock().asBlockStmt().getStatements())
                                shell.eval(st.toString());
                            for (CatchClause catchClause : tryStmt.getCatchClauses()) {
                                for (Statement st : catchClause.getBody().getStatements())
                                    shell.eval(st.toString());
                            }
                            tryStmt.getFinallyBlock().ifPresent(x -> {
                                for (Statement st : x.getStatements())
                                    shell.eval(st.toString());
                            });
                        } else if (stmts.isWhileStmt()) {
                            WhileStmt whileStmt = stmts.asWhileStmt();
                            for (Statement st : whileStmt.getBody().asBlockStmt().getStatements())
                                shell.eval(st.toString());
                        }
                        break;
                }
            }
            if (!completion.remaining().isBlank())
                completion = shell.sourceCodeAnalysis().analyzeCompletion(completion.remaining());
            else break;
        }
    }

    private static String getWord(String s, int pos) {
        Matcher nMatcher = Pattern.compile("^[a-zA-Z0-9-_]*").matcher(s.substring(pos));
        Matcher pMatcher = Pattern.compile("[a-zA-Z0-9-_]*$").matcher(s.substring(0, pos));

        if (pMatcher.find() && nMatcher.find())
            return pMatcher.group() + nMatcher.group();
        return "";
    }
}