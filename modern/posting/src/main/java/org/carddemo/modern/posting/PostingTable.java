package org.carddemo.modern.posting;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.carddemo.modern.codec.Field;
import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RecordLayout;
import org.springframework.jdbc.core.JdbcTemplate;

record PostingTable(String name, RecordLayout layout, List<String> keys) {
    static final PostingTable ACCOUNT = new PostingTable("account", Layouts.ACCOUNT, List.of("ACCT-ID"));
    static final PostingTable XREF = new PostingTable("card_xref", Layouts.XREF, List.of("XREF-CARD-NUM"));
    static final PostingTable BALANCE = new PostingTable("tran_cat_balance", Layouts.CATEGORY_BALANCE,
            List.of("TRANCAT-ACCT-ID", "TRANCAT-TYPE-CD", "TRANCAT-CD"));
    static final PostingTable DISCLOSURE = new PostingTable("disclosure_group", Layouts.DISCLOSURE,
            List.of("DIS-ACCT-GROUP-ID", "DIS-TRAN-TYPE-CD", "DIS-TRAN-CAT-CD"));
    static final PostingTable TYPE = new PostingTable("transaction_type", Layouts.TRAN_TYPE, List.of("TRAN-TYPE"));
    static final PostingTable CATEGORY = new PostingTable("transaction_category", Layouts.TRAN_CATEGORY,
            List.of("TRAN-TYPE-CD", "TRAN-CAT-CD"));
    static final PostingTable TRANSACTION = new PostingTable("posted_transaction", Layouts.TRAN, List.of("TRAN-ID"));
    static final PostingTable INITIAL = new PostingTable("initial_transaction", Layouts.DALYTRAN, List.of());
    static final PostingTable DAILY = new PostingTable("daily_transaction", Layouts.DALYTRAN, List.of());
    static final PostingTable DATES = new PostingTable("date_parameters", Layouts.DATE_PARAMETERS, List.of());
    static final PostingTable REJECT = new PostingTable("rejected_transaction", Layouts.REJTRAN, List.of());
    static final List<PostingTable> ALL = List.of(ACCOUNT, XREF, BALANCE, DISCLOSURE, TYPE, CATEGORY,
            TRANSACTION, INITIAL, DAILY, DATES, REJECT);

    private List<Field> fields() {
        return layout.fields().stream().filter(field -> field.kind() != Field.Kind.FILLER).toList();
    }

    static String column(String field) { return field.toLowerCase(Locale.ROOT).replace('-', '_'); }

    boolean insert(JdbcTemplate jdbc, Map<String, Object> record) { return write(jdbc, record, false) != 0; }

    void store(JdbcTemplate jdbc, Map<String, Object> record) { write(jdbc, record, true); }

    private int write(JdbcTemplate jdbc, Map<String, Object> record, boolean replace) {
        var fields = fields();
        var columns = fields.stream().map(field -> column(field.name())).collect(Collectors.joining(", "));
        var parameters = fields.stream().map(field -> "?").collect(Collectors.joining(", "));
        var conflict = "";
        if (!keys.isEmpty()) {
            var keyColumns = keys.stream().map(PostingTable::column).collect(Collectors.joining(", "));
            var updates = fields.stream().filter(field -> !keys.contains(field.name()))
                    .map(field -> column(field.name()) + " = EXCLUDED." + column(field.name()))
                    .collect(Collectors.joining(", "));
            conflict = " ON CONFLICT (" + keyColumns + ") " + (replace ? "DO UPDATE SET " + updates : "DO NOTHING");
        }
        var values = fields.stream().map(field -> record.getOrDefault(field.name(),
                field.kind() == Field.Kind.TEXT ? "" : BigDecimal.ZERO)).toArray();
        return jdbc.update("INSERT INTO " + name + " (" + columns + ") VALUES (" + parameters + ")" + conflict, values);
    }

    List<Map<String, Object>> read(JdbcTemplate jdbc) {
        var order = keys.isEmpty() ? "record_number" : keys.stream().map(key -> column(key)
                + (fields().stream().anyMatch(field -> field.name().equals(key) && field.kind() == Field.Kind.TEXT)
                ? " COLLATE \"C\"" : "")).collect(Collectors.joining(", "));
        return jdbc.query("SELECT * FROM " + name + " ORDER BY " + order, (rs, row) -> {
            Map<String, Object> record = new LinkedHashMap<>();
            for (var field : fields()) record.put(field.name(), field.kind() == Field.Kind.TEXT
                    ? rs.getString(column(field.name())) : rs.getBigDecimal(column(field.name())));
            return record;
        });
    }

    Map<String, Object> find(JdbcTemplate jdbc, Object... values) {
        if (keys.size() != values.length || keys.isEmpty()) throw new IllegalArgumentException("invalid lookup key");
        var where = keys.stream().map(key -> column(key) + " = ?").collect(Collectors.joining(" AND "));
        var rows = jdbc.query("SELECT * FROM " + name + " WHERE " + where, (rs, row) -> {
            Map<String, Object> record = new LinkedHashMap<>();
            for (var field : fields()) record.put(field.name(), field.kind() == Field.Kind.TEXT
                    ? rs.getString(column(field.name())) : rs.getBigDecimal(column(field.name())));
            return record;
        }, values);
        return rows.isEmpty() ? null : rows.getFirst();
    }
}
