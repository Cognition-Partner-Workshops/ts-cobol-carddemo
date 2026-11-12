package org.carddemo.modern.interest;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.carddemo.modern.codec.Field;
import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RecordLayout;
import org.springframework.jdbc.core.JdbcTemplate;

record InterestTables(String name, RecordLayout layout, List<String> keys) {
    static final InterestTables ACCOUNT = new InterestTables("account", Layouts.ACCOUNT, List.of("ACCT-ID"));
    static final InterestTables XREF = new InterestTables("card_xref", Layouts.XREF, List.of("XREF-CARD-NUM"));
    static final InterestTables DISCLOSURE = new InterestTables("disclosure_group", Layouts.DISCLOSURE,
            List.of("DIS-ACCT-GROUP-ID", "DIS-TRAN-TYPE-CD", "DIS-TRAN-CAT-CD"));
    static final InterestTables BALANCE = new InterestTables("tran_cat_balance", Layouts.CATEGORY_BALANCE,
            List.of("TRANCAT-ACCT-ID", "TRANCAT-TYPE-CD", "TRANCAT-CD"));
    static final InterestTables TRANSACTION = new InterestTables("\"transaction\"", Layouts.TRAN, List.of("TRAN-ID"));
    static final List<InterestTables> ALL = List.of(ACCOUNT, XREF, DISCLOSURE, BALANCE, TRANSACTION);

    private List<Field> fields() {
        return layout.fields().stream().filter(field -> field.kind() != Field.Kind.FILLER).toList();
    }

    private static String column(String field) { return field.toLowerCase(java.util.Locale.ROOT).replace('-', '_'); }

    void store(JdbcTemplate jdbc, Map<String, Object> record) {
        var fields = fields();
        var columns = fields.stream().map(field -> column(field.name())).collect(Collectors.joining(", "));
        var parameters = fields.stream().map(field -> "?").collect(Collectors.joining(", "));
        var keyColumns = keys.stream().map(InterestTables::column).collect(Collectors.joining(", "));
        var updates = fields.stream().filter(field -> !keys.contains(field.name()))
                .map(field -> column(field.name()) + " = EXCLUDED." + column(field.name())).collect(Collectors.joining(", "));
        var values = fields.stream().map(field -> record.getOrDefault(field.name(),
                field.kind() == Field.Kind.TEXT ? "" : BigDecimal.ZERO)).toArray();
        jdbc.update("INSERT INTO " + name + " (" + columns + ") VALUES (" + parameters + ") ON CONFLICT ("
                + keyColumns + ") DO UPDATE SET " + updates, values);
    }

    List<Map<String, Object>> read(JdbcTemplate jdbc) {
        var order = keys.stream().map(key -> column(key) + (fields().stream()
                .anyMatch(field -> field.name().equals(key) && field.kind() == Field.Kind.TEXT) ? " COLLATE \"C\"" : ""))
                .collect(Collectors.joining(", "));
        return jdbc.query("SELECT * FROM " + name + " ORDER BY " + order, (rs, row) -> {
            Map<String, Object> record = new LinkedHashMap<>();
            for (var field : fields()) record.put(field.name(), field.kind() == Field.Kind.TEXT
                    ? rs.getString(column(field.name())) : rs.getBigDecimal(column(field.name())));
            return record;
        });
    }
}
