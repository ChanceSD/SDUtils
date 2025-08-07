package me.chancesd.sdutils.display.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.chancesd.sdutils.display.chat.content.StaticContentProvider;

/**
 * Example command showing how to use the ChatMenu system with modern page number navigation
 */
public class ExampleCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ChatMenu helpMenu;

    public ExampleCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Create a help menu with pagination
        this.helpMenu = ChatMenu.builder()
            .header("&6=== Plugin Commands &7(Page {page}/{total}) &6===")
            .footer("&6=====================================")
            .linesPerPage(8)
            .contentProvider(new StaticContentProvider() {{
                addLine("&a/plugin help &7- Show this help menu", "/plugin help", "Click to show help");
                addLine("&a/plugin reload &7- Reload the plugin", "/plugin reload", "Click to reload");
                addLine("&a/plugin info &7- Plugin information", "/plugin info", "Click for info");
                addLine("&a/plugin list &7- List all players", "/plugin list", "Click to list players");
                addLine("&a/plugin stats &7- View statistics", "/plugin stats", "Click for stats");
                addLine("&a/plugin settings &7- Change settings", "/plugin settings", "Click for settings");
                addLine("&a/plugin toggle &7- Toggle features", "/plugin toggle", "Click to toggle");
                addLine("&a/plugin about &7- About the plugin", "/plugin about", "Click for about");
                addLine("&a/plugin debug &7- Debug information", "/plugin debug", "Click for debug info");
                addLine("&a/plugin status &7- Server status", "/plugin status", "Click for status");
            }})
            .navigation(NavigationButtons.builder()
                .navigationPrefix("/plugin") // Commands will be "/plugin 1", "/plugin 2", etc.
                .previousText("&7[&c← Previous&7]")
                .nextText("&7[&cNext →&7]")
                .build())
            .build();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            helpMenu.show(player);
            return true;
        }

        // Handle page number navigation: /plugin 1, /plugin 2, etc.
        if (args.length == 1) {
            try {
                int page = Integer.parseInt(args[0]);
                if (page > 0) {
                    helpMenu.show(player, page);
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid page number!");
                return true;
            }
        }

        return false;
    }
}
