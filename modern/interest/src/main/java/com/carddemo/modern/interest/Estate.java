package com.carddemo.modern.interest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The legacy estate this job proves itself against: copybooks and the chain definition. */
@Component
public class Estate {

    public static final String CHAIN = "interest";

    private final Path root;
    private final JsonNode chain;

    public Estate(@Value("${estate.root:.}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        try {
            this.chain = new ObjectMapper().readTree(this.root.resolve("tools/runjcl/chains.json").toFile()).get(CHAIN);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path root() {
        return root;
    }

    public JsonNode chain() {
        return chain;
    }

    /** The chain's frozen clock ({@code COB_CURRENT_DATE}, e.g. {@code 2022-07-05 10:00:00.00}). */
    public Clock clock() {
        LocalDateTime t = LocalDateTime.parse(chain.get("clock").asText(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"));
        return Clock.fixed(t.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }
}
