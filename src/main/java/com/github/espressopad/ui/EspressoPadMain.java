package com.github.espressopad.ui;

import com.github.espressopad.controllers.EspressoPadController;
import com.github.espressopad.editor.TextEditor;
import com.jthemedetecor.OsThemeDetector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class EspressoPadMain extends Application {
    private EspressoPadController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(this.getClass().getResource("espressopad.fxml")));
        Scene scene = new Scene(loader.load(), 800d, 600d);
        this.controller = loader.getController();
        this.controller.setupStageListeners(stage);

        setThemeResource(scene);

        stage.setScene(scene);
        stage.setTitle("Espresso Pad");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        try {
            super.stop();
            for (TextEditor x : this.controller.getEditors()) {
                x.getSubscriber().unsubscribe();
                x.stop();
            }
            this.controller.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setThemeResource(Scene scene) {
        String darkStyle = EspressoPadMain.class.getResource("dark-theme.css").toExternalForm();
        if (OsThemeDetector.isSupported() && OsThemeDetector.getDetector().isDark())
            scene.getStylesheets().add(darkStyle);
        else scene.getStylesheets().remove(darkStyle);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
