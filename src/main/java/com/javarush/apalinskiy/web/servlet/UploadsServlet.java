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
 * Servlet responsible for serving static uploaded files (images and other media).
 *
 * <p>Files are served from a base uploads directory, resolved via
 * {@link com.javarush.apalinskiy.web.util.Uploads#resolveBaseDir(jakarta.servlet.ServletContext)}.
 * All requests are verified against path traversal and non-existent files.</p>
 *
 * <p>Supports basic MIME type detection for common image formats and sets
 * long-lived cache headers (1 year) for immutable resources.</p>
 */
public class UploadsServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UploadsServlet.class);

    private Path baseDir;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.baseDir = resolveBaseDir(config.getServletContext());
        log.debug("Uploads baseDir={}", baseDir);
    }

    /**
     * Serves the requested uploaded file if it exists and is within the base directory.
     *
     * @param req  HTTP request (expects path after /uploads/)
     * @param resp HTTP response
     * @throws IOException if I/O or access error occurs
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
