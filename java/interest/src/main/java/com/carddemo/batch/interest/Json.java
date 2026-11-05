package com.carddemo.batch.interest;

import java.util.Map;
import java.util.TreeMap;

/** Minimal writer matching Python's {@code json.dumps(obj, indent=1, sort_keys=True)} for maps of ints/strings. */
final class Json {
    private Json() {
    }

    static String write(Map<String, ?> value) {
        StringBuilder out = new StringBuilder();
        write(out, value, 0);
        return out.toString();
    }

    private static void write(StringBuilder out, Object value, int depth) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> entry : new TreeMap<>(map).entrySet()) {
                out.append(" ".repeat(depth + 1)).append('"').append(entry.getKey()).append("\": ");
                write(out, entry.getValue(), depth + 1);
                out.append(++i < map.size() ? ",\n" : "\n");
            }
            out.append(" ".repeat(depth)).append('}');
        } else if (value instanceof Number) {
            out.append(value);
        } else {
            out.append('"').append(value).append('"');
        }
    }
}
