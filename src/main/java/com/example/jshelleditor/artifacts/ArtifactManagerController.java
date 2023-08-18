package com.example.jshelleditor.artifacts;

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
import java.util.ResourceBundle;

public class ArtifactManagerController implements Initializable {
    public Button loadArtifacts;
    @FXML
    private Button jarFinder;
    @FXML
    private RadioButton toggleJarFinder;
    @FXML
    private RadioButton toggleSearch;
    @FXML
    private ListView<String> results;
    @FXML
    private Button dependencySearcher;
    @FXML
    private TextField dependencyQuery;
    private String artifacts;

    public String getArtifacts() {
        return this.artifacts;
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
                dependencySearcher.setDisable(!newValue);
            }
        });
    }

    public void pickJar(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        File file = chooser.showOpenDialog(this.jarFinder.getScene().getWindow());
        if (file != null) {
            this.artifacts = file.getPath();
            this.results.getItems().add(this.artifacts);
            this.loadArtifacts.setVisible(true);
        }
    }

    public void searchDependencies(ActionEvent event) {
        try {
            ArtifactResolver resolver = new ArtifactResolver(); // creates a resolver with repo list defaulting to Maven Central.
            Pair<Path, Path> dep = resolver.download(dependencyQuery.getText());
            this.artifacts = dep.getSecond().toString();
            this.results.getItems().addAll(this.artifacts);
            this.loadArtifacts.setVisible(true);
        } catch (IOException e) {
            loadArtifacts.setVisible(false);
            throw new RuntimeException(e);
        }
    }
}
