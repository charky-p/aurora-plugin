package org.charky.aurora.features.system;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import java.util.Random;

// Empty chunks for the worlds made with /aurora world create, everything in them is built by hand
public class VoidGenerator extends ChunkGenerator {
    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunkData) {

    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }
}