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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Authoring servlet that powers the quest editor page (create/update draft nodes, upload images).
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Resolve {@link QuestAuthoringService} from the application context.</li>
 *   <li><b>GET</b>:
 *     <ul>
 *       <li>{@code ?new} — clears the current editor draft and removes {@code editingQuestId} from session;</li>
 *       <li>{@code ?load=<questId>} — loads an existing quest into the editor and stores {@code editingQuestId} in session;</li>
 *       <li>Prefills the form from {@code id} (node id) or restores the last form state from the session;</li>
 *       <li>Forwards to {@code WebConst.Jsp.CREATE}.</li>
 *     </ul>
 *   </li>
 *   <li><b>POST</b>:
 *     <ul>
 *       <li>{@code action=replaceNode} — validates input, optionally processes image upload, constructs {@link QuestNode}
 *           via {@link FormQuestNodeParser#parseNode(HttpServletRequest)}, persists it with {@link QuestAuthoringService#saveNode(QuestNode)};</li>
 *       <li>{@code action=deleteNode} — deletes a node from the draft by id;</li>
 *       <li>On errors, stashes user inputs to session and redirects back with an error flash.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Image uploads</h3>
 * <ul>
 *   <li>Accepted only for {@code multipart/*} requests; {@code Content-Type} must start with {@code image/}.</li>
 *   <li>Target directory is resolved via an application-specific method {@code resolveBaseDir(getServletContext())}.</li>
 *   <li>Filename is sanitized; extension inferred from original name or content type; stored under {@code /uploads/quest-<ts>.<ext>}.</li>
 *   <li>Requires multipart handling to be configured (e.g., {@code @MultipartConfig} or web.xml).</li>
 * </ul>
 *
 * <h3>Form prefill/restore (GET)</h3>
 * <ul>
 *   <li>With {@code id}, attempts to prefill fields from the corresponding draft node.</li>
 *   <li>Otherwise restores the last submitted-but-failed form from session attributes: {@code form_id}, {@code form_text},
 *       {@code form_final}, {@code form_options}, {@code form_image}.</li>
 * </ul>
 *
 * <h3>Flash & redirects</h3>
 * <ul>
 *   <li>Uses {@link Web#redirectOk} and {@link Web#redirectErr} for user feedback.</li>
 *   <li>OK cases also set {@code clear=1} to present a clean form.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>When replacing a non-final node without any options, the request is rejected with a helpful message.</li>
 *   <li>If no new image is uploaded, preserves the existing node image (if any).</li>
 * </ul>
 *
 * @see QuestAuthoringService
 * @see FormQuestNodeParser
 * @see QuestNode
 * @see Option
 * @see WebConst
 * @see Web
 */
public class CreateQuestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(CreateQuestServlet.class);

    private QuestAuthoringService authoring;

    /**
     * Resolves {@link QuestAuthoringService} from the servlet context.
     *
     * @throws UnavailableException if the service is missing
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.authoring = Web.ctxBean(config.getServletContext(), WebConst.Ctx.AUTHORING_SERVICE, QuestAuthoringService.class);
            log.debug("CreateQuestServlet initialized");
        } catch (IllegalStateException e) {
            log.error("Initialization failed: QuestAuthoringService not found in context", e);
            throw new UnavailableException("QuestAuthoringService not found in context");
        }
    }

    /**
     * Renders the editor page or performs draft management actions:
     * <ul>
     *   <li>{@code ?new}: clears the draft and resets {@code editingQuestId} in session, then redirects with OK flash;</li>
     *   <li>{@code ?load=<questId>}: loads quest into editor, stores {@code editingQuestId} in session, redirects with OK flash;</li>
     *   <li>Otherwise: optionally prefill form by {@code id} or restore from session, then forward to the editor JSP.</li>
     * </ul>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (req.getParameter(WebConst.Param.NEW) != null) {
            authoring.clearEditorDraft();
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.removeAttribute("editingQuestId");
            }
            log.info("Editor draft cleared (new). userSessionId={}", (s == null ? "null" : s.getId()));
            Web.redirectOk(req, resp, WebConst.Path.CREATE, "An empty draft of the quest has been created");
            return;
        }
        String loadId = Web.trimOrNull(req.getParameter(WebConst.Param.LOAD));
        if (loadId != null) {
            try {
                authoring.loadToEditor(loadId);
                req.getSession(true).setAttribute("editingQuestId", loadId);
                log.info("Quest loaded into editor questId={}", loadId);
                Web.redirectOk(req, resp, WebConst.Path.CREATE, "The quest is uploaded to the editor");
            } catch (Exception e) {
                log.warn("Failed to load quest into editor questId={} msg={}", loadId, e.getMessage());
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
                        log.debug("Prefilled form from node id={}", id);
                    }
                } catch (NumberFormatException ignored) {
                    log.debug("Invalid node id for prefill: '{}'", idStr);
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
                    log.debug("Restored form from session");
                }
            }
        }
        Web.forward(req, resp, WebConst.Jsp.CREATE);
    }

    /**
     * Handles editor actions:
     * <ul>
     *   <li>{@code replaceNode}: validates final/non-final constraints, processes image upload if present,
     *       merges with existing image if omitted, saves the node, redirects with OK;</li>
     *   <li>{@code deleteNode}: validates id and removes node from the draft, redirects with OK;</li>
     *   <li>Unknown action: redirects with an error.</li>
     * </ul>
     * On validation errors, stashes form inputs to session and redirects back with error flash.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getParameter(WebConst.Param.ACTION);
        if (action == null || action.isBlank()) {
            log.warn("Create action missing");
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
                        log.warn("replaceNode denied: non-final node without options");
                        Web.redirectErr(req, resp, WebConst.Path.CREATE, "For the NON-final branch, you must specify at least one answer option.\n");
                        return;
                    }
                    String uploadedPath;
                    try {
                        uploadedPath = handleImageUpload(req);
                    } catch (ServletException e) {
                        stashFormForRedirect(req);
                        log.warn("Image upload error: {}", e.getMessage());
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
                    log.info("Node saved id={} final={} hasImage={}", node.getId(), node.isFin(), (node.getImage() != null && !node.getImage().isBlank()));
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
                        log.warn("deleteNode: bad id '{}'", idRaw);
                        Web.redirectErr(req, resp, WebConst.Path.CREATE, "Specify the correct ID to delete");
                        return;
                    }
                    boolean removed = authoring.deleteNode(id);
                    if (!removed) {
                        stashFormForRedirect(req);
                        log.warn("deleteNode: node not found id={}", id);
                        Web.redirectErr(req, resp, WebConst.Path.CREATE, "Node #" + id + " not found in the draft");
                        return;
                    }
                    log.info("Node deleted id={}", id);
                    Web.redirect(req, resp, WebConst.Path.CREATE,
                            Map.of(WebConst.Param.CLEAR, "1", WebConst.Attr.OK, "Node #" + id + " deleted"));
                }
                default -> sendError(resp, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            stashFormForRedirect(req);
            log.warn("Create operation failed: {}", e.getMessage());
            Web.redirectErr(req, resp, WebConst.Path.CREATE, e.getMessage());
        }
    }

    /**
     * Processes an image upload (if any) and stores it under the configured uploads directory.
     * <p>
     * Returns the web path (e.g., {@code /uploads/quest-<ts>.jpg}) or {@code null} if no upload present.
     * Throws {@link ServletException} for invalid payloads (non-image, oversized).
     * </p>
     */
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

    /**
     * Returns a copy of {@link QuestNode} with its image path set, preserving its final/non-final shape.
     */
    private QuestNode withImage(QuestNode src, String imagePath) {
        if (src.isFin()) {
            return QuestNode.fin(src.getId(), src.getText(), imagePath);
        } else {
            return QuestNode.nonFin(src.getId(), src.getText(), src.getOptions(), imagePath);
        }
    }

    /**
     * Saves current form fields to the session so they can be restored after a redirect on error.
     */
    private void stashFormForRedirect(HttpServletRequest req) {
        HttpSession s = req.getSession(true);
        s.setAttribute("form_id", Optional.ofNullable(req.getParameter(WebConst.Param.ID)).orElse(""));
        s.setAttribute("form_text", Optional.ofNullable(req.getParameter(WebConst.Param.TEXT)).orElse(""));
        s.setAttribute("form_final", req.getParameter(WebConst.Param.FINAL) != null);
        s.setAttribute("form_options", Optional.ofNullable(req.getParameter(WebConst.Param.OPTIONS)).orElse(""));
    }

    /**
     * Redirects back to the editor with an encoded error message in the query string.
     */
    private void sendError(HttpServletResponse resp, String msg) throws IOException {
        resp.sendRedirect(resp.encodeRedirectURL(
                WebConst.Path.CREATE + "?" + WebConst.Attr.ERROR + "=" + Web.urlEncode(Objects.toString(msg, ""))
        ));
    }
}
