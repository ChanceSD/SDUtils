package me.chancesd.sdutils.display.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import me.chancesd.sdutils.display.chat.content.StaticContentProvider;
import net.md_5.bungee.api.chat.BaseComponent;

class ChatMenuTest {

    @Mock
    private Player player;

    @Mock
    private Player.Spigot spigot;

    private UUID playerUuid;
    private ChatMenu menu;

	@BeforeAll
	static void beforeAll() {
		Bukkit.setServer(Mockito.mock(Server.class, RETURNS_MOCKS));
	}

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        playerUuid = UUID.randomUUID();

        when(player.getName()).thenReturn("TestPlayer");
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.spigot()).thenReturn(spigot);
    }

    @Test
    void shouldDisplayAllContentInBasicMenu() {
        final StaticContentProvider content = new StaticContentProvider();
        content.addLine("Line 1");
        content.addLine("Line 2");
        content.addLine("Line 3");

        menu = ChatMenu.builder()
                .header("Test Menu")
                .contentProvider(content)
                .build();

        menu.show(player);

        // Verify content size
        assertEquals(3, menu.getContent(player).size(), "Menu should contain all lines");

        // Header + 3 content lines = 4 messages
        verify(spigot, times(4)).sendMessage(any(BaseComponent.class));
    }

    @Test
    void shouldNavigateToNextPage() {
        final StaticContentProvider content = new StaticContentProvider();
        content.addLine("Line 1");
        content.addLine("Line 2");
        content.addLine("Line 3");
        content.addLine("Line 4");

        menu = ChatMenu.builder()
                .header("Test Menu")
                .linesPerPage(2)
                .contentProvider(content)
                .navigation(NavigationButtons.builder().build())
                .build();

        menu.show(player);
        clearInvocations(spigot);

        menu.show(player, 2); // Navigate to page 2 directly

        // Header + 2 content lines + navigation = 4 messages
        verify(spigot, times(4)).sendMessage(any(BaseComponent.class));
    }
}
