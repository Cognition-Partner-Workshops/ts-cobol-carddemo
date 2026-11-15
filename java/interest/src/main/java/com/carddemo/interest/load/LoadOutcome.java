package com.carddemo.interest.load;

import java.util.List;

/**
 * What one load did. Loaded: the figures of each table as the table holds them (RC1, RC2), the
 * key check (RC4) and the profile (TL4). Not loaded: the reject list; no table was changed.
 */
public record LoadOutcome(long runId, List<TableLoad> tables, List<RejectedRecord> rejects, ExtractProfile profile) {

    /** One replaced table; isKeyRangeOfFile: its lowest and highest key are the first and last record of the file. */
    public record TableLoad(ExtractFile file, LoadFigures figures, boolean isKeyRangeOfFile) {
    }

    public LoadOutcome {
        tables = List.copyOf(tables);
        rejects = List.copyOf(rejects);
    }

    public boolean isLoaded() {
        return rejects.isEmpty();
    }

    public TableLoad table(ExtractFile file) {
        return tables.stream().filter(table -> table.file() == file).findFirst().orElseThrow();
    }
}
