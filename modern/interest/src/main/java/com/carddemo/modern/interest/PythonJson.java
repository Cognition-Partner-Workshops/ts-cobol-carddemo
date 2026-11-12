package com.carddemo.modern.interest;

import java.util.Map;
import java.util.TreeMap;

/** {@code json.dumps(value, indent=1, sort_keys=True)} for nested maps of strings and numbers. */
final class PythonJson {

    private PythonJson() {
    }

    static String dumps(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, 0);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value, int depth) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            var sorted = new TreeMap<String, Object>();
            map.forEach((k, v) -> sorted.put(k.toString(), v));
            int i = 0;
            for (var e : sorted.entrySet()) {
                sb.append(" ".repeat(depth + 1)).append(quote(e.getKey())).append(": ");
                write(sb, e.getValue(), depth + 1);
                sb.append(++i < sorted.size() ? ",\n" : "\n");
            }
            sb.append(" ".repeat(depth)).append('}');
        } else if (value instanceof Number) {
            sb.append(value);
        } else {
            sb.append(quote(String.valueOf(value)));
        }
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
