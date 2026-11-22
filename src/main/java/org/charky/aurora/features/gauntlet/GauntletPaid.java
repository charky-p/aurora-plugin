package org.charky.aurora.features.gauntlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

// The last week or day a board paid out, kept in a file of its own so a restart pays
// nothing twice. 0 before anything was paid
final class GauntletPaid {
    private final File file;
    private final Logger logger;

    GauntletPaid(File dataFolder, String name, Logger logger) {
        this.file = new File(dataFolder, name);
        this.logger = logger;
    }

    long read() {
        if (!file.exists()) return 0;
        try {
            return Long.parseLong(Files.readString(file.toPath()).trim());
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    void write(long paid) {
        try {
            Files.writeString(file.toPath(), String.valueOf(paid));
        } catch (IOException e) {
            logger.warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
