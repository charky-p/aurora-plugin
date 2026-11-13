package org.charky.aurora.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagerTest {
    // 1 to count, as a list command would hand over its names
    private static List<Integer> entries(int count) {
        return IntStream.rangeClosed(1, count).boxed().toList();
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
    void anEmptyListStillHasOnePage() {
        assertEquals(1, Pager.pageCount(0));
    }

    @Test
    void aFullPageDoesNotStartTheNext() {
        assertEquals(1, Pager.pageCount(10));
        assertEquals(2, Pager.pageCount(11));
    }

    @Test
    void pagesRoundUp() {
        assertEquals(3, Pager.pageCount(21));
    }

    @Test
    void noPageArgumentOpensTheFirstTen() {
        List<String> heard = new ArrayList<>();

        List<Integer> page = Pager.open(sender(heard), new String[0], entries(25), "MINES", 3);

        assertEquals(entries(10), page);
        assertTrue(heard.getFirst().contains("MINES (1/3)"), heard.getFirst());
    }

    @Test
    void theLastPageHoldsWhatIsLeft() {
        List<Integer> page = Pager.open(sender(new ArrayList<>()), new String[]{"3"}, entries(25), "MINES", 3);

        assertEquals(List.of(21, 22, 23, 24, 25), page);
    }

    @Test
    void aPageThatDoesNotExistIsRefused() {
        List<String> heard = new ArrayList<>();

        assertNull(Pager.open(sender(heard), new String[]{"4"}, entries(25), "MINES", 3));
        assertNull(Pager.open(sender(heard), new String[]{"0"}, entries(25), "MINES", 3));

        assertEquals(2, heard.size());
        assertTrue(heard.getFirst().contains("Page 4 does not exist. (Max: 3)"), heard.getFirst());
    }

    @Test
    void aPageThatIsNotANumberIsRefused() {
        List<String> heard = new ArrayList<>();

        assertNull(Pager.open(sender(heard), new String[]{"two"}, entries(25), "MINES", 3));

        assertTrue(heard.getFirst().contains("Invalid page number: two"), heard.getFirst());
    }
}
