package com.javarush.apalinskiy.web.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.javarush.apalinskiy.web.util.Uploads.resolveBaseDir;

/**
 * Static file server for user-uploaded assets (e.g., images) under a configured base directory.
 * <p>
 * This servlet:
 * <ul>
 *   <li>Resolves a <b>base upload directory</b> during {@link #init(ServletConfig)} via a helper
 *       (e.g., {@code resolveBaseDir(ServletContext)} defined elsewhere in the codebase).</li>
 *   <li>Serves files by relative path from {@code getPathInfo()} with strong cache headers
 *       ({@code Cache-Control: public, max-age=31536000, immutable}).</li>
 *   <li>Prevents path traversal and access outside the base directory using normalization and checks.</li>
 *   <li>Detects {@code Content-Type} via {@link Files#probeContentType(Path)} with common image fallbacks.</li>
 * </ul>
 *
 * <h3>Security considerations</h3>
 * <ul>
 *   <li>Rejects empty paths and attempts to escape the base directory (absolute paths or {@code ..}).</li>
 *   <li>Ensures the resolved path stays under {@code baseDir} and refers to a regular file.</li>
 *   <li>Does not execute or transform files; it only streams bytes as-is.</li>
 * </ul>
 *
 * <h3>Caching</h3>
 * <ul>
 *   <li>Sets an aggressive immutable cache policy suitable for content-addressed or unique file names.</li>
 *   <li>If your deployment allows overwriting a file at the same URL, consider adding ETag/Last-Modified logic instead.</li>
 * </ul>
 */
public class UploadsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UploadsServlet.class);

    private Path baseDir;

    /**
     * Initializes the servlet by resolving the base upload directory from the servlet context.
     *
     * @param config servlet config provided by the container
     * @throws ServletException if initialization fails
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.baseDir = resolveBaseDir(config.getServletContext());
        log.debug("Uploads baseDir={}", baseDir);
    }

    /**
     * Serves a file addressed by {@code pathInfo} from the configured base directory.
     * <p>
     * Flow:
     * <ol>
     *   <li>Validate {@code pathInfo} is present and non-root.</li>
     *   <li>Normalize the path and reject absolute paths or any that start with {@code ..} (path traversal).</li>
     *   <li>Resolve against {@link #baseDir} and verify the resulting path still starts with {@code baseDir},
     *       exists, and is a regular file.</li>
     *   <li>Determine content type (probe + extension fallbacks) and set {@code Content-Type}.</li>
     *   <li>Set cache headers ({@code public, max-age=31536000, immutable}) and {@code Content-Length}.</li>
     *   <li>Stream the file bytes to the response output stream.</li>
     * </ol>
     * </p>
     *
     * <p>Errors:</p>
     * <ul>
     *   <li>404 for missing/empty path or non-existing file;</li>
     *   <li>403 for traversal attempts;</li>
     *   <li>500 will be surfaced by the container if streaming fails unexpectedly.</li>
     * </ul>
     *
     * @param req  HTTP request (uses {@link HttpServletRequest#getPathInfo()})
     * @param resp HTTP response used to send the file content and headers
     * @throws IOException if I/O fails while reading or writing the file
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            log.warn("Uploads 404: empty pathInfo uri={}", req.getRequestURI());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        Path rel = Paths.get(clean).normalize();
        if (rel.isAbsolute() || rel.startsWith("..")) {
            log.warn("Uploads forbidden: path traversal attempt rel='{}' uri={}", rel, req.getRequestURI());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Path file = baseDir.resolve(rel).normalize();
        if (!file.startsWith(baseDir) || !Files.exists(file) || !Files.isRegularFile(file)) {
            log.warn("Uploads 404: file not found rel='{}' uri={}", rel, req.getRequestURI());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            String name = file.getFileName().toString().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (name.endsWith(".png")) {
                contentType = "image/png";
            } else if (name.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (name.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (name.endsWith(".svg")) {
                contentType = "image/svg+xml";
            } else contentType = "application/octet-stream";
        }
        long size = Files.size(file);
        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        resp.setContentLengthLong(Files.size(file));
        try (OutputStream os = resp.getOutputStream()) {
            Files.copy(file, os);
        }
        log.info("Uploads served rel='{}' type={} size={}B", rel, contentType, size);
    }
}
