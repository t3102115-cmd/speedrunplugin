package speedrunmod.quantiom.speedrun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedrunPlugin extends JavaPlugin implements Listener {

    public static SpeedrunPlugin instance;

    public static final long COOLDOWN = 60_000;        // 1 Minute
    public static final long DELETE_AFTER = 86_400_000; // 1 Tag

    public final Map<UUID, Long> cooldowns = new HashMap<>();

    private final Map<UUID, Long> startTimes = new HashMap<>();
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();
    private final Map<UUID, String> activeWorlds = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        getCommand("speedrun").setExecutor(new SpeedrunCommand());
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // ================= TIMER =================

    public void startSpeedrun(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) return;

        activeWorlds.put(uuid, worldName);
        startTimes.put(uuid, System.currentTimeMillis());

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - startTimes.get(uuid);
                long sec = elapsed / 1000;
                long min = sec / 60;
                sec %= 60;

                player.sendActionBar(
                        Component.text("Speedrun: " + min + "m " + sec + "s")
                                .color(NamedTextColor.GREEN)
                );

            }
        };

        task.runTaskTimer(this, 0L, 20L);
        tasks.put(uuid, task);
    }

    public void stopSpeedrun(Player player, boolean success) {
        UUID uuid = player.getUniqueId();

        if (tasks.containsKey(uuid)) {
            tasks.get(uuid).cancel();
            tasks.remove(uuid);
        }

        if (startTimes.containsKey(uuid)) {
            long elapsed = System.currentTimeMillis() - startTimes.get(uuid);
            long sec = elapsed / 1000;
            long min = sec / 60;
            sec %= 60;

            if (success) {
                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName()
                        + " beat the speedrun in " + min + "m " + sec + "s!");
            }

            startTimes.remove(uuid);
        }

        String base = activeWorlds.remove(uuid);
        if (base != null) {
            deleteWorld(base);
        }
    }

    // ================= EVENTS =================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopSpeedrun(event.getPlayer(), false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        player.sendMessage(ChatColor.RED + "You died! Speedrun failed.");
        stopSpeedrun(player, false);
    }

    @EventHandler
    public void onDragonKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        Player killer = dragon.getKiller();
        if (killer == null) return;

        stopSpeedrun(killer, true);
    }

    // ================= WORLD DELETE =================

    private void deleteWorld(String base) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + base);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + base + "_nether");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv delete " + base + "_end");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv remove " + base + " --remove-players");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv remove " + base + "_nether" + " --remove-players");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv remove " + base + "_end" + " --remove-players");
    }
}
