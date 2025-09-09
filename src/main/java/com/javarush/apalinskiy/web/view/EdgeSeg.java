package com.javarush.apalinskiy.web.view;

import lombok.Getter;

@Getter
public class EdgeSeg {

    private final int from;
    private final int to;
    private final String label;
    private final double sx;
    private final double sy;
    private final double tx;
    private final double ty;

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
