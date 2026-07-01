package dev.alexissdev.kronos.common.config;

import java.util.Map;

public final class MessagesConfig {

    private final Map<String, String> messages;

    public MessagesConfig(Map<String, String> messages) {
        this.messages = messages;
    }

    public String get(String key) {
        return colorize(messages.getOrDefault(key, "&c[msg:" + key + "]"));
    }

    public String format(String key, Object... pairs) {
        String msg = get(key);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            msg = msg.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return msg;
    }

    private static String colorize(String text) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&') {
                char c = chars[i + 1];
                if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) >= 0) {
                    chars[i] = '§';
                    chars[i + 1] = Character.toLowerCase(c);
                    i++;
                }
            }
        }
        return new String(chars);
    }
}
