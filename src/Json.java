/** 极简 JSON 工具：转义与字符串字段提取（无第三方依赖） */
public final class Json {

    private Json() {
    }

    /** 转义字符串中的特殊字符 */
    public static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.toString();
    }

    /** 从请求体 JSON 中提取字符串字段值，如 get("{\"title\":\"a\"}","title") -> "a" */
    public static String get(String json, String key) {
        if (json == null) return "";
        String pat = "\"" + key + "\"";
        int i = json.indexOf(pat);
        if (i < 0) return "";
        int colon = json.indexOf(':', i + pat.length());
        if (colon < 0) return "";
        int q1 = json.indexOf('"', colon);
        if (q1 < 0) return "";
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return "";
        return json.substring(q1 + 1, q2)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
