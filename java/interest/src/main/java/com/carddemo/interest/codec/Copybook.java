package com.carddemo.interest.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the record description of a copybook into a {@link RecordLayout}. It knows what the five
 * copybooks of the stream use - levels, group items, PIC 9(n), PIC S9(n)V9(m), PIC X(n), FILLER,
 * USAGE DISPLAY - and marks every other item as refused (rule E5): a USAGE such as COMP-3 or COMP,
 * and any clause or picture it does not know. A refused field stops the first decode or encode.
 */
public final class Copybook {

    static final String FILLER = "FILLER";

    private static final Set<String> USAGES = Set.of("DISPLAY", "COMP", "COMP-1", "COMP-2", "COMP-3", "COMP-4",
            "COMP-5", "COMPUTATIONAL", "COMPUTATIONAL-1", "COMPUTATIONAL-2", "COMPUTATIONAL-3", "COMPUTATIONAL-4",
            "COMPUTATIONAL-5", "BINARY", "PACKED-DECIMAL", "POINTER", "INDEX", "NATIONAL", "DISPLAY-1");
    private static final Pattern TEXT = Pattern.compile("X\\((\\d+)\\)|(X+)");
    private static final Pattern UNSIGNED = Pattern.compile("9\\((\\d+)\\)|(9+)");
    private static final Pattern SIGNED = Pattern.compile("S(?:9\\((\\d+)\\)|(9+))(?:V(?:9\\((\\d+)\\)|(9+)))?");
    /** A long holds 18 digits. */
    private static final int MAX_DIGITS = 18;

    private Copybook() {
    }

    private record Item(int level, String name, String picture, String usage, String unknownClause) {
    }

    public static RecordLayout parse(String copybook, String source) {
        List<Item> items = items(source);
        RecordLayout.Builder layout = RecordLayout.of(copybook);
        List<Item> openGroups = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            openGroups.removeIf(group -> group.level() >= item.level());
            boolean isGroup = item.picture() == null && i + 1 < items.size() && items.get(i + 1).level() > item.level();
            String usage = item.usage() != null ? item.usage()
                    : openGroups.stream().map(Item::usage).filter(u -> u != null).reduce((first, last) -> last).orElse("DISPLAY");
            if (isGroup && item.unknownClause() == null) {
                openGroups.add(new Item(item.level(), item.name(), null, usage, null));
            } else {
                add(layout, item, usage);
            }
        }
        return layout.build();
    }

    private static void add(RecordLayout.Builder layout, Item item, String usage) {
        if (item.unknownClause() != null) {
            layout.refused(item.name(), "clause " + item.unknownClause() + " is not supported");
        } else if (!usage.equals("DISPLAY")) {
            layout.refused(item.name(), "USAGE " + usage + " is not DISPLAY (rule E5)");
        } else if (item.picture() == null) {
            layout.refused(item.name(), "no PICTURE");
        } else {
            Matcher text = TEXT.matcher(item.picture());
            Matcher unsigned = UNSIGNED.matcher(item.picture());
            Matcher signed = SIGNED.matcher(item.picture());
            if (text.matches()) {
                int length = count(text, 1);
                if (item.name().equals(FILLER)) {
                    layout.filler(length);
                } else {
                    layout.text(item.name(), length);
                }
            } else if (unsigned.matches() && count(unsigned, 1) <= MAX_DIGITS) {
                layout.unsigned(item.name(), count(unsigned, 1));
            } else if (signed.matches() && count(signed, 1) + count(signed, 3) <= MAX_DIGITS) {
                layout.signed(item.name(), count(signed, 1), count(signed, 3));
            } else {
                layout.refused(item.name(), "PICTURE " + item.picture() + " is not supported");
            }
        }
    }

    /** The count of a picture symbol written as 9(n) (group) or as 99..9 (group + 1); 0 when absent. */
    private static int count(Matcher matcher, int group) {
        if (matcher.group(group) != null) {
            return Integer.parseInt(matcher.group(group));
        }
        return matcher.group(group + 1) != null ? matcher.group(group + 1).length() : 0;
    }

    private static List<Item> items(String source) {
        StringBuilder text = new StringBuilder();
        for (String line : source.split("\\R")) {
            String code = line.length() > 72 ? line.substring(0, 72) : line;
            boolean isComment = code.stripLeading().startsWith("*") || (code.length() > 6 && code.charAt(6) == '*');
            if (!isComment) {
                text.append(code.toUpperCase(Locale.ROOT)).append(' ');
            }
        }
        List<Item> items = new ArrayList<>();
        for (String statement : text.toString().split("\\.(?=\\s)")) {
            String[] words = statement.trim().split("\\s+");
            if (words.length < 2 || !words[0].matches("\\d{2}")) {
                continue;
            }
            int level = Integer.parseInt(words[0]);
            if (level == 88) {
                continue;
            }
            String picture = null;
            String usage = null;
            String unknownClause = level == 66 || level == 77 ? "level " + words[0] : null;
            for (int i = 2; i < words.length; i++) {
                String word = words[i];
                if (word.equals("PIC") || word.equals("PICTURE")) {
                    i += i + 1 < words.length && words[i + 1].equals("IS") ? 2 : 1;
                    picture = i < words.length ? words[i] : "";
                } else if (word.equals("USAGE") || word.equals("IS")) {
                    continue;
                } else if (USAGES.contains(word)) {
                    usage = word;
                } else if (unknownClause == null) {
                    unknownClause = word;
                }
            }
            items.add(new Item(level, words[1], picture, usage, unknownClause));
        }
        return items;
    }
}
