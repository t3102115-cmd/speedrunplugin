package speedrunmod.quantiom.speedrun;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpeedrunCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        long now = System.currentTimeMillis();
        long last = SpeedrunPlugin.instance.cooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < SpeedrunPlugin.COOLDOWN) {
            long sec = (SpeedrunPlugin.COOLDOWN - (now - last)) / 1000;
            player.sendMessage("§cCooldown! Wait another " + sec + " seconds.");
            return true;
        }

        SpeedrunPlugin.instance.cooldowns.put(player.getUniqueId(), now);

        int count = 1;
        while (Bukkit.getWorld(player.getName() + "_" + count) != null) {
            count++;
        }

        String base = player.getName() + "_" + count;

        // === Multiverse World Creation ===
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mv create " + base + " normal");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mv create " + base + "_nether nether");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mv create " + base + "_end the_end");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mvnp link nether " + base + " " + base + "_nether");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mvnp link end " + base + " " + base + "_end");

        // === Teleport + Start Timer ===
        Bukkit.getScheduler().runTaskLater(SpeedrunPlugin.instance, () -> {
            World w = Bukkit.getWorld(base);
            if (w != null) {
                player.teleport(w.getSpawnLocation());
                SpeedrunPlugin.instance.startSpeedrun(player, base);
            }
        }, 40L);

        // === Auto Delete after 1 Day ===
        Bukkit.getScheduler().runTaskLater(
                SpeedrunPlugin.instance,
                () -> SpeedrunPlugin.instance.stopSpeedrun(player, false),
                SpeedrunPlugin.DELETE_AFTER / 50
        );

        player.sendMessage("§aSpeedrun world §e" + base + " §acreated!");
        return true;
    }
}
