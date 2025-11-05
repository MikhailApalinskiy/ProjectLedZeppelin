package com.javarush.apalinskiy.web.view;

import lombok.Getter;

/**
 * Represents a directed edge segment in a quest graph model.
 * <p>
 * Each edge connects two nodes ({@code from → to}) and contains
 * both visual coordinates and a text label (choice).
 * Used by the SVG rendering logic to draw connections between quest nodes.
 */
@Getter
public class EdgeSeg {

    /**
     * Source node ID.
     */
    private final int from;

    /**
     * Target node ID.
     */
    private final int to;

    /**
     * Label or choice text displayed along the edge.
     */
    private final String label;

    /**
     * Start X coordinate of the edge line.
     */
    private final double sx;

    /**
     * Start Y coordinate of the edge line.
     */
    private final double sy;

    /**
     * End X coordinate of the edge line.
     */
    private final double tx;

    /**
     * End Y coordinate of the edge line.
     */
    private final double ty;

    /**
     * Constructs an edge segment.
     *
     * @param from  source node ID
     * @param to    target node ID
     * @param label edge label (e.g. option text)
     * @param sx    start X coordinate
     * @param sy    start Y coordinate
     * @param tx    end X coordinate
     * @param ty    end Y coordinate
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
