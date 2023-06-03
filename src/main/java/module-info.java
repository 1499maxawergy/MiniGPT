module ru.maxawergy.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.net.http;
    requires java.prefs;
    requires java.sql;
    requires org.xerial.sqlitejdbc;


    opens ru.maxawergy.desktop to javafx.fxml;
    exports ru.maxawergy.desktop;
}