package com.github.espressopad;

import com.github.abrarsyed.jastyle.ASFormatter;
import com.github.abrarsyed.jastyle.constants.EnumFormatStyle;
import com.github.abrarsyed.jastyle.constants.SourceMode;
import com.github.espressopad.artifacts.ArtifactManager;
import com.github.espressopad.editor.TextEditor;
import com.github.espressopad.editor.TextEditorAutoComplete;
import com.github.espressopad.io.ConsoleInputStream;
import com.github.espressopad.io.ConsoleOutputStream;
import com.github.espressopad.xml.XmlHandler;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
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
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.swing.filechooser.FileSystemView;
import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EspressoPadController implements Initializable {
    @FXML
    private VBox mainBox;
    @FXML
    private TextArea output;
    @FXML
    private Button run;
    @FXML
    private Button clear;
    @FXML
    private TabPane tabPane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private TreeView<File> treeView;
    @FXML
    private WebView documentationView;
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
    private Button prevMatch;
    @FXML
    private Button nextMatch;
    @FXML
    private ToggleButton matchWord;
    @FXML
    private ToggleButton matchRegex;
    @FXML
    private TextField replacementText;
    @FXML
    private Button replaceOne;
    @FXML
    private Button replaceAll;

    private final XmlHandler handler = new XmlHandler();
    private final List<TextEditor> editors = new ArrayList<>();
    private final List<TextEditorAutoComplete> tacs = new ArrayList<>();
    private TextEditor editor;
    private final Map<TextEditor, File> savedOpenFiles = new LinkedHashMap<>();
    private TextEditorAutoComplete tac;
    private ConsoleOutputStream out;
    private PrintStream printStream;
    private ConsoleInputStream in;
    private JShell shell;
    private File[] shelfChildren;
    private File currentFile = null;
    private int currentSelectionIndex = 0;

    public WebView getDocumentationView() {
        return this.documentationView;
    }

    public List<TextEditor> getEditors() {
        return this.editors;
    }

    public Map<TextEditor, File> getSavedOpenFiles() {
        return this.savedOpenFiles;
    }

    public List<TextEditorAutoComplete> getAutocompletes() {
        return this.tacs;
    }

    private TextEditor getCurrentTextEditor() {
        return this.editors.get(this.tabPane.getSelectionModel().getSelectedIndex() + 1);
    }

    public JShell getShell() {
        return shell;
    }

    @Override
    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Tab tab = this.newTabButton(this.tabPane);
        this.splitPane.setDividerPosition(0, .3);
        this.splitPane.setPrefHeight(this.mainBox.getPrefHeight());
        this.tabPane.getTabs().add(tab);
        this.editor = new TextEditor(this.tabPane.getTabs().get(this.tabPane.getTabs().indexOf(tab) - 1));
        this.handler.writeImportXml(List.of("java.util.stream.*", "java.util.*", "java.io.*"));
        this.tac = new TextEditorAutoComplete(this.editor);
        this.tac.setController(this);
        this.tacs.add(this.tac);
        this.editors.add(this.editor);
        this.out = new ConsoleOutputStream(this.output);
        this.printStream = new PrintStream(this.out);
        this.in = new ConsoleInputStream();
        this.shell = JShell.builder().out(this.printStream).err(this.printStream).in(this.in).build();

        this.findReplaceBox.managedProperty().bind(this.findReplaceBox.visibleProperty());
        //TODO: Change this to be more cross platform
        this.output.setFont(Font.font("Consolas", 14d));

        if (this.handler.getArtifactFile().exists()) {
            for (String s : this.handler.parseArtifactXml())
                this.shell.addToClasspath(s);
        }

        this.run.setOnAction(event -> runCode());
        this.clear.setOnAction(event -> output.clear());

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
        this.initFindReplace();

        try {
            File shelf = Path.of(System.getProperty("user.dir"), "shelf").toFile();
            this.shelfChildren = shelf.listFiles();
            if (shelf.exists() && this.shelfChildren != null && this.shelfChildren.length > 0) {
                for (int i = 0; i < this.shelfChildren.length; i++) {
                    File shelfFile = this.shelfChildren[i];
                    tab = new Tab(String.format("*Shelved %d", i + 1));
                    this.tabPane.getTabs().add(this.tabPane.getTabs().size() - 1, tab);
                    TextEditor textEditor = new TextEditor(tab);
                    textEditor.getCodeArea().replaceText(Files.readString(shelfFile.toPath()));
                    TextEditorAutoComplete autoComplete = new TextEditorAutoComplete(textEditor, shelfFile.getName());
                    autoComplete.setController(this);
                    this.tacs.add(autoComplete);
                    this.editors.add(textEditor);
                    this.setTabEvents(textEditor, autoComplete);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private static String getFileExtension(String fileName) {
        String extension = "";

        int i = fileName.lastIndexOf('.');
        int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        if (i > p)
            extension = fileName.substring(i + 1);
        return extension;
    }

    static boolean compareFilesByLine(Path path1, Path path2) {
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
        this.printStream.close();
        this.shell.close();
    }

    public void setupStageListeners(Stage stage) {
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
                output.setMaxHeight(outputPane.getMaxHeight());
                output.setPrefHeight(.3 * screenBounds.getHeight());
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
        stage.setOnShown(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                PauseTransition transition = new PauseTransition(Duration.seconds(.5));
                transition.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        getCurrentTextEditor().getCodeArea().requestFocus();
                    }
                });
                transition.play();
            }
        });

        this.splitPane.setPrefHeight(stage.getHeight());
        this.tabParent.setPrefHeight(.75 * screenBounds.getHeight());
        this.output.setPrefHeight(.3 * screenBounds.getHeight());
        for (TextEditor textEditor : this.editors)
            textEditor.getCodeArea().setPrefHeight(.9 * this.tabParent.getPrefHeight());
    }

    private void closeAllPopups() {
        tacs.stream().filter(x -> x.getAutoCompletePopup() != null && x.getAutoCompletePopup().isShowing())
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
                    Tab tab = new Tab(String.format("New Tab %d", tabPane.getTabs().size()));
                    // Adding new tab before the "button" tab
                    tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
                    TextEditor textEditor = new TextEditor(tab);
                    TextEditorAutoComplete autoComplete = new TextEditorAutoComplete(textEditor, currentFile);
                    autoComplete.setController(EspressoPadController.this);
                    tacs.add(autoComplete);
                    editors.add(textEditor);
                    // Selecting the tab before the button, which is the newly created one
                    tabPane.getSelectionModel().select(tabPane.getTabs().size() - 2);
                    setTabEvents(textEditor, autoComplete);
                }
            }
        });
        return addTab;
    }

    private ObservableList<String> getMonospaceFonts() {
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
        return FXCollections.observableArrayList(mFamilyList);
    }

    private void runCode() {
        this.output.setText("");
        String code = this.getEditors()
                .get(tabPane.getTabs().indexOf(tabPane.getSelectionModel().getSelectedItem()) + 1)
                .getCodeArea().getText();
        SourceCodeAnalysis.CompletionInfo completion = shell.sourceCodeAnalysis().analyzeCompletion(code);
        this.shell.eval(handler.parseImportXml()
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
                        System.out.printf("Code evaluation successful at \"%s\" ", src);
                        if (snippet.value() != null && !snippet.value().isBlank()) {
                            System.out.printf("and returned value %s", snippet.value());
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
                        this.printStream.println(errors);
                        break;
                }
                //Runtime errors
                if (snippet.exception() != null) {
                    System.err.printf("Code evaluation failed at \"%s\".\n", src);
                    this.printStream.printf("Code evaluation failed at \"%s\"\nDiagnostic info:\n", src);
                    snippet.exception().printStackTrace(this.printStream);
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
                        } else if (Objects.equals(getFileExtension(item.getName()), "jepl") || item.isDirectory()) {
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
                    dialog.setTitle(((Stage) tabPane.getScene().getWindow()).getTitle());
                    dialog.setHeaderText(String.format("Rename %s to:", fileName));
                    dialog.setContentText("New file name:");
                    dialog.showAndWait().ifPresent(x -> {
                        Path path = Path.of(file.getParent(), String.format("%s.%s", x, getFileExtension(fileName)));
                        if (!Files.exists(path)) {
                            if (file.renameTo(path.toFile())) {
                                refreshFileTree();
                                getCurrentTextEditor().getTab().setText(String.valueOf(path.getFileName()));
                            } else new Alert(Alert.AlertType.ERROR, String.format("Renaming '%s' failed.", fileName))
                                    .showAndWait();
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
                    new Alert(Alert.AlertType.WARNING,
                            String.format("Are you sure you want to delete file '%s'?", file.getName()),
                            ButtonType.OK, ButtonType.CANCEL)
                            .showAndWait()
                            .filter(x -> x == ButtonType.OK)
                            .ifPresent(x -> {
                                if (file.delete())
                                    refreshFileTree();
                                else new Alert(Alert.AlertType.ERROR, String.format("Deleting '%s' failed.",
                                        file.getName())).showAndWait();
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
                    tacs.remove(autoComplete);
                    savedOpenFiles.remove(textEditor);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private FileChooser setupFileChooser(Path path) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JEPL Files", "*.jepl"));
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

    public void createNewFile(ActionEvent event) {
        this.tabPane.getSelectionModel().select(this.tabPane.getTabs().size() - 1);
    }

    public void openFile(ActionEvent event) throws IOException {
        File file = this.setupFileChooser(this.validateDefaultDirectory())
                .showOpenDialog(this.mainBox.getScene().getWindow());
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
            } else
                this.tabPane.getSelectionModel()
                        .select(new ArrayList<>(this.savedOpenFiles.values()).indexOf(file) + 1);
        }
    }

    public void closeFile(ActionEvent event) {
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

    public void saveFile(ActionEvent event) throws IOException {
        TextEditor textEditor = this.getCurrentTextEditor();
        if (this.savedOpenFiles.containsKey(textEditor)) {
            Files.writeString(this.savedOpenFiles.get(textEditor).toPath(), textEditor.getCodeArea().getText());
            textEditor.getTab().setText(textEditor.getTab().getText().replaceFirst("^\\*", ""));
        } else this.saveFileAs(null);
    }

    public void saveFileAs(ActionEvent event) throws IOException {
        File file = this.setupFileChooser(this.validateDefaultDirectory())
                .showSaveDialog(this.mainBox.getScene().getWindow());
        if (file != null) {
            Files.writeString(file.toPath(), this.getCurrentTextEditor().getCodeArea().getText());
            this.tabPane.getSelectionModel().getSelectedItem().setText(file.getName());
            this.savedOpenFiles.put(this.getCurrentTextEditor(), file);
            if (this.shelfChildren != null && this.shelfChildren.length > 0) {
                for (File x : this.shelfChildren) {
                    if (compareFilesByLine(x.toPath(), file.toPath()) && x.delete())
                        break;
                }
            }
            this.refreshFileTree();
        }
    }

    public void exit(ActionEvent event) {
        Platform.exit();
    }

    public void undo(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().undo();
    }

    public void redo(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().redo();
    }

    public void cut(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().cut();
    }

    public void copy(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().copy();
    }

    public void paste(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().paste();
    }

    public void selectAllText(ActionEvent event) {
        this.getCurrentTextEditor().getCodeArea().selectAll();
    }

    public void goToLine(ActionEvent event) {
        CodeArea codeArea = this.getCurrentTextEditor().getCodeArea();
        TwoDimensional.Position caretPos = codeArea.offsetToPosition(codeArea.getCaretPosition(),
                TwoDimensional.Bias.Forward);
        TextInputDialog dialog = new TextInputDialog(String.format("%d:%d", caretPos.getMajor() + 1,
                caretPos.getMinor()));
        dialog.setTitle(((Stage) tabPane.getScene().getWindow()).getTitle());
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

    public void duplicateLine(ActionEvent event) {
        TwoDimensional.Position caretPos = this.getCurrentTextEditor().getCodeArea()
                .offsetToPosition(this.getCurrentTextEditor().getCodeArea().getCaretPosition(),
                        TwoDimensional.Bias.Forward);
        String currentLine = this.getCurrentTextEditor().getCodeArea()
                .getText(caretPos.getMajor()).substring(0, caretPos.getMinor());
        this.getCurrentTextEditor().getCodeArea().insertText(caretPos.getMajor(), caretPos.getMinor(),
                String.format("\n%s", currentLine));
    }

    public void reformat(ActionEvent event) {
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

    public void runCode(ActionEvent event) {
        this.runCode();
    }

    public void manageArtifacts(ActionEvent event) {
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

    public void showAbout(ActionEvent event) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                new About(tabPane.getScene().getWindow()).start(new Stage());
            }
        });
    }

    public void toggleFindReplace(ActionEvent event) {
        this.findReplaceBox.setVisible(!this.findReplaceBox.isVisible());
        this.toggleFindReplaceMenuItem.setSelected(this.findReplaceBox.isVisible());
        if (this.findReplaceBox.isVisible())
            this.findText.requestFocus();
    }

    private List<Integer> indicesOf(String haystack, String needle,
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

    private void initFindReplace() {
        this.prevMatch.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                List<Integer> indices = indicesOf(getCurrentTextEditor().getCodeArea().getText(), findText.getText(),
                        !matchCase.isSelected(), matchRegex.isSelected(), matchWord.isSelected());
                currentSelectionIndex--;
                if (currentSelectionIndex < 0)
                    currentSelectionIndex = indices.size() - 1;
                getSearchResults(indices);
            }
        });
        this.nextMatch.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                List<Integer> indices = indicesOf(getCurrentTextEditor().getCodeArea().getText(), findText.getText(),
                        !matchCase.isSelected(), matchRegex.isSelected(), matchWord.isSelected());
                currentSelectionIndex++;
                if (currentSelectionIndex >= indices.size())
                    currentSelectionIndex = 0;
                getSearchResults(indices);
            }
        });
        this.replaceOne.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String findTxt = findText.getText();
                CodeArea area = getCurrentTextEditor().getCodeArea();

                List<Integer> indices = indicesOf(area.getText(), findTxt,
                        !matchCase.isSelected(), matchRegex.isSelected(), matchWord.isSelected());
                if (!indices.isEmpty()) {
                    int j = indices.get(currentSelectionIndex);
                    area.replaceText(j, j + findTxt.length(), replacementText.getText());
                }
            }
        });
        this.replaceAll.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String findTxt = findText.getText();
                CodeArea area = getCurrentTextEditor().getCodeArea();
                String txt = area.getText();

                List<Integer> indices = indicesOf(txt, findTxt,
                        !matchCase.isSelected(), matchRegex.isSelected(), matchWord.isSelected());
                if (!indices.isEmpty()) {
                    List<String> searches = indices.stream()
                            .map(i -> area.getText(i, i + findTxt.length()))
                            .collect(Collectors.toList());
                    for (String s : searches)
                        txt = txt.replace(s, replacementText.getText());
                    area.replaceText(txt);
                }
            }
        });
    }

    private void getSearchResults(List<Integer> indices) {
        CodeArea area = this.getCurrentTextEditor().getCodeArea();
        TwoDimensional.Position caretPos = area.offsetToPosition(area.getCaretPosition(),
                TwoDimensional.Bias.Forward);

        if (!indices.isEmpty()) {
            int j = indices.get(currentSelectionIndex);
            area.selectRange(j, j + this.findText.getText().length());
            findResults.setText(String.format("Result %d of %d", currentSelectionIndex + 1, indices.size()));
        } else {
            area.moveTo(caretPos.getMajor(), caretPos.getMinor());
            findResults.setText("No Results");
        }
    }
}
