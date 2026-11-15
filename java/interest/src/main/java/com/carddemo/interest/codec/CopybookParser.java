package com.carddemo.interest.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reads the record layout out of a copybook source (fixed format: columns 1-6 sequence, column 7
 * indicator, 8-72 text; the estate's copybooks are tab-indented, AGENTS.md). Group items add no
 * bytes. A USAGE other than DISPLAY is carried into the layout so that the codec can refuse the
 * field by name (rule E5). REDEFINES, OCCURS and SIGN SEPARATE are absent from the five layouts
 * (standards section 4, rows i and k) and are refused here rather than interpreted.
 */
public final class CopybookParser {

    private static final Set<String> NOT_DISPLAY_USAGES = Set.of(
            "COMP", "COMP-1", "COMP-2", "COMP-3", "COMP-4", "COMP-5", "COMPUTATIONAL", "COMPUTATIONAL-1",
            "COMPUTATIONAL-2", "COMPUTATIONAL-3", "COMPUTATIONAL-4", "COMPUTATIONAL-5", "BINARY",
            "PACKED-DECIMAL", "POINTER", "INDEX", "NATIONAL", "DISPLAY-1");
    private static final Set<String> REFUSED_CLAUSES = Set.of("REDEFINES", "OCCURS", "SEPARATE", "RENAMES");

    private CopybookParser() {
    }

    public static RecordLayout parse(String copybook, String source) {
        RecordLayout.Builder layout = RecordLayout.builder(copybook);
        for (String sentence : sentences(source)) {
            List<String> words = List.of(sentence.trim().split("\\s+"));
            if (words.size() < 2 || !words.get(0).matches("\\d{1,2}")) {
                throw new IllegalArgumentException(copybook + ": cannot read '" + sentence.trim() + "'");
            }
            int level = Integer.parseInt(words.get(0));
            String name = words.get(1).toUpperCase(Locale.ROOT);
            String picture = null;
            String usage = FieldSpec.DISPLAY;
            for (int i = 1; i < words.size(); i++) {
                String word = words.get(i).toUpperCase(Locale.ROOT);
                if (REFUSED_CLAUSES.contains(word) || level == 66 || level == 88) {
                    throw new IllegalArgumentException(copybook + ": field " + name + ": " + word + " is not supported");
                }
                if ((word.equals("PIC") || word.equals("PICTURE")) && i + 1 < words.size()) {
                    picture = words.get(i + 1).equalsIgnoreCase("IS") ? words.get(i + 2) : words.get(i + 1);
                    if (i == 1) {
                        name = FieldSpec.FILLER;
                    }
                } else if (NOT_DISPLAY_USAGES.contains(word)) {
                    usage = word;
                }
            }
            if (picture != null) {
                layout.field(name, picture, usage);
            } else if (!FieldSpec.DISPLAY.equals(usage)) {
                layout.field(name, "", usage);
            }
        }
        return layout.build();
    }

    /** The source text without sequence area, comments and line breaks, cut at the periods that end an entry. */
    private static List<String> sentences(String source) {
        StringBuilder text = new StringBuilder();
        for (String raw : source.split("\\R")) {
            String line = raw.replace("\t", "        ");
            if (line.length() < 7 || line.charAt(6) == '*' || line.charAt(6) == '/') {
                continue;
            }
            text.append(' ').append(line, 7, Math.min(line.length(), 72));
        }
        List<String> sentences = new ArrayList<>();
        for (String sentence : text.toString().split("\\.(\\s+|$)")) {
            if (!sentence.isBlank()) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }
}
