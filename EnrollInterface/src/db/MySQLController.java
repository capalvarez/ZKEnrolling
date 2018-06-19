package db;

import utils.DatabaseConfig;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.*;


public class MySQLController {
    private Connection connection = null;

    public MySQLController(DatabaseConfig config) throws ClassNotFoundException{
        Class.forName("com.mysql.jdbc.Driver");

        String completeURL = "jdbc:mysql://" +config.serverUrl + ":" + config.port + "/" + config.database;

        try{
            connection = DriverManager.getConnection(completeURL, config.user, config.password);
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void insertTemplate(String rut, String template, int length, int finger) throws SQLException{
        String sqlUser = "INSERT INTO User (rut) SELECT * FROM (SELECT '" + rut + "') as tmp " +
                "WHERE NOT EXISTS(" +
                "SELECT rut FROM User WHERE rut='" + rut + "'" +
                ") LIMIT 1";

        Statement statement = connection.createStatement();
        statement.execute(sqlUser);

        String sqlTemplate = "INSERT INTO Template (template, length, finger, user) VALUES ('" + template + "','" + length + "','" + finger + "'," +
        "(SELECT idUser from User WHERE rut='" + rut + "'))";

        statement.execute(sqlTemplate);
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
