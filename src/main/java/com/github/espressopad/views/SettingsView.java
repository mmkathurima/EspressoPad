package com.github.espressopad.views;

import com.github.espressopad.controller.EspressoPadController;
import com.github.espressopad.controller.SettingsController;
import com.github.espressopad.models.ArtifactModel;
import com.github.espressopad.models.SettingsModel;
import com.github.espressopad.utils.Utilities;
import com.github.espressopad.utils.SerializationUtilities;
import com.github.espressopad.views.components.PlaceHolderTextField;
import com.github.espressopad.views.components.TextEditor;
import com.squareup.tools.maven.resolution.ResolvedArtifact;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SettingsView {
    private final Logger logger = LoggerFactory.getLogger(SettingsView.class);
    private final SettingsController controller = new SettingsController();
    private final SerializationUtilities handler = new SerializationUtilities();
    private final JTabbedPane view = new JTabbedPane();
    private final SettingsModel settings;
    private JDialog dialog;
    private JRadioButton searchDependencyRadio;
    private JButton searchDependencyBtn;
    private JRadioButton pickJarRadio;
    private JButton pickJarBtn;
    private PlaceHolderTextField artifactIDText;
    private PlaceHolderTextField extensionText;
    private PlaceHolderTextField classifierText;
    private PlaceHolderTextField versionText;
    private PlaceHolderTextField groupIDText;
    private JList<String> searchResultsList;
    private JButton addArtifactButton;
    private JList<String> installedArtifactsList;
    private JButton removeInstalledArtifactBtn;
    private JButton saveInstalledArtifactBtn;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", Locale.getDefault());
    private final String[] textEditorThemeList = new String[]{
            this.resourceBundle.getString("default"), this.resourceBundle.getString("default.system.selection"),
            this.resourceBundle.getString("dark"), "Druid", "Monokai", "Eclipse", "IDEA", "Visual Studio"
    };
    private final Map<String, String> textEditorThemes = IntStream.range(0, this.textEditorThemeList.length)
            .boxed()
            .collect(Collectors.toMap(k -> new String[]{
                    "default.xml", "default-alt.xml", "dark.xml", "druid.xml", "monokai.xml",
                    "eclipse.xml", "idea.xml", "vs.xml"
            }[k], v -> this.textEditorThemeList[v]));
    private PlaceHolderTextField importText;
    private JList<String> importList;
    private JButton removeImportBtn;
    private JSpinner fontSizeSpinner;
    private JComboBox<Font> fontComboBox;
    private JComboBox<UIManager.LookAndFeelInfo> lafComboBox;
    private JComboBox<String> textEditorThemeComboBox;
    private final List<TextEditor> textEditors;
    private final DefaultListModel<String> searchResultsModel = new DefaultListModel<>();
    private final DefaultListModel<String> installedArtifactModel = new DefaultListModel<>();
    private final DefaultListModel<String> importsModel = new DefaultListModel<>();
    private JCheckBox wordWrapCheck;

    public SettingsView(List<TextEditor> textEditors, SettingsModel settings) {
        this.settings = settings;
        this.textEditors = textEditors;
        this.setupAppearance();
        this.setupDependenciesView();
        this.setupImportsManagement();
        this.deactivatePickJar();
        this.toggleArtifactButtonState();
        this.toggleInstalledArtifactButtonState();
        this.toggleRemoveImportButtonState();
    }

    public void show() {
        Frame frame = JOptionPane.getFrameForComponent(this.textEditors.get(0));
        this.dialog = new JDialog(frame, this.resourceBundle.getString("settings"), true);
        this.dialog.setSize(new Dimension(700, 500));
        this.dialog.setContentPane(this.view);
        this.dialog.setLocationRelativeTo(frame);
        this.dialog.setVisible(true);
    }

    private void setupAppearance() {
        JPanel panel = new JPanel(new GridBagLayout());
        Font font = this.textEditors.get(0).getFont();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(this.resourceBundle.getString("editor.font.size")), gbc);
        gbc.gridx = 1;
        SpinnerModel fontSizeModel = new SpinnerNumberModel(font.getSize(), 8, 36, 1);
        this.fontSizeSpinner = new JSpinner(fontSizeModel);
        panel.add(this.fontSizeSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel(this.resourceBundle.getString("editor.font")), gbc);
        gbc.gridx = 1;
        this.fontComboBox = new JComboBox<>(Utilities.getMonospaceFonts());
        this.fontComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
            ) {
                value = ((Font) value).getName();
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        for (int i = 0; i < this.fontComboBox.getItemCount(); i++) {
            if (Objects.equals(this.fontComboBox.getItemAt(i).getFontName(), font.getFontName())) {
                this.fontComboBox.setSelectedIndex(i);
                break;
            }
        }
        panel.add(this.fontComboBox, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel(this.resourceBundle.getString("editor.theme")), gbc);
        gbc.gridx = 1;
        this.textEditorThemeComboBox = new JComboBox<>(this.textEditorThemeList);
        if (this.settings != null) {
            String theme = this.settings.getTheme();
            this.textEditorThemeComboBox.setSelectedItem(
                    this.textEditorThemes.get(theme.substring(theme.lastIndexOf('/') + 1))
            );
        }
        panel.add(this.textEditorThemeComboBox, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel(this.resourceBundle.getString("word.wrap")), gbc);
        gbc.gridx = 1;
        this.wordWrapCheck = new JCheckBox();
        this.wordWrapCheck.setSelected(this.settings != null && this.settings.isWordWrap());
        panel.add(this.wordWrapCheck, gbc);
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel(this.resourceBundle.getString("change.look.and.feel")), gbc);
        gbc.gridx = 1;
        this.lafComboBox = new JComboBox<>(UIManager.getInstalledLookAndFeels());
        this.lafComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
            ) {
                value = ((UIManager.LookAndFeelInfo) value).getName();
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        for (int i = 0; i < this.lafComboBox.getItemCount(); i++) {
            UIManager.LookAndFeelInfo lookAndFeelInfo = this.lafComboBox.getItemAt(i);
            if (lookAndFeelInfo.getClassName().equals(UIManager.getLookAndFeel().getClass().getName())) {
                this.lafComboBox.setSelectedIndex(i);
                break;
            }
        }
        panel.add(this.lafComboBox, gbc);
        gbc.gridy = 5;
        JButton saveAppearanceButton = new JButton();
        saveAppearanceButton.setIcon(FontIcon.of(FontAwesomeSolid.SAVE, 15));
        saveAppearanceButton.addActionListener(event -> this.saveAppearanceChanges());
        panel.add(saveAppearanceButton, gbc);

        this.view.addTab(this.resourceBundle.getString("appearance"), panel);
    }

    private void saveAppearanceChanges() {
        try {
            String themeLocation = String.format(
                    "/org/fife/ui/rsyntaxtextarea/themes/%s",
                    this.textEditorThemes.entrySet()
                            .stream()
                            .filter(kv -> kv.getValue().equals(this.textEditorThemeComboBox.getSelectedItem()))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse("default.xml")
            );
            InputStream in = this.getClass().getResourceAsStream(themeLocation);
            Theme theme = Theme.load(in);
            Font font = Utilities.deriveFont(
                    null, Font.PLAIN,
                    Integer.parseInt(String.valueOf(this.fontSizeSpinner.getValue())),
                    ((Font) this.fontComboBox.getSelectedItem())
            );
            boolean wordWrap = this.wordWrapCheck.isSelected();
            String laf = ((UIManager.LookAndFeelInfo) this.lafComboBox.getSelectedItem()).getClassName();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (TextEditor textEditor : SettingsView.this.textEditors) {
                        theme.apply(textEditor);
                        textEditor.setFont(font);
                        textEditor.setLineWrap(wordWrap);
                    }
                }
            });
            UIManager.setLookAndFeel(laf);
            SwingUtilities.updateComponentTreeUI(this.dialog);
            SwingUtilities.updateComponentTreeUI(JOptionPane.getFrameForComponent(this.textEditors.get(0)));

            SettingsModel settings = new SettingsModel();
            settings.setFont(font.getFontName());
            settings.setFontSize(font.getSize());
            settings.setTheme(themeLocation);
            settings.setWordWrap(wordWrap);
            settings.setLookAndFeel(laf);
            this.handler.writeSettingsXml(settings);
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException | IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void setupDependenciesView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        panel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(panel2, BorderLayout.NORTH);
        this.searchDependencyRadio = new JRadioButton();
        this.searchDependencyRadio.setSelected(true);
        this.searchDependencyRadio.setText(this.resourceBundle.getString("search.for.dependencies"));
        this.searchDependencyRadio.addActionListener(event -> {
            this.deactivatePickJar();
            this.setSearchDependenciesState(true);
        });
        GridBagConstraints gbc = new GridBagConstraints();
        Insets defaultInsets = gbc.insets;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(this.searchDependencyRadio, gbc);
        this.pickJarRadio = new JRadioButton();
        this.pickJarRadio.setText(this.resourceBundle.getString("pick.jar.file"));
        this.pickJarRadio.addActionListener(event -> {
            this.setSearchDependenciesState(false);
            this.activatePickJar();
        });
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel2.add(this.pickJarRadio, gbc);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.searchDependencyRadio);
        buttonGroup.add(this.pickJarRadio);
        JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.8;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(panel3, gbc);
        this.groupIDText = new PlaceHolderTextField();
        this.groupIDText.setPlaceHolder(this.resourceBundle.getString("group.id"));
        gbc.insets = defaultInsets;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(this.groupIDText, gbc);
        this.artifactIDText = new PlaceHolderTextField();
        this.artifactIDText.setPlaceHolder(this.resourceBundle.getString("artifact.id"));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(this.artifactIDText, gbc);
        this.extensionText = new PlaceHolderTextField();
        this.extensionText.setPlaceHolder(this.resourceBundle.getString("extension"));
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(this.extensionText, gbc);
        this.classifierText = new PlaceHolderTextField();
        this.classifierText.setPlaceHolder(this.resourceBundle.getString("classifier"));
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(this.classifierText, gbc);
        this.versionText = new PlaceHolderTextField();
        this.versionText.setPlaceHolder(this.resourceBundle.getString("version"));
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(this.versionText, gbc);
        this.searchDependencyBtn = new JButton();
        this.searchDependencyBtn.setIcon(FontIcon.of(FontAwesomeSolid.SEARCH, 15));
        this.searchDependencyBtn.addActionListener(event -> this.resolveArtifacts());
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(this.searchDependencyBtn, gbc);
        this.pickJarBtn = new JButton();
        this.pickJarBtn.setText(this.resourceBundle.getString("pick.jar"));
        this.pickJarBtn.addActionListener(event -> this.pickJar());
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel2.add(this.pickJarBtn, gbc);
        JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        panel4.setPreferredSize(new Dimension(78, 300));
        panel4.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(panel4, BorderLayout.CENTER);
        this.searchResultsModel.addListDataListener(new SearchResultsDataListener());
        this.searchResultsList = new JList<>();
        this.searchResultsList.setBorder(BorderFactory.createTitledBorder(this.resourceBundle.getString("search.results")));
        this.searchResultsList.setModel(this.searchResultsModel);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.8;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(new JScrollPane(this.searchResultsList), gbc);
        this.addArtifactButton = new JButton();
        this.addArtifactButton.setIcon(FontIcon.of(FontAwesomeSolid.ARROW_RIGHT, 15));
        this.addArtifactButton.addActionListener(event -> this.addArtifact());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = .1;
        panel4.add(this.addArtifactButton, gbc);
        if (Files.exists(this.handler.getArtifactFile()))
            this.installedArtifactModel.addAll(this.handler.parseArtifactXml());
        this.installedArtifactModel.addListDataListener(new InstalledArtifactsDataListener());
        this.installedArtifactsList = new JList<>();
        this.installedArtifactsList.setBorder(BorderFactory.createTitledBorder(this.resourceBundle.getString("installed.artifacts")));
        this.installedArtifactsList.setModel(this.installedArtifactModel);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(new JScrollPane(this.installedArtifactsList), gbc);
        JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weighty = 0;
        panel4.add(panel5, gbc);
        this.removeInstalledArtifactBtn = new JButton();
        this.removeInstalledArtifactBtn.setIcon(FontIcon.of(FontAwesomeSolid.MINUS, 15));
        this.removeInstalledArtifactBtn.addActionListener(event -> this.removeArtifact());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel5.add(this.removeInstalledArtifactBtn, gbc);
        this.saveInstalledArtifactBtn = new JButton();
        this.saveInstalledArtifactBtn.setIcon(FontIcon.of(FontAwesomeSolid.SAVE, 15));
        this.saveInstalledArtifactBtn.addActionListener(event -> this.saveArtifacts());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel5.add(this.saveInstalledArtifactBtn, gbc);

        this.view.addTab(this.resourceBundle.getString("manage.dependencies"), panel);
        this.view.setTabPlacement(SwingConstants.LEFT);
    }

    private void setupImportsManagement() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        this.importText = new PlaceHolderTextField();
        this.importText.setPlaceHolder(this.resourceBundle.getString("wildcard.import.string.e.g.java.net"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = .9;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(this.importText, gbc);
        JButton addImportBtn = new JButton();
        addImportBtn.setIcon(FontIcon.of(FontAwesomeSolid.PLUS, 15));
        addImportBtn.addActionListener(event -> this.addImport(this.importText.getText()));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = .1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(addImportBtn, gbc);
        this.importsModel.addAll(this.handler.parseImportXml());
        this.importsModel.addListDataListener(new ImportsDataListener());
        this.importList = new JList<>();
        this.importList.setBorder(BorderFactory.createTitledBorder(this.resourceBundle.getString("default.imports")));
        this.importList.setModel(this.importsModel);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(this.importList), gbc);
        this.removeImportBtn = new JButton();
        this.removeImportBtn.setIcon(FontIcon.of(FontAwesomeSolid.MINUS, 15));
        this.removeImportBtn.addActionListener(event -> this.removeImport());
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(this.removeImportBtn, gbc);
        JButton saveImportBtn = new JButton();
        saveImportBtn.setIcon(FontIcon.of(FontAwesomeSolid.SAVE, 15));
        saveImportBtn.addActionListener(event -> this.saveImports());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        panel.add(saveImportBtn, gbc);

        this.view.addTab(this.resourceBundle.getString("manage.imports"), panel);
    }

    private void pickJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(this.resourceBundle.getString("jar.file"), "jar"));
        if (chooser.showOpenDialog(this.view) == JFileChooser.APPROVE_OPTION) {
            this.installedArtifactModel.addElement(chooser.getSelectedFile().getPath());
            this.saveInstalledArtifactBtn.setEnabled(true);
        } else this.saveInstalledArtifactBtn.setEnabled(false);
    }

    private void addArtifact() {
        try {
            ArtifactModel dependency = this.controller.downloadArtifacts(this.getDependencyString());
            String localArtifactPath = dependency.getLocalArtifactPath().toString();
            if (!this.installedArtifactModel.contains(localArtifactPath))
                this.installedArtifactModel.addElement(localArtifactPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeArtifact() {
        this.installedArtifactModel.removeElement(this.installedArtifactsList.getSelectedValue());
    }

    private void saveArtifacts() {
        List<String> artifacts = Collections.list(this.installedArtifactModel.elements());
        if (!artifacts.isEmpty()) {
            this.logger.debug(this.resourceBundle.getString("artifacts"), artifacts);
            for (String artifact : artifacts)
                EspressoPadController.getShell().addToClasspath(artifact);

            this.handler.writeArtifactXml(artifacts);
            JOptionPane.showMessageDialog(
                    JOptionPane.getFrameForComponent(this.view),
                    this.resourceBundle.getString("artifacts.added.to.classpath"),
                    this.resourceBundle.getString("changes.saved"),
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void addImport(String importString) {
        if (importString != null && !importString.isBlank() && !this.importsModel.contains(importString)) {
            this.importsModel.addElement(importString);
            this.importText.setText("");
            this.importText.requestFocusInWindow();
        }
    }

    private void removeImport() {
        this.importsModel.removeElement(this.importList.getSelectedValue());
    }

    private void saveImports() {
        this.handler.writeImportXml(Collections.list(this.importsModel.elements()));
    }

    private void deactivatePickJar() {
        this.pickJarBtn.setEnabled(false);
    }

    private void activatePickJar() {
        this.pickJarBtn.setEnabled(true);
        this.searchDependencyRadio.setSelected(false);
    }

    private void setSearchDependenciesState(boolean status) {
        this.groupIDText.setEnabled(status);
        this.artifactIDText.setEnabled(status);
        this.extensionText.setEnabled(status);
        this.classifierText.setEnabled(status);
        this.versionText.setEnabled(status);
        this.searchDependencyBtn.setEnabled(status);
        if (status)
            this.pickJarRadio.setSelected(false);
    }

    private void resolveArtifacts() {
        String dependencyString = this.getDependencyString();
        this.searchResultsModel.clear();
        if (dependencyString != null && !dependencyString.isBlank()) {
            ResolvedArtifact resolvedArtifact = this.controller.resolveArtifacts(dependencyString);
            if (resolvedArtifact != null) {
                this.searchResultsModel.addElement(
                        String.format("%s - %s - %s", resolvedArtifact.getModel().getName(),
                                resolvedArtifact.getModel().getDescription(), resolvedArtifact.getCoordinate()));
                this.saveInstalledArtifactBtn.setEnabled(true);
            } else {
                this.searchResultsModel.addElement(
                        String.format(this.resourceBundle.getString("no.results.found.for.s"), dependencyString)
                );
                this.saveInstalledArtifactBtn.setEnabled(false);
            }
        }
    }

    private String getDependencyString() {
        String dep = String.format("%s:%s:%s", this.groupIDText.getText(), this.artifactIDText.getText(),
                this.versionText.getText());
        if (!this.extensionText.getText().isBlank())
            dep = String.format("%s:%s:%s:%s", this.groupIDText.getText(),
                    this.artifactIDText.getText(), this.extensionText.getText(), this.versionText.getText());
        if (!this.classifierText.getText().isBlank())
            dep = String.format("%s:%s:%s:%s", this.groupIDText.getText(),
                    this.artifactIDText.getText(), this.classifierText.getText(), this.versionText.getText());
        if (!this.extensionText.getText().isBlank() && !this.classifierText.getText().isBlank())
            dep = String.format("%s:%s:%s:%s:%s", this.groupIDText.getText(),
                    this.artifactIDText.getText(), this.extensionText.getText(), this.classifierText.getText(),
                    this.versionText.getText());
        return dep;
    }

    private void toggleArtifactButtonState() {
        this.addArtifactButton.setEnabled(this.searchResultsList.getModel().getSize() != 0);
    }

    private void toggleRemoveImportButtonState() {
        this.removeImportBtn.setEnabled(this.importList.getModel().getSize() != 0);
    }

    private void toggleInstalledArtifactButtonState() {
        boolean notEmpty = this.searchResultsList.getModel().getSize() != 0;
        this.removeInstalledArtifactBtn.setEnabled(notEmpty);
        this.saveInstalledArtifactBtn.setEnabled(notEmpty);
    }

    private class SearchResultsDataListener implements ListDataListener {
        @Override
        public void intervalAdded(ListDataEvent e) {
            SettingsView.this.toggleArtifactButtonState();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            SettingsView.this.toggleArtifactButtonState();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            SettingsView.this.toggleArtifactButtonState();
        }
    }

    private class InstalledArtifactsDataListener implements ListDataListener {
        @Override
        public void intervalAdded(ListDataEvent e) {
            SettingsView.this.toggleInstalledArtifactButtonState();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            SettingsView.this.toggleInstalledArtifactButtonState();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            SettingsView.this.toggleInstalledArtifactButtonState();
        }
    }

    private class ImportsDataListener implements ListDataListener {

        @Override
        public void intervalAdded(ListDataEvent e) {
            SettingsView.this.toggleRemoveImportButtonState();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            SettingsView.this.toggleRemoveImportButtonState();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            SettingsView.this.toggleRemoveImportButtonState();
        }
    }
}
