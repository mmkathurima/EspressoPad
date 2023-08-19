package com.example.jshelleditor.editor;

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
    private final CodeArea codeArea;
    private final ExecutorService executor;
    private Subscription subscriber;
    protected JShell shell;

    public TextEditor(Tab tab) {
        this.codeArea = new CodeArea();
        this.executor = Executors.newSingleThreadExecutor();
        this.getCodeArea().setParagraphGraphicFactory(LineNumberFactory.get(getCodeArea()));
        this.setSubscriber(getCodeArea().multiPlainChanges()
                .successionEnds(Duration.ofMillis(200))
                .retainLatestUntilLater(executor)
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(getCodeArea().multiPlainChanges())
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
        this.getCodeArea().setStyle("-fx-font-family: consolas, monospace; -fx-font-size: 11pt;");
        this.getCodeArea().setId("code-area");
        this.getCodeArea().setWrapText(true);
        tab.setContent(new StackPane(new VirtualizedScrollPane<>(this.getCodeArea())));
        tab.getTabPane().getStylesheets().add(this.getClass().getResource("java-keywords.css").toExternalForm());
    }

    public void stop() throws IOException {
        this.executor.shutdown();
        this.shell.stop();
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = TextEditorConstants.PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass; /* never happens */
            if (matcher.group("KEYWORD") != null) styleClass = "keyword";
            else styleClass = matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                            matcher.group("BRACKET") != null ? "bracket" :
                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                            matcher.group("STRING") != null ? "string" :
                                                    matcher.group("COMMENT") != null ? "comment" :
                                                            null;
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public Subscription getSubscriber() {
        return subscriber;
    }

    protected void setSubscriber(Subscription subscriber) {
        this.subscriber = subscriber;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = getCodeArea().getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return TextEditor.computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        getCodeArea().setStyleSpans(0, highlighting);
    }
}
