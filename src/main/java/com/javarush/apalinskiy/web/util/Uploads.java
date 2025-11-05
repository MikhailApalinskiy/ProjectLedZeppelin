package com.javarush.apalinskiy.web.util;

import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for resolving and preparing the base directory used for user uploads.
 *
 * <p>Resolution order:</p>
 * <ol>
 *   <li>Cached context attribute {@link WebConst.InitParam#CTX_UPLOADS_DIR}</li>
 *   <li>Servlet init parameter {@code uploads.dir}</li>
 *   <li>System property {@code TEXTQUEST_UPLOADS_DIR}</li>
 *   <li>Environment variable {@code TEXTQUEST_UPLOADS_DIR}</li>
 *   <li>Fallback to {@code catalina.base/uploads} or user working dir</li>
 *   <li>Finally: system temp directory under {@code textquest-uploads}</li>
 * </ol>
 *
 * <p>The method ensures that the resolved directory exists and is cached in
 * {@link ServletContext} for subsequent calls.</p>
 */
public final class Uploads {

    private Uploads() {
    }

    /**
     * Resolves and ensures existence of the uploads base directory.
     *
     * @param ctx servlet context (used for caching and resolving)
     * @return normalized and existing base directory path
     */
    public static Path resolveBaseDir(ServletContext ctx) {
        Object cached = ctx.getAttribute(WebConst.InitParam.CTX_UPLOADS_DIR);
        if (cached instanceof Path p) {
            return p;
        }
        String fromParam = Web.trimOrNull(ctx.getInitParameter("uploads.dir"));
        String s = expandVars(Web.trimOrNull(fromParam));
        if (s == null) {
            s = Web.trimOrNull(System.getProperty("TEXTQUEST_UPLOADS_DIR"));
        }
        if (s == null) {
            s = Web.trimOrNull(System.getenv("TEXTQUEST_UPLOADS_DIR"));
        }
        Path base;
        if (s != null) {
            base = Paths.get(s).normalize();
        } else {
            String baseHome = System.getProperty("catalina.base",
                    System.getProperty("user.dir"));
            base = Paths.get(baseHome, "uploads").normalize();
        }
        if (isBlank(base.toString())) {
            File tmp = (File) ctx.getAttribute("jakarta.servlet.context.tempdir");
            if (tmp != null) {
                base = tmp.toPath().resolve("textquest-uploads").normalize();
            } else {
                base = Paths.get(System.getProperty("java.io.tmpdir"), "textquest-uploads").normalize();
            }
        }
        try {
            Files.createDirectories(base);
        } catch (Exception e) {
            Path fallback = Paths.get(System.getProperty("java.io.tmpdir"), "textquest-uploads").normalize();
            try {
                Files.createDirectories(fallback);
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create uploads dir", ex);
            }
            base = fallback;
        }
        ctx.setAttribute(WebConst.InitParam.CTX_UPLOADS_DIR, base);
        return base;
    }

    /**
     * Expands environment or system variables in a string like
     * {@code ${VAR_NAME}} using both {@link System#getProperty(String)} and
     * {@link System#getenv(String)}.
     */
    private static String expandVars(String val) {
        if (val == null) {
            return null;
        }
        Pattern p = Pattern.compile("\\$\\{([^}]+)}");
        Matcher m = p.matcher(val);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String rep = System.getProperty(key);
            if (rep == null) {
                rep = System.getenv(key);
            }
            if (rep == null) {
                rep = "";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
