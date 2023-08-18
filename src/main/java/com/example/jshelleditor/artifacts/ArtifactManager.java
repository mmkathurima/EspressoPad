package com.example.jshelleditor.artifacts;

import com.example.jshelleditor.JShellEditorController;
import com.example.jshelleditor.TextEditorAutoComplete;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ArtifactManager extends Application {
    private final JShellEditorController jController;
    private ArtifactManagerController controller;

    public ArtifactManager(JShellEditorController controller) {
        this.jController = controller;
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(this.getClass()
                .getResource("artifactmanager.fxml")));
        Scene scene = new Scene(loader.load());
        this.controller = loader.getController();
        this.controller.loadArtifacts.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String artifacts = controller.getArtifacts();
                if (artifacts != null && !artifacts.isBlank()) {
                    System.out.printf("Artifacts: %s\n", artifacts);
                    jController.getShell().addToClasspath(artifacts);
                    for (TextEditorAutoComplete x : jController.getAutocompletes())
                        x.getShell().addToClasspath(artifacts);

                    new Alert(Alert.AlertType.INFORMATION, "Artifacts added to classpath.").showAndWait();
                    stage.close();
                }
            }
        });
        stage.setScene(scene);
        stage.setTitle("Add dependencies (WORK IN PROGRESS)");
        stage.show();
    }
}
