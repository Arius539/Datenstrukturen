package org.fpj.database;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class Connection {
private Connection conn;

    public Connection getConnection() throws SQLException {
        if (this.conn == null) {


    String url = "jdbc:postgresql://kdb.sh:6082/fpj_2025_g1";
    Properties props = new Properties();
    props.setProperty("user", "fpj_2025_g1");
    props.setProperty("password", "modulFPJ_Projekt_813");
    Connection connect = (Connection) DriverManager.getConnection(url, props);
    this.conn = connect;
    return connect;
} else { return this.conn; }
    }
}
