package de.hft_stuttgart.ip1;

import java.util.List;

public class Table {
    public final float x, y; // y is TOP edge
    public final int cols, rows;
    public final List<Float> w, h;

    public Table(float x, float y, int cols, int rows, List<Float> w, List<Float> h) {
        this.x = x; this.y = y; this.cols = cols; this.rows = rows;
        this.w = w; this.h = h;
    }

    public float totalW() { float s = 0; for (float v : w) s += v; return s; }
    public float totalH() { float s = 0; for (float v : h) s += v; return s; }

    public float colW(int c) { return w.get(c); }
    public float rowH(int r) { return h.get(r); }

    public float cellX(int c) {
        float xx = x;
        for (int i = 0; i < c; i++) xx += w.get(i);
        return xx;
    }

    public float cellY(int r) {
        float yy = y;
        for (int i = 0; i < r; i++) yy -= h.get(i);
        return yy - h.get(r);
    }

    public float[] cellTextPos(int c, int r, float inset) {
        float px = cellX(c) + inset;
        float py = cellY(r) + h.get(r) - inset - 2;
        return new float[]{px, py};
    }
}
