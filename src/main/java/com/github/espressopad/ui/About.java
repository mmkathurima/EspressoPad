package com.github.espressopad.ui;

import com.github.espressopad.utils.Utils;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.HyperlinkLabel;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class About extends Application {
    private final Stage stage;

    public About(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void start(Stage stage) {
        String title = this.stage.getTitle();
        TableView<Map<String, String>> tableView = new TableView<>();
        VBox dependencyView = new VBox();

        tableView.setEditable(false);

        TableColumn<Map<String, String>, String> keyColumn = new TableColumn<>("Property Key");
        keyColumn.setCellValueFactory(new MapValueFactory("key"));

        TableColumn<Map<String, String>, String> valueColumn = new TableColumn<>("Property Value");
        valueColumn.setCellValueFactory(new MapValueFactory("value"));

        List<Map<String, String>> properties = System.getProperties()
                .entrySet()
                .stream()
                .map(x -> Map.of("key", String.valueOf(x.getKey()),
                        "value", String.valueOf(x.getValue())))
                .filter(x -> !Objects.equals(x.get("key"), "line.separator"))
                .collect(Collectors.toList());

        tableView.getColumns().addAll(keyColumn, valueColumn);
        tableView.getItems().addAll(properties);
        tableView.getSortOrder().setAll(keyColumn);

        Label productName = new Label(title);
        productName.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.EXTRA_BOLD, 36d));
        productName.setAlignment(Pos.CENTER);

        Label details = new Label(String.format("v0.23\n©%d\nRuntime: %s %s %s\nVM: %s", Year.now().getValue(),
                System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"),
                System.getProperty("os.arch"), System.getProperty("java.vm.name")));
        details.setAlignment(Pos.CENTER);
        details.setPadding(new Insets(0, 0, 5, 0));

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
        githubLinkBtn.setPadding(new Insets(3, 5, 3, 5));

        HBox btnBox = new HBox(closeBtn, githubLinkBtn);
        btnBox.setSpacing(10d);

        Label t = new Label(String.format("%s uses the following libraries:\n", this.stage.getTitle()));
        HyperlinkLabel ikonli = new HyperlinkLabel("● [Ikonli by Kordamp]");
        ikonli.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource() instanceof Hyperlink) {
                    try {
                        Desktop.getDesktop().browse(new URL("https://github.com/kordamp/ikonli").toURI());
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        HyperlinkLabel controlsfx = new HyperlinkLabel("● [ControlsFX]");
        controlsfx.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource() instanceof Hyperlink) {
                    try {
                        Desktop.getDesktop().browse(new URL("https://github.com/controlsfx/controlsfx").toURI());
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        HyperlinkLabel richtext = new HyperlinkLabel("● [RichtextFX by FXMisc]");
        richtext.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource() instanceof Hyperlink) {
                    try {
                        Desktop.getDesktop().browse(new URL("https://github.com/FXMisc/RichTextFX").toURI());
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        HyperlinkLabel mavenArcheologist = new HyperlinkLabel("● [Maven Archeologist by Square]");
        mavenArcheologist.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource() instanceof Hyperlink) {
                    try {
                        Desktop.getDesktop().browse(new URL("https://github.com/square/maven-archeologist").toURI());
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        HyperlinkLabel jastyle = new HyperlinkLabel("● [jAstyle by AbrarSyed]");
        jastyle.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource() instanceof Hyperlink) {
                    try {
                        Desktop.getDesktop().browse(new URL("https://github.com/AbrarSyed/jastyle").toURI());
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        HyperlinkLabel jsystemthemedetector = new HyperlinkLabel("● [jSystemThemeDetector by Dansoftowner]");
        jsystemthemedetector.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource() instanceof Hyperlink) {
                    try {
                        Desktop.getDesktop()
                                .browse(new URL("https://github.com/Dansoftowner/jSystemThemeDetector").toURI());
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        HyperlinkLabel jsoup = new HyperlinkLabel("● [jsoup by jhy]");
        jsoup.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource() instanceof Hyperlink) {
                    try {
                        Desktop.getDesktop().browse(new URL("https://jsoup.org/").toURI());
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        HyperlinkLabel unbescape = new HyperlinkLabel("● [unbescape]");
        unbescape.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (event.getSource() instanceof Hyperlink) {
                    try {
                        Desktop.getDesktop().browse(new URL("https://www.unbescape.org/").toURI());
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        dependencyView.getChildren().addAll(t, ikonli, controlsfx, richtext, mavenArcheologist, jastyle,
                jsystemthemedetector, jsoup, unbescape);
        TextField filterProperty = new TextField();
        filterProperty.setPromptText("Filter properties");
        filterProperty.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                tableView.getItems().setAll(properties.stream()
                        .filter(x -> x.entrySet()
                                .stream()
                                .anyMatch(y -> y.getKey().contains(filterProperty.getText())
                                        || y.getValue().contains(filterProperty.getText())))
                        .collect(Collectors.toList()));
                tableView.getSortOrder().setAll(keyColumn);
            }
        });

        VBox vBox = new VBox(productName, details);
        //vBox.setStyle("-fx-padding: 5px 1em;-fx-border-insets: 5px;-fx-background-insets: 5px;");

        VBox propertyBox = new VBox(filterProperty, tableView);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("Properties", propertyBox),
                new Tab("Libraries used", dependencyView));
        for (Tab x : tabPane.getTabs())
            x.setClosable(false);

        VBox box = new VBox(vBox, tabPane, btnBox);
        box.setPrefWidth(500d);

        for (VBox v : new VBox[]{propertyBox, box, dependencyView}) {
            v.setSpacing(15d);
            v.setPadding(new Insets(15d, 10d, 5d, 10d));
        }

        ScrollPane scrollPane = new ScrollPane(box);
        scrollPane.setPrefWidth(515);

        Scene scene = new Scene(scrollPane);
        Utils.setThemeResource(scene);

        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setMaxWidth(550);
        stage.initOwner(this.stage);
        stage.setTitle(String.format("About %s", title));
        stage.show();
        closeBtn.requestFocus();
    }
}
