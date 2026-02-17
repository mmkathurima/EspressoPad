package com.github.espressopad.controller;

import com.github.espressopad.io.ConsoleInputStream;
import com.github.espressopad.io.ConsoleOutputStream;
import com.github.espressopad.models.ViewModel;
import com.github.espressopad.utils.SerializationUtilities;
import com.github.espressopad.views.components.FileTree;
import com.github.espressopad.views.components.MessageConsole;
import com.github.espressopad.views.components.TextEditor;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.stmt.*;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EspressoPadController {
    private static final JShell shell = JShell.builder().out(null).in(null).err(null).build();
    private final Logger logger = LoggerFactory.getLogger(EspressoPadController.class);
    private final DefaultCompletionProvider provider = new DefaultCompletionProvider();
    private final SerializationUtilities handler = new SerializationUtilities();
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", Locale.getDefault());

    public static JShell getShell() {
        return shell;
    }

    public void addArtifactsAndImports(JShell shell) {
        if (Files.exists(this.handler.getArtifactFile())) {
            for (String s : this.handler.parseArtifactXml())
                shell.addToClasspath(s);
        }
        if (!Files.exists(this.handler.getImportsFile()))
            this.handler.writeImportXml(List.of("java.util.stream.*", "java.util.*", "java.io.*"));
        else shell.eval(this.handler.parseImportXml()
                .stream()
                .map(imports -> String.format("import %s;", imports))
                .collect(Collectors.joining()));
    }

    public void setupTextChangeListener(TextEditor textEditor) {
        AutoCompletion ac = new AutoCompletion(this.provider);
        this.provider.setAutoActivationRules(true, ".");
        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        //ac.setShowDescWindow(true);
        ac.install(textEditor);
        textEditor.addCaretListener(event -> this.setupCaretChangeEvent(textEditor));
        textEditor.getDocument().addDocumentListener(new TextEditorListener(textEditor));
    }

    private void setupCaretChangeEvent(TextEditor textEditor) {
        textEditor.getViewModel()
                .getStatusBar()
                .setCharacterPosition(
                        String.format(
                                "%d:%d", textEditor.getCaretLineNumber() + 1,
                                textEditor.getCaretOffsetFromLineStart() + 1
                        )
                );
    }

    private void setupTextChangeEvent(TextEditor textEditor) {
        Executors.newFixedThreadPool(3).submit(() -> this.onChangeEvent(textEditor));
    }

    private void onChangeEvent(TextEditor textEditor) {
        try {
            textEditor.setDirty(true);
            SourceCodeAnalysis.CompletionInfo completionInfo = shell.sourceCodeAnalysis()
                    .analyzeCompletion(textEditor.getText());
            while (completionInfo.source() != null && !completionInfo.source().isBlank()) {
                if (completionInfo.completeness() != SourceCodeAnalysis.Completeness.COMPLETE) break;
                List<SnippetEvent> snippetEvents = shell.eval(completionInfo.source());
                for (SnippetEvent snippetEvent : snippetEvents) {
                    switch (snippetEvent.snippet().kind()) {
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
                if (!completionInfo.remaining().isBlank())
                    completionInfo = shell.sourceCodeAnalysis().analyzeCompletion(completionInfo.remaining());
                else break;
            }
        } catch (IllegalStateException e) {
        }
        try {
            int lineStartOffsetOfCurrentLine = textEditor.getLineStartOffsetOfCurrentLine();
            String currentLine = textEditor.getText(lineStartOffsetOfCurrentLine,
                    textEditor.getCaretPosition() - lineStartOffsetOfCurrentLine);
            List<SourceCodeAnalysis.Suggestion> suggestions = shell.sourceCodeAnalysis()
                    .completionSuggestions(currentLine, currentLine.length(), new int[1]);

            this.provider.clear();
            List<Completion> completions = new ArrayList<>();
            for (SourceCodeAnalysis.Suggestion suggestion : suggestions) {
                completions.add(new BasicCompletion(this.provider, suggestion.continuation()));
            }
            this.provider.addCompletions(completions);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public Path setupTreeMouseListener(FileTree fileTree, MouseEvent event) {
        int selRow = fileTree.getRowForLocation(event.getX(), event.getY());
        TreePath selPath = fileTree.getPathForLocation(event.getX(), event.getY());
        if (selRow != -1 && selPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
            fileTree.setSelectionPath(selPath);
            JPopupMenu contextMenu = new JPopupMenu();
            JMenuItem refreshMenuItem = new JMenuItem(this.resourceBundle.getString("refresh.tree"));
            refreshMenuItem.addActionListener(e -> fileTree.refreshTree());
            contextMenu.add(refreshMenuItem);
            if (node.isLeaf()) {
                Path path = Path.of(String.valueOf(node));
                if (SwingUtilities.isRightMouseButton(event)) {
                    JMenuItem renameMenuItem = new JMenuItem(this.resourceBundle.getString("rename.file2"));
                    renameMenuItem.addActionListener(e -> this.renameFile(fileTree, path));
                    contextMenu.add(renameMenuItem);
                    JMenuItem deleteMenuItem = new JMenuItem(this.resourceBundle.getString("delete.file2"));
                    deleteMenuItem.addActionListener(e -> this.deleteFile(fileTree, path));
                    contextMenu.add(deleteMenuItem);
                    JMenuItem openFileLocationMenuItem = new JMenuItem(this.resourceBundle.getString("open.file.location"));
                    openFileLocationMenuItem.addActionListener(e -> this.openFileLocation(path));
                    contextMenu.add(openFileLocationMenuItem);
                    contextMenu.show(fileTree, event.getX(), event.getY());
                } else if (event.getClickCount() == 2) return path;
            } else if (SwingUtilities.isRightMouseButton(event)) {
                JMenuItem openFileLocationMenuItem = new JMenuItem(this.resourceBundle.getString("open.file.location"));
                openFileLocationMenuItem.addActionListener(e -> this.openFileLocation(Path.of(String.valueOf(node))));
                contextMenu.add(openFileLocationMenuItem);
                contextMenu.show(fileTree, event.getX(), event.getY());
            }
        }
        return null;
    }

    private void renameFile(FileTree fileTree, Path path) {
        Object newName = JOptionPane.showInputDialog(
                JOptionPane.getFrameForComponent(fileTree),
                String.format(this.resourceBundle.getString("rename.s"), path.getFileName()),
                this.resourceBundle.getString("rename.file"),
                JOptionPane.QUESTION_MESSAGE,
                UIManager.getIcon("OptionPane.questionIcon"),
                null,
                path.getFileName()
        );
        String s = String.valueOf(newName);
        try {
            if (newName != null && !s.isBlank()) {
                Files.move(path, path.getParent().resolve(s));
                fileTree.refreshTree();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteFile(FileTree fileTree, Path path) {
        try {
            if (JOptionPane.showConfirmDialog(
                    JOptionPane.getFrameForComponent(fileTree),
                    String.format(this.resourceBundle.getString("delete.file.s"), path.getFileName()),
                    this.resourceBundle.getString("delete.file"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                Files.delete(path);
                fileTree.refreshTree();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openFileLocation(Path path) {
        try {
            if (!Files.isDirectory(path))
                Desktop.getDesktop().open(path.getParent().toFile());
            else Desktop.getDesktop().open(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run(ViewModel viewModel, AbstractButton[] abstractButtons) {
        for (AbstractButton abstractButton : abstractButtons)
            abstractButton.setEnabled(false);
        String code = viewModel.getTextEditor().getText();
        JTextPane resultView = viewModel.getResultView();
        resultView.setText("");

        JProgressBar progressBar = viewModel.getStatusBar().getProgressBar();
        progressBar.setIndeterminate(true);
        viewModel.getStatusBar().setStatusLabel(this.resourceBundle.getString("running"));
        MessageConsole stderrConsole = new MessageConsole(resultView);
        MessageConsole stdoutConsole = new MessageConsole(resultView);

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try (ConsoleOutputStream consoleOutputStream = stdoutConsole.redirectOut(Color.black, null);
                     ConsoleOutputStream consoleErrorStream = stderrConsole.redirectErr(new Color(0xB22222), null);
                     ConsoleInputStream consoleInputStream = new ConsoleInputStream(viewModel.getStatusBar());
                     PrintStream out = new PrintStream(consoleOutputStream, true);
                     PrintStream errStream = new PrintStream(consoleErrorStream, true);
                     JShell shell = JShell.builder().out(out).err(errStream).in(consoleInputStream).build()) {

                    EspressoPadController.this.addArtifactsAndImports(shell);
                    SourceCodeAnalysis.CompletionInfo completion = shell.sourceCodeAnalysis().analyzeCompletion(code);
                    List<SnippetEvent> l = shell.eval(EspressoPadController.this.handler.parseImportXml()
                            .stream()
                            .map(imports -> String.format("import %s;", imports))
                            .collect(Collectors.joining()));

                    while (!completion.source().isBlank()) {
                        List<SnippetEvent> snippets = shell.eval(completion.source());

                        for (var snippet : snippets) {
                            // Check the status of the evaluation
                            String src = snippet.snippet().source().trim();
                            switch (snippet.status()) {
                                case VALID:
                                    EspressoPadController.this.logger.debug(src);
                                    break;
                                case REJECTED: //Compile time errors
                                    List<String> errors = shell.diagnostics(snippet.snippet())
                                            .map(x -> String.format("\n\"%s\" -> %s\n", src,
                                                    x.getMessage(Locale.ENGLISH)))
                                            .collect(Collectors.toList());
                                    EspressoPadController.this.logger.error(
                                            EspressoPadController.this.resourceBundle.getString(
                                                    "code.evaluation.failed.diagnostic.info"
                                            ), errors
                                    );
                                    errStream.println(errors);
                            }
                            //Runtime errors
                            if (snippet.exception() != null) {
                                EspressoPadController.this.logger.error(
                                        EspressoPadController.this.resourceBundle.getString("code.evaluation.failed.at"),
                                        src
                                );
                                errStream.printf(
                                        EspressoPadController.this.resourceBundle.getString(
                                                "code.evaluation.failed.at.s.diagnostic.info"
                                        ),
                                        src
                                );
                                snippet.exception().printStackTrace(errStream);
                                EspressoPadController.this.logger.error(
                                        EspressoPadController.this.resourceBundle.getString("evaluation.error"),
                                        snippet.exception()
                                );
                                try {
                                    throw snippet.exception();
                                } catch (JShellException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        if (!completion.remaining().isBlank())
                            completion = shell.sourceCodeAnalysis().analyzeCompletion(completion.remaining());
                        else break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    progressBar.setValue(progressBar.getMinimum());
                    progressBar.setIndeterminate(false);
                    viewModel.getStatusBar().setStatusLabel(EspressoPadController.this.resourceBundle.getString("ready"));
                    for (AbstractButton abstractButton : abstractButtons)
                        abstractButton.setEnabled(true);
                }
            }
        });
    }

    public void close() {
        shell.close();
    }

    private class TextEditorListener implements DocumentListener {
        private final TextEditor textEditor;

        TextEditorListener(TextEditor textEditor) {
            this.textEditor = textEditor;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            //setupTextChangeEvent(this.textEditor);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            //setupTextChangeEvent(this.textEditor);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            EspressoPadController.this.setupTextChangeEvent(this.textEditor);
        }
    }
}
