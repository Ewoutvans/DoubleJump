package cloud.zeroprox.doublejump;

import cloud.zeroprox.doublejumpmixins.event.ToggleFlyEvent;
import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Plugin(id = "doublejump", name = "DoubleJump", description = "DoubleJump", url = "https://zeroprox.cloud", authors = {"ewoutvs_", "Alagild"}, dependencies = @Dependency(id = "doublejumpmixins", optional = true))
public class DoubleJump {

    @Inject
    private Logger logger;
    private Cooldown cooldown = new Cooldown();
    private static DoubleJump instance;
    private Random random = new Random();

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    private ConfigurationNode rootNode;

    public Set<UUID> toggled = new HashSet<>();

    CommandSpec toggleCmd = CommandSpec.builder()
            .description(Text.of("Toggle doublejump"))
            .permission("doublejump.toggle")
            .executor((CommandSource src, CommandContext args) -> {
                if (!(src instanceof Player)) {
                    throw new CommandException(Text.of(TextColors.RED, "You need to be a player"));
                }
                if (toggled.contains(((Player) src).getUniqueId())) {
                    toggled.remove(((Player) src).getUniqueId());
                    if (this.toggle_stop) {
                        src.sendMessage(this.message_on);
                    } else {
                        src.sendMessage(this.message_off);
                    }
                } else {
                    toggled.add(((Player) src).getUniqueId());
                    if (this.toggle_stop) {
                        src.sendMessage(this.message_off);
                    } else {
                        src.sendMessage(this.message_on);
                    }
                }
                ((Player)src).offer(Keys.IS_FLYING, false);
                ((Player)src).offer(Keys.CAN_FLY, false);
                return CommandResult.success();
            })
            .build();

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        instance = this;
        Sponge.getCommandManager().register(this, toggleCmd, "doublejump", "doublejumptoggle", "djtoggle", "djt");
        configManager = HoconConfigurationLoader.builder().setPath(defaultConfig).build();
        try {
            rootNode = configManager.load();
            loadConfig();
        } catch(IOException e) {
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        try {
            loadConfig();
        } catch (IOException e) {
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
        this.toggled.clear();
    }

    private void loadConfig() throws IOException, ObjectMappingException {
        if (rootNode.getNode("settings").isVirtual()) {
            logger.info("Creating configuration");

            rootNode.getNode("settings", "message_activated").setValue(TypeToken.of(Text.class), Text.of(TextColors.RED, "■■■■ Jump activated ■■■■"));
            rootNode.getNode("settings", "message_recharging").setValue(TypeToken.of(Text.class), Text.of(TextColors.RED, "■■■■ Jump is recharging ■■■■"));
            rootNode.getNode("settings", "message_recharged").setValue(TypeToken.of(Text.class), Text.of(TextColors.GREEN, "■■■■ Jump is recharged ■■■■"));
            rootNode.getNode("settings", "message_on").setValue(TypeToken.of(Text.class), Text.of(TextColors.GREEN, "You are now effected by doublejump."));
            rootNode.getNode("settings", "message_off").setValue(TypeToken.of(Text.class), Text.of(TextColors.GREEN, "You are now not anymore effected by doublejump."));
            rootNode.getNode("settings", "strength").setValue(1D);
            rootNode.getNode("settings", "cooldown").setValue(5);
            rootNode.getNode("settings", "particle_type").setValue(ParticleTypes.DRAGON_BREATH.getId());
            rootNode.getNode("settings", "sound_type").setValue(SoundTypes.ENTITY_ENDERDRAGON_GROWL.getId());
            rootNode.getNode("settings", "toggle_stop").setValue(true);
            rootNode.getNode("settings", "effects").setValue(true);

            configManager.save(rootNode);
            loadConfig();
        } else if (rootNode.getNode("settings", "particle_type").isVirtual()) {
            rootNode.getNode("settings", "particle_type").setValue(ParticleTypes.DRAGON_BREATH.getId());
            rootNode.getNode("settings", "sound_type").setValue(SoundTypes.ENTITY_ENDERDRAGON_GROWL.getId());
            rootNode.getNode("settings", "toggle_stop").setValue(true);
            rootNode.getNode("settings", "message_on").setValue(TypeToken.of(Text.class), Text.of(TextColors.GREEN, "You are now effected by doublejump."));
            rootNode.getNode("settings", "message_off").setValue(TypeToken.of(Text.class), Text.of(TextColors.GREEN, "You are now not anymore effected by doublejump."));
            rootNode.getNode("settings", "effects").setValue(true);

            configManager.save(rootNode);
            loadConfig();
        } else {
            this.message_activated = rootNode.getNode("settings", "message_activated").getValue(TypeToken.of(Text.class));
            this.message_recharging = rootNode.getNode("settings", "message_recharging").getValue(TypeToken.of(Text.class));
            this.message_recharged = rootNode.getNode("settings", "message_recharged").getValue(TypeToken.of(Text.class));
            this.message_on = rootNode.getNode("settings", "message_on").getValue(TypeToken.of(Text.class));
            this.message_off = rootNode.getNode("settings", "message_off").getValue(TypeToken.of(Text.class));
            this.strength = rootNode.getNode("settings", "strength").getDouble();
            this.cooldowntime = rootNode.getNode("settings", "cooldown").getInt();
            Optional<ParticleType> typeOptional = Sponge.getRegistry().getType(ParticleType.class, rootNode.getNode("settings", "particle_type").getString());
            this.particle_type = typeOptional.orElse(ParticleTypes.DRAGON_BREATH);
            Optional<SoundType> soundOptional = Sponge.getRegistry().getType(SoundType.class, rootNode.getNode("settings", "sound_type").getString());
            this.sound_type = soundOptional.orElse(SoundTypes.ENTITY_ENDERDRAGON_GROWL);
            this.toggle_stop =  rootNode.getNode("settings", "toggle_stop").getBoolean();
            this.effects = rootNode.getNode("settings", "effects").getBoolean();
        }
    }

    Text message_activated;
    Text message_recharging;
    Text message_recharged;
    Text message_on;
    Text message_off;
    Double strength;
    int cooldowntime;
    ParticleType particle_type;
    SoundType sound_type;
    boolean toggle_stop;
    boolean effects;


    @Listener
    public void onToggleFlyEvent(ToggleFlyEvent event) {
        Optional<Player> playerOptional = event.getCause().first(Player.class);
        if (playerOptional.isPresent()) {
            Player player = playerOptional.get();
            if (player.hasPermission("doublejump.activate") && player.get(Keys.GAME_MODE).get().equals(GameModes.SURVIVAL)) {
                if (toggle_stop && toggled.contains(player.getUniqueId())) return;
                if (!toggle_stop && !toggled.contains(player.getUniqueId())) return;
                if (cooldown.hasCooldown(player)) {
                    player.sendMessage(ChatTypes.ACTION_BAR, message_recharging);
                    player.offer(Keys.IS_FLYING, false);
                    player.offer(Keys.CAN_FLY, false);
                } else {
                    player.sendMessage(ChatTypes.ACTION_BAR, message_activated);
                    player.offer(Keys.IS_FLYING, false);
                    player.offer(Keys.CAN_FLY, false);
                    player.setVelocity(player.getVelocity().add(0, strength, 0));

                    if (effects) {
                        player.playSound(this.sound_type, player.getLocation().getPosition(), 0.2);
                        for (int x = 20; x >= 0; x--)
                            player.spawnParticles(ParticleEffect.builder().type(this.particle_type).quantity(10).velocity(
                                    Vector3d.createRandomDirection(random).mul(0.18)
                            ).build(), player.getLocation().getPosition());
                    }
                    cooldown.setCoolmap(player, cooldowntime);
                }
            }
        }
    }

    @Listener
    public void onMove(MoveEntityEvent event) {
        if (event.getTargetEntity() instanceof Player) {
            Player player = (Player) event.getTargetEntity();
            if (player.hasPermission("doublejump.activate") && player.get(Keys.GAME_MODE).get().equals(GameModes.SURVIVAL)) {
                if (toggle_stop && toggled.contains(player.getUniqueId())) return;
                if (!toggle_stop && !toggled.contains(player.getUniqueId())) return;
                player.offer(Keys.CAN_FLY, true);
            }
        }
    }

    public static DoubleJump getInstance() {
        return instance;
    }
}
