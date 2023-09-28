package com.github.espressopad.controllers;

import com.github.abrarsyed.jastyle.ASFormatter;
import com.github.abrarsyed.jastyle.constants.EnumFormatStyle;
import com.github.abrarsyed.jastyle.constants.SourceMode;
import com.github.espressopad.editor.TextEditor;
import com.github.espressopad.editor.TextEditorAutoComplete;
import com.github.espressopad.io.ConsoleErrorStream;
import com.github.espressopad.io.ConsoleInputStream;
import com.github.espressopad.io.ConsoleOutputStream;
import com.github.espressopad.ui.About;
import com.github.espressopad.ui.ArtifactManager;
import com.github.espressopad.ui.EspressoPadMain;
import com.github.espressopad.ui.FilePathTreeItem;
import com.github.espressopad.xml.XmlHandler;
import com.jthemedetecor.OsThemeDetector;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.controlsfx.control.PopOver;
import org.controlsfx.dialog.ProgressDialog;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.kordamp.ikonli.javafx.FontIcon;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EspressoPadController implements Initializable {
    @FXML
    private VBox mainBox;
    @FXML
    private WebView output;
    @FXML
    private TabPane tabPane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private TreeView<File> treeView;
    @FXML
    private VBox tabParent;
    @FXML
    private VBox findReplaceBox;
    @FXML
    private VBox outputPane;
    @FXML
    private CheckMenuItem toggleFindReplaceMenuItem;
    @FXML
    private TextField findText;
    @FXML
    private ToggleButton matchCase;
    @FXML
    private Label findResults;
    @FXML
    private ToggleButton matchWord;
    @FXML
    private ToggleButton matchRegex;
    @FXML
    private TextField replacementText;
    @FXML
    private Label cursorPosition;

    private final WebView documentationView = new WebView();
    private final XmlHandler handler = new XmlHandler();
    private final ObservableList<TextEditor> editors = FXCollections.observableArrayList();
    private final List<TextEditorAutoComplete> autoCompleters = new ArrayList<>();
    private final Map<TextEditor, File> savedOpenFiles = new LinkedHashMap<>();
    private ConsoleOutputStream out;
    private ConsoleErrorStream err;
    private PrintStream outStream, errStream;
    private ConsoleInputStream in;
    private JShell shell;
    private File[] shelvedFiles;
    private File currentFile = null;
    private int currentSelectionIndex = 0;
    private String html;
    private Document document;
    private Path homePath;
    private Stage stage;
    private Path dumpFile;
    private final PopOver popOver = new PopOver(this.documentationView);

    public WebView getDocumentationView() {
        Optional<Bounds> pos = this.getCurrentTextEditor().getCodeArea().getCaretBounds();
        this.popOver.setPrefHeight(200d);
        this.documentationView.setPrefHeight(200d);
        this.popOver.show(this.stage, pos.get().getMaxX(), pos.get().getMaxY());
        return this.documentationView;
    }

    public List<TextEditor> getEditors() {
        return this.editors;
    }

    public Map<TextEditor, File> getSavedOpenFiles() {
        return this.savedOpenFiles;
    }

    public List<TextEditorAutoComplete> getAutocompletes() {
        return this.autoCompleters;
    }

    public JShell getShell() {
        return this.shell;
    }

    public WebView getOutput() {
        return this.output;
    }

    public Document getDocument() {
        return this.document;
    }

    private TextEditor getCurrentTextEditor() {
        return this.editors.get(this.tabPane.getSelectionModel().getSelectedIndex() + 1);
    }

    public Path getHomePath() {
        return this.homePath;
    }

    public Path getDumpFile() {
        return this.dumpFile;
    }

    public boolean isFindReplaceVisible() {
        return this.findReplaceBox.isVisible() && this.toggleFindReplaceMenuItem.isSelected();
    }

    @Override
    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Tab tab;
        this.homePath = new File(URLDecoder.decode(this.getClass().getProtectionDomain().getCodeSource()
                .getLocation().getPath(), StandardCharsets.UTF_8)).toPath().getParent();
        this.dumpFile = this.homePath.resolve("lib").resolve("dump.jar");
        tab = this.newTabButton(this.tabPane);
        this.splitPane.setDividerPosition(0, .3);
        this.splitPane.setPrefHeight(this.mainBox.getPrefHeight());
        this.tabPane.getTabs().add(tab);
        TextEditor editor = new TextEditor(this.tabPane.getTabs().get(this.tabPane.getTabs().indexOf(tab) - 1));
        this.handler.writeImportXml(List.of("java.util.stream.*", "java.util.*", "java.io.*"));
        TextEditorAutoComplete tac = new TextEditorAutoComplete(editor, this);
        this.autoCompleters.add(tac);
        this.editors.add(editor);
        this.editors.addListener(new ListChangeListener<TextEditor>() {
            @Override
            public void onChanged(Change<? extends TextEditor> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (TextEditor textEditor : c.getAddedSubList()) {
                            CodeArea codeArea = textEditor.getCodeArea();
                            codeArea.caretPositionProperty().addListener(new ChangeListener<Integer>() {
                                @Override
                                public void changed(ObservableValue<? extends Integer> observable, Integer oldValue,
                                                    Integer newValue) {
                                    textEditor.getHighlighter().highlightBracket(newValue);
                                    setCurrentPosition(codeArea);
                                }
                            });
                        }
                    }
                }
            }
        });
        try (InputStream stream = this.getClass().getResourceAsStream("default-webview.html")) {
            if (stream != null) {
                this.html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                this.output.getEngine().loadContent(this.html);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.out = new ConsoleOutputStream(this);
        this.outStream = new PrintStream(this.out);
        this.err = new ConsoleErrorStream(this);
        this.errStream = new PrintStream(this.err);
        this.in = new ConsoleInputStream();
        this.shell = JShell.builder().out(this.outStream).err(this.errStream).in(this.in).build();

        this.findReplaceBox.managedProperty().bind(this.findReplaceBox.visibleProperty());
        this.output.prefHeightProperty().bind(this.outputPane.heightProperty());

        if (this.handler.getArtifactFile().exists()) {
            for (String s : this.handler.parseArtifactXml())
                this.shell.addToClasspath(s);
        }

        //System.out.println(dumpFile);
        this.shell.addToClasspath(dumpFile.toString());

        this.treeView.addEventHandler(KeyEvent.KEY_RELEASED, event -> openFileFromTreeView());
        this.treeView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    openFileFromTreeView();
                }
            }
        });

        this.refreshFileTree();
        this.setupContextMenu();
        this.setCurrentPosition(editor.getCodeArea());

        try {
            File shelf = this.homePath.resolve("shelf").toFile();
            this.shelvedFiles = shelf.listFiles();
            if (shelf.exists() && this.shelvedFiles != null && this.shelvedFiles.length > 0) {
                for (int i = 0; i < this.shelvedFiles.length; i++) {
                    File shelfFile = this.shelvedFiles[i];
                    tab = new Tab(String.format("*Shelved %d", i + 1));
                    this.tabPane.getTabs().add(this.tabPane.getTabs().size() - 1, tab);
                    TextEditor textEditor = new TextEditor(tab);
                    textEditor.getCodeArea().replaceText(Files.readString(shelfFile.toPath()));
                    TextEditorAutoComplete autoComplete = new TextEditorAutoComplete(textEditor, shelfFile.getName(),
                            this);
                    this.autoCompleters.add(autoComplete);
                    this.editors.add(textEditor);
                    this.setTabEvents(textEditor, autoComplete);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (WebView webView : new WebView[]{this.output, this.documentationView}) {
            WebEngine engine = webView.getEngine();
            engine.getLoadWorker().stateProperty().addListener(new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Worker.State> obs, Worker.State oldState,
                                    Worker.State newState) {
                    if (newState == Worker.State.SUCCEEDED) {
                        if (OsThemeDetector.isSupported() && OsThemeDetector.getDetector().isDark()) {
                            org.w3c.dom.Document doc = engine.getDocument();
                            Element styleNode = doc.createElement("style");
                            org.w3c.dom.Text styleContent = doc.createTextNode(
                                    "body { background-color: #333; color: white; }" +
                                            "summary { background-color: #202020 !important; }" +
                                            "a { color: lightskyblue; } a:active, a:visited { color: mediumorchid; }" +
                                            ".err { color: #D76A66 !important;}");
                            styleNode.appendChild(styleContent);
                            doc.getDocumentElement().getElementsByTagName("head").item(0).appendChild(styleNode);
                        }
                    }
                }
            });
        }
        this.documentationView.getEngine().loadContent("<div/>");

        this.findText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (findText.getText().isBlank())
                    resetHighlighting();
                currentSelectionIndex = 0;
                getSearchResults();
            }
        });

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (info.getName().equals("Nimbus")) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
    }

    private int getTreeItemComparator(TreeItem<File> o1, TreeItem<File> o2) {
        if (!o1.isLeaf() && o2.isLeaf())
            return -1;
        else if (o1.isLeaf() && o2.isLeaf())
            return 0;
        else if (o1.isLeaf() && !o2.isLeaf())
            return 1;
        return -1;
    }

    private TwoDimensional.Position getCursorPosition(CodeArea codeArea) {
        return codeArea.offsetToPosition(codeArea.getCaretPosition(), TwoDimensional.Bias.Forward);
    }

    private void setCurrentPosition(CodeArea codeArea) {
        TwoDimensional.Position caretPos = this.getCursorPosition(codeArea);
        cursorPosition.setText(String.format("Line %d, Col %d",
                caretPos.getMajor() + 1, caretPos.getMinor() + 1));
    }

    private String getFileExtension(String fileName) {
        String extension = "";

        int i = fileName.lastIndexOf('.');
        int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        if (i > p)
            extension = fileName.substring(i + 1);
        return extension;
    }

    private boolean compareFilesByLine(Path path1, Path path2) {
        try (BufferedReader bf1 = Files.newBufferedReader(path1);
             BufferedReader bf2 = Files.newBufferedReader(path2)) {
            for (String line1, line2; (line1 = bf1.readLine()) != null; ) {
                line2 = bf2.readLine();
                if (!Objects.equals(line1, line2))
                    return false;
            }
            return bf2.readLine() == null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() throws IOException {
        this.in.close();
        this.out.close();
        this.err.close();
        this.outStream.close();
        this.errStream.close();
        this.shell.close();
    }

    public void setupStageListeners(Stage stage) {
        this.stage = stage;
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        stage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean hidden, Boolean shown) {
                if (hidden) closeAllPopups();
            }
        });
        stage.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                closeAllPopups();
            }
        });
        stage.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                splitPane.setDividerPosition(0, .2);
            }
        });
        stage.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                splitPane.setPrefHeight(newValue.doubleValue());
                outputPane.setMaxHeight(.5 * newValue.doubleValue());
                outputPane.setMinHeight(.2 * newValue.doubleValue());
            }
        });
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                for (var editor : getEditors()) {
                    if (savedOpenFiles.containsKey(editor))
                        saveFile(editor);
                }
            }
        });

        this.splitPane.setPrefHeight(stage.getHeight());
        this.tabParent.setPrefHeight(.75 * screenBounds.getHeight());
        for (TextEditor textEditor : this.editors)
            textEditor.getCodeArea().setPrefHeight(.9 * this.tabParent.getPrefHeight());
    }

    private void openFileFromTreeView() {
        TreeItem<File> treeItem = treeView.getSelectionModel().getSelectedItem();
        if (treeItem != null) {
            File file = treeItem.getValue();
            if (file != null) {
                try {
                    openFile(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void closeAllPopups() {
        this.autoCompleters.stream().filter(x -> x.getAutoCompletePopup() != null && x.getAutoCompletePopup().isShowing())
                .forEach(x -> x.getAutoCompletePopup().hide());
    }

    // Tab that acts as a button and adds a new tab and selects it
    private Tab newTabButton(TabPane tabPane) {
        Tab addTab = new Tab(); // You can replace the text with an icon
        addTab.setGraphic(new FontIcon("fas-plus"));
        addTab.setClosable(false);

        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
                if (newTab == addTab) {
                    Tab tab = new Tab("New Tab");
                    // Adding new tab before the "button" tab
                    tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
                    TextEditor textEditor = new TextEditor(tab);
                    TextEditorAutoComplete autoComplete = new TextEditorAutoComplete(textEditor, currentFile,
                            EspressoPadController.this);
                    autoCompleters.add(autoComplete);
                    editors.add(textEditor);

                    // Selecting the tab before the button, which is the newly created one
                    tabPane.getSelectionModel().select(tabPane.getTabs().size() - 2);
                    setTabEvents(textEditor, autoComplete);

                    PauseTransition transition = new PauseTransition(Duration.seconds(.1));
                    transition.setOnFinished(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            getCurrentTextEditor().getCodeArea().requestFocus();
                        }
                    });
                    transition.play();
                }
            }
        });
        return addTab;
    }

    private List<String> getMonospaceFonts() {
        final Text th = new Text("1 l");
        final Text tk = new Text("MWX");

        List<String> fontFamilyList = Font.getFamilies();
        List<String> mFamilyList = new ArrayList<>();

        for (String fontFamilyName : fontFamilyList) {
            Font font = Font.font(fontFamilyName, FontWeight.NORMAL, FontPosture.REGULAR, 14.0d);
            th.setFont(font);
            tk.setFont(font);
            if (th.getLayoutBounds().getWidth() == tk.getLayoutBounds().getWidth())
                mFamilyList.add(fontFamilyName);
        }
        return mFamilyList;
    }

    private void runCode() {
        this.document = Jsoup.parse((String) this.output.getEngine().executeScript("document.documentElement.outerHTML"));
        Task<Void> runTask = new Task<Void>() {
            @Override
            protected Void call() {
                String code = getEditors()
                        .get(tabPane.getTabs().indexOf(tabPane.getSelectionModel().getSelectedItem()) + 1)
                        .getCodeArea().getText();
                SourceCodeAnalysis.CompletionInfo completion = shell.sourceCodeAnalysis().analyzeCompletion(code);
                List<SnippetEvent> l = shell.eval(handler.parseImportXml()
                        .stream()
                        .map(imports -> String.format("import %s;", imports))
                        .collect(Collectors.joining()));
                System.err.println(l.stream().map(x -> shell.diagnostics(x.snippet()).map(y -> y.getMessage(Locale.ENGLISH))
                        .collect(Collectors.toList())).collect(Collectors.toList()));
                while (!completion.source().isBlank()) {
                    List<SnippetEvent> snippets = shell.eval(completion.source());

                    for (var snippet : snippets) {
                        // Check the status of the evaluation
                        String src = snippet.snippet().source().trim();
                        switch (snippet.status()) {
                            case VALID:
                                System.out.printf("Code evaluation successful at \"%s\" ", src);
                                if (snippet.value() != null && !snippet.value().isBlank()) {
                                    System.out.print("and returned a value");
                                    //this.printStream.printf("\"%s\" ==> %s\n", src, snippet.value());
                                }
                                System.out.println();
                                break;
                            case REJECTED: //Compile time errors
                                List<String> errors = shell.diagnostics(snippet.snippet())
                                        .map(x -> String.format("\n\"%s\" -> %s\n", src,
                                                x.getMessage(Locale.ENGLISH)))
                                        .collect(Collectors.toList());
                                System.err.printf("Code evaluation failed.\nDiagnostic info:\n%s\n", errors);
                                errStream.println(errors);
                                break;
                        }
                        //Runtime errors
                        if (snippet.exception() != null) {
                            System.err.printf("Code evaluation failed at \"%s\".\n", src);
                            errStream.printf("Code evaluation failed at \"%s\"\nDiagnostic info:\n", src);
                            snippet.exception().printStackTrace(errStream);
                            snippet.exception().printStackTrace(System.err);
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
                return null;
            }
        };
        ProgressDialog progressDialog = new ProgressDialog(runTask);
        Task<Void> progressTask = new Task<Void>() {
            @Override
            protected Void call() {
                progressDialog.setContentText("Running...");
                progressDialog.setTitle("Running");
                progressDialog.setHeaderText("Please wait");
                EspressoPadMain.setThemeResource(progressDialog.getDialogPane().getScene());
                progressDialog.setOnCloseRequest(new EventHandler<DialogEvent>() {
                    @Override
                    public void handle(DialogEvent event) {
                        shell.stop();
                    }
                });
                return null;
            }
        };
        progressTask.setOnRunning(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                progressDialog.showAndWait();
            }
        });

        org.jsoup.nodes.Element output = document.getElementById("output");
        if (output != null && !output.html().isBlank())
            output.html("");

        this.output.getEngine().executeScript("document.getElementById('output').innerHTML = '';");
        new Thread(runTask).start();
        new Thread(progressTask).start();
    }

    @FXML
    private void clearOutput(ActionEvent event) {
        this.output.getEngine().loadContent(html);
    }

    private void setupContextMenu() {
        this.treeView.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {
            @Override
            public TreeCell<File> call(TreeView<File> param) {
                TreeCell<File> cell = new TreeCell<>() {
                    @Override
                    protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        } else if (Objects.equals(getFileExtension(item.getName()), "jsh") || item.isDirectory()) {
                            setText(item.getName());
                            setGraphic(new FontIcon(item.isFile() ? "fas-file-code" : "fas-folder-open"));
                        }
                    }
                };
                ContextMenu contextMenu = createContextMenu(cell);
                cell.setContextMenu(contextMenu);
                return cell;
            }
        });
    }

    private ContextMenu createContextMenu(TreeCell<File> cell) {
        ContextMenu cm = new ContextMenu();
        MenuItem renameFile = new MenuItem("Rename File");
        renameFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                File file = cell.getItem();
                if (file != null) {
                    String fileName = file.getName();
                    String fileNameWOExt = fileName.replaceFirst("[.][^.]+$", "");

                    TextInputDialog dialog = new TextInputDialog(fileNameWOExt);
                    EspressoPadMain.setThemeResource(dialog.getDialogPane().getScene());
                    dialog.setTitle(stage.getTitle());
                    dialog.setHeaderText(String.format("Rename %s to:", fileName));
                    dialog.setContentText("New file name:");
                    dialog.showAndWait().ifPresent(x -> {
                        Path path = Path.of(file.getParent(), String.format("%s.%s", x, getFileExtension(fileName)));
                        if (!Files.exists(path)) {
                            if (file.renameTo(path.toFile())) {
                                refreshFileTree();
                                getCurrentTextEditor().getTab().setText(String.valueOf(path.getFileName()));
                            } else {
                                Alert alert = new Alert(Alert.AlertType.ERROR,
                                        String.format("Renaming '%s' failed.", fileName));
                                EspressoPadMain.setThemeResource(alert.getDialogPane().getScene());
                                alert.showAndWait();
                            }
                        }
                    });
                }
            }
        });

        MenuItem deleteFile = new MenuItem("Delete File");
        deleteFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                File file = cell.getItem();
                if (file != null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                            String.format("Are you sure you want to delete file '%s'?", file.getName()),
                            ButtonType.OK, ButtonType.CANCEL);
                    EspressoPadMain.setThemeResource(alert.getDialogPane().getScene());
                    alert.showAndWait()
                            .filter(x -> x == ButtonType.OK)
                            .ifPresent(x -> {
                                if (file.delete())
                                    refreshFileTree();
                                else {
                                    Alert dialog = new Alert(Alert.AlertType.ERROR, String.format("Deleting '%s' failed.",
                                            file.getName()));
                                    EspressoPadMain.setThemeResource(dialog.getDialogPane().getScene());
                                    dialog.showAndWait();
                                }
                            });
                }
            }
        });

        MenuItem refreshTree = new MenuItem("Refresh tree");
        refreshTree.setOnAction(event -> refreshFileTree());

        MenuItem openInFiles = new MenuItem("Open File Location");
        openInFiles.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                File file = cell.getItem();
                if (file != null) {
                    try {
                        if (file.isFile())
                            Desktop.getDesktop().open(new File(file.getParent()));
                        else Desktop.getDesktop().open(file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        this.treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<File>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<File>> observable, TreeItem<File> oldValue,
                                TreeItem<File> newValue) {
                if (newValue != null) {
                    if (newValue.getValue().isDirectory())
                        cm.getItems().removeAll(renameFile, deleteFile);
                    else if (newValue.getValue().isFile() && !cm.getItems().contains(renameFile) &&
                            !cm.getItems().contains(deleteFile))
                        cm.getItems().addAll(0, Arrays.asList(renameFile, deleteFile));
                }
            }
        });

        cm.getItems().addAll(renameFile, deleteFile, refreshTree, openInFiles);
        return cm;
    }

    private void refreshFileTree() {
        this.treeView.setRoot(new FilePathTreeItem(this.validateDefaultDirectory().toFile()));
        this.treeView.getRoot().setExpanded(true);
        this.iterateTree(null, this.treeView.getRoot());
        this.treeView.getRoot()
                .getChildren()
                .setAll(this.treeView.getRoot().getChildren().sorted(this::getTreeItemComparator));
    }

    private Path validateDefaultDirectory() {
        Path path = Path.of(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "EspressoPad");
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return path;
    }

    private void setTabEvents(TextEditor textEditor, TextEditorAutoComplete autoComplete) {
        textEditor.getTab().setOnCloseRequest(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                try {
                    if (tabPane.getTabs().size() == 2) {
                        event.consume();
                        return;
                    }
                    if (savedOpenFiles.containsKey(textEditor))
                        saveFile((ActionEvent) null);

                    textEditor.getSubscriber().unsubscribe();
                    textEditor.stop();
                    editors.remove(textEditor);
                    autoCompleters.remove(autoComplete);
                    savedOpenFiles.remove(textEditor);

                    for (var editor : editors)
                        editor.getCodeArea().setPrefHeight(stage.getHeight());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private FileChooser setupFileChooser(Path path) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                String.format("%s Files", this.stage.getTitle()), "*.jsh"));
        chooser.setInitialDirectory(path.toFile());
        return chooser;
    }

    private void createNewFile(File file) {
        this.currentFile = file;
        this.createNewFile((ActionEvent) null);
        if (this.getCurrentTextEditor().getTab().getText()
                .replaceFirst("^\\*", "")
                .equals(file.getName()))
            this.currentFile = null;
    }

    @FXML
    private void createNewFile(ActionEvent event) {
        this.tabPane.getSelectionModel().select(this.tabPane.getTabs().size() - 1);
    }

    @FXML
    private void openFile(ActionEvent event) throws IOException {
        File file = this.setupFileChooser(this.validateDefaultDirectory())
                .showOpenDialog(this.stage);
        this.openFile(file);
    }

    private void openFile(File file) throws IOException {
        if (file != null) {
            if (!this.savedOpenFiles.containsValue(file)) {
                this.createNewFile(file);
                TextEditor textEditor = this.getCurrentTextEditor();
                textEditor.getCodeArea().replaceText(Files.readString(file.toPath()));
                this.tabPane.getSelectionModel().getSelectedItem().setText(file.getName());
                this.savedOpenFiles.put(textEditor, file);
            } else this.savedOpenFiles.entrySet()
                    .stream()
                    .filter(x -> x.getValue() == file)
                    .findFirst()
                    .ifPresent(x -> tabPane.getSelectionModel().select(editors.indexOf(x.getKey()) - 1));
        }
    }

    @FXML
    private void closeFile(ActionEvent event) {
        Tab currentTab = this.tabPane.getSelectionModel().getSelectedItem();
        Event.fireEvent(currentTab, new Event(currentTab, currentTab, Tab.TAB_CLOSE_REQUEST_EVENT));
        Event.fireEvent(currentTab, new Event(currentTab, currentTab, Tab.CLOSED_EVENT));
        this.tabPane.getTabs().remove(currentTab);
        this.savedOpenFiles.remove(this.getCurrentTextEditor());
    }

    private void saveFile(TextEditor editor) {
        try {
            if (this.savedOpenFiles.containsKey(editor)) {
                Files.writeString(this.savedOpenFiles.get(editor).toPath(), editor.getCodeArea().getText());
                editor.getTab().setText(editor.getTab().getText().replaceFirst("^\\*", ""));
            } else this.saveFileAs(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void saveFile(ActionEvent event) throws IOException {
        TextEditor textEditor = this.getCurrentTextEditor();
        if (this.savedOpenFiles.containsKey(textEditor)) {
            Files.writeString(this.savedOpenFiles.get(textEditor).toPath(), textEditor.getCodeArea().getText());
            textEditor.getTab().setText(textEditor.getTab().getText().replaceFirst("^\\*", ""));
        } else this.saveFileAs(null);
    }

    @FXML
    private void saveFileAs(ActionEvent event) throws IOException {
        File file = this.setupFileChooser(this.validateDefaultDirectory())
                .showSaveDialog(this.stage);
        if (file != null) {
            Files.writeString(file.toPath(), this.getCurrentTextEditor().getCodeArea().getText());
            this.tabPane.getSelectionModel().getSelectedItem().setText(file.getName());
            this.savedOpenFiles.put(this.getCurrentTextEditor(), file);
            File shelf = this.homePath.resolve("shelf").toFile();
            if (shelf.exists()) {
                this.shelvedFiles = shelf.listFiles();
                if (this.shelvedFiles != null && this.shelvedFiles.length > 0) {
                    for (File f : this.shelvedFiles) {
                        if (this.compareFilesByLine(f.toPath(), file.toPath()) && f.delete())
                            break;
                    }
                }
                this.refreshFileTree();
                this.iterateTree(file, this.treeView.getRoot());
            }
        }
    }

    private void iterateTree(File file, TreeItem<File> root) {
        for (TreeItem<File> child : root.getChildren()) {
            if (child.getChildren().isEmpty() && file != null) {
                if (child.getValue().getPath().equals(file.getPath())) {
                    this.treeView.getSelectionModel().select(child);
                    child.getParent().setExpanded(true);
                }
            } else {
                child.getChildren().setAll(child.getChildren().sorted(this::getTreeItemComparator));
                this.iterateTree(file, child);
            }
        }
    }

    @FXML
    private void exit(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void undo(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().undo();
    }

    @FXML
    private void redo(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().redo();
    }

    @FXML
    private void cut(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().cut();
    }

    @FXML
    private void copy(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().copy();
    }

    @FXML
    private void paste(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().paste();
    }

    @FXML
    private void selectAllText(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().selectAll();
    }

    @FXML
    private void goToLine(ActionEvent event) {
        CodeArea codeArea = this.getCurrentTextEditor().getCodeArea();
        TwoDimensional.Position caretPos = this.getCursorPosition(codeArea);
        TextInputDialog dialog = new TextInputDialog(String.format("%d:%d", caretPos.getMajor() + 1,
                caretPos.getMinor()));
        EspressoPadMain.setThemeResource(dialog.getDialogPane().getScene());
        dialog.setTitle(this.stage.getTitle());
        dialog.setHeaderText("Go to line:");
        dialog.setContentText("[Line] [:column]:");
        dialog.showAndWait().ifPresent(x -> {
            if (x.matches("^\\d+:\\d+$")) {
                String[] goTo = x.split(":");

                int row = Integer.parseInt(goTo[0]) - 1, col = Integer.parseInt(goTo[1]);
                if (row < 0)
                    row = 0;
                if (row > codeArea.getParagraphs().size())
                    row = codeArea.getParagraphs().size() - 1;
                int longestCol = codeArea.getText(row).length();
                if (col < 0)
                    col = 0;
                if (col > longestCol)
                    col = longestCol;
                codeArea.moveTo(row, col);
            }
        });
    }

    @FXML
    private void duplicateLine(ActionEvent event) {
        TwoDimensional.Position caretPos = this.getCursorPosition(this.getCurrentTextEditor().getCodeArea());
        String currentLine = this.getCurrentTextEditor().getCodeArea()
                .getText(caretPos.getMajor()).substring(0, caretPos.getMinor());
        this.getCurrentTextEditor().getCodeArea().insertText(caretPos.getMajor(), caretPos.getMinor(),
                String.format("\n%s", currentLine));
    }

    @FXML
    private void reformat(ActionEvent event) {
        ASFormatter formatter = new ASFormatter();
        formatter.setSourceStyle(SourceMode.JAVA);
        formatter.setFormattingStyle(EnumFormatStyle.JAVA);
        formatter.setSwitchIndent(true);
        formatter.setCaseIndent(true);
        formatter.setTabSpaceConversionMode(true);
        formatter.setLabelIndent(true);

        try (Reader reader = new BufferedReader(new StringReader(
                this.getCurrentTextEditor().getCodeArea().getText()))) {
            try (Writer writer = new StringWriter()) {
                formatter.format(reader, writer);
                this.getCurrentTextEditor().getCodeArea().replaceText(writer.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void runCode(ActionEvent event) {
        this.runCode();
    }

    @FXML
    private void manageArtifacts(ActionEvent event) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new ArtifactManager(EspressoPadController.this).start(new Stage());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @FXML
    private void showAbout(ActionEvent event) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                new About(stage).start(new Stage());
            }
        });
    }

    @FXML
    private void toggleFindReplace(ActionEvent event) {
        this.findReplaceBox.setVisible(!this.findReplaceBox.isVisible());
        this.toggleFindReplaceMenuItem.setSelected(this.findReplaceBox.isVisible());
        if (this.findReplaceBox.isVisible()) {
            String selection = this.getCurrentTextEditor().getCodeArea().getSelectedText();
            if (!selection.isBlank())
                this.findText.setText(selection);
            this.findText.requestFocus();
            this.currentSelectionIndex = 0;
            this.getSearchResults();
        } else this.resetHighlighting();
    }

    public List<Integer> indicesOf(String haystack, String needle,
                                   boolean ignoreCase, boolean matchRegex, boolean matchWord) {
        List<Integer> matches = new ArrayList<>();
        if (needle == null || needle.isBlank()) return matches;

        int index = haystack.indexOf(needle);

        if (ignoreCase && !matchRegex && !matchWord) {
            // Convert both the text and substring to lowercase for a case-insensitive search
            haystack = haystack.toLowerCase();
            needle = needle.toLowerCase();
            index = haystack.indexOf(needle);
        }

        if (matchRegex || matchWord) {
            Pattern pattern;
            if (matchWord) {
                if (ignoreCase)
                    pattern = Pattern.compile(String.format("\\b%s\\b", Pattern.quote(needle)), Pattern.CASE_INSENSITIVE);
                else pattern = Pattern.compile(String.format("\\b%s\\b", Pattern.quote(needle)));
            } else {
                if (ignoreCase)
                    pattern = Pattern.compile(needle, Pattern.CASE_INSENSITIVE);
                else pattern = Pattern.compile(needle);
            }

            Matcher matcher = pattern.matcher(haystack);
            while (matcher.find()) {
                index = matcher.start();
                matches.add(index);
            }
        } else while (index >= 0) {
            matches.add(index);
            index = haystack.indexOf(needle, index + 1);
        }
        return matches;
    }

    @FXML
    private void getPreviousMatch(ActionEvent event) {
        List<Integer> indices = this.indicesOf(this.getCurrentTextEditor().getCodeArea().getText(), this.findText.getText(),
                !this.matchCase.isSelected(), this.matchRegex.isSelected(), this.matchWord.isSelected());
        this.currentSelectionIndex--;
        if (this.currentSelectionIndex < 0)
            this.currentSelectionIndex = indices.size() - 1;
        this.getSearchResults(indices);
    }

    @FXML
    private void getNextMatch(ActionEvent event) {
        List<Integer> indices = this.indicesOf(this.getCurrentTextEditor().getCodeArea().getText(), this.findText.getText(),
                !this.matchCase.isSelected(), this.matchRegex.isSelected(), this.matchWord.isSelected());
        this.currentSelectionIndex++;
        if (this.currentSelectionIndex >= indices.size())
            this.currentSelectionIndex = 0;
        this.getSearchResults(indices);
    }

    @FXML
    private void replaceOneMatch(ActionEvent event) {
        String findTxt = this.findText.getText();
        CodeArea area = this.getCurrentTextEditor().getCodeArea();

        List<Integer> indices = this.indicesOf(area.getText(), findTxt,
                !this.matchCase.isSelected(), this.matchRegex.isSelected(), this.matchWord.isSelected());
        if (!indices.isEmpty()) {
            int j = indices.get(this.currentSelectionIndex);
            area.replaceText(j, j + findTxt.length(), this.replacementText.getText());
        }
    }

    @FXML
    private void replaceAllMatches(ActionEvent event) {
        String findTxt = this.findText.getText();
        CodeArea area = this.getCurrentTextEditor().getCodeArea();
        String txt = area.getText();

        List<Integer> indices = this.indicesOf(txt, findTxt,
                !this.matchCase.isSelected(), this.matchRegex.isSelected(), this.matchWord.isSelected());
        if (!indices.isEmpty()) {
            List<String> searches = indices.stream()
                    .map(i -> area.getText(i, i + findTxt.length()))
                    .collect(Collectors.toList());
            for (String s : searches)
                txt = txt.replace(s, this.replacementText.getText());
            area.replaceText(txt);
        }
    }

    public void getSearchResults() {
        this.getSearchResults(this.indicesOf(this.getCurrentTextEditor().getCodeArea().getText(), this.findText.getText(),
                !this.matchCase.isSelected(), this.matchRegex.isSelected(), this.matchWord.isSelected()));
    }

    private void getSearchResults(List<Integer> indices) {
        CodeArea area = this.getCurrentTextEditor().getCodeArea();
        TwoDimensional.Position caretPos = this.getCursorPosition(area);
        this.resetHighlighting();

        if (!indices.isEmpty()) {
            int j = indices.get(currentSelectionIndex);
            int len = this.findText.getText().length();
            for (int index : indices)
                area.setStyle(index, index + len, Collections.singletonList("findMatch"));
            //area.moveTo(j, NavigationActions.SelectionPolicy.CLEAR);
            area.selectRange(j, j + len);
            findResults.setText(String.format("Result %d of %d", currentSelectionIndex + 1, indices.size()));
        } else {
            area.moveTo(caretPos.getMajor(), caretPos.getMinor());
            findResults.setText("No Results");
        }
    }

    public void resetHighlighting() {
        this.getCurrentTextEditor().applyHighlighting(TextEditor.computeHighlighting(
                this.getCurrentTextEditor().getCodeArea().getText()));
    }
}
