package speedrunmod.quantiom.speedrun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SpeedrunTimerListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> startTimes = new HashMap<>();
    private final Set<UUID> running = new HashSet<>();

    public SpeedrunTimerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    //  Start Timer wenn Spieler Speedrun-Welt betritt
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (isSpeedrunWorld(world)) {
            startTimer(player);
        }
    }

    private void startTimer(Player player) {
        if (running.contains(player.getUniqueId())) return;

        running.add(player.getUniqueId());
        startTimes.put(player.getUniqueId(), System.currentTimeMillis());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !running.contains(player.getUniqueId())) {
                    cancel();
                    return;
                }

                long time = System.currentTimeMillis() - startTimes.get(player.getUniqueId());
                player.sendActionBar(
                        Component.text(" " + formatTime(time), NamedTextColor.GOLD)
                );
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    //  Ende wenn Credits kommen (Teleport ins End-Credits-Dimension)
    @EventHandler
    public void onCredits(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) return;

        Player player = event.getPlayer();
        World to = event.getTo().getWorld();

        if (to.getEnvironment() == World.Environment.THE_END && running.contains(player.getUniqueId())) {
            finish(player);
        }
    }

    private void finish(Player player) {
        long time = System.currentTimeMillis() - startTimes.get(player.getUniqueId());
        running.remove(player.getUniqueId());
        startTimes.remove(player.getUniqueId());

        Bukkit.broadcast(
                Component.text(
                        player.getName() + " beat the game in " + formatTime(time) + "!",
                        NamedTextColor.GREEN
                )
        );
    }

    private boolean isSpeedrunWorld(World world) {
        return world.getName().contains(" ");
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        if (h > 0)
            return String.format("%d:%02d:%02d", h, m, s);
        else
            return String.format("%02d:%02d", m, s);
    }
}
