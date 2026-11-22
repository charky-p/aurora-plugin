package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.util.List;

// The altar every altar_every floors from altar_first: the angel's pact (a full heal and
// a heart more for the run), the devil's (an epic boon for hearts) or walking on
public class AltarMenu extends PagedMenu {
    static final String ANGEL = "angel";
    static final String DEVIL = "devil";
    static final String SKIP = "skip";
    private static final List<String> CHOICES = List.of(ANGEL, DEVIL, SKIP);

    private final GauntletManager manager;
    private final GauntletRuns runs;

    public AltarMenu(GauntletManager manager, GauntletRuns runs) {
        super(manager.registry(), "The Altar");
        this.manager = manager;
        this.runs = runs;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        int angel = (int) manager.data().setting("altar_angel_hearts", 1);
        int devil = (int) manager.data().setting("altar_devil_hearts", 2);
        return List.of(
                button(Material.GOLDEN_APPLE, "<yellow><b>Angel's Pact", List.of("<gray>Heal to full and <white>+" + angel + " max heart"
                        + (angel == 1 ? "" : "s") + "</white> for this run")),
                button(Material.WITHER_SKELETON_SKULL, "<dark_red><b>Devil's Pact", List.of("<gray>A random <light_purple>epic boon</light_purple>,",
                        "<gray>but <red>" + devil + " fewer max hearts</red> for this run")),
                button(Material.BARRIER, "<gray><b>Walk On", List.of("<gray>Leave the altar as it is")));
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>An altar on the way up."),
                TextUtils.parse("<!italic><gray>The next floor starts once you choose."));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index < CHOICES.size()) runs.altar(player, CHOICES.get(index));
    }

    @Override
    protected void back(Player player) {
        player.closeInventory();
    }
}
