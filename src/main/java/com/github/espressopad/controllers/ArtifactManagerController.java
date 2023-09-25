package com.github.espressopad.controllers;

import com.github.espressopad.xml.XmlHandler;
import com.squareup.tools.maven.resolution.Artifact;
import com.squareup.tools.maven.resolution.ArtifactResolver;
import com.squareup.tools.maven.resolution.ResolvedArtifact;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import kotlin.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

public class ArtifactManagerController implements Initializable {
    @FXML
    private
    Button loadArtifacts;
    @FXML
    private
    Button saveImports;
    @FXML
    private
    ListView<String> importView;
    @FXML
    private Button removeImportBtn;
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
    private TextField groupID;
    @FXML
    private TextField artifactID;
    @FXML
    private TextField extension;
    @FXML
    private TextField classifier;
    @FXML
    private TextField version;
    @FXML
    private Button dependencyResolver;
    @FXML
    private ListView<String> searchResults;

    private final XmlHandler handler = new XmlHandler();
    // creates a resolver with repo list defaulting to Maven Central.
    private final ArtifactResolver resolver = new ArtifactResolver();

    public XmlHandler getHandler() {
        return this.handler;
    }

    public List<String> getArtifacts() {
        return this.artifactView.getItems();
    }

    public List<String> getImports() {
        return this.importView.getItems();
    }

    public Button getLoadArtifacts() {
        return this.loadArtifacts;
    }

    public Button getSaveImports() {
        return this.saveImports;
    }

    public ListView<String> getImportView() {
        return this.importView;
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
                List.of(dependencyResolver, groupID, artifactID, extension, classifier, version)
                        .forEach(x -> x.setDisable(!newValue));
            }
        });

        this.importView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                removeImportBtn.setDisable(newValue == null);
            }
        });

        this.removeArtifactBtn.disableProperty().bind(Bindings.isEmpty(this.artifactView.getItems()));
        if (this.handler.getArtifactFile().exists())
            this.artifactView.getItems().addAll(this.handler.parseArtifactXml());

        if (this.handler.getImportsFile().exists())
            this.importView.getItems().addAll(this.handler.parseImportXml());
    }

    public void downloadArtifacts() {
        try {
            Pair<Path, Path> dependency = this.resolver.download(this.getDependencyString());
            this.artifactView.getItems().add(dependency.getSecond().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getDependencyString() {
        String dep = String.format("%s:%s:%s", groupID.getText(), artifactID.getText(),
                version.getText());
        if (!extension.getText().isBlank())
            dep = String.format("%s:%s:%s:%s", groupID.getText(),
                    artifactID.getText(), extension.getText(), version.getText());
        if (!classifier.getText().isBlank())
            dep = String.format("%s:%s:%s:%s", groupID.getText(),
                    artifactID.getText(), classifier.getText(), version.getText());
        if (!extension.getText().isBlank() && !classifier.getText().isBlank())
            dep = String.format("%s:%s:%s:%s:%s", groupID.getText(),
                    artifactID.getText(), extension.getText(), classifier.getText(),
                    version.getText());
        return dep;
    }

    @FXML
    private void pickJar(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        File file = chooser.showOpenDialog(this.jarFinder.getScene().getWindow());
        if (file != null) {
            this.artifactView.getItems().add(file.getPath());
            this.loadArtifacts.setDisable(false);
        } else this.loadArtifacts.setDisable(true);
    }

    @FXML
    private void resolveDependencies(ActionEvent event) {
        TextField[] txtFields = {groupID, artifactID, version};
        Background bg = new TextField().getBackground();
        Background validation = new Background(new BackgroundFill(
                Color.hsb(0, .2, 1),
                CornerRadii.EMPTY,
                Insets.EMPTY));
        for (TextField txt : txtFields) {
            if (txt.getText().isBlank()) {
                txt.setBackground(validation);
                return;
            } else if (txt.getBackground() == validation)
                txt.setBackground(bg);
        }

        Artifact artifact = this.resolver.artifactFor(this.getDependencyString());
        ResolvedArtifact resolvedArtifact = this.resolver.resolveArtifact(artifact);
        if (resolvedArtifact != null) {
            this.searchResults.getItems().setAll(String.format("%s:%s:%s", resolvedArtifact.getArtifactId(),
                    resolvedArtifact.getGroupId(), resolvedArtifact.getVersion()));
            this.loadArtifacts.setDisable(false);
        } else {
            this.searchResults.getItems().setAll(String.format("No results found for %s", this.getDependencyString()));
            this.loadArtifacts.setDisable(true);
        }
    }

    @FXML
    private void removeArtifact(ActionEvent event) {
        String selected = this.artifactView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isBlank())
            this.artifactView.getItems().remove(selected);
    }

    @FXML
    private void addImport(ActionEvent event) {
        String importStmtText = this.importStmt.getText();
        if (!importStmtText.isBlank() && !this.importView.getItems().contains(importStmtText)) {
            this.importView.getItems().add(importStmtText);
            this.importStmt.clear();
            this.importStmt.requestFocus();
        }
    }

    @FXML
    private void removeImport(ActionEvent event) {
        String selected = this.importView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isBlank())
            this.importView.getItems().remove(selected);
    }
}
