package com.carddemo.interest.jdbc;

import java.util.List;
import java.util.Locale;

import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordLayout;

/**
 * The four replica tables (architecture, section Data ownership) and the copybook each one maps
 * (data-map-interest.md). A column is named after its copybook field, lower case with underscores;
 * the FILLER column has the table's own name.
 */
public enum ReplicaTable {
    ACCOUNT("carddemo.account", CardDemoLayouts.ACCOUNT, "acct_filler", List.of("ACCT-ID")),
    CARD_XREF("carddemo.card_xref", CardDemoLayouts.CARD_XREF, "xref_filler", List.of("XREF-CARD-NUM")),
    TRAN_CAT_BALANCE("carddemo.tran_cat_balance", CardDemoLayouts.CATEGORY_BALANCE, "trancat_filler",
            List.of("TRANCAT-ACCT-ID", "TRANCAT-TYPE-CD", "TRANCAT-CD")),
    DISCLOSURE_GROUP("carddemo.disclosure_group", CardDemoLayouts.DISCLOSURE_GROUP, "dis_filler",
            List.of("DIS-ACCT-GROUP-ID", "DIS-TRAN-TYPE-CD", "DIS-TRAN-CAT-CD"));

    private final String tableName;
    private final RecordLayout layout;
    private final String fillerColumn;
    private final List<String> keyFields;

    ReplicaTable(String tableName, RecordLayout layout, String fillerColumn, List<String> keyFields) {
        this.tableName = tableName;
        this.layout = layout;
        this.fillerColumn = fillerColumn;
        this.keyFields = keyFields;
    }

    /** Schema-qualified: the connections of the job have search_path batch (application.yml). */
    public String tableName() {
        return tableName;
    }

    public RecordLayout layout() {
        return layout;
    }

    /** The copybook fields of the primary key, in key order. */
    public List<String> keyFields() {
        return keyFields;
    }

    public String column(LayoutField field) {
        return field.kind() == FieldKind.FILLER ? fillerColumn : field.name().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public String column(String fieldName) {
        return column(layout.field(fieldName));
    }

    /** The columns in copybook field order, comma separated. */
    String columnList() {
        return String.join(", ", layout.fields().stream().map(this::column).toList());
    }

    /** ORDER BY of the primary key; text keys in the C collation (E6). */
    String keyOrder() {
        return String.join(", ", keyFields.stream()
                .map(name -> layout.field(name).kind() == FieldKind.TEXT ? column(name) + " COLLATE \"C\"" : column(name))
                .toList());
    }
}
