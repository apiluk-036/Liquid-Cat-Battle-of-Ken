import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

public class Main extends Application {

    private static final double WIDTH = 960;
    private static final double HEIGHT = 640;

    private static final String IMG_BACKGROUND = "/assets/images/background";
    private static final String IMG_LOGO       = "/assets/images/logo";
    private static final String IMG_PLAY       = "/assets/images/play";
    private static final String IMG_SETTING    = "/assets/images/setting";
    private static final String SND_BGM        = "/assets/sounds/bgfighting";
    private static final String SND_CLICK      = "/assets/sounds/click-sound";

    private static final String[] IMAGE_EXTS = {"png", "jpg", "jpeg", "webp", "gif"};
    private static final String[] SOUND_EXTS = {"mp3", "wav", "m4a", "aac", "aif", "aiff"};

    private MediaPlayer bgmPlayer;
    private AudioClip clickSound;
    private StackPane settingsPanel;

    @Override
    public void start(Stage primaryStage) {
        initSounds();

        Pane root = new Pane();
        root.setPrefSize(WIDTH, HEIGHT);

        // background
        ImageView bgView = new ImageView(loadImage(IMG_BACKGROUND));
        bgView.setFitWidth(WIDTH);
        bgView.setFitHeight(HEIGHT);

        // logo
        double logoWidth = 820;
        ImageView logoView = new ImageView(loadImage(IMG_LOGO));
        logoView.setFitWidth(logoWidth);
        logoView.setPreserveRatio(true);
        logoView.setLayoutX((WIDTH - logoWidth) / 2);
        logoView.setLayoutY(15);

        // button
        ImageView playBtn = createImageButton(IMG_PLAY, 270, 430, this::onPlay);
        ImageView settingsBtn = createImageButton(IMG_SETTING, 210, 535, this::onSettings);

        // setting
        settingsPanel = createSettingsPanel();

        root.getChildren().addAll(bgView, logoView, playBtn, settingsBtn, settingsPanel);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setTitle("Liquid Cat Battle of Ken");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // image
    private ImageView createImageButton(String basePath, double width, double y, Runnable action) {
        ImageView btn = new ImageView(loadImage(basePath));
        btn.setFitWidth(width);
        btn.setPreserveRatio(true);
        btn.setLayoutX((WIDTH - width) / 2);
        btn.setLayoutY(y);
        btn.setCursor(Cursor.HAND);

        btn.setOnMouseEntered(e -> { btn.setScaleX(1.08); btn.setScaleY(1.08); });
        btn.setOnMouseExited(e -> { btn.setScaleX(1.0); btn.setScaleY(1.0); });
        btn.setOnMousePressed(e -> {
            playClick();   // play sound immediately on press
            btn.setScaleX(0.95);
            btn.setScaleY(0.95);
        });
        btn.setOnMouseReleased(e -> { btn.setScaleX(1.08); btn.setScaleY(1.08); });

        btn.setOnMouseClicked(e -> action.run());
        return btn;
    }

    // setting
    private static final String BTN_STYLE =
            "-fx-background-color: linear-gradient(to bottom, #ffc857, #e8890c);"
                    + "-fx-background-radius: 9; -fx-border-color: #b8670a; -fx-border-radius: 9;"
                    + "-fx-border-width: 2; -fx-text-fill: white; -fx-font-size: 20px;"
                    + "-fx-font-weight: bold; -fx-min-width: 40; -fx-min-height: 36;"
                    + "-fx-padding: 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 4, 0.2, 0, 2);";
    private static final String BTN_STYLE_HOVER =
            BTN_STYLE.replace("#ffc857, #e8890c", "#ffd982, #f39a1e");

    private StackPane createSettingsPanel() {
        // ----- Title -----
        Label title = new Label("SETTINGS");
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #5a3a1c;");

        Region divider = new Region();
        divider.setPrefHeight(2);
        divider.setMaxWidth(260);
        divider.setStyle("-fx-background-color: linear-gradient(to right, transparent, #c9a96a, transparent);");

        // ----- Music slider (bgfighting) -----
        Slider musicSlider = new Slider(0, 1, 0.5);
        musicSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (bgmPlayer != null) bgmPlayer.setVolume(newV.doubleValue());
        });

        // ----- Sound slider (click-sound) -----
        Slider soundSlider = new Slider(0, 1, 0.8);
        soundSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (clickSound != null) clickSound.setVolume(newV.doubleValue());
        });

        VBox content = new VBox(14, title, divider,
                createVolumeRow("Music", musicSlider),
                createVolumeRow("Sound", soundSlider));
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(22, 26, 22, 26));

        // ----- Close (X) button, top-right corner -----
        Button closeBtn = new Button("\u2715");
        closeBtn.setCursor(Cursor.HAND);
        String xStyle = "-fx-background-color: transparent; -fx-font-size: 20px;"
                + "-fx-font-weight: bold; -fx-text-fill: ";
        closeBtn.setStyle(xStyle + "#7a4a1a;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(xStyle + "#e8890c;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(xStyle + "#7a4a1a;"));
        closeBtn.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> playClick());   // play sound immediately on press
        closeBtn.setOnAction(e -> settingsPanel.setVisible(false));
        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(6, 10, 0, 0));

        // ----- Inner paper -----
        StackPane paper = new StackPane(content, closeBtn);
        paper.setStyle("-fx-background-color: radial-gradient(center 50% 45%, radius 75%, #fffaf0, #f1deaa);"
                + "-fx-background-radius: 12; -fx-border-color: #c9a96a;"
                + "-fx-border-radius: 12; -fx-border-width: 2;");

        // ----- Outer wooden frame -----
        StackPane frame = new StackPane(paper);
        frame.setPadding(new Insets(16));
        frame.setPrefSize(430, 330);
        frame.setMaxSize(430, 330);
        frame.setLayoutX((WIDTH - 430) / 2);
        frame.setLayoutY((HEIGHT - 330) / 2);
        frame.setStyle("-fx-background-color: linear-gradient(to bottom, #b07a36, #6b4423);"
                + "-fx-background-radius: 22; -fx-border-color: #3f2610;"
                + "-fx-border-radius: 22; -fx-border-width: 4;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0.3, 0, 6);");
        frame.setVisible(false);
        return frame;
    }

    // Volume row: label + [ - ] slider [ + ]
    private VBox createVolumeRow(String name, Slider slider) {
        Label label = new Label(name);
        label.setStyle("-fx-font-size: 18px; -fx-text-fill: #8a7a60;");

        slider.setPrefWidth(240);
        styleSlider(slider);

        Button minus = createRoundButton("\u2212");
        minus.setOnAction(e -> slider.setValue(slider.getValue() - 0.1));

        Button plus = createRoundButton("+");
        plus.setOnAction(e -> slider.setValue(slider.getValue() + 0.1));

        HBox line = new HBox(14, minus, slider, plus);
        line.setAlignment(Pos.CENTER);
        HBox.setHgrow(slider, Priority.ALWAYS);

        VBox row = new VBox(4, label, line);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Button createRoundButton(String text) {
        Button b = new Button(text);
        b.setStyle(BTN_STYLE);
        b.setCursor(Cursor.HAND);
        b.setOnMouseEntered(e -> b.setStyle(BTN_STYLE_HOVER));
        b.setOnMouseExited(e -> b.setStyle(BTN_STYLE));
        b.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> playClick());   // play sound immediately on press
        return b;
    }

    // Style the slider as a wooden line with a round orange knob
    private void styleSlider(Slider s) {
        Runnable apply = () -> {
            Node track = s.lookup(".track");
            Node thumb = s.lookup(".thumb");
            if (track != null) {
                track.setStyle("-fx-background-color: #8b6a3e; -fx-background-radius: 4;"
                        + "-fx-background-insets: 0; -fx-padding: 3;");
            }
            if (thumb != null) {
                thumb.setStyle("-fx-background-color: radial-gradient(center 40% 35%, radius 70%, #ffd27a, #e8890c);"
                        + "-fx-background-radius: 20; -fx-background-insets: 0;"
                        + "-fx-border-color: #b8670a; -fx-border-radius: 20; -fx-border-width: 2;"
                        + "-fx-padding: 8;");
            }
        };
        s.skinProperty().addListener((obs, oldSkin, newSkin) -> apply.run());
        apply.run();
    }

    // ---------- Button actions ----------
    private void onPlay() {
        System.out.println("PLAY clicked -> go to the game screen (not implemented yet)");
        // TODO: switch to the game Scene here
    }

    private void onSettings() {
        settingsPanel.setVisible(true);
        settingsPanel.toFront();
    }

    // ---------- Sounds ----------
    private void initSounds() {
        try {
            URL bgmUrl = findResource(SND_BGM, SOUND_EXTS);
            if (bgmUrl != null) {
                bgmPlayer = new MediaPlayer(new Media(bgmUrl.toExternalForm()));
                bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                bgmPlayer.setVolume(0.5);
                bgmPlayer.play();
            } else {
                System.err.println("Music file not found: " + SND_BGM + ".(mp3/wav/m4a)");
            }

            URL clickUrl = findResource(SND_CLICK, SOUND_EXTS);
            if (clickUrl != null) {
                clickSound = new AudioClip(clickUrl.toExternalForm());
                clickSound.setVolume(0.8);
                clickSound.play(0.0);   // play once silently to warm up the audio system
            } else {
                System.err.println("Click sound file not found: " + SND_CLICK + ".(mp3/wav/m4a)");
            }
        } catch (Exception e) {
            // if a sound file has a problem, the game can still start normally
            System.err.println("Failed to load sound: " + e.getMessage());
        }
    }

    private void playClick() {
        if (clickSound != null) clickSound.play();
    }

    // ---------- Load images ----------
    private Image loadImage(String basePath) {
        URL url = findResource(basePath, IMAGE_EXTS);
        if (url == null) {
            throw new IllegalStateException("Image file not found: " + basePath
                    + ".(png/jpg/jpeg/webp). Check the file name and the resources folder.");
        }
        return new Image(url.toExternalForm());
    }

    // Try each possible file extension one by one
    private URL findResource(String basePath, String[] exts) {
        // Method 1: look on the classpath (with and without the /resources prefix)
        String[] prefixes = {"", "/resources"};
        for (String prefix : prefixes) {
            for (String ext : exts) {
                URL url = Main.class.getResource(prefix + basePath + "." + ext);
                if (url != null) return url;
            }
        }
        // Method 2: if still not found, read directly from the project's src/resources folder
        for (String ext : exts) {
            File f = new File("src/resources" + basePath + "." + ext);
            if (f.exists()) {
                try {
                    return f.toURI().toURL();
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}