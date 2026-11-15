package com.carddemo.interest.jdbc.load;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.LoadFigures;

/**
 * What loadExtractsStep writes to carddemo.interest_run (architecture.md sections 3 and 5): one
 * row per load, with status LOADED and the loaded counts and sums, or with status FAILED. The
 * column of a figure is named after its table or column: loaded_&lt;table&gt;_count, loaded_&lt;column&gt;_sum.
 */
public final class InterestRunLoadJournal {

    public static final String LOADED = "LOADED";
    public static final String FAILED = "FAILED";

    private final JdbcTemplate jdbc;

    public InterestRunLoadJournal(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** One figure of the load: the file and what its table holds. */
    public record Loaded(ExtractFile file, LoadFigures figures) {
    }

    /** The run id of the new row. */
    public long loaded(String parmDate, Instant startedAt, List<Loaded> tables) {
        List<String> columns = new ArrayList<>(List.of("parm_date", "started_at", "status"));
        List<Object> values = new ArrayList<>(List.of(parmDate, Timestamp.from(startedAt), LOADED));
        for (Loaded table : tables) {
            columns.add("loaded_" + table.file().table().substring("carddemo.".length()) + "_count");
            values.add(table.figures().count());
            table.figures().sums().forEach((column, sum) -> {
                columns.add("loaded_" + column + "_sum");
                values.add(sum);
            });
        }
        return insert(columns, values);
    }

    /** The run id of the new row. A failed load has no figures: the tables hold what they held. */
    public long failed(String parmDate, Instant startedAt, Instant finishedAt) {
        return insert(List.of("parm_date", "started_at", "finished_at", "status"),
                List.of(parmDate, Timestamp.from(startedAt), Timestamp.from(finishedAt), FAILED));
    }

    private long insert(List<String> columns, List<Object> values) {
        return jdbc.queryForObject("INSERT INTO carddemo.interest_run (" + String.join(", ", columns) + ") VALUES ("
                + columns.stream().map(column -> "?").collect(Collectors.joining(", ")) + ") RETURNING run_id",
                Long.class, values.toArray());
    }
}
