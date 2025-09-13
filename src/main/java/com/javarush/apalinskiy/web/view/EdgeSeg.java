package com.javarush.apalinskiy.web.view;

import lombok.Getter;

/**
 * Immutable view-model for a directed edge segment drawn in the quest graph.
 * <p>
 * This class carries presentation-only data and is safe to expose to JSP/EL.
 * Coordinates are expected to be already laid out by the server-side layout
 * (e.g., graph router); no validation or geometry is performed here.
 * </p>
 *
 * <h3>Coordinate system</h3>
 * <ul>
 *   <li>Units: SVG user units (pixels).</li>
 *   <li>Origin: top-left corner ({@code 0,0}).</li>
 *   <li>{@code (sx, sy)} — start point of the edge; {@code (tx, ty)} — end point.</li>
 * </ul>
 *
 * <h3>Semantics</h3>
 * <ul>
 *   <li>{@code from} → {@code to} denotes the directed link between nodes.</li>
 *   <li>{@code label} is optional; may be {@code null} if the edge has no label.</li>
 * </ul>
 *
 * <p>
 * Note: Any domain-level validation (e.g., forbidding self-loops or ensuring
 * node IDs exist) should be performed upstream.
 * </p>
 */
@Getter
public class EdgeSeg {

    /**
     * Identifier of the source node this edge originates from.
     */
    private final int from;
    /**
     * Identifier of the target node this edge points to.
     */
    private final int to;
    /**
     * Optional text displayed near the edge; may be {@code null}.
     */
    private final String label;
    /**
     * X coordinate of the edge's start point (pixels).
     */
    private final double sx;
    /**
     * Y coordinate of the edge's start point (pixels).
     */
    private final double sy;
    /**
     * X coordinate of the edge's end point (pixels).
     */
    private final double tx;
    /**
     * Y coordinate of the edge's end point (pixels).
     */
    private final double ty;

    /**
     * Creates a new directed edge segment between two nodes with given screen coordinates.
     *
     * @param from  source node id
     * @param to    target node id
     * @param label optional edge label; may be {@code null}
     * @param sx    start X (px)
     * @param sy    start Y (px)
     * @param tx    end X (px)
     * @param ty    end Y (px)
     */
    public EdgeSeg(int from, int to, String label, double sx, double sy, double tx, double ty) {
        this.from = from;
        this.to = to;
        this.label = label;
        this.sx = sx;
        this.sy = sy;
        this.tx = tx;
        this.ty = ty;
    }
}
