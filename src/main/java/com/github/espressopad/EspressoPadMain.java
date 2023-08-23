package com.github.espressopad;

import com.github.espressopad.editor.TextEditor;
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

        stage.setScene(scene);
        stage.setTitle("Espresso Pad");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
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
}
