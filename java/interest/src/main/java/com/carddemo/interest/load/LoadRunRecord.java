package com.carddemo.interest.load;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The loader's part of carddemo.interest_run: the row of the run, the loaded_* figures of each
 * table as it is replaced (RC1-RC4) and the status the load ends with. The figure columns are
 * named after the table and the summed column (db/postgres/02-interest.sql, FI-4).
 */
final class LoadRunRecord {

    static final String STATUS_LOADING = "LOADING";
    static final String STATUS_LOADED = "LOADED";
    static final String STATUS_FAILED = "FAILED";

    private static final String RUN_TABLE = ExtractFile.SCHEMA + ".interest_run";
    private static final int SUM_SCALE = 2;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    LoadRunRecord(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    long start(String parmDate) {
        return jdbc.queryForObject("INSERT INTO " + RUN_TABLE + " (parm_date, started_at, status,"
                + " loaded_g02_difference_count, loaded_g03_difference_count) VALUES (?, ?, ?, 0, 0) RETURNING run_id",
                Long.class, parmDate, Timestamp.from(clock.instant()), STATUS_LOADING);
    }

    void recordTable(long runId, ExtractFile file, TableFigures figures, long g02Differences, long g03Differences) {
        StringBuilder sql = new StringBuilder("UPDATE " + RUN_TABLE + " SET loaded_" + file.table() + "_count = ?, loaded_"
                + file.table() + "_key_count = ?, loaded_g02_difference_count = loaded_g02_difference_count + ?,"
                + " loaded_g03_difference_count = loaded_g03_difference_count + ?");
        List<Object> arguments = new ArrayList<>(List.of(figures.rowCount(), figures.keyCount(), g02Differences, g03Differences));
        for (Map.Entry<String, BigDecimal> sum : figures.sums().entrySet()) {
            sql.append(", loaded_").append(sum.getKey()).append("_sum = ?");
            // Sums of scale-2 amounts and of whole ids: nothing to round (standards S3.3).
            arguments.add(sum.getValue().setScale(SUM_SCALE, RoundingMode.UNNECESSARY));
        }
        sql.append(" WHERE run_id = ?");
        arguments.add(runId);
        jdbc.update(sql.toString(), arguments.toArray());
    }

    /** The load is done; the run goes on with the calculation, so finished_at stays empty. */
    void markLoaded(long runId) {
        jdbc.update("UPDATE " + RUN_TABLE + " SET status = ? WHERE run_id = ?", STATUS_LOADED, runId);
    }

    void markFailed(long runId) {
        jdbc.update("UPDATE " + RUN_TABLE + " SET status = ?, finished_at = ? WHERE run_id = ?",
                STATUS_FAILED, Timestamp.from(clock.instant()), runId);
    }
}
