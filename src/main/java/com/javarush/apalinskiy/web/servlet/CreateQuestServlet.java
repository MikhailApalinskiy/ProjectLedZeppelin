package com.javarush.apalinskiy.web.servlet;

import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.javarush.apalinskiy.web.util.Uploads.resolveBaseDir;

/**
 * Servlet powering the “Create/Edit Quest” editor.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Initialize and load the in-browser editor draft</li>
 *   <li>Prefill the form from an existing node or session-stashed values</li>
 *   <li>Handle node save (replace) and node delete operations</li>
 *   <li>Handle optional image uploads for nodes</li>
 * </ul>
 *
 * <p>GET endpoints:</p>
 * <ul>
 *   <li>{@code ?new} — clears the current draft and starts a fresh one</li>
 *   <li>{@code ?load=<questId>} — loads an existing quest into the editor</li>
 *   <li>Otherwise renders the editor page, attempting to prefill the form</li>
 * </ul>
 *
 * <p>POST actions:</p>
 * <ul>
 *   <li>{@code replaceNode} — create/update a node (with validation and optional image)</li>
 *   <li>{@code deleteNode} — remove a node by id</li>
 * </ul>
 */
public class CreateQuestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(CreateQuestServlet.class);

    private QuestAuthoringService authoring;

    /**
     * Resolves the {@link QuestAuthoringService} from the application context.
     *
     * @param config servlet config
     * @throws ServletException if the service bean is missing
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
     * Renders the editor page and handles draft lifecycle navigation.
     *
     * <p>Supported query parameters:</p>
     * <ul>
     *   <li>{@code new} — start an empty draft</li>
     *   <li>{@code load} — quest id to load into the editor</li>
     *   <li>{@code clear=1} — skip prefill logic and render an empty form</li>
     *   <li>{@code id} — node id to prefill the form from</li>
     * </ul>
     *
     * <p>Prefill strategy (if not {@code clear=1}):</p>
     * <ol>
     *   <li>Try to prefill by the node id from {@code id}</li>
     *   <li>Else restore stashed form values from session (after redirect)</li>
     *   <li>Else prefill from the first node present in the draft</li>
     * </ol>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (req.getParameter(WebConst.Param.NEW) != null) {
            authoring.clearEditorDraft();
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.removeAttribute("editingQuestId");
                s.removeAttribute("editingQuestName");
            }
            log.info("Editor draft cleared (new). userSessionId={}", (s == null ? "null" : s.getId()));
            Web.redirectOk(req, resp, WebConst.Path.CREATE, "An empty draft of the quest has been created");
            return;
        }
        String loadId = Web.trimOrNull(req.getParameter(WebConst.Param.LOAD));
        if (loadId != null) {
            try {
                authoring.loadToEditor(loadId);
                Optional<CustomQuest> opt = authoring.getFromCatalog(loadId);
                HttpSession s = req.getSession(true);
                if (opt.isPresent() && opt.get().getModerationStatus() == CustomQuest.ModerationStatus.LIVE) {
                    s.setAttribute("editingQuestId", loadId);
                    s.setAttribute("editingQuestName", opt.get().getName());
                } else {
                    s.removeAttribute("editingQuestId");
                    s.removeAttribute("editingQuestName");
                }
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
            boolean formAlreadySet = false;
            String idStr = req.getParameter(WebConst.Param.ID);
            if (idStr != null && !idStr.isBlank()) {
                try {
                    int id = Integer.parseInt(idStr.trim());
                    QuestNode node = authoring.get(id);
                    if (node != null) {
                        prefillFromNode(req, node);
                        formAlreadySet = true;
                        log.debug("Prefilled form from node id={}", id);
                    }
                } catch (NumberFormatException ignored) {
                    log.debug("Invalid node id for prefill: '{}'", idStr);
                }
            }
            if (!formAlreadySet) {
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
                        formAlreadySet = true;
                        log.debug("Restored form from session");
                    }
                }
            }
            if (!formAlreadySet) {
                List<QuestNode> nodes = authoring.nodes();
                if (nodes != null && !nodes.isEmpty()) {
                    QuestNode first = nodes.getFirst();
                    prefillFromNode(req, first);
                    log.debug("Prefilled form from FIRST node id={}", first.getId());
                } else {
                    log.debug("No nodes in draft — leave form empty");
                }
            }
        }
        Web.forward(req, resp, WebConst.Jsp.CREATE);
    }

    /**
     * Handles editor actions: replace node or delete node.
     *
     * <p>For {@code replaceNode}:</p>
     * <ul>
     *   <li>Validates that non-final nodes have at least one option</li>
     *   <li>Parses form values into a {@link QuestNode}</li>
     *   <li>Handles optional image upload and merges with existing image if omitted</li>
     *   <li>Saves the node via {@link QuestAuthoringService#saveNode(QuestNode)}</li>
     * </ul>
     *
     * <p>For {@code deleteNode}:</p>
     * <ul>
     *   <li>Validates numeric node id</li>
     *   <li>Deletes via {@link QuestAuthoringService#deleteNode(int)}</li>
     * </ul>
     *
     * <p>On validation errors, stashes the form in session and redirects back with a message.</p>
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
                    log.info("Node saved id={} final={} hasImage={}", node.getId(), node.getFin(), (node.getImage() != null && !node.getImage().isBlank()));
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
     * Handles optional multipart image upload for a node.
     *
     * <p>Validates content type as {@code image/*} and saves the file under app’s uploads directory.
     * Returns the public path (e.g., {@code /uploads/quest-<ts>.jpg}) or {@code null} if nothing uploaded.</p>
     *
     * @param req HTTP request (possibly multipart)
     * @return public image path or {@code null}
     * @throws IOException      if saving the file fails
     * @throws ServletException if multipart size limits are exceeded or type is invalid
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
     * Returns a copy of the node that carries the provided image path.
     *
     * @param src       original node
     * @param imagePath image path to set
     * @return node with the image path applied (keeps final/non-final semantics)
     */
    private QuestNode withImage(QuestNode src, String imagePath) {
        if (src.getFin()) {
            return QuestNode.fin(src.getId(), src.getText(), imagePath);
        } else {
            return QuestNode.nonFin(src.getId(), src.getText(), src.getOptions(), imagePath);
        }
    }

    /**
     * Stashes form values into the session to survive a redirect after validation errors.
     *
     * @param req HTTP request containing form fields
     */
    private void stashFormForRedirect(HttpServletRequest req) {
        HttpSession s = req.getSession(true);
        s.setAttribute("form_id", Optional.ofNullable(req.getParameter(WebConst.Param.ID)).orElse(""));
        s.setAttribute("form_text", Optional.ofNullable(req.getParameter(WebConst.Param.TEXT)).orElse(""));
        s.setAttribute("form_final", req.getParameter(WebConst.Param.FINAL) != null);
        s.setAttribute("form_options", Optional.ofNullable(req.getParameter(WebConst.Param.OPTIONS)).orElse(""));
    }

    /**
     * Redirects to the editor with an URL-encoded error message.
     *
     * @param resp response to send the redirect
     * @param msg  error message to show
     * @throws IOException if redirect fails
     */
    private void sendError(HttpServletResponse resp, String msg) throws IOException {
        resp.sendRedirect(resp.encodeRedirectURL(
                WebConst.Path.CREATE + "?" + WebConst.Attr.ERROR + "=" + Web.urlEncode(Objects.toString(msg, ""))
        ));
    }

    /**
     * Prefills editor form attributes from a given node.
     *
     * @param req  request to receive attributes
     * @param node node whose values are used
     */
    private static void prefillFromNode(HttpServletRequest req, QuestNode node) {
        req.setAttribute("form_id", node.getId());
        req.setAttribute("form_text", node.getText());
        req.setAttribute("form_final", node.getFin());
        req.setAttribute("form_image", node.getImage());
        if (!node.getFin()) {
            StringBuilder sb = new StringBuilder();
            for (Option o : node.getOptions()) {
                if (o == null || o.getNext() == null) continue;
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(o.getChoice()).append(" -> ").append(o.getNext());
            }
            req.setAttribute("form_options", sb.toString());
        }
    }
}
