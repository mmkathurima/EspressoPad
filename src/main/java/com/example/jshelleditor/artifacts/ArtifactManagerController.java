package com.example.jshelleditor.artifacts;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import net.unit8.erebus.Erebus;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.DependencyResolutionException;

import java.net.URL;
import java.util.ResourceBundle;

public class ArtifactManagerController implements Initializable {
    public Button loadArtifacts;
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
        this.dependencySearcher.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    Erebus erebus = new Erebus.Builder().build();
                    artifacts = erebus.resolveAsClasspath(dependencyQuery.getText());
                    if (artifacts.isBlank()) {
                        loadArtifacts.setVisible(false);
                        return;
                    } else loadArtifacts.setVisible(true);
                    results.getItems().setAll(artifacts);
                } catch (DependencyCollectionException | DependencyResolutionException e) {
                    loadArtifacts.setVisible(false);
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
