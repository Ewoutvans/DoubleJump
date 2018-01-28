package cloud.zeroprox.doublejump;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Cooldown {

    private HashMap<UUID, Long> coolmap = new HashMap<>();

    public void setCoolmap(Player player, int seconds) {
        coolmap.put(player.getUniqueId(), System.currentTimeMillis() / 1000);
        Sponge.getScheduler().createTaskBuilder().execute(task -> {
            coolmap.remove(player.getUniqueId());
            if (DoubleJump.getInstance().toggle_stop && DoubleJump.getInstance().toggled.contains(player.getUniqueId())) return;
            if (!DoubleJump.getInstance().toggle_stop && !DoubleJump.getInstance().toggled.contains(player.getUniqueId())) return;
            player.offer(Keys.CAN_FLY, true);
            player.sendMessage(ChatTypes.ACTION_BAR, DoubleJump.getInstance().message_recharged);
        }).delay(seconds, TimeUnit.SECONDS)
        .submit(DoubleJump.getInstance());
    }

    public boolean hasCooldown(Player player) {
        return coolmap.containsKey(player.getUniqueId());
    }

    public Long getTimeLeft(Player player, int seconds) {
        long current = System.currentTimeMillis() / 1000L;
        return seconds - (current - coolmap.get(player.getUniqueId()));
    }

    public void resetCooldown(Player player) {
        coolmap.remove(player.getUniqueId());
    }
}
