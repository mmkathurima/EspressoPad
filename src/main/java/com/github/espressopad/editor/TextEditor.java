package com.github.espressopad.editor;

import javafx.concurrent.Task;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import jdk.jshell.JShell;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

public class TextEditor {
    private final CustomCodeArea codeArea;
    private final ExecutorService executor;
    private final Tab tab;
    private final BracketHighlighter highlighter;
    private JShell shell;
    private Subscription subscriber;

    public Subscription getSubscriber() {
        return subscriber;
    }

    private void setSubscriber(Subscription subscriber) {
        this.subscriber = subscriber;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public Tab getTab() {
        return this.tab;
    }

    public BracketHighlighter getHighlighter() {
        return this.highlighter;
    }

    public JShell getShell() {
        return this.shell;
    }

    public void setShell(JShell shell) {
        this.shell = shell;
    }

    public TextEditor(Tab tab) {
        this.tab = tab;
        this.codeArea = new CustomCodeArea();
        this.highlighter = new BracketHighlighter(this.codeArea);
        this.executor = Executors.newSingleThreadExecutor();
        this.codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        this.setSubscriber(codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(200))
                .retainLatestUntilLater(executor)
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(codeArea.multiPlainChanges())
                .filterMap(t -> {
                    if (t.isSuccess())
                        return Optional.of(t.get());
                    else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                }).subscribe(this::applyHighlighting));

        // call when no longer need it: `cleanupWhenFinished.unsubscribe();`

        //this.codeArea.replaceText(0, 0, TextEditorConstants.sampleCode);
        this.codeArea.setStyle("-fx-font-family: consolas, monospace; -fx-font-size: 11pt;");
        this.codeArea.setId("code-area");
        this.codeArea.setWrapText(true);
        tab.setContent(new StackPane(new VirtualizedScrollPane<>(this.codeArea)));
        tab.getTabPane().getStylesheets().add(this.getClass().getResource("editor.css").toExternalForm());
    }

    public void stop() throws IOException {
        this.executor.shutdown();
        this.shell.stop();
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = TextEditorConstants.PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass; /* never happens */
            if (matcher.group("KEYWORD") != null)
                styleClass = "keyword";
            else if (matcher.group("PAREN") != null)
                styleClass = "paren";
            else if (matcher.group("BRACE") != null)
                styleClass = "brace";
            else if (matcher.group("BRACKET") != null)
                styleClass = "bracket";
            else if (matcher.group("SEMICOLON") != null)
                styleClass = "semicolon";
            else if (matcher.group("STRING") != null)
                styleClass = "string";
            else if (matcher.group("NUMBER") != null &&
                    !matcher.group("NUMBER").isBlank() &&
                    !matcher.group("NUMBER").equals("."))
                styleClass = "number";
            else if (matcher.group("COMMENT") != null)
                styleClass = "comment";
            else
                styleClass = null;
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override
            protected StyleSpans<Collection<String>> call() {
                return TextEditor.computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    public void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        codeArea.setStyleSpans(0, highlighting);
    }
}
