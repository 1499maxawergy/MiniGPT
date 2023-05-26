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
import javafx.stage.Stage;

import java.io.*;
import java.util.Optional;

public class MainUI extends Application {

    private static final String FILE_PATH = "token.txt";
    private static String TOKEN;

    public GPTRequest gptRequest = new GPTRequest();
    private TextField prompt;
    private Button tokenButton;
    private Button sendButton;
    private TextArea resultTextArea;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Создаем корневой контейнер VBox

        // Проверка наличия сохраненного значения
        String savedValue = loadValueFromFile();
        if (savedValue != null) {
            TOKEN = savedValue;
        } else {
            // Если значение не было сохранено, показываем окно с вводом значения
            showSettingsDialog();
        }

        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setSpacing(10);
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        // Создаем текстовое поле 2
        prompt = new TextField();
        prompt.setStyle("-fx-background-color: #333333; -fx-text-fill: white;");
        prompt.setPromptText("Prompt");
        root.getChildren().add(prompt);

        // Создаем кнопку отправки
        sendButton = new Button("Send request");
        sendButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");

        // Кнопка замены токена
        tokenButton = new Button("Token");
        tokenButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        tokenButton.setOnAction(event -> showSettingsDialog());

        Button aboutButton = new Button("About");
        aboutButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        aboutButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("About");
            alert.setHeaderText(null);
            alert.setContentText("MiniGPT\n\nAuthor: 1499maxawergy\nVersion: 1.2\nGithub: https://github.com/1499maxawergy");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add("dialog.css");
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/icon.png")));
            // Отображение окна
            alert.showAndWait();
        });

        HBox hBox = new HBox(sendButton, tokenButton, aboutButton);
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
            if (event.getCode() == KeyCode.ENTER) {
                startRequestFromUI();
            }
        });

        // Создаем сцену и задаем размеры
        Scene scene = new Scene(root, 400, 300);

        // Задаем заголовок окна
        primaryStage.setTitle("MiniChatGPT by 1499maxawergy");

        // Устанавливаем сцену на окно
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        // Отображаем окно
        primaryStage.show();
    }

    private void startRequestFromUI() {
        // Отключаем кнопку на время запроса
        sendButton.setDisable(true);
        tokenButton.setDisable(true);

        // Получаем текст из текстовых полей
        String promptString = prompt.getText();

        if (promptString.isBlank()) {
            resultTextArea.setText("Prompt area is blank. Write prompt and try again");
            return;
        }

        prompt.setText("");
        resultTextArea.setText("Request is compiling. Wait...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Отправка запроса
                String result = getResponse(promptString);
                // Выводим результат в текстовое поле
                resultTextArea.setText(result);
                return null;
            }
        };

        // Устанавливаем обработчики событий для задачи
        task.setOnSucceeded(e -> {
            // Здесь можно выполнить действия после успешного завершения запроса
            sendButton.setDisable(false); // Включаем кнопку после выполнения запроса
            tokenButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            // Здесь можно выполнить действия при ошибке выполнения запроса
            sendButton.setDisable(false); // Включаем кнопку после выполнения запроса
            tokenButton.setDisable(false);
        });

        // Запускаем задачу в новом фоновом потоке
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Помечаем поток как демон, чтобы он завершился при закрытии приложения
        thread.start();
    }

    private String getResponse(String prompt) {
        return gptRequest.getCompletion(TOKEN, prompt);
    }

    private void showSettingsDialog() {
        // Создание диалогового окна с полем ввода
        TextInputDialog dialog = new TextInputDialog(TOKEN);
        dialog.setTitle("Token input");
        dialog.setHeaderText("Enter your ChatGPT API token");

        // Установка стилей
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add("dialog.css");
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/icon.png")));

        // Отображение диалогового окна и ожидание результата
        Optional<String> result = dialog.showAndWait();

        // Обработка введенного значения
        result.ifPresent(this::saveValueToFile);
    }

    private void saveValueToFile(String value) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            writer.write(value);
            TOKEN = value;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadValueFromFile() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
                return reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}