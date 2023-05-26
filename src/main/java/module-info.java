module ru.maxawergy.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.net.http;


    opens ru.maxawergy.desktop to javafx.fxml;
    exports ru.maxawergy.desktop;
}