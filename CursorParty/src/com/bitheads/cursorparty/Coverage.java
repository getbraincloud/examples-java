package com.bitheads.cursorparty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Pure port of the cpp reference client's coverage.cpp rasterized-ownership-grid
// algorithm: every splotch is stamped as a filled circle onto a coarse grid, in
// paint order, last one wins; coverage% = owned-cell-count * cellArea / canvasArea.
public class Coverage
{
    private static final float CANVAS_W = 800.0f;
    private static final float CANVAS_H = 600.0f;
    private static final float SPLOTCH_RADIUS = 32.0f; // matches GameScreen's 64px rendered diameter
    private static final float CELL_SIZE = 2.0f;

    public static class CoverageEntry
    {
        public String cxId = "";
        public int colorIndex = 0;
        public float coveragePct = 0f;
        public int visibleCount = 0;
        public int rank = 0;
        public int beaten = 0;
    }

    // Java's Splotch has no ownerCxId (never extended the wire format that far —
    // same limitation the react port accepted), so attribution is by colorIndex
    // only, matched against the first live member with that colour.
    public static List<CoverageEntry> computeCoverage(List<Splotch> splotches, List<User> members)
    {
        int gridW = (int) Math.ceil(CANVAS_W / CELL_SIZE);
        int gridH = (int) Math.ceil(CANVAS_H / CELL_SIZE);
        int[] owner = new int[gridW * gridH];
        java.util.Arrays.fill(owner, -1);

        ArrayList<CoverageEntry> result = new ArrayList<>();
        for (User m : members)
        {
            CoverageEntry e = new CoverageEntry();
            e.cxId = m.cxId;
            e.colorIndex = m.colorIndex;
            result.add(e);
        }

        int cellRadius = (int) Math.ceil(SPLOTCH_RADIUS / CELL_SIZE);
        float r2 = SPLOTCH_RADIUS * SPLOTCH_RADIUS;

        for (Splotch s : splotches)
        {
            int memberIndex = -1;
            for (int i = 0; i < members.size(); i++)
            {
                if (members.get(i).colorIndex == s.colorIndex)
                {
                    memberIndex = i;
                    break;
                }
            }

            float px = (float) (s.pos.getX() * CANVAS_W);
            float py = (float) (s.pos.getY() * CANVAS_H);
            int centerGX = (int) Math.floor(px / CELL_SIZE);
            int centerGY = (int) Math.floor(py / CELL_SIZE);

            int gxMin = Math.max(0, centerGX - cellRadius);
            int gxMax = Math.min(gridW - 1, centerGX + cellRadius);
            int gyMin = Math.max(0, centerGY - cellRadius);
            int gyMax = Math.min(gridH - 1, centerGY + cellRadius);

            for (int gy = gyMin; gy <= gyMax; gy++)
            {
                float cy = (gy + 0.5f) * CELL_SIZE;
                for (int gx = gxMin; gx <= gxMax; gx++)
                {
                    float cx = (gx + 0.5f) * CELL_SIZE;
                    float dx = cx - px;
                    float dy = cy - py;
                    if (dx * dx + dy * dy <= r2)
                        owner[gy * gridW + gx] = memberIndex; // unconditional overwrite: paint order = last one wins
                }
            }
        }

        for (int c = 0; c < owner.length; c++)
        {
            if (owner[c] >= 0)
                result.get(owner[c]).visibleCount++;
        }

        float cellArea = CELL_SIZE * CELL_SIZE;
        float canvasArea = CANVAS_W * CANVAS_H;
        for (CoverageEntry e : result)
            e.coveragePct = Math.min(100f, e.visibleCount * cellArea / canvasArea * 100f);

        Collections.sort(result, (a, b) -> {
            if (a.coveragePct != b.coveragePct) return Float.compare(b.coveragePct, a.coveragePct);
            if (a.visibleCount != b.visibleCount) return Integer.compare(b.visibleCount, a.visibleCount);
            return a.cxId.compareTo(b.cxId);
        });

        int rank = 1;
        for (int i = 0; i < result.size(); i++)
        {
            if (i > 0)
            {
                CoverageEntry prev = result.get(i - 1);
                CoverageEntry cur = result.get(i);
                if (cur.coveragePct != prev.coveragePct || cur.visibleCount != prev.visibleCount)
                    rank = i + 1;
            }
            result.get(i).rank = rank;
        }

        for (CoverageEntry e : result)
        {
            int beaten = 0;
            for (CoverageEntry other : result)
                if (other.rank > e.rank) beaten++;
            e.beaten = beaten;
        }

        return result;
    }
}
