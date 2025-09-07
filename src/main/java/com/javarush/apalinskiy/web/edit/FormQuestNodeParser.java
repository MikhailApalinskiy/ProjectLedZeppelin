package com.javarush.apalinskiy.web.edit;

import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.web.util.Web;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class FormQuestNodeParser {

    private static final Pattern NEWLINE = Pattern.compile("\\R+");
    private static final int MAX_OPTS = 500;
    private static final boolean STRICT_FINAL = false;

    private FormQuestNodeParser() {
    }

    public static QuestNode parseNode(HttpServletRequest req) {
        int id = parseInt(req.getParameter("id"), "id");
        String text = req.getParameter("text");
        boolean fin = parseBool(req.getParameter("final"));
        String image = trimToNull(req.getParameter("image"));
        if (fin) {
            if (STRICT_FINAL && hasAnyOptions(req.getParameter("options"), req)) {
                throw new IllegalArgumentException("The final node should have no options");
            }
            return QuestNode.fin(id, text, image);
        }
        List<Option> options = parseOptions(req.getParameter("options"), req);
        return QuestNode.of(id, text, options, false, image);
    }

    private static List<Option> parseOptions(String optionsTextarea, HttpServletRequest req) {
        if (!isBlank(optionsTextarea)) {
            return parseOptionsTextarea(optionsTextarea);
        }
        return parseOptionsIndexed(req);
    }

    private static List<Option> parseOptionsTextarea(String raw) {
        String[] lines = NEWLINE.split(raw);
        List<Option> res = new ArrayList<>();
        for (String line : lines) {
            String s = cleanupSeparators(line).trim();
            if (s.isEmpty()) {
                continue;
            }
            String[] kv = s.split("->", 2);
            if (kv.length != 2) {
                throw new IllegalArgumentException(
                        "Incorrect option format: '" + line + "'. Expected 'text -> nextId'"
                );
            }
            String choice = kv[0].trim();
            int next = parseInt(kv[1], "option.next");
            res.add(new Option(choice, next));
        }
        return res;
    }

    private static List<Option> parseOptionsIndexed(HttpServletRequest req) {
        List<Option> res = new ArrayList<>();
        for (int i = 1; i <= MAX_OPTS; i++) {
            String ch = req.getParameter("opt_choice_" + i);
            String nx = req.getParameter("opt_next_" + i);
            boolean chBlank = isBlank(ch);
            boolean nxBlank = isBlank(nx);
            if (chBlank && nxBlank) {
                continue;
            }
            if (chBlank || nxBlank) {
                throw new IllegalArgumentException(
                        "Incomplete version №" + i + ": also need text (opt_choice_" + i + "), and nextId (opt_next_" + i + ")"
                );
            }
            res.add(new Option(ch.trim(), parseInt(nx, "opt_next_" + i)));
        }
        return res;
    }

    private static boolean hasAnyOptions(String optionsTextarea, HttpServletRequest req) {
        if (!isBlank(optionsTextarea)) {
            for (String line : NEWLINE.split(optionsTextarea)) {
                if (!line.trim().isEmpty()) return true;
            }
        }
        for (int i = 1; i <= MAX_OPTS; i++) {
            String ch = req.getParameter("opt_choice_" + i);
            String nx = req.getParameter("opt_next_" + i);
            if (!isBlank(ch) || !isBlank(nx)) return true;
        }
        return false;
    }

    private static int parseInt(String s, String field) {
        try {
            if (s == null) {
                throw new NumberFormatException("null");
            }
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid number in the field '" + field + "': " + s);
        }
    }

    private static boolean parseBool(String s) {
        if (s == null) {
            return false;
        }
        String v = s.trim().toLowerCase(Locale.ROOT);
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("on");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimToNull(String s) {
        return Web.trimOrNull(s);
    }

    private static String cleanupSeparators(String line) {
        if (line == null) {
            return "";
        }
        String s = line;
        s = s.replace("—>", "->");
        s = s.replace("-->", "->");
        s = s.replace("→", "->");
        s = s.replace("=>", "->");
        s = s.replace(" –>", "->").replace(" —>", "->").replace(" −>", "->");
        return s;
    }
}
