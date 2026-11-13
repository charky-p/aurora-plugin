package org.charky.aurora.features.mine;

import java.util.ArrayList;
import java.util.List;

// Which blocks Blast breaks around the one mined: a patch on the face that was hit,
// like a hammer. Mining a wall breaks a patch of that wall, mining a floor or ceiling
// a patch of it. Pure numbers, no Bukkit, so every direction is tested
public final class BlastShape {
    // Blast level -> width, length, depth. Width runs to the player's right. Length
    // runs up a wall, or away from the player on a floor or ceiling. Depth runs into
    // the block. Odd sizes are centred on the block mined, even ones grow to the
    // right and up or away. Past the table the last holds.
    // Kept in step with BLAST_SHAPES in scripts/balance/balance_sim.py
    private static final int[][] SHAPES = {
            {2, 1, 1},
            {2, 2, 1},
            {3, 3, 1},
    };

    private BlastShape() {
    }

    // Offsets from the block mined to every other block broken. The face is the side
    // of the block that was hit, as a direction pointing out of it. Facing is the
    // compass direction the player looks in, it only matters for a floor or ceiling
    public static List<int[]> offsets(int level, int faceX, int faceY, int faceZ, int facingX, int facingZ) {
        if (level <= 0) return List.of();
        int[] shape = SHAPES[Math.min(level, SHAPES.length) - 1];

        int[] into = {-faceX, -faceY, -faceZ};
        int[] right;
        int[] length;
        if (faceY != 0) {
            right = new int[]{-facingZ, 0, facingX};
            length = new int[]{facingX, 0, facingZ};
        } else {
            // Looking into the wall, the right hand side runs along it
            right = new int[]{-into[2], 0, into[0]};
            length = new int[]{0, 1, 0};
        }

        List<int[]> offsets = new ArrayList<>();
        for (int w = -(shape[0] - 1) / 2; w <= shape[0] / 2; w++) {
            for (int l = -(shape[1] - 1) / 2; l <= shape[1] / 2; l++) {
                for (int d = 0; d < shape[2]; d++) {
                    if (w == 0 && l == 0 && d == 0) continue;
                    offsets.add(new int[]{
                            w * right[0] + l * length[0] + d * into[0],
                            w * right[1] + l * length[1] + d * into[1],
                            w * right[2] + l * length[2] + d * into[2]});
                }
            }
        }
        return offsets;
    }
}
