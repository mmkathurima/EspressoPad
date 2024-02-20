package com.github.espressopad.ui;

import com.github.espressopad.controllers.ArtifactManagerController;
import com.github.espressopad.controllers.EspressoPadController;
import com.github.espressopad.editor.TextEditorAutoComplete;
import com.github.espressopad.utils.Utils;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArtifactManager extends Application {
    private final EspressoPadController jController;
    private ArtifactManagerController controller;
    private final Logger logger = Utils.getLogger(ArtifactManager.class);

    public ArtifactManager(EspressoPadController controller) {
        this.jController = controller;
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(this.getClass()
                .getResource("artifactmanager.fxml")));
        Scene scene = new Scene(loader.load());
        this.controller = loader.getController();
        this.controller.getLoadArtifacts().setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                controller.downloadArtifacts();
                List<String> artifacts = controller.getArtifacts();
                if (artifacts != null && !artifacts.isEmpty()) {
                    logger.debug("Artifacts: {}", artifacts);
                    for (String artifact : artifacts)
                        jController.getShell().addToClasspath(artifact);

                    for (TextEditorAutoComplete x : jController.getAutocompletes()) {
                        for (String artifact : artifacts)
                            x.getShell().addToClasspath(artifact);
                    }

                    controller.getHandler().writeArtifactXml(artifacts);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Artifacts added to classpath.");
                    Utils.setThemeResource(alert.getDialogPane().getScene());
                    alert.showAndWait();
                    //stage.close();
                }
            }
        });
        this.controller.getSaveImports().setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String importList = controller.getImports()
                        .stream()
                        .map(x -> String.format("import %s;", x))
                        .collect(Collectors.joining());
                jController.getShell().eval(String.format("import %s", importList));

                for (TextEditorAutoComplete x : jController.getAutocompletes())
                    x.getShell().eval(String.format("import %s;", importList));

                controller.getHandler().writeImportXml(controller.getImportView().getItems());
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Imports added.");
                Utils.setThemeResource(alert.getDialogPane().getScene());
                alert.showAndWait();
            }
        });
        Utils.setThemeResource(scene);

        stage.setScene(scene);
        stage.setTitle("Manage dependencies");
        stage.show();
    }
}
