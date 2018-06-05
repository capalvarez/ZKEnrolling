package utils;

public class DatabaseConfig {
    public String serverUrl;
    public String port;
    public String database;
    public String user;
    public String password;

    public DatabaseConfig(String server, String port, String db, String user, String password){
        this.serverUrl = server;
        this.port = port;
        this.database = db;
        this.user = user;
        this.password = password;
    }
}
