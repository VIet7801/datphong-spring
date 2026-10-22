package com.vn.datphong.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Dựng JSON phẳng cho payload outbox/audit (khóa -> chuỗi/số/boolean/null) mà không phụ thuộc API Jackson. */
public final class Json {
    private Json() {
    }

    public static String obj(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("Cần cặp khóa/giá trị");
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(String.valueOf(kv[i]))).append(':');
            Object v = kv[i + 1];
            if (v == null) {
                sb.append("null");
            } else if (v instanceof java.math.BigDecimal bd) {
                sb.append(bd.toPlainString());
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append(quote(String.valueOf(v)));
            }
        }
        return sb.append('}').toString();
    }

    public static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    /** Đọc lại payload phẳng do {@link #obj} tạo ra (đủ cho worker thông báo; không phải parser JSON tổng quát). */
    public static Map<String, String> parseFlat(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        if (json == null) {
            return out;
        }
        int i = json.indexOf('{');
        int n = json.length();
        i++;
        while (i < n) {
            while (i < n && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) {
                i++;
            }
            if (i >= n || json.charAt(i) == '}') {
                break;
            }
            int[] end = new int[1];
            String key = readString(json, i, end);
            i = end[0];
            while (i < n && (json.charAt(i) == ':' || Character.isWhitespace(json.charAt(i)))) {
                i++;
            }
            String val;
            if (json.charAt(i) == '"') {
                val = readString(json, i, end);
                i = end[0];
            } else {
                int j = i;
                while (j < n && json.charAt(j) != ',' && json.charAt(j) != '}') {
                    j++;
                }
                val = json.substring(i, j).trim();
                if (val.equals("null")) {
                    val = null;
                }
                i = j;
            }
            out.put(key, val);
        }
        return out;
    }

    private static String readString(String s, int start, int[] end) {
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char d = s.charAt(i + 1);
                switch (d) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                        i += 4;
                    }
                    default -> sb.append(d);
                }
                i += 2;
            } else if (c == '"') {
                end[0] = i + 1;
                return sb.toString();
            } else {
                sb.append(c);
                i++;
            }
        }
        end[0] = s.length();
        return sb.toString();
    }
}

