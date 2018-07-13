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


    public void insertUserIfNotExists(String rut) throws SQLException{
        String sqlUser = "INSERT INTO User (rut) SELECT * FROM (SELECT '" + rut + "') as tmp " +
                "WHERE NOT EXISTS(" +
                "SELECT rut FROM User WHERE rut='" + rut + "'" +
                ") LIMIT 1";

        Statement statement = connection.createStatement();
        statement.execute(sqlUser);
    }

    public void insertTemplate(String rut, String template, int length, int finger) throws SQLException{
        this.insertUserIfNotExists(rut);
        Statement statement = connection.createStatement();

        String previousTmpSql = "SELECT idTemplate FROM Template WHERE user = (SELECT idUser from User WHERE rut='" + rut + "') AND finger = '" + (finger+1)  +"'";
        statement.execute(previousTmpSql);

        ResultSet resultSet = statement.getResultSet();
        int templateId = -1;

        while(resultSet.next()){
            templateId = resultSet.getInt("idTemplate");
        }

        String sqlTemplate;
        if(templateId < 0){
            sqlTemplate = "INSERT INTO Template (template, length, finger, user) VALUES ('" + template + "','" + length + "','" + (finger+1) + "'," +
                    "(SELECT idUser from User WHERE rut='" + rut + "'))";
        } else {
            sqlTemplate = "UPDATE Template SET template ='" + template + "', length='" + length + "' WHERE idTemplate = '" + templateId +"'";
        }

        statement.execute(sqlTemplate);
    }

    public void insertPassword(String rut, String password) throws SQLException {
        this.insertUserIfNotExists(rut);

        Statement statement = connection.createStatement();
        String sqlSetPassword = "UPDATE User SET password='" + password + "' WHERE rut='"+ rut +"'";
        statement.execute(sqlSetPassword);
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
