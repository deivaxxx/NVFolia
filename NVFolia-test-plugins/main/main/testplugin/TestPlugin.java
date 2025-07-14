package io.nvfolia.testplugin;

import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import net.minecraft.util.RandomSource;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class TestPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("Enabling test plugin for NVFolia");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getCommandMap().register("rtp", new BukkitCommand("rtp") {
            @Override
            public boolean execute(@NotNull final CommandSender sender, @NotNull final String commandLabel, final @NotNull String @NotNull [] args) {
                if (sender instanceof Player player) {
                    RandomSource randomSource = RandomSource.create();
                    int blockX = build(randomSource);
                    int blockZ = build(randomSource);
                    player.teleportAsync(
                        new Location(
                            player.getWorld(), blockX, 90, blockZ, player.getYaw(), player.getPitch()
                        )
                    );
                    return true;
                }
                return false;
            }
        });
    }

    public int build(@NotNull RandomSource randomSource) {
        int number = randomSource.nextInt(100000);
        return randomSource.nextBoolean() ? -number : number;
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling test plugin for NVFolia");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent playerRespawnEvent) {
        getLogger().info("PlayerRespawnEvent called!");
        // uncomment when testing respawn location modification
        // playerRespawnEvent.setRespawnLocation(new Location(
        //     Bukkit.getWorld("world"), 0, 9000, 0
        // ));
        // uncomment when testing player kicking during respawn
        // playerRespawnEvent.getPlayer().kick(Component.text("Bye bye"));
    }

    @EventHandler
    public void onTeleportPlayer(PlayerTeleportEvent playerTeleportEvent) {
        getLogger().info("PlayerTeleportEvent called!");
    }

    @EventHandler
    public void onTeleportEntity(EntityTeleportEvent playerTeleportEvent) {
        getLogger().info("EntityTeleportEvent called!");
    }

    @EventHandler
    public void onTeleportEndGateway(PlayerTeleportEndGatewayEvent playerTeleportEndGatewayEvent) {
        getLogger().info("PlayerTeleportEndGatewayEvent called!");
    }
}
