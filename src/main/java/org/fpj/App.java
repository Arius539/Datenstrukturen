package org.fpj;

import javafx.application.Application;
import org.fpj.database.Connection;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;

@SpringBootApplication
public class App {
    public static void main(String[] args) throws SQLException {
        Connection connect = new Connection();
        Connection conn = connect.getConnection();
        Application.launch(JavafxApp.class, args);
    }
}