package com.carddemo.interest.batch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** FUNCTION CURRENT-DATE, honouring GnuCOBOL's COB_CURRENT_DATE so test runs are reproducible. */
@Configuration
public class ClockConfig {

    private static final DateTimeFormatter FROZEN = DateTimeFormatter.ofPattern("yyyy[-][/]MM[-][/]dd HH:mm:ss[.SS]");

    @Bean
    public Supplier<LocalDateTime> carddemoClock(@Value("${carddemo.clock:}") String frozen) {
        if (frozen == null || frozen.isBlank()) {
            return LocalDateTime::now;
        }
        LocalDateTime fixed = parse(frozen);
        return () -> fixed;
    }

    static LocalDateTime parse(String value) {
        return LocalDateTime.parse(value.trim(), FROZEN);
    }
}
