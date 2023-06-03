package ru.maxawergy.desktop;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class MainUI extends Application {
    // Локальное значение токена, которое мы берем из реестра
    private static String TOKEN;
    // Локальное значение очистки поля запроса после его отправки
    private boolean CLEAR;
    // Локальное значение количества сохраняемых запросов в истории
    private int MAX_HISTORY = 10;
    // Все настройки пользователя храним в реестре приложения в ветке prefmain
    private final Preferences userPreferences = Preferences.userRoot().node("prefmain");
    // Подключение к базе данных SQLite
    private static Connection connection;
    // Путь к папке пользователя
    private static String locationUserHome = System.getProperty("user.home");
    // Путь к файлу БД
    private static String locationDB = locationUserHome + "/.minigpt/history.db";

    // Объект класса запроса по API

    public GPTRequest gptRequest = new GPTRequest();
    // Текстовое поле ввода запроса в UI
    private TextField prompt;
    // Кнопка замены настроек в UI
    private Button settingButton;
    // Кнопка просмотра/очистки истории в UI
    private Button historyButton;
    // Кнопка отправки запроса в UI
    private Button sendButton;
    // Поле вывода ответа на запрос
    private TextArea resultTextArea;


    public static void main(String[] args) {
        launch(args);
    }

    private void createDBFile() throws IOException {
        File miniGptDir = new File(locationUserHome + "/.minigpt");
        if (!miniGptDir.exists()) {
            miniGptDir.mkdir();
        }
        File dbFile = new File(locationUserHome + "/.minigpt/history.db");
        if (!dbFile.exists()){
            dbFile.createNewFile();
        }
    }

    private static void checkDrivers() {
        try {
            Class.forName("org.sqlite.JDBC");
            DriverManager.registerDriver(new org.sqlite.JDBC());
        } catch (ClassNotFoundException | SQLException classNotFoundException) {
            Logger.getAnonymousLogger().log(Level.SEVERE, LocalDateTime.now() + ": Could not start SQLite Drivers");
        }
    }

    private static Connection connect(String location) {
        String dbPrefix = "jdbc:sqlite:";
        Connection connection;
        try {
            connection = DriverManager.getConnection(dbPrefix + location);
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            Logger.getAnonymousLogger().log(Level.SEVERE,
                    LocalDateTime.now() + ": Could not connect to SQLite DB at " +
                            location);
            return null;
        }
        return connection;
    }

    private static void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS history (request TEXT, response TEXT)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("Error creating table history");
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Проверка драйверов SQLite
        checkDrivers();
        // Создание файла базы данных
        createDBFile();
        // Подключение к БД
        connection = connect(locationDB);
        // Проверка наличия таблицы history. Если ее нет, то создание таблицы history
        createTable();
        // Проверка наличия сохраненного значения
        if (userPreferences.get("token", null) != null
                && userPreferences.get("clear", null) != null
                && userPreferences.get("history", null) != null) {
            TOKEN = userPreferences.get("token", null);
            CLEAR = userPreferences.getBoolean("clear", true);
            MAX_HISTORY = userPreferences.getInt("history", 10);
        } else {
            // Если значение не было сохранено, показываем окно с вводом значения
            showSettingsDialog();
        }
        // Создаем корневой контейнер VBox
        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setSpacing(10);
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        // Создаем текстовое поле тела запроса
        prompt = new TextField();
        prompt.setStyle("-fx-background-color: #333333; -fx-text-fill: white;");
        prompt.setPromptText("Prompt");
        root.getChildren().add(prompt);

        // Создаем кнопку отправки
        sendButton = new Button("Send request");
        sendButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");

        // Кнопка замены токена
        settingButton = new Button("Settings");
        settingButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        settingButton.setOnAction(event -> showSettingsDialog());

        // Кнопка просмотра/очистки истории
        historyButton = new Button("History");
        historyButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        historyButton.setOnAction(event -> {
            try {
                showHistoryDialog();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        Button aboutButton = new Button("About");
        aboutButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        aboutButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("About");
            alert.setHeaderText(null);
            alert.setContentText("MiniGPT\n\nAuthor: 1499maxawergy\nVersion: 1.3\nGithub: https://github.com/1499maxawergy");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add("dialog.css");
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/icon.png")));
            // Отображение окна
            alert.showAndWait();
        });

        HBox hBox = new HBox(sendButton, settingButton, historyButton, aboutButton);
        hBox.setSpacing(10);
        root.getChildren().add(hBox);

        // Создаем текстовое поле для вывода результата
        resultTextArea = new TextArea();
        resultTextArea.setStyle("-fx-control-inner-background: #444444; -fx-text-fill: white; -fx-text-alignment: left");
        resultTextArea.setEditable(false);
        resultTextArea.setWrapText(true);

        primaryStage.heightProperty().addListener((observable, oldValue, newValue) -> {
            double sceneHeight = (double) newValue;
            double textAreaHeight = sceneHeight - 100; // Высота TextArea будет на 100 пикселей меньше высоты сцены
            resultTextArea.setPrefHeight(textAreaHeight);
        });

        root.getChildren().add(resultTextArea);

        // Обработчик нажатия на кнопку отправки
        sendButton.setOnAction(event -> {
            startRequestFromUI();
        });
        prompt.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !sendButton.isDisabled()) {
                startRequestFromUI();
            }
        });

        // Создаем сцену и задаем размеры
        Scene scene = new Scene(root, 400, 300);

        // Задаем заголовок окна
        primaryStage.setTitle("MiniChatGPT by 1499maxawergy");
        // Устанавливаем сцену на окно
        primaryStage.setScene(scene);
        // Устанавливаем иконку приложения на сцену
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        // Отображаем окно
        primaryStage.show();
    }

    private void startRequestFromUI() {
        // Отключаем кнопку на время запроса
        sendButton.setDisable(true);
        settingButton.setDisable(true);
        historyButton.setDisable(true);

        // Получаем текст из текстовых полей
        String promptString = prompt.getText();

        if (promptString.isBlank()) {
            resultTextArea.setText("Prompt area is blank. Write prompt and try again");
            sendButton.setDisable(false); // Включаем кнопку после получения ошибки
            settingButton.setDisable(false);
            historyButton.setDisable(false);
            return;
        }

        String request = prompt.getText();

        if (CLEAR) {
            prompt.setText("");
        }

        resultTextArea.setText("Waiting for response from ChatGPT...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Отправка запроса
                String result = getResponse(promptString);
                // Выводим результат в текстовое поле
                resultTextArea.setText(result);
                // Записываем запрос в БД
                addHistoryItem(request, result);
                return null;
            }
        };

        // Устанавливаем обработчики событий для задачи
        task.setOnSucceeded(e -> {
            // Здесь можно выполнить действия после успешного завершения запроса
            sendButton.setDisable(false); // Включаем кнопку после выполнения запроса
            settingButton.setDisable(false);
            historyButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            // Здесь можно выполнить действия при ошибке выполнения запроса
            sendButton.setDisable(false); // Включаем кнопку после выполнения запроса
            settingButton.setDisable(false);
            historyButton.setDisable(false);
        });

        // Запускаем задачу в новом фоновом потоке
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Помечаем поток как демон, чтобы он завершился при закрытии приложения
        thread.start();
    }

    private String getResponse(String prompt) {
        return gptRequest.getCompletion(TOKEN, prompt);
    }

    private void showHistoryDialog() throws SQLException {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("History");

        TextArea historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setWrapText(true);
        historyArea.setStyle("-fx-control-inner-background: #444444; -fx-text-fill: white; -fx-text-alignment: left");


        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            double sceneHeight = (double) newValue;
            double textAreaHeight = sceneHeight - 100; // Высота TextArea будет на 100 пикселей меньше высоты сцены
            historyArea.setPrefHeight(textAreaHeight);
        });

        StringBuilder historyRequested = new StringBuilder();
        List<String []> allHistory = getAllHistory();
        for (String[] reqAndResp : allHistory) {
            historyRequested.append("Request: ").append(reqAndResp[0]).append("\nResponse: ").append(reqAndResp[1]).append("\n\n");
        }

        historyArea.setText(historyRequested.toString());

        Button clearButton = new Button("Clear history");
        clearButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-alignment: bottom-right");
        clearButton.setOnAction(event -> {
            try {
                clearHistory();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            stage.close();
        });
        Button submitButton = new Button("OK");
        submitButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-alignment: bottom-right");
        submitButton.setOnAction(event -> {
            stage.close();
        });

        HBox buttons = new HBox();
//        buttons.setPadding(new Insets(10));
        buttons.setSpacing(10);
        buttons.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        buttons.getChildren().addAll(clearButton, submitButton);

        VBox settings = new VBox();
        settings.setPadding(new Insets(10));
        settings.setSpacing(10);
        settings.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        settings.getChildren().addAll(historyArea, buttons);
        Scene scene = new Scene(settings, 400, 300);
        stage.setScene(scene);
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/icon.png")));
        stage.showAndWait();
    }

    private void showSettingsDialog() {
        // Создание диалогового окна с полями ввода
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Settings");

        TextField tokenField = new TextField();
        tokenField.setPromptText("Token");
        tokenField.setText(userPreferences.get("token", ""));

        Label tokenLabel = new Label("Enter your API token from ChatGPT");
        tokenLabel.setLabelFor(tokenField);
        tokenLabel.setStyle("-fx-text-fill: white;");

        TextField historyMaxField = new TextField();
        historyMaxField.setPromptText("Max history elements");
        historyMaxField.setText(userPreferences.get("history", "10"));

        Label historyLabel = new Label("Enter how many requests save to history (Default: 10)");
        historyLabel.setLabelFor(historyMaxField);
        historyLabel.setStyle("-fx-text-fill: white;");

        CheckBox promptClear = new CheckBox("Clear prompt after sending request");
        promptClear.setStyle("-fx-text-fill: white;");
        if (userPreferences.getBoolean("clear", true)) {
            promptClear.fire();
        }

        Button submitButton = new Button("Submit");
        submitButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-alignment: bottom-right");
        submitButton.setOnAction(event -> {
            TOKEN = tokenField.getText();
            userPreferences.put("token", tokenField.getText());
            CLEAR = promptClear.isSelected();
            userPreferences.putBoolean("clear", promptClear.isSelected());
            MAX_HISTORY = Integer.parseInt(historyMaxField.getText());
            userPreferences.putInt("history", Integer.parseInt(historyMaxField.getText()));
            stage.close();
        });

        VBox settings = new VBox();
        settings.setPadding(new Insets(10));
        settings.setSpacing(10);
        settings.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        settings.getChildren().addAll(tokenLabel, tokenField, historyLabel, historyMaxField, promptClear, submitButton);
        Scene scene = new Scene(settings, 300, 240);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/icon.png")));
        stage.showAndWait();
    }

    public List<String[]> getAllHistory() throws SQLException {
        List<String[]> historyList = new ArrayList<>();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT rowid, request, response FROM history ORDER BY rowid DESC");
        while (rs.next()) {
            String[] historyItem = new String[]{rs.getString("request"), rs.getString("response")};
            historyList.add(historyItem);
        }
        stmt.close();
        return historyList;
    }

    public void addHistoryItem(String request, String response) throws SQLException {
        int currentCount = getAllHistory().size();
        if (currentCount >= MAX_HISTORY) {
            PreparedStatement pstmt = connection.prepareStatement("DELETE FROM history WHERE rowid = (SELECT MIN(rowid) FROM history)");
            pstmt.executeUpdate();
            pstmt.close();
        }
        PreparedStatement pstmt = connection.prepareStatement("INSERT INTO history(request, response) VALUES(?, ?)");
        pstmt.setString(1, request);
        pstmt.setString(2, response);
        pstmt.executeUpdate();
        pstmt.close();
    }

    public void clearHistory() throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement("DELETE FROM history");
        pstmt.executeUpdate();
        pstmt.close();
    }
}