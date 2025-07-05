package me.ray.aethelgardRPG.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final AethelgardRPG plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(AethelgardRPG plugin) {
        this.plugin = plugin;
        setupHikariCP();
    }

    private void setupHikariCP() {
        FileConfiguration config = plugin.getConfigManager().getDatabaseConfig(); // Use database.yml

        if (!config.getBoolean("enabled", false)) { // Corrected path for database.yml
            plugin.getLogger().info("Database is disabled in database.yml. Skipping connection."); // Corrected log message
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();
        // Configurações essenciais (ainda lidas do config.yml)
        String driver = config.getString("driver", "org.sqlite.JDBC"); // Default para SQLite se não especificado
        hikariConfig.setDriverClassName(driver);

        if (driver.contains("sqlite")) {
            hikariConfig.setJdbcUrl(config.getString("url", "jdbc:sqlite:" + plugin.getDataFolder().getPath() + "/database.db"));
            // SQLite não precisa de usuário/senha na maioria das configurações
        } else {
            String baseUrl = config.getString("url", "jdbc:mysql://localhost:3306/aethelgardrpg");
            if (driver.toLowerCase().contains("mysql") || driver.toLowerCase().contains("mariadb")) {
                StringBuilder finalUrl = new StringBuilder(baseUrl);
                boolean hasParams = baseUrl.contains("?");

                if (!baseUrl.toLowerCase().contains("autoreconnect=")) {
                    finalUrl.append(hasParams ? "&" : "?").append("autoReconnect=true");
                    hasParams = true; // Garante que o próximo parâmetro use '&'
                }
                if (!baseUrl.toLowerCase().contains("usessl=")) {
                    finalUrl.append(hasParams ? "&" : "?").append("useSSL=false");
                }
                hikariConfig.setJdbcUrl(finalUrl.toString());
            } else {
                // Para outros drivers (não SQLite, não MySQL/MariaDB), usa a URL como está
                hikariConfig.setJdbcUrl(baseUrl);
            }
            hikariConfig.setUsername(config.getString("username", "root"));
            hikariConfig.setPassword(config.getString("password", "password"));
        }

        // Pool settings
        hikariConfig.setMaximumPoolSize(config.getInt("pool-settings.max-pool-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("pool-settings.min-idle", 5));
        hikariConfig.setConnectionTimeout(config.getLong("pool-settings.connection-timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("pool-settings.idle-timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("pool-settings.max-lifetime", 1800000));

        // Recommended properties for MySQL/MariaDB (aplicar apenas se for o driver correspondente)
        if (driver.toLowerCase().contains("mysql") || driver.toLowerCase().contains("mariadb")) {
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        }

        try {
            dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("Database connection pool initialized successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database connection pool: " + e.getMessage());
            dataSource = null; // Ensure dataSource is null if setup fails
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database is not connected or not enabled.");
        }
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}