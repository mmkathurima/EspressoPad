package com.github.espressopad.artifacts;

import com.github.espressopad.EspressoPadController;
import com.github.espressopad.editor.TextEditorAutoComplete;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArtifactManager extends Application {
    private final EspressoPadController jController;
    private ArtifactManagerController controller;

    public ArtifactManager(EspressoPadController controller) {
        this.jController = controller;
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(this.getClass()
                .getResource("artifactmanager.fxml")));
        Scene scene = new Scene(loader.load());
        this.controller = loader.getController();
        this.controller.loadArtifacts.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                controller.downloadArtifacts();
                List<String> artifacts = controller.getArtifacts();
                if (artifacts != null && !artifacts.isEmpty()) {
                    System.out.printf("Artifacts: %s\n", artifacts);
                    for (String artifact : artifacts)
                        jController.getShell().addToClasspath(artifact);

                    for (TextEditorAutoComplete x : jController.getAutocompletes()) {
                        for (String artifact : artifacts)
                            x.getShell().addToClasspath(artifact);
                    }

                    controller.getHandler().writeArtifactXml(artifacts);
                    new Alert(Alert.AlertType.INFORMATION, "Artifacts added to classpath.").showAndWait();
                    //stage.close();
                }
            }
        });
        this.controller.saveImports.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String importList = controller.getImports()
                        .stream()
                        .map(x -> String.format("import %s;", x))
                        .collect(Collectors.joining());
                jController.getShell().eval(String.format("import %s", importList));

                for (TextEditorAutoComplete x : jController.getAutocompletes())
                    x.getShell().eval(String.format("import %s;", importList));

                controller.getHandler().writeImportXml(controller.importView.getItems());
                new Alert(Alert.AlertType.INFORMATION, "Imports added.").showAndWait();
            }
        });
        stage.setScene(scene);
        stage.setTitle("Manage dependencies");
        stage.show();
    }
}
