package com.javarush.apalinskiy.web.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.javarush.apalinskiy.web.util.Uploads.resolveBaseDir;

public class UploadsServlet extends HttpServlet {

    private Path baseDir;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.baseDir = resolveBaseDir(config.getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        Path rel = Paths.get(clean).normalize();
        if (rel.isAbsolute() || rel.startsWith("..")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Path file = baseDir.resolve(rel).normalize();
        if (!file.startsWith(baseDir) || !Files.exists(file) || !Files.isRegularFile(file)) {
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
        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        resp.setContentLengthLong(Files.size(file));
        try (OutputStream os = resp.getOutputStream()) {
            Files.copy(file, os);
        }
    }
}
