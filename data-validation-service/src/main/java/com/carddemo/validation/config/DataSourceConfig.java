package com.carddemo.validation.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configures named {@link DataSource} instances and exposes them through a
 * registry so that validation services can look up the correct datasource
 * for each table pair.
 *
 * <p>Datasources are declared in YAML under {@code datasources.*}:
 * <pre>
 * datasources:
 *   legacy-db2:
 *     url: jdbc:db2://host:50000/CARDDB
 *     username: db2admin
 *     password: secret
 *     driver-class-name: com.ibm.db2.jcc.DB2Driver
 *   target-postgres:
 *     url: jdbc:postgresql://host:5432/carddemo
 *     username: pgadmin
 *     password: secret
 *     driver-class-name: org.postgresql.Driver
 * </pre>
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    /**
     * Map of datasource name to JDBC connection properties, populated from
     * the {@code datasources} YAML key.
     */
    @Bean
    @ConfigurationProperties(prefix = "datasources")
    public Map<String, DataSourceProperties> datasourceDefinitions() {
        return new HashMap<>();
    }

    /**
     * Registry of {@link JdbcTemplate} instances keyed by datasource name.
     *
     * <p>Datasources whose JDBC driver class is not on the classpath are
     * silently skipped so the application can still start in environments
     * where only a subset of databases is available (e.g. tests).</p>
     */
    @Bean
    public Map<String, JdbcTemplate> jdbcTemplateRegistry(
            Map<String, DataSourceProperties> datasourceDefinitions) {

        Map<String, JdbcTemplate> registry = new HashMap<>();
        for (Map.Entry<String, DataSourceProperties> entry : datasourceDefinitions.entrySet()) {
            DataSourceProperties props = entry.getValue();
            if (props.getUrl() == null || props.getUrl().isBlank()) {
                continue; // skip unconfigured entries
            }

            // Verify the driver class is available before attempting to build
            String driverClass = props.getDriverClassName();
            if (driverClass != null && !driverClass.isBlank()) {
                try {
                    Class.forName(driverClass);
                } catch (ClassNotFoundException e) {
                    log.warn("Skipping datasource '{}': driver class '{}' not found on classpath",
                            entry.getKey(), driverClass);
                    continue;
                }
            }

            try {
                DataSource ds = DataSourceBuilder.create()
                        .url(props.getUrl())
                        .username(props.getUsername())
                        .password(props.getPassword())
                        .driverClassName(driverClass)
                        .build();
                registry.put(entry.getKey(), new JdbcTemplate(ds));
                log.info("Registered datasource '{}'", entry.getKey());
            } catch (Exception e) {
                log.warn("Skipping datasource '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        return registry;
    }

    /**
     * Simple POJO holding JDBC connection properties for a single datasource.
     */
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }
}
