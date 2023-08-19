package com.example.jshelleditor.artifacts;

import com.example.jshelleditor.xml.XmlHandler;
import com.squareup.tools.maven.resolution.ArtifactResolver;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import kotlin.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

public class ArtifactManagerController implements Initializable {
    private final XmlHandler handler = new XmlHandler();
    @FXML
    Button loadArtifacts;
    @FXML
    Button saveImports;
    @FXML
    private Button removeImportBtn;
    @FXML
    private Button addImportBtn;
    @FXML
    private Button removeArtifactBtn;
    @FXML
    private TextField importStmt;
    @FXML
    private RadioButton toggleJarFinder;
    @FXML
    private RadioButton toggleSearch;
    @FXML
    private Button jarFinder;
    @FXML
    private ListView<String> artifactView;
    @FXML
    private TextField dependencyQuery;
    @FXML
    private Button dependencyResolver;
    @FXML
    ListView<String> importView;

    XmlHandler getHandler() {
        return handler;
    }

    List<String> getArtifacts() {
        return this.artifactView.getItems();
    }

    List<String> getImports() {
        return this.importView.getItems();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.jarFinder.setDisable(true);
        this.toggleJarFinder.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                jarFinder.setDisable(!newValue);
            }
        });
        this.toggleSearch.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                dependencyQuery.setDisable(!newValue);
                dependencyResolver.setDisable(!newValue);
            }
        });

        this.importView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                removeImportBtn.setVisible(newValue != null);
            }
        });
        if (this.handler.getArtifactFile().exists())
            this.artifactView.getItems().addAll(this.handler.parseArtifactXml());

        if (this.handler.getImportsFile().exists())
            this.importView.getItems().addAll(this.handler.parseImportXml());
    }

    public void pickJar(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        File file = chooser.showOpenDialog(this.jarFinder.getScene().getWindow());
        if (file != null) {
            this.artifactView.getItems().add(file.getPath());
            this.loadArtifacts.setVisible(true);
            this.removeArtifactBtn.setVisible(true);
        } else {
            this.loadArtifacts.setVisible(false);
            this.removeArtifactBtn.setVisible(false);
        }
    }

    public void resolveDependencies(ActionEvent event) {
        try {
            ArtifactResolver resolver = new ArtifactResolver(); // creates a resolver with repo list defaulting to Maven Central.
            Pair<Path, Path> dep = resolver.download(dependencyQuery.getText());
            this.artifactView.getItems().add(dep.getSecond().toString());
            this.loadArtifacts.setVisible(true);
            this.removeArtifactBtn.setVisible(true);
        } catch (IOException e) {
            this.loadArtifacts.setVisible(false);
            this.removeArtifactBtn.setVisible(false);
            throw new RuntimeException(e);
        }
    }

    public void removeArtifact(ActionEvent event) {
        String selected = this.artifactView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isBlank())
            this.artifactView.getItems().remove(selected);
    }

    public void addImport(ActionEvent event) {
        String importStmtText = this.importStmt.getText();
        if (!importStmtText.isBlank())
            this.importView.getItems().add(importStmtText);
    }

    public void removeImport(ActionEvent event) {
        String selected = this.importView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isBlank())
            this.importView.getItems().remove(selected);
    }
}
