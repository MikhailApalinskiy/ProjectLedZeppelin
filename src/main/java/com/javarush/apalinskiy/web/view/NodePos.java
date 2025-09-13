package com.javarush.apalinskiy.web.view;

import lombok.Getter;

import java.util.List;

/**
 * Immutable-ish view-model of a node's position and presentation data
 * used to render the quest graph on JSP/SVG.
 * <p>
 * The class is intended for the view layer only and exposes read-only fields
 * via Lombok-generated getters. Geometry is assumed to be precomputed
 * by the server-side layout.
 * </p>
 *
 * <h3>Coordinate system</h3>
 * <ul>
 *   <li>Units: SVG user units (pixels).</li>
 *   <li>Origin: top-left corner ({@code 0,0}).</li>
 *   <li>{@code x}, {@code y} represent the node anchor (usually the center or top-left, depending on renderer).</li>
 * </ul>
 *
 * <h3>Semantics</h3>
 * <ul>
 *   <li>{@code id} — unique identifier of the node in the current graph.</li>
 *   <li>{@code start} — the node is the designated start of the quest.</li>
 *   <li>{@code fin} — the node is terminal (no outgoing choices) and is rendered as a finish/end node.</li>
 *   <li>{@code labelLines} — pre-split lines of the node caption; never {@code null} (empty if absent).</li>
 *   <li>{@code image} — optional image path/URL to represent the node visually; may be {@code null}.</li>
 * </ul>
 *
 * <h3>Null &amp; normalization rules</h3>
 * <ul>
 *   <li>If {@code labelLines} is {@code null}, it is normalized to {@code List.of()}.</li>
 *   <li>If {@code image} is {@code null} or blank, it is normalized to {@code null}.</li>
 * </ul>
 *
 * <h3>Immutability note</h3>
 * <p>
 * While fields are {@code final}, the provided {@code labelLines} list is stored as-is.
 * If a mutable list is passed in, external modifications will be reflected here.
 * Pass an immutable list (e.g., {@code List.of(...)} or {@code List.copyOf(...)})
 * if strict immutability is required.
 * </p>
 */
@Getter
public final class NodePos {

    /**
     * Unique identifier of the node.
     */
    private final int id;
    /**
     * X coordinate of the node (pixels).
     */
    private final int x;
    /**
     * Y coordinate of the node (pixels).
     */
    private final int y;
    /**
     * Whether this node is terminal (finish).
     */
    private final boolean fin;
    /**
     * Whether this node is the designated start node.
     */
    private final boolean start;
    /**
     * Pre-split lines of the node label; never {@code null}.
     */
    private final List<String> labelLines;
    /**
     * Optional image path or URL for the node; {@code null} if absent.
     */
    private final String image;

    /**
     * Creates a positioned node model for rendering.
     *
     * @param id         unique node id within the current graph
     * @param x          X coordinate in pixels
     * @param y          Y coordinate in pixels
     * @param fin        whether the node is terminal (finish)
     * @param start      whether the node is the start node
     * @param labelLines pre-split label lines; {@code null} becomes {@code List.of()}
     * @param image      optional image path/URL; blank becomes {@code null}
     */
    public NodePos(int id, int x, int y, boolean fin, boolean start, List<String> labelLines, String image) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.fin = fin;
        this.start = start;
        this.labelLines = (labelLines == null) ? List.of() : labelLines;
        this.image = (image == null || image.isBlank()) ? null : image;
    }
}
