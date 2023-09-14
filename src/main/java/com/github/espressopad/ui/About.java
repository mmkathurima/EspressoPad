package com.github.espressopad.ui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.MapValueFactory;
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
import java.util.Map;
import java.util.stream.Collectors;

public class About extends Application {
    private final Stage stage;

    public About(Window window) {
        this.stage = (Stage) window;
    }

    @Override
    public void start(Stage stage) {
        String title = this.stage.getTitle();
        TableView<Map<String, String>> tableView = new TableView<>();
        tableView.setEditable(false);

        TableColumn<Map<String, String>, String> keyColumn = new TableColumn<>("Property Key");
        keyColumn.setCellValueFactory(new MapValueFactory("key"));

        TableColumn<Map<String, String>, String> valueColumn = new TableColumn<>("Property Value");
        valueColumn.setCellValueFactory(new MapValueFactory("value"));

        tableView.getColumns().addAll(keyColumn, valueColumn);

        tableView.getItems().addAll(System.getProperties()
                .entrySet()
                .stream()
                .map(x -> Map.of("key", String.valueOf(x.getKey()), "value", String.valueOf(x.getValue())))
                .collect(Collectors.toList()));

        tableView.getSortOrder().setAll(keyColumn);

        Label productName = new Label(title);
        productName.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.EXTRA_BOLD, 36d));
        productName.setAlignment(Pos.CENTER);

        Label details = new Label(String.format("v0.16.1\n©%d\nRuntime: %s %s %s\nVM: %s", Year.now().getValue(),
                System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"),
                System.getProperty("os.arch"), System.getProperty("java.vm.name")));
        details.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("OK");
        closeBtn.setPadding(new Insets(5d, 10d, 5d, 10d));
        closeBtn.setOnAction(e -> stage.close());

        Button githubLinkBtn = new Button();
        githubLinkBtn.setFocusTraversable(true);
        githubLinkBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    Desktop.getDesktop().browse(new URL("https://www.github.com/Titans068").toURI());
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        FontIcon gitIcon = new FontIcon("fab-github");
        gitIcon.setIconSize(20);
        githubLinkBtn.setGraphic(gitIcon);

        HBox btnBox = new HBox(closeBtn, githubLinkBtn);
        btnBox.setSpacing(10d);

        VBox vBox = new VBox(productName, details, tableView, btnBox);
        vBox.setSpacing(15d);
        vBox.setPadding(new Insets(15d));
        vBox.setPrefWidth(500d);
        //vBox.setStyle("-fx-padding: 5px 1em;-fx-border-insets: 5px;-fx-background-insets: 5px;");

        Scene scene = new Scene(vBox);
        EspressoPadMain.setThemeResource(scene);

        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);
        stage.initOwner(this.stage);
        stage.setTitle(String.format("About %s", title));
        stage.show();
        closeBtn.requestFocus();
    }
}
