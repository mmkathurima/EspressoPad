package com.github.espressopad.views;

import com.github.espressopad.utils.Utilities;
import com.github.espressopad.views.components.HyperlinkLabel;
import com.github.espressopad.views.components.MultiLineCellRenderer;
import org.kordamp.ikonli.fontawesome5.FontAwesomeBrands;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.time.Year;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AboutView {
    private final JDialog dialog = new JDialog();
    private final JFrame frame;
    private JPanel contentPane;
    private JButton buttonOK;
    private JTable propertiesTable;
    private JTextField filterPropertiesText;
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", Locale.getDefault());

    public AboutView(JFrame frame) {
        this.frame = frame;
        this.setupUI();
        this.setupTable();

        this.buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutView.this.onOK();
            }
        });
    }

    private void browseGithub(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(URI.create("https://www.github.com/mmkathurima"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onOK() {
        this.dialog.dispose();
    }

    public void show() {
        this.dialog.setTitle(this.resourceBundle.getString("about.espresso.pad"));
        this.dialog.setContentPane(this.contentPane);
        this.dialog.setModal(true);
        this.dialog.getRootPane().setDefaultButton(this.buttonOK);
        this.dialog.setSize(new Dimension(550, 450));
        this.dialog.setLocationRelativeTo(this.frame);
        this.dialog.setVisible(true);
    }

    private void setupUI() {
        this.contentPane = new JPanel();
        this.contentPane.setLayout(new BorderLayout(0, 0));
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        this.contentPane.add(panel1, BorderLayout.SOUTH);
        this.buttonOK = new JButton();
        this.buttonOK.setHorizontalAlignment(0);
        this.buttonOK.setText("OK");
        String url;
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 10);
        panel1.add(this.buttonOK, gbc);
        JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(spacer1, gbc);
        JTabbedPane tabbedPane1 = new JTabbedPane();
        this.contentPane.add(tabbedPane1, BorderLayout.CENTER);
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        tabbedPane1.addTab(this.resourceBundle.getString("properties"), panel2);
        this.propertiesTable = new JTable();
        this.propertiesTable.setDefaultRenderer(Object.class, new MultiLineCellRenderer());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0.9;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel2.add(new JScrollPane(this.propertiesTable), gbc);
        this.filterPropertiesText = new JTextField();
        this.filterPropertiesText.setBorder(BorderFactory.createTitledBorder(this.resourceBundle.getString("filter.properties")));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 0, 10);
        panel2.add(this.filterPropertiesText, gbc);
        JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        tabbedPane1.addTab(this.resourceBundle.getString("libraries.used"), new JScrollPane(panel3));
        JLabel label1 = new JLabel();
        label1.setText(this.resourceBundle.getString("espresso.pad.uses.the.following.libraries"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(20, 0, 10, 0);
        panel3.add(label1, gbc);
        JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(spacer2, gbc);
        JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel3.add(spacer3, gbc);
        HyperlinkLabel label9 = new HyperlinkLabel(this.resourceBundle.getString("rsyntaxtextarea.by.bobbylight"));
        url = "https://github.com/bobbylight/RSyntaxTextArea";
        label9.setURL(url);
        label9.setToolTipText(url);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel3.add(label9, gbc);
        HyperlinkLabel label2 = new HyperlinkLabel(this.resourceBundle.getString("ikonli.by.kordamp"));
        url = "https://github.com/kordamp/ikonli";
        label2.setURL(url);
        label2.setToolTipText(url);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel3.add(label2, gbc);
        HyperlinkLabel label3 = new HyperlinkLabel(this.resourceBundle.getString("maven.archeologist.by.square"));
        url = "https://github.com/square/maven-archeologist";
        label3.setURL(url);
        label3.setToolTipText(url);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel3.add(label3, gbc);
        HyperlinkLabel label4 = new HyperlinkLabel(this.resourceBundle.getString("jastyle.by.abrar.syed"));
        url = "https://github.com/AbrarSyed/jastyle";
        label4.setURL(url);
        label4.setToolTipText(url);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel3.add(label4, gbc);
        HyperlinkLabel label5 = new HyperlinkLabel("JavaParser");
        url = "https://javaparser.org/";
        label5.setURL(url);
        label5.setToolTipText(url);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel3.add(label5, gbc);
        HyperlinkLabel label6 = new HyperlinkLabel("SLF4J");
        url = "https://www.slf4j.org/";
        label6.setURL(url);
        label6.setToolTipText(url);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel3.add(label6, gbc);
        HyperlinkLabel label7 = new HyperlinkLabel("Logback");
        url = "https://logback.qos.ch/";
        label7.setURL(url);
        label7.setToolTipText(url);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel3.add(label7, gbc);
        HyperlinkLabel label8 = new HyperlinkLabel("DockingFrames");
        url = "https://www.docking-frames.org/";
        label8.setURL(url);
        label8.setToolTipText(url);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel3.add(label8, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        this.contentPane.add(panel4, BorderLayout.NORTH);
        JLabel label10 = new JLabel();
        Font label10Font = Utilities.deriveFont(null, Font.BOLD, 26, label10.getFont());
        if (label10Font != null) label10.setFont(label10Font);
        label10.setText(this.resourceBundle.getString("espresso.pad"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 10, 0, 0);
        panel4.add(label10, gbc);
        JLabel label11 = new JLabel();
        label11.setText(String.format("<html><p>%s</p><p>© %d</p><html>",
                        this.resourceBundle.getString("espresso.pad.version"), Year.now().getValue()));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 10, 10, 0);
        panel4.add(label11, gbc);
        JButton viewGithubBtn = new JButton();
        viewGithubBtn.setIcon(FontIcon.of(FontAwesomeBrands.GITHUB, 15));
        viewGithubBtn.setToolTipText(this.resourceBundle.getString("view.my.github.profile"));
        viewGithubBtn.addActionListener(this::browseGithub);
        viewGithubBtn.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 0, 10);
        panel4.add(viewGithubBtn, gbc);
        JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(spacer4, gbc);
        this.filterPropertiesText.grabFocus();
        this.filterPropertiesText.requestFocusInWindow();
    }

    private void setupTable() {
        Map<String, String> properties = System.getProperties()
                .entrySet()
                .stream()
                .map(x -> Map.entry(String.valueOf(x.getKey()), String.valueOf(x.getValue())))
                .filter(x -> !Objects.equals(x.getKey(), "line.separator"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{
                this.resourceBundle.getString("property.key"), this.resourceBundle.getString("property.value")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Map.Entry<String, String> entry : properties.entrySet())
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        this.propertiesTable.setModel(tableModel);
        this.filterPropertiesText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                this.onChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                this.onChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                this.onChange();
            }

            private void onChange() {
                Map<String, String> filteredProperties = properties.entrySet()
                        .stream()
                        .filter(x -> x.getKey().toLowerCase().contains(AboutView.this.filterPropertiesText.getText().toLowerCase()) ||
                                x.getValue().toLowerCase().contains(AboutView.this.filterPropertiesText.getText().toLowerCase()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                tableModel.setRowCount(0);
                for (Map.Entry<String, String> entry : filteredProperties.entrySet())
                    tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});

            }
        });
    }
}
