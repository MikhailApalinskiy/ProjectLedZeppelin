package com.javarush.apalinskiy.web.view;

import lombok.Getter;

import java.util.List;

/**
 * Represents a positioned quest node within the SVG-like graph layout.
 * <p>
 * Each {@code NodePos} stores layout coordinates, label lines, image reference,
 * and flags indicating whether the node is a start or a finish node.
 * Instances are immutable.
 */
@Getter
public final class NodePos {

    /**
     * Unique quest node ID.
     */
    private final int id;

    /**
     * X coordinate of the node’s top-left corner.
     */
    private final int x;

    /**
     * Y coordinate of the node’s top-left corner.
     */
    private final int y;

    /**
     * Whether this node is a final (ending) node.
     */
    private final boolean fin;

    /**
     * Whether this node is the starting node.
     */
    private final boolean start;

    /**
     * Wrapped label text lines displayed inside the node.
     */
    private final List<String> labelLines;

    /**
     * Optional image URL or path associated with this node.
     */
    private final String image;

    /**
     * Constructs a positioned node representation.
     *
     * @param id         quest node ID
     * @param x          X coordinate
     * @param y          Y coordinate
     * @param fin        {@code true} if this is a final node
     * @param start      {@code true} if this is the start node
     * @param labelLines text lines displayed within the node; empty list if {@code null}
     * @param image      optional image URL or path; {@code null} if blank
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
