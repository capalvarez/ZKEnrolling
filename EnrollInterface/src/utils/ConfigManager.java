package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    Properties config;

    public ConfigManager(String configFilePath){
        this.config = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(configFilePath);
            config.load(input);
        } catch (IOException e){
            System.out.println("No config file found!");
        }
    }

    public DatabaseConfig getDBConfig(){
        String server = this.config.getProperty("SERVER_URL");
        String port = this.config.getProperty("PORT");
        String database = this.config.getProperty("DATABASE");
        String user = this.config.getProperty("USER");
        String password = this.config.getProperty("PASSWORD");

        return new DatabaseConfig(server, port, database, user, password);
    }

    public String getLocalStoragePath(){
        String localStorage = this.config.getProperty("FINGERPRINT_LOCAL_STORAGE");

        return localStorage;
    }
}

