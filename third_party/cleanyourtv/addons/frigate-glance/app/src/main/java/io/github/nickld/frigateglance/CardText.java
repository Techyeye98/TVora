package io.github.nickld.frigateglance;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

/** The card's footer: which button does what, in words a remote's owner recognises. */
final class CardText {
    private static final Map<String, String> SHORT = Map.of(
            "KEYCODE_DPAD_LEFT", "◀",
            "KEYCODE_DPAD_RIGHT", "▶",
            "KEYCODE_DPAD_UP", "▲",
            "KEYCODE_DPAD_DOWN", "▼",
            "KEYCODE_PROG_RED", "Red",
            "KEYCODE_PROG_GREEN", "Green",
            "KEYCODE_PROG_YELLOW", "Yellow",
            "KEYCODE_PROG_BLUE", "Blue");

    private CardText() {}

    static String footer(String dismissKey, String snoozeKey, int snoozeMinutes) {
        return footer(dismissKey, snoozeKey, snoozeMinutes, null);
    }

    /** fullscreenKey null while the full screen feature is off. Keys that need a hold get a pill on the card, no word. */
    static String footer(String dismissKey, String snoozeKey, int snoozeMinutes, String fullscreenKey) {
        return String.join(" · ", parts(dismissKey, snoozeKey, snoozeMinutes, fullscreenKey).values());
    }

    /** The footer's actions in order (KeyRules.DISMISS, SNOOZE, FULLSCREEN) with their labels. */
    static Map<Integer, String> parts(String dismissKey, String snoozeKey, int snoozeMinutes, String fullscreenKey) {
        Map<Integer, String> m = new LinkedHashMap<>();
        m.put(KeyRules.DISMISS, key(dismissKey) + " Dismiss");
        m.put(KeyRules.SNOOZE, key(snoozeKey) + " Snooze " + duration(snoozeMinutes));
        if (fullscreenKey != null) m.put(KeyRules.FULLSCREEN, key(fullscreenKey) + " Full screen");
        return m;
    }

    static String duration(int minutes) {
        return minutes % 60 == 0 ? (minutes / 60) + "h" : minutes + " min";
    }

    static String key(String name) {
        String s = SHORT.get(name);
        if (s != null) return s;
        String[] words = name.replaceFirst("^KEYCODE_", "").toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return out.toString();
    }
}
