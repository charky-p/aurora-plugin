package org.charky.aurora.features.fishing;

import org.charky.aurora.common.loot.Fortune;
import org.charky.aurora.core.models.FishingSpotModel.FishModel;

import java.util.List;

// What a cast brings up and how long a bite takes. Pure numbers, tested directly, and
// kept in step with scripts/balance/fishing_prices.py, which prices the rod from them
public final class FishingRules {
    // Score a Luck level adds to the 1-100 roll
    static final int LUCK_SCORE = 2;
    // The wait for a bite in ticks: vanilla's is about 100 to 600, Lure shortens both
    // ends down to a floor
    static final int BITE_MIN_TICKS = 100, BITE_MIN_FLOOR = 20, BITE_MIN_STEP = 6;
    static final int BITE_MAX_TICKS = 400, BITE_MAX_FLOOR = 60, BITE_MAX_STEP = 25;

    private FishingRules() {
    }

    // The fish with the highest score not above roll + Luck, null if none is that low
    public static FishModel bite(List<FishModel> fish, int roll, int luck) {
        int score = roll + LUCK_SCORE * Math.max(0, luck);
        FishModel best = null;
        for (FishModel candidate : fish) {
            if (candidate.score <= score && (best == null || candidate.score > best.score)) best = candidate;
        }
        return best;
    }

    // How many come up: the amount rolled, times the Fortune curve past the fish's
    // density, 0 when the rod's Fortune is below it and the fish gets away
    public static int amount(FishModel fish, int rolled, int fortune) {
        double multiplier = Fortune.multiplier(fortune, fish.density);
        return multiplier <= 0 ? 0 : Fortune.scale(rolled, multiplier);
    }

    // {shortest, longest} wait for a bite at this Lure, in ticks
    public static int[] biteRange(int lure) {
        int level = Math.max(0, lure);
        return new int[]{Math.max(BITE_MIN_FLOOR, BITE_MIN_TICKS - BITE_MIN_STEP * level),
                Math.max(BITE_MAX_FLOOR, BITE_MAX_TICKS - BITE_MAX_STEP * level)};
    }
}
