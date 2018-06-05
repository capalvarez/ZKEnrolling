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

    public void insertImage(String rut, byte[] image) throws SQLException{
        String sql = "INSERT INTO Image (content, idUser) VALUES (?, " +
                "(SELECT idUser FROM User WHERE rut='" + rut + "'))";
        PreparedStatement statement = connection.prepareStatement(sql);

        Blob imageBlob = new SerialBlob(image);
        statement.setBlob(1, imageBlob);
        statement.execute();
    }

    public void insertTemplate(String rut, String template, int length) throws SQLException{
        String sqlUser = "INSERT INTO User (rut) SELECT * FROM (SELECT '" + rut + "') as tmp " +
                "WHERE NOT EXISTS(" +
                "SELECT rut FROM User WHERE rut='" + rut + "'" +
                ") LIMIT 1";

        Statement statement = connection.createStatement();
        statement.execute(sqlUser);

        String sqlTemplate = "INSERT INTO Template (template, length, user) VALUES ('" + template + "','" + length + "'," +
                "(SELECT idUser from User WHERE rut='" + rut + "'))";

        statement.execute(sqlTemplate);
    }
}
