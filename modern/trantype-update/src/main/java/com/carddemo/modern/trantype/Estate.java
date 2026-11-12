package com.carddemo.modern.trantype;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The legacy estate this job proves itself against: the chain definition in chains.json. */
@Component
public class Estate {

    public static final String CHAIN = "trantype-update";

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

    /** LRECL of an input dataset ({@code inputs} in chains.json). */
    public int lrecl(String dsn) {
        return chain.get("inputs").get(dsn).asInt();
    }

    /** {@code database.seed}, relative to the case's {@code input/} directory. */
    public String seed() {
        return chain.get("database").get("seed").asText();
    }

    /** {@code database.tables}: snapshot table -> ORDER BY columns, in chains.json order. */
    public Map<String, String> tables() {
        Map<String, String> tables = new LinkedHashMap<>();
        chain.get("database").get("tables").fields().forEachRemaining(e -> tables.put(e.getKey(), e.getValue().asText()));
        return tables;
    }
}
