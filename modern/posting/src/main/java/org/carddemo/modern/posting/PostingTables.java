package org.carddemo.modern.posting;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.carddemo.modern.codec.*;
import org.springframework.jdbc.core.JdbcTemplate;

record PostingTables(String name, RecordLayout layout, List<String> keys) {
    static final PostingTables ACCOUNT = new PostingTables("account", Layouts.ACCOUNT, List.of("ACCT-ID"));
    static final PostingTables XREF = new PostingTables("card_xref", Layouts.XREF, List.of("XREF-CARD-NUM"));
    static final PostingTables BALANCE = new PostingTables("tran_cat_balance", Layouts.CATEGORY_BALANCE, List.of("TRANCAT-ACCT-ID", "TRANCAT-TYPE-CD", "TRANCAT-CD"));
    static final PostingTables TRAN = new PostingTables("posted_transaction", Layouts.TRAN, List.of("TRAN-ID"));
    static final PostingTables TYPE = new PostingTables("transaction_type", PostingLayouts.TYPE, List.of("TRAN-TYPE"));
    static final PostingTables CATEGORY = new PostingTables("transaction_category", PostingLayouts.CATEGORY, List.of("TRAN-TYPE-CD", "TRAN-CAT-CD"));
    static final List<PostingTables> ALL = List.of(ACCOUNT, XREF, BALANCE, TRAN, TYPE, CATEGORY);
    static String col(String name) { return name.toLowerCase(Locale.ROOT).replace('-', '_'); }
    List<Field> fields() { return layout.fields().stream().filter(f -> f.kind() != Field.Kind.FILLER).toList(); }
    int write(JdbcTemplate jdbc, Map<String, Object> row, boolean replace) {
        var fields = fields();
        var columns = fields.stream().map(f -> col(f.name())).collect(Collectors.joining(","));
        var values = fields.stream().map(f -> row.getOrDefault(f.name(), f.kind() == Field.Kind.TEXT ? "" : BigDecimal.ZERO)).toArray();
        var updates = fields.stream().filter(f -> !keys.contains(f.name())).map(f -> col(f.name()) + "=EXCLUDED." + col(f.name())).collect(Collectors.joining(","));
        return jdbc.update("INSERT INTO " + name + " (" + columns + ") VALUES (" + String.join(",", Collections.nCopies(fields.size(), "?"))
                + ") ON CONFLICT (" + keys.stream().map(PostingTables::col).collect(Collectors.joining(",")) + ") "
                + (replace ? "DO UPDATE SET " + updates : "DO NOTHING"), values);
    }
    List<Map<String, Object>> read(JdbcTemplate jdbc) {
        var order = keys.stream().map(k -> col(k) + (fields().stream().anyMatch(f -> f.name().equals(k) && f.kind() == Field.Kind.TEXT) ? " COLLATE \"C\"" : "")).collect(Collectors.joining(","));
        return jdbc.query("SELECT * FROM " + name + " ORDER BY " + order, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            for (var f : fields()) row.put(f.name(), f.kind() == Field.Kind.TEXT ? rs.getString(col(f.name())) : rs.getBigDecimal(col(f.name())));
            return row;
        });
    }
}
