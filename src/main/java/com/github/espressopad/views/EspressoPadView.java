package com.github.espressopad.views;

import bibliothek.gui.DockFrontend;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.DefaultDockable;
import bibliothek.gui.dock.SplitDockStation;
import bibliothek.gui.dock.event.DockFrontendAdapter;
import bibliothek.gui.dock.station.split.SplitDockProperty;
import bibliothek.gui.dock.util.Priority;
import bibliothek.gui.dock.util.color.ColorManager;
import com.github.espressopad.controller.EspressoPadController;
import com.github.espressopad.controller.TextEditorController;
import com.github.espressopad.models.SettingsModel;
import com.github.espressopad.models.ViewModel;
import com.github.espressopad.utils.SerializationUtilities;
import com.github.espressopad.utils.Utilities;
import com.github.espressopad.views.components.FileTree;
import com.github.espressopad.views.components.TextEditor;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EspressoPadView extends JPanel {
    private final EspressoPadController controller = new EspressoPadController();
    private static final AtomicInteger tabCounter = new AtomicInteger(1);
    private final JTabbedPane tabPane = new JTabbedPane();
    private final JToolBar toolBar = new JToolBar();
    private final JMenuBar menuBar = new JMenuBar();
    private JButton runBtn;
    private JMenuItem runMenuItem;
    private final List<ViewModel> viewModels = new ArrayList<>();
    private final TextEditorController editorController = new TextEditorController();
    private final SerializationUtilities handler = new SerializationUtilities();
    private final JFrame frame;
    private boolean ignore = false;
    private SettingsModel settings;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", Locale.getDefault());
    private FileTree fileTree;
    private final Color activeTabColor = new Color(0x7A8A99);

    public EspressoPadView(JFrame frame) {
        this.frame = frame;
        this.frame.addWindowListener(new WindowClosingListener());
        this.settings = this.handler.parseSettingsXml();
        try {
            if (this.settings != null) {
                String laf = this.settings.getLookAndFeel();
                UIManager.setLookAndFeel(laf);
                SwingUtilities.updateComponentTreeUI(this.tabPane);
            }
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException ioe) {
            throw new RuntimeException(ioe);
        }
        this.createTab(false);
        this.addTabButton();
        this.setupInterface();
        this.setupMiddleMouseListener();
        this.controller.addArtifactsAndImports(EspressoPadController.getShell());
    }

    private void setupInterface() {
        this.setupDocking();
        this.setupMenuBar();
        this.setupToolBar();
    }

    private void setupDocking() {
        DockFrontend frontend = new DockFrontend(this.frame);
        ColorManager colors = frontend.getController().getColors();
        colors.put(Priority.CLIENT, "title.active.left", this.activeTabColor);
        colors.put(Priority.CLIENT, "title.active.right", this.activeTabColor);
        SplitDockStation splitDockStation = new SplitDockStation();
        frontend.addRoot("root", splitDockStation);
        this.fileTree = new FileTree(Utilities.validateDefaultDirectory());
        this.fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Path file = EspressoPadView.this.controller.setupTreeMouseListener(EspressoPadView.this.fileTree, e);
                if (file != null) {
                    EspressoPadView.this.openFile(file);
                    EspressoPadView.this.closeAllDuplicateTabs();
                }
            }
        });
        DefaultDockable fileTreeDockable = Utilities.createDockable(
                new JScrollPane(this.fileTree),
                this.resourceBundle.getString("files")
        );
        fileTreeDockable.setTitleIcon(FontIcon.of(FontAwesomeRegular.FILE, 15));
        frontend.addDockable("fileTree", fileTreeDockable);
        frontend.setHideable(fileTreeDockable, true);
        frontend.addFrontendListener(new FrontendAdapter(fileTreeDockable, frontend));
        splitDockStation.drop(fileTreeDockable, new SplitDockProperty(0, 0, .25, 1));
        DefaultDockable tabPaneDockable = Utilities.createDockable(this.tabPane, this.resourceBundle.getString("workspace"));
        tabPaneDockable.setTitleIcon(FontIcon.of(FontAwesomeRegular.FILE, 11));
        frontend.addDockable("results", tabPaneDockable);
        frontend.setHideable(tabPaneDockable, false);
        splitDockStation.drop(tabPaneDockable, new SplitDockProperty(0.25, 0, .75, 1));
        this.setLayout(new BorderLayout());
        this.add(splitDockStation, BorderLayout.CENTER);
    }

    private void setupTextEditorAppearance(TextEditor textEditor) {
        try {
            this.settings = this.handler.parseSettingsXml();
            if (this.settings == null) return;
            String themeLocation = this.settings.getTheme();
            String font = this.settings.getFont();
            int fontSize = this.settings.getFontSize();
            boolean wordWrap = this.settings.isWordWrap();
            InputStream in = this.getClass().getResourceAsStream(themeLocation);
            Theme theme = Theme.load(in);
            theme.apply(textEditor);
            textEditor.setFont(new Font(font, Font.PLAIN, fontSize));
            textEditor.setLineWrap(wordWrap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupToolBar() {
        JButton newFileBtn = new JButton(FontIcon.of(FontAwesomeSolid.FILE, 15));
        newFileBtn.setToolTipText(this.resourceBundle.getString("new.file"));
        newFileBtn.addActionListener(event -> this.createTab(true));
        this.toolBar.add(newFileBtn);

        JButton openFileBtn = new JButton(FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 15));
        openFileBtn.setToolTipText(this.resourceBundle.getString("open.file"));
        openFileBtn.addActionListener(event -> this.openFile());
        this.toolBar.add(openFileBtn);

        JButton saveFileBtn = new JButton(FontIcon.of(FontAwesomeSolid.SAVE, 15));
        saveFileBtn.setToolTipText(this.resourceBundle.getString("save"));
        saveFileBtn.addActionListener(event -> this.saveFile());
        this.toolBar.add(saveFileBtn);

        this.toolBar.addSeparator();

        JButton undoFileBtn = new JButton(FontIcon.of(FontAwesomeSolid.UNDO, 15));
        undoFileBtn.setToolTipText(this.resourceBundle.getString("undo"));
        undoFileBtn.addActionListener(event -> this.editorController.undo(this.getCurrentTextEditor()));
        this.toolBar.add(undoFileBtn);

        JButton redoFileBtn = new JButton(FontIcon.of(FontAwesomeSolid.REDO, 15));
        redoFileBtn.setToolTipText(this.resourceBundle.getString("redo"));
        redoFileBtn.addActionListener(event -> this.editorController.redo(this.getCurrentTextEditor()));
        this.toolBar.add(redoFileBtn);

        this.toolBar.addSeparator();

        JButton cutFileBtn = new JButton(FontIcon.of(FontAwesomeSolid.CUT, 15));
        cutFileBtn.setToolTipText(this.resourceBundle.getString("cut"));
        cutFileBtn.addActionListener(event -> this.editorController.cut(this.getCurrentTextEditor()));
        this.toolBar.add(cutFileBtn);

        JButton copyFileBtn = new JButton(FontIcon.of(FontAwesomeSolid.COPY, 15));
        copyFileBtn.setToolTipText(this.resourceBundle.getString("copy"));
        copyFileBtn.addActionListener(event -> this.editorController.copy(this.getCurrentTextEditor()));
        this.toolBar.add(copyFileBtn);

        JButton pasteFileBtn = new JButton(FontIcon.of(FontAwesomeSolid.PASTE, 15));
        pasteFileBtn.setToolTipText(this.resourceBundle.getString("paste"));
        pasteFileBtn.addActionListener(event -> this.editorController.paste(this.getCurrentTextEditor()));
        this.toolBar.add(pasteFileBtn);

        this.toolBar.addSeparator();

        JButton findBtn = new JButton(FontIcon.of(FontAwesomeSolid.SEARCH, 15));
        findBtn.setToolTipText(this.resourceBundle.getString("find"));
        findBtn.addActionListener(event -> this.editorController.findAction(this.getCurrentView()));
        this.toolBar.add(findBtn);

        this.toolBar.addSeparator();

        this.runBtn = new JButton(FontIcon.of(FontAwesomeSolid.PLAY, 15));
        this.runBtn.setToolTipText(this.resourceBundle.getString("run"));
        this.runBtn.addActionListener(event ->
                this.controller.run(this.getCurrentView(), new AbstractButton[]{this.runBtn, this.runMenuItem}));
        this.toolBar.add(this.runBtn);

        this.toolBar.addSeparator();

        this.add(this.toolBar, BorderLayout.NORTH);
    }

    private void setupMenuBar() {
        JMenu fileMenu = new JMenu(this.resourceBundle.getString("file"));
        int ctrlDownMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenuItem newFileItem = new JMenuItem(this.resourceBundle.getString("new.file"));
        newFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrlDownMask));
        newFileItem.setMnemonic('N');
        newFileItem.addActionListener(event -> this.createTab(true));
        fileMenu.add(newFileItem);

        JMenuItem openFileItem = new JMenuItem(this.resourceBundle.getString("open.file"));
        openFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ctrlDownMask));
        openFileItem.setMnemonic('O');
        openFileItem.addActionListener(event -> this.openFile());
        fileMenu.add(openFileItem);

        fileMenu.add(new JSeparator());

        JMenuItem saveFileItem = new JMenuItem(this.resourceBundle.getString("save.file"));
        saveFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ctrlDownMask));
        saveFileItem.setMnemonic('S');
        saveFileItem.addActionListener(event -> this.saveFile());
        fileMenu.add(saveFileItem);

        JMenuItem saveAsFileItem = new JMenuItem(this.resourceBundle.getString("save.file.as"));
        saveAsFileItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, ctrlDownMask | InputEvent.SHIFT_DOWN_MASK)
        );
        saveAsFileItem.setMnemonic('A');
        saveAsFileItem.addActionListener(event -> this.saveFileAs());
        fileMenu.add(saveAsFileItem);

        fileMenu.add(new JSeparator());

        JMenuItem closeFileItem = new JMenuItem(this.resourceBundle.getString("close.file"));
        closeFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ctrlDownMask));
        closeFileItem.setMnemonic('C');
        closeFileItem.addActionListener(event -> this.removeCurrentTab());
        fileMenu.add(closeFileItem);

        fileMenu.add(new JSeparator());

        JMenuItem exitItem = new JMenuItem(this.resourceBundle.getString("exit"));
        exitItem.setMnemonic('x');
        exitItem.addActionListener(event -> this.exit());
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu(this.resourceBundle.getString("edit"));
        JMenuItem undoItem = new JMenuItem(this.resourceBundle.getString("undo"));
        undoItem.setMnemonic('u');
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrlDownMask));
        undoItem.addActionListener(event -> this.editorController.undo(this.getCurrentTextEditor()));
        editMenu.add(undoItem);

        JMenuItem redoItem = new JMenuItem(this.resourceBundle.getString("redo"));
        redoItem.setMnemonic('r');
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrlDownMask));
        redoItem.addActionListener(event -> this.editorController.redo(this.getCurrentTextEditor()));
        editMenu.add(redoItem);

        editMenu.add(new JSeparator());

        JMenuItem cutItem = new JMenuItem(this.resourceBundle.getString("cut"));
        cutItem.setMnemonic('t');
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ctrlDownMask));
        cutItem.addActionListener(event -> this.editorController.cut(this.getCurrentTextEditor()));
        editMenu.add(cutItem);

        JMenuItem copyItem = new JMenuItem(this.resourceBundle.getString("copy"));
        copyItem.setMnemonic('c');
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrlDownMask));
        copyItem.addActionListener(event -> this.editorController.copy(this.getCurrentTextEditor()));
        editMenu.add(copyItem);

        JMenuItem pasteItem = new JMenuItem(this.resourceBundle.getString("paste"));
        pasteItem.setMnemonic('p');
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ctrlDownMask));
        pasteItem.addActionListener(event -> this.editorController.paste(this.getCurrentTextEditor()));
        editMenu.add(pasteItem);

        editMenu.add(new JSeparator());

        JMenuItem findItem = new JMenuItem(this.resourceBundle.getString("find"));
        findItem.setMnemonic('f');
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrlDownMask));
        findItem.addActionListener(event -> this.editorController.findAction(this.getCurrentView()));
        editMenu.add(findItem);

        JMenuItem replaceItem = new JMenuItem(this.resourceBundle.getString("replace"));
        replaceItem.setMnemonic('e');
        replaceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ctrlDownMask));
        replaceItem.addActionListener(event -> this.editorController.replaceAction(this.getCurrentView()));
        editMenu.add(replaceItem);

        editMenu.add(new JSeparator());

        JMenuItem selectAllItem = new JMenuItem(this.resourceBundle.getString("select.all"));
        selectAllItem.setMnemonic('a');
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrlDownMask));
        selectAllItem.addActionListener(event -> this.editorController.selectAll(this.getCurrentTextEditor()));
        editMenu.add(selectAllItem);

        JMenuItem goToLineItem = new JMenuItem(this.resourceBundle.getString("go.to.line"));
        goToLineItem.setMnemonic('g');
        goToLineItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ctrlDownMask));
        goToLineItem.addActionListener(event -> this.editorController.setupGoToLine(this.getCurrentTextEditor()));
        editMenu.add(goToLineItem);

        JMenuItem duplicateSelectionItem = new JMenuItem(this.resourceBundle.getString("duplicate"));
        duplicateSelectionItem.setMnemonic('d');
        duplicateSelectionItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ctrlDownMask));
        duplicateSelectionItem.addActionListener(event ->
                this.editorController.duplicateSelectionAction(this.getCurrentTextEditor()));
        editMenu.add(duplicateSelectionItem);

        JMenuItem reformatSelectionItem = new JMenuItem(this.resourceBundle.getString("reformat"));
        reformatSelectionItem.setMnemonic('o');
        reformatSelectionItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_L, ctrlDownMask | InputEvent.ALT_DOWN_MASK)
        );
        reformatSelectionItem.addActionListener(event ->
                this.editorController.reformatSelectionAction(this.getCurrentTextEditor()));
        editMenu.add(reformatSelectionItem);

        JMenu runMenu = new JMenu(this.resourceBundle.getString("run"));
        this.runMenuItem = new JMenuItem(this.resourceBundle.getString("run"));
        this.runMenuItem.addActionListener(event ->
                this.controller.run(this.getCurrentView(), new AbstractButton[]{this.runMenuItem, this.runBtn}));
        runMenu.add(this.runMenuItem);

        JMenu toolsMenu = new JMenu(this.resourceBundle.getString("tools"));
        JMenuItem settingsMenuItem = new JMenuItem(this.resourceBundle.getString("settings"));
        settingsMenuItem.addActionListener(event -> {
                    this.settings = this.handler.parseSettingsXml();
                    new SettingsView(
                            this.viewModels.stream().map(ViewModel::getTextEditor).collect(Collectors.toList()),
                            this.settings
                    ).show();
                }
        );
        toolsMenu.add(settingsMenuItem);

        JMenu helpMenu = new JMenu(this.resourceBundle.getString("help"));
        JMenuItem aboutMenuItem = new JMenuItem(this.resourceBundle.getString("about"));
        aboutMenuItem.addActionListener(event -> new AboutView(this.frame).show());
        helpMenu.add(aboutMenuItem);

        this.menuBar.add(fileMenu);
        this.menuBar.add(editMenu);
        this.menuBar.add(runMenu);
        this.menuBar.add(toolsMenu);
        this.menuBar.add(helpMenu);
        this.frame.setJMenuBar(this.menuBar);
    }

    private ViewModel getCurrentView() {
        return this.viewModels.get(this.tabPane.getSelectedIndex());
    }

    private TextEditor getCurrentTextEditor() {
        return this.getCurrentView().getTextEditor();
    }

    private JPanel createTab(boolean prepend) {
        String title;
        if (this.tabPane.getTabCount() > 1)
            title = String.format(this.resourceBundle.getString("tab.d"), tabCounter.incrementAndGet());
        else title = this.resourceBundle.getString("tab1");
        ViewModel model = new ViewModel();
        model.setTitle(title);
        this.setupTab(model);

        JPanel tab = model.getTab();
        TextEditor textEditor = model.getTextEditor();
        if (!prepend)
            this.tabPane.addTab(title, tab);
        else this.tabPane.insertTab(title, null, tab, null, this.tabPane.getTabCount() - 1);

        this.setupClosableTabs(title);
        this.setupTextEditorAppearance(textEditor);
        this.controller.setupTextChangeListener(textEditor);
        this.viewModels.add(model);
        return tab;
    }

    private void setupTab(ViewModel model) {
        model.getTab().setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(new JScrollPane(model.getResultView()));
        panel.add(model.getStatusBar());
        DockFrontend frontend = new DockFrontend(this.frame);
        ColorManager colors = frontend.getController().getColors();
        colors.put(Priority.CLIENT, "title.active.left", this.activeTabColor);
        colors.put(Priority.CLIENT, "title.active.right", this.activeTabColor);
        SplitDockStation station = new SplitDockStation();
        frontend.addRoot("root", station);
        RTextScrollPane scrollPane = new RTextScrollPane(model.getTextEditor());
        model.getTextEditor().setScrollPane(scrollPane);
        DefaultDockable textDock = Utilities.createDockable(scrollPane, model.getTitle());
        frontend.addDockable("document", textDock);
        frontend.setHideable(textDock, false);
        station.drop(textDock, new SplitDockProperty(0, 0, 1, .6));
        DefaultDockable dockable = Utilities.createDockable(panel, this.resourceBundle.getString("results"));
        dockable.setTitleIcon(FontIcon.of(FontAwesomeSolid.GLASSES, 11));
        frontend.addDockable("results", dockable);
        frontend.setHideable(dockable, false);
        station.drop(dockable, new SplitDockProperty(0, .75, 1, .4));
        model.getTab().add(station, BorderLayout.CENTER);
        model.getTextEditor().requestFocusInWindow();
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(Utilities.validateDefaultDirectory().toFile());
        chooser.setFileFilter(new FileNameExtensionFilter(this.resourceBundle.getString("jsh.file"), "jsh"));
        if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
            this.openFile(chooser.getSelectedFile().toPath());
            this.closeAllDuplicateTabs();
        }
    }

    private JPanel openFile(Path file) {
        ViewModel model = new ViewModel();
        try {
            model.setBackingFile(file);
            model.setTitle(file.getFileName().toString());
            for (ViewModel viewModel : this.viewModels) {
                if (viewModel.getTitle().equals(model.getTitle()) &&
                        !file.equals(viewModel.getBackingFile())) {
                    model.setTitle(file.toString());
                    break;
                }
            }
            this.setupTab(model);
            JPanel tab = model.getTab();
            TextEditor textEditor = model.getTextEditor();
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                textEditor.setText(reader.lines().collect(Collectors.joining("\n")));
            }
            this.setupTextEditorAppearance(textEditor);
            this.tabPane.insertTab(model.getTitle(), null, tab, null, this.tabPane.getTabCount() - 1);
            this.tabPane.setSelectedComponent(tab);
            this.setupClosableTabs(model.getTitle());
            this.controller.setupTextChangeListener(textEditor);
            this.viewModels.add(model);
            return tab;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addTabButton() {
        this.tabPane.addTab(null, FontIcon.of(FontAwesomeSolid.PLUS, 12), new JPanel());
        this.tabPane.getModel().addChangeListener(this::tabStateChanged);
    }

    private void tabStateChanged(ChangeEvent e) {
        if (!this.ignore) {
            this.ignore = true;
            try {
                int selected = this.tabPane.getSelectedIndex();
                FontIcon icon = (FontIcon) this.tabPane.getIconAt(selected);
                if (icon != null && FontAwesomeSolid.PLUS.getCode() == icon.getIkon().getCode() &&
                        selected == this.tabPane.getTabCount() - 1) {
                    JPanel pane = this.createTab(false);
                    String tl = String.format(this.resourceBundle.getString("tab.d"), tabCounter.get());
                    this.tabPane.insertTab(tl, null, pane, null, this.tabPane.getTabCount() - 2);
                    this.tabPane.setSelectedComponent(pane);
                    this.setupClosableTabs(tl);
                }
            } finally {
                this.ignore = false;
            }
        }
    }

    private void saveFile() {
        try {
            ViewModel currentViewModel = this.getCurrentView();
            Document document = currentViewModel.getResultView().getDocument();
            String resultText = document.getText(0, document.getLength());
            Path savedFile = this.editorController.saveFile(currentViewModel);
            if (savedFile != null) {
                this.openFile(savedFile);
                this.tabPane.removeTabAt(this.viewModels.indexOf(currentViewModel));
                this.setupClosableTabs(savedFile.getFileName().toString());
                this.viewModels.remove(currentViewModel);
                this.fileTree.refreshTree();
                this.getCurrentView()
                        .getResultView()
                        .getDocument()
                        .insertString(0, resultText, new SimpleAttributeSet());
            }
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveFileAs() {
        ViewModel currentViewModel = this.getCurrentView();
        Path savedFile = this.editorController.saveFileAs(currentViewModel);
        if (savedFile != null) {
            this.openFile(savedFile);
            this.fileTree.refreshTree();
        }
    }

    private void setupClosableTabs(String title) {
        int index = this.tabPane.indexOfTab(title);
        JPanel pnlTab = new JPanel(new GridBagLayout());
        pnlTab.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        JButton btnClose = new JButton();
        btnClose.setIcon(FontIcon.of(FontAwesomeSolid.TIMES, 12));
        CompoundBorder border = BorderFactory.createCompoundBorder(BorderFactory
                .createEmptyBorder(), BorderFactory.createEmptyBorder(2, 2, 0, 2));
        btnClose.setBorder(border);
        btnClose.setContentAreaFilled(false);
        btnClose.setFocusable(false);
        btnClose.addActionListener(e -> this.removeTabByTitle(title));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 0, 1);

        pnlTab.add(lblTitle, gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        pnlTab.add(btnClose, gbc);

        this.tabPane.setTabComponentAt(index, pnlTab);
    }

    private void setupMiddleMouseListener() {
        this.tabPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (tabPane.getBoundsAt(tabPane.getSelectedIndex()).contains(e.getPoint()) && tabPane.getTabCount() > 2
                        && SwingUtilities.isMiddleMouseButton(e)) {
                    int index = tabPane.indexAtLocation(e.getX(), e.getY());
                    if (index != -1) {
                        String title = tabPane.getTitleAt(index);
                        tabPane.removeTabAt(index);
                        tabPane.setSelectedComponent(tabPane.getComponentAt(tabPane.getTabCount() - 2));
                        for (Iterator<ViewModel> iterator = viewModels.iterator(); iterator.hasNext(); ) {
                            ViewModel viewModel = iterator.next();
                            if (viewModel.getTitle().equals(title))
                                iterator.remove();
                        }
                    }
                }
            }
        });
    }

    private void removeCurrentTab() {
        if (this.tabPane.getTabCount() <= 2) return;
        this.viewModels.remove(this.tabPane.getSelectedIndex());
        this.tabPane.remove(this.tabPane.getSelectedComponent());
        this.tabPane.setSelectedComponent(this.tabPane.getComponentAt(this.tabPane.getTabCount() - 2));
    }

    private void removeTabByTitle(String title) {
        if (this.tabPane.getTabCount() <= 2) return;
        for (Iterator<ViewModel> iterator = this.viewModels.iterator(); iterator.hasNext(); ) {
            ViewModel viewModel = iterator.next();
            if (viewModel.getTitle().equals(title)) {
                iterator.remove();
                this.tabPane.remove(viewModel.getTab());
            }
        }
        this.tabPane.setSelectedComponent(this.tabPane.getComponentAt(this.tabPane.getTabCount() - 2));
    }

    private void closeAllDuplicateTabs() {
        String selectedTitle = this.tabPane.getTitleAt(this.tabPane.getSelectedIndex());
        Set<String> checker = new HashSet<>();
        for (int i = 0; i < this.tabPane.getTabCount(); i++) {
            if (!checker.add(this.tabPane.getTitleAt(i)))
                this.tabPane.removeTabAt(i);
        }
        checker.clear();
        for (Iterator<ViewModel> iterator = this.viewModels.iterator(); iterator.hasNext(); ) {
            ViewModel viewModel = iterator.next();
            Path backingFile = viewModel.getBackingFile();
            if (backingFile != null && !checker.add(backingFile.toString()))
                iterator.remove();
        }
        this.tabPane.setSelectedIndex(this.tabPane.indexOfTab(selectedTitle));
    }

    private boolean checkUnsaved() {
        List<ViewModel> unsavedViewModels = this.viewModels.stream()
                .filter(viewModel -> viewModel.getTextEditor().isDirty())
                .collect(Collectors.toList());

        try {
            if (!unsavedViewModels.isEmpty()) {
                List<String> unsavedFiles = new ArrayList<>();
                for (int i = 0; i < this.viewModels.size(); i++) {
                    ViewModel viewModel = this.viewModels.get(i);
                    Path backingFile = viewModel.getBackingFile();
                    if (viewModel.getTextEditor().isDirty()) {
                        if (backingFile != null)
                            unsavedFiles.add(viewModel.getBackingFile().toString());
                        else unsavedFiles.add(this.tabPane.getTitleAt(i));
                    }
                }
                switch (new SavePromptDialog(this.frame, unsavedFiles).getResult()) {
                    case JOptionPane.YES_OPTION:
                        for (ViewModel unsavedFile : unsavedViewModels) {
                            Path backingFile = unsavedFile.getBackingFile();
                            if (backingFile != null)
                                try (BufferedWriter writer = Files.newBufferedWriter(backingFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                                    writer.write(unsavedFile.getTextEditor().getText());
                                }
                            else this.editorController.saveFileAs(unsavedFile);
                        }
                        return true;
                    case JOptionPane.NO_OPTION:
                        return true;
                    case JOptionPane.CANCEL_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private void exit() {
        if (this.checkUnsaved()) {
            this.controller.close();
            System.exit(0);
        }
    }

    private class WindowClosingListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            EspressoPadView.this.exit();
        }
    }

    private class FrontendAdapter extends DockFrontendAdapter {
        private final JButton showFileTree;
        private final DefaultDockable fileTreeDockable;
        private final DockFrontend frontend;

        public FrontendAdapter(DefaultDockable fileTreeDockable, DockFrontend frontend) {
            this.fileTreeDockable = fileTreeDockable;
            this.frontend = frontend;
            this.showFileTree = new JButton(FontIcon.of(FontAwesomeSolid.WINDOW_RESTORE, 15));
            this.showFileTree.setToolTipText(EspressoPadView.this.resourceBundle.getString("restore.default.view"));
            this.showFileTree.addActionListener(this::setupShowFileTree);
        }

        @Override
        public void hidden(DockFrontend dockFrontend, Dockable dockable) {
            if (dockable == this.fileTreeDockable)
                EspressoPadView.this.toolBar.add(this.showFileTree);
        }

        @Override
        public void shown(DockFrontend frontend, Dockable dockable) {
            if (dockable == this.fileTreeDockable) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EspressoPadView.this.toolBar.remove(FrontendAdapter.this.showFileTree);
                        SwingUtilities.updateComponentTreeUI(EspressoPadView.this.toolBar);
                    }
                });
            }
        }

        private void setupShowFileTree(ActionEvent event) {
            if (this.frontend.isHidden(this.fileTreeDockable)) {
                this.frontend.show(this.fileTreeDockable);
            }
        }
    }
}
