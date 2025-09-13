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
 * Utility class for resolving and managing the base directory used for file uploads.
 * <p>
 * The resolution order for the uploads directory is as follows:
 * <ol>
 *   <li>Cached value in the {@link ServletContext} attribute
 *       {@link WebConst.InitParam#CTX_UPLOADS_DIR} (if already resolved).</li>
 *   <li>{@code uploads.dir} context parameter (from {@code web.xml}).</li>
 *   <li>System property {@code TEXTQUEST_UPLOADS_DIR}.</li>
 *   <li>Environment variable {@code TEXTQUEST_UPLOADS_DIR}.</li>
 *   <li>Fallback to {@code ${catalina.base}/uploads} or {@code ${user.dir}/uploads}.</li>
 *   <li>As a last resort, the servlet container temp directory
 *       ({@code jakarta.servlet.context.tempdir}) or {@code java.io.tmpdir}.</li>
 * </ol>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Supports variable expansion in paths using syntax like {@code ${HOME}}.</li>
 *   <li>Ensures the directory exists by creating it if necessary.</li>
 *   <li>Caches the resolved path in the {@link ServletContext} for reuse.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * This utility is stateless; the resolved directory is cached in the context.
 */
public final class Uploads {

    private Uploads() {
    }

    /**
     * Resolves the base directory for file uploads using the context, system properties,
     * environment variables, and fallbacks.
     *
     * @param ctx the servlet context
     * @return the resolved and created base directory path
     * @throws IllegalStateException if no directory can be created
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
     * Expands variables in the form {@code ${VAR}} using system properties or
     * environment variables. If no value is found, it replaces with an empty string.
     *
     * @param val the string possibly containing variables
     * @return the expanded and trimmed string, or {@code null} if result is empty
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

    /**
     * Checks if a string is blank (null or whitespace only).
     */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
