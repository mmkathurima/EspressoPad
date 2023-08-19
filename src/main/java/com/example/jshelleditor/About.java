package com.example.jshelleditor;

import com.example.jshelleditor.editor.TextEditorConstants;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Year;

public class About extends Application {
    private final Stage stage;

    public About(Window window) {
        this.stage = (Stage) window;
    }

    @Override
    public void start(Stage stage) {
        TextArea textArea = new TextArea();
        String title = this.stage.getTitle();
        textArea.setWrapText(true);
        textArea.setEditable(false);

        Label productName = new Label(title);
        productName.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.EXTRA_BOLD, 36d));
        productName.setAlignment(Pos.CENTER);

        Label details = new Label(String.format("v0.6\n©%d\nRuntime: %s %s %s\nVM: %s", Year.now().getValue(),
                System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"),
                System.getProperty("os.arch"), System.getProperty("java.vm.name")));
        details.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("OK");
        closeBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> stage.close());

        Button gitBtn = new Button();
        gitBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    Desktop.getDesktop().browse(new URL("https://www.github.com/Titans068").toURI());
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        FontIcon gitIcon = new FontIcon("fab-github");
        gitIcon.setIconSize(20);
        gitBtn.setGraphic(gitIcon);

        HBox btnBox = new HBox(closeBtn, gitBtn);
        btnBox.setSpacing(10d);

        VBox vBox = new VBox(productName, details, textArea, btnBox);
        vBox.setSpacing(15d);
        vBox.setPadding(new Insets(15d));
        //vBox.setStyle("-fx-padding: 5px 1em;-fx-border-insets: 5px;-fx-background-insets: 5px;");

        stage.setScene(new Scene(vBox));
        for (String key : TextEditorConstants.properties)
            textArea.appendText(String.format("%s - %s\n", key, System.getProperty(key)));
        textArea.positionCaret(0);

        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);
        stage.initOwner(this.stage);
        stage.setTitle(String.format("About %s", title));
        stage.show();
    }
}
