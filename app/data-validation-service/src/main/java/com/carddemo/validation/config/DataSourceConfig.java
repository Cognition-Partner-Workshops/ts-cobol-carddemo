package com.carddemo.validation.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configures the two data sources used by the validation service:
 * <ul>
 *   <li>{@code postgres} – the modernized PostgreSQL database (primary)</li>
 *   <li>{@code legacy}   – the DB2 / legacy database</li>
 * </ul>
 */
@Configuration
public class DataSourceConfig {

    // ── PostgreSQL (target / primary) ────────────────────────────────

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.postgres")
    public DataSourceProperties postgresDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource postgresDataSource() {
        return postgresDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "postgresJdbcTemplate")
    @Primary
    public JdbcTemplate postgresJdbcTemplate() {
        return new JdbcTemplate(postgresDataSource());
    }

    // ── DB2 / Legacy (source) ────────────────────────────────────────

    @Bean
    @ConfigurationProperties("spring.datasource.legacy")
    public DataSourceProperties legacyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource legacyDataSource() {
        return legacyDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "legacyJdbcTemplate")
    public JdbcTemplate legacyJdbcTemplate() {
        return new JdbcTemplate(legacyDataSource());
    }
}
