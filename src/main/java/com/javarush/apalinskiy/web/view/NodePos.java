package com.javarush.apalinskiy.web.view;

import lombok.Getter;

import java.util.List;

@Getter
public final class NodePos {
    private final int id;
    private final int x;
    private final int y;
    private final boolean fin;
    private final boolean start;
    private final List<String> labelLines;
    private final String image;

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
