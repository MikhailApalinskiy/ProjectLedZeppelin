package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.web.util.FormQuestNodeParser;
import com.javarush.apalinskiy.domain.quest.Option;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.service.quest.QuestAuthoringService;
import com.javarush.apalinskiy.web.util.Web;
import com.javarush.apalinskiy.app.WebConst;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.javarush.apalinskiy.web.util.Uploads.resolveBaseDir;

public class CreateQuestServlet extends HttpServlet {

    private QuestAuthoringService authoring;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.authoring = Web.ctxBean(config.getServletContext(), WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
        } catch (IllegalStateException e) {
            throw new UnavailableException("QuestAuthoringService not found in context");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (req.getParameter(WebConst.Param.NEW) != null) {
            authoring.clearEditorDraft();
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.removeAttribute("editingQuestId");
            }
            Web.redirectOk(req, resp, WebConst.Path.CREATE, "An empty draft of the quest has been created");
            return;
        }
        String loadId = Web.trimOrNull(req.getParameter(WebConst.Param.LOAD));
        if (loadId != null) {
            try {
                authoring.loadToEditor(loadId);
                req.getSession(true).setAttribute("editingQuestId", loadId);
                Web.redirectOk(req, resp, WebConst.Path.CREATE, "The quest is uploaded to the editor");
            } catch (Exception e) {
                Web.redirectErr(req, resp, WebConst.Path.CREATE, "Couldn't upload the quest: " + e.getMessage());
            }
            return;
        }
        boolean clear = "1".equals(req.getParameter(WebConst.Param.CLEAR));
        if (!clear) {
            String idStr = req.getParameter(WebConst.Param.ID);
            if (idStr != null && !idStr.isBlank()) {
                try {
                    int id = Integer.parseInt(idStr.trim());
                    QuestNode node = authoring.get(id);
                    if (node != null) {
                        req.setAttribute("form_id", node.getId());
                        req.setAttribute("form_text", node.getText());
                        req.setAttribute("form_final", node.isFin());
                        req.setAttribute("form_image", node.getImage());
                        if (!node.isFin()) {
                            StringBuilder sb = new StringBuilder();
                            for (Option o : node.getOptions()) {
                                if (o == null || o.next() == null) {
                                    continue;
                                }
                                if (!sb.isEmpty()) {
                                    sb.append('\n');
                                }
                                sb.append(o.choice()).append(" -> ").append(o.next());
                            }
                            req.setAttribute("form_options", sb.toString());
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            HttpSession sess = req.getSession(false);
            if (sess != null) {
                Object fId = sess.getAttribute("form_id");
                if (fId != null) {
                    req.setAttribute("form_id", fId);
                    req.setAttribute("form_text", sess.getAttribute("form_text"));
                    req.setAttribute("form_final", sess.getAttribute("form_final"));
                    req.setAttribute("form_options", sess.getAttribute("form_options"));
                    req.setAttribute("form_image", sess.getAttribute("form_image"));
                    sess.removeAttribute("form_id");
                    sess.removeAttribute("form_text");
                    sess.removeAttribute("form_final");
                    sess.removeAttribute("form_options");
                    sess.removeAttribute("form_image");
                }
            }
        }
        Web.forward(req, resp, WebConst.Jsp.CREATE);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getParameter(WebConst.Param.ACTION);
        if (action == null || action.isBlank()) {
            Web.redirectErr(req, resp, WebConst.Path.CREATE, "The action is not specified");
            return;
        }
        try {
            switch (action) {
                case "replaceNode" -> {
                    boolean isFinal = req.getParameter(WebConst.Param.FINAL) != null;
                    String optionsRaw = Optional.ofNullable(req.getParameter(WebConst.Param.OPTIONS)).orElse("").trim();
                    if (!isFinal && optionsRaw.isBlank()) {
                        stashFormForRedirect(req);
                        Web.redirectErr(req, resp, WebConst.Path.CREATE, "For the NON-final branch, you must specify at least one answer option.\n");
                        return;
                    }
                    String uploadedPath;
                    try {
                        uploadedPath = handleImageUpload(req);
                    } catch (ServletException e) {
                        stashFormForRedirect(req);
                        Web.redirectErr(req, resp, WebConst.Path.CREATE, "Image upload error: " + e.getMessage());
                        return;
                    }
                    QuestNode node = FormQuestNodeParser.parseNode(req);
                    if (uploadedPath != null) {
                        node = withImage(node, uploadedPath);
                    } else {
                        QuestNode existing = authoring.get(node.getId());
                        if (existing != null && existing.getImage() != null &&
                                (node.getImage() == null || node.getImage().isBlank())) {
                            node = withImage(node, existing.getImage());
                        }
                    }
                    authoring.saveNode(node);
                    Web.redirect(req, resp, WebConst.Path.CREATE,
                            Map.of(WebConst.Param.CLEAR, "1", WebConst.Attr.OK, "Node #" + node.getId() + " saved"));
                }
                case "deleteNode" -> {
                    String idRaw = Optional.ofNullable(req.getParameter(WebConst.Param.ID)).orElse("").trim();
                    final int id;
                    try {
                        id = Integer.parseInt(idRaw);
                    } catch (NumberFormatException e) {
                        stashFormForRedirect(req);
                        Web.redirectErr(req, resp, WebConst.Path.CREATE, "Specify the correct ID to delete");
                        return;
                    }
                    boolean removed = authoring.deleteNode(id);
                    if (!removed) {
                        stashFormForRedirect(req);
                        Web.redirectErr(req, resp, WebConst.Path.CREATE, "Node #" + id + " not found in the draft");
                        return;
                    }
                    Web.redirect(req, resp, WebConst.Path.CREATE,
                            Map.of(WebConst.Param.CLEAR, "1", WebConst.Attr.OK, "Node #" + id + " deleted"));
                }
                default -> sendError(resp, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            stashFormForRedirect(req);
            Web.redirectErr(req, resp, WebConst.Path.CREATE, e.getMessage());
        }
    }

    private String handleImageUpload(HttpServletRequest req) throws IOException, ServletException {
        String reqCt = req.getContentType();
        if (reqCt == null || !reqCt.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        Part part;
        try {
            part = req.getPart(WebConst.Param.IMAGE_FILE);
        } catch (IllegalStateException ise) {
            throw new ServletException("Uploaded file is too large", ise);
        }
        if (part == null || part.getSize() == 0) {
            return null;
        }
        String ct = Optional.ofNullable(part.getContentType()).orElse("");
        if (!ct.startsWith("image/")) {
            throw new ServletException("Only image files are allowed");
        }
        Path uploadsDir = resolveBaseDir(getServletContext());
        String submitted = Optional.ofNullable(part.getSubmittedFileName()).orElse("image");
        String baseName = Paths.get(submitted).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String ext = "";
        int dot = baseName.lastIndexOf('.');
        if (dot > 0 && dot < baseName.length() - 1) {
            ext = baseName.substring(dot);
        } else {
            String[] cts = ct.split("/");
            if (cts.length == 2) {
                String guess = cts[1];
                if ("jpeg".equalsIgnoreCase(guess)) guess = "jpg";
                ext = "." + guess.toLowerCase();
            }
        }
        String uniq = "quest-" + System.currentTimeMillis() + ext;
        Path target = uploadsDir.resolve(uniq).normalize();
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + uniq;
    }

    private QuestNode withImage(QuestNode src, String imagePath) {
        if (src.isFin()) {
            return QuestNode.fin(src.getId(), src.getText(), imagePath);
        } else {
            return QuestNode.nonFin(src.getId(), src.getText(), src.getOptions(), imagePath);
        }
    }

    private void stashFormForRedirect(HttpServletRequest req) {
        HttpSession s = req.getSession(true);
        s.setAttribute("form_id", Optional.ofNullable(req.getParameter(WebConst.Param.ID)).orElse(""));
        s.setAttribute("form_text", Optional.ofNullable(req.getParameter(WebConst.Param.TEXT)).orElse(""));
        s.setAttribute("form_final", req.getParameter(WebConst.Param.FINAL) != null);
        s.setAttribute("form_options", Optional.ofNullable(req.getParameter(WebConst.Param.OPTIONS)).orElse(""));
    }

    private void sendError(HttpServletResponse resp, String msg) throws IOException {
        resp.sendRedirect(resp.encodeRedirectURL(
                WebConst.Path.CREATE + "?" + WebConst.Attr.ERROR + "=" + Web.urlEncode(Objects.toString(msg, ""))
        ));
    }
}
