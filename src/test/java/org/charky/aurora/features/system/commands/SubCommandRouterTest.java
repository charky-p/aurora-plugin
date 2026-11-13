package org.charky.aurora.features.system.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// /aurora mine, spawner, portal and trader all route through this
class SubCommandRouterTest {
    // Remembers what it was last run with
    private static final class Recorder implements AuroraSubCommand {
        private final String name;
        private String[] executed;
        private String[] completed;

        Recorder(String name) {
            this.name = name;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            executed = args;
        }

        @Override
        public List<String> tabComplete(Player player, String[] args) {
            completed = args;
            return List.of("from-" + name);
        }

        @Override
        public String getUsage() {
            return "<thing>";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "Test command " + name;
        }
    }

    private static final class Router extends SubCommandRouter {
        Router(AuroraSubCommand... commands) {
            super("mine", "Test router");
            for (AuroraSubCommand command : commands) register(command);
        }
    }

    // A sender that only collects the plain text of what it is told
    private static CommandSender sender(List<String> heard) {
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class}, (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args != null && args[0] instanceof Component message) {
                        heard.add(PlainTextComponentSerializer.plainText().serialize(message));
                    }
                    return null;
                });
    }

    @Test
    void theSubcommandGetsTheArgumentsAfterItsName() {
        Recorder create = new Recorder("create");
        Recorder delete = new Recorder("delete");

        new Router(create, delete).execute(sender(new ArrayList<>()), new String[]{"create", "coal", "50"});

        assertArrayEquals(new String[]{"coal", "50"}, create.executed);
        assertNull(delete.executed);
    }

    @Test
    void subcommandNamesIgnoreCase() {
        Recorder create = new Recorder("create");

        new Router(create).execute(sender(new ArrayList<>()), new String[]{"CREATE"});

        assertArrayEquals(new String[0], create.executed);
    }

    @Test
    void noSubcommandListsThemAll() {
        Recorder create = new Recorder("create");
        List<String> heard = new ArrayList<>();

        new Router(create, new Recorder("delete")).execute(sender(heard), new String[0]);

        assertNull(create.executed);
        String all = String.join("\n", heard);
        assertTrue(all.contains("MINE COMMANDS"), all);
        // Each line is the subcommand's own full usage with its description
        assertTrue(all.contains("/aurora create <thing> » Test command create"), all);
        assertTrue(all.contains("/aurora delete <thing> » Test command delete"), all);
    }

    @Test
    void anUnknownSubcommandSaysSoAndRunsNothing() {
        Recorder create = new Recorder("create");
        List<String> heard = new ArrayList<>();

        new Router(create).execute(sender(heard), new String[]{"explode"});

        assertNull(create.executed);
        assertTrue(heard.getFirst().contains("Unknown mine command: explode"), heard.getFirst());
    }

    @Test
    void theFirstArgumentCompletesToMatchingNamesInOrder() {
        Router router = new Router(new Recorder("settp"), new Recorder("setblocks"), new Recorder("create"));

        assertEquals(List.of("setblocks", "settp"), router.tabComplete(null, new String[]{"SET"}));
        assertEquals(List.of("create", "setblocks", "settp"), router.tabComplete(null, new String[]{""}));
    }

    @Test
    void laterArgumentsAreCompletedByTheSubcommand() {
        Recorder create = new Recorder("create");
        Router router = new Router(create);

        assertEquals(List.of("from-create"), router.tabComplete(null, new String[]{"create", "co"}));
        assertArrayEquals(new String[]{"co"}, create.completed);
        assertEquals(List.of(), router.tabComplete(null, new String[]{"explode", "co"}));
    }

    @Test
    void itDescribesItselfForTheParentCommand() {
        Router router = new Router();

        assertEquals("mine", router.getName());
        assertEquals("/aurora mine <subcommand>", router.getFullUsage());
        assertEquals("Test router", router.getDescription());
    }
}
