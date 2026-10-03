package eu.endermite.commandwhitelist.bukkit.listeners.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import eu.endermite.commandwhitelist.bukkit.CommandWhitelistBukkit;
import eu.endermite.commandwhitelist.common.CWPermission;
import eu.endermite.commandwhitelist.common.CommandUtil;
import eu.endermite.commandwhitelist.common.ConfigCache;
import eu.endermite.commandwhitelist.common.commands.CWCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Locale;

public class PacketCommandPreProcessListener extends PacketListenerAbstract {
    public PacketCommandPreProcessListener() {
        super(PacketListenerPriority.HIGHEST);
    }

    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    public void unregister() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        String command;
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            String message = new WrapperPlayClientChatMessage(event).getMessage();
            if (!message.startsWith("/")) return;
            command = message.substring(1);
        } else if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            command = new WrapperPlayClientChatCommand(event).getCommand();
        } else if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND_UNSIGNED) {
            command = new WrapperPlayClientChatCommandUnsigned(event).getCommand();
        } else {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || player.hasPermission(CWPermission.BYPASS.permission())) return;
        ConfigCache config = CommandWhitelistBukkit.getConfigCache();
        String label = CommandUtil.getCommandLabel(command).toLowerCase(Locale.ROOT);
        if (!CommandWhitelistBukkit.getCommands(player).contains(label)) {
            event.setCancelled(true);
            Component message = CWCommand.getParsedErrorMessage(command,
                    config.prefix + CommandWhitelistBukkit.getCommandDeniedMessage(label));
            switch (config.messageType) {
                case CHAT -> CommandWhitelistBukkit.getAudiences().player(player).sendMessage(message);
                case ACTIONBAR -> CommandWhitelistBukkit.getAudiences().player(player).sendActionBar(message);
            }
            return;
        }
        for (String blocked : CommandWhitelistBukkit.getSuggestions(player)) {
            if (command.toLowerCase(Locale.ROOT).startsWith(blocked)) {
                event.setCancelled(true);
                CommandWhitelistBukkit.getAudiences().player(player).sendMessage(
                        CWCommand.miniMessage.deserialize(config.prefix + config.subcommand_denied));
                return;
            }
        }
        // Preserve the original command and signatures; filtering does not rewrite packets.
    }
}
