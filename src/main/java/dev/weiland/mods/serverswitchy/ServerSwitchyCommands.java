package dev.weiland.mods.serverswitchy;

import io.netty.buffer.Unpooled;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.arguments.ComponentArgument.getComponent;
import static net.minecraft.commands.arguments.ComponentArgument.textComponent;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = ServerSwitchy.MOD_ID)
public class ServerSwitchyCommands {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        if (ServerSwitchyConfig.COMMON.enableDebugCommand.get()) {
            event.getDispatcher().register(
                    Commands.literal("serverswitchy")
                            .then(Commands.argument("target", players())
                                    .then(Commands.argument("targetIp", string())
                                            .then(Commands.argument("title", textComponent())
                                                    .executes(cmd -> {
                                                        var players = getPlayers(cmd, "target");
                                                        var targetIp = getString(cmd, "targetIp");
                                                        var title = getComponent(cmd, "title");
                                                        return runDebugCommand(players, targetIp, title, false);
                                                    })
                                                    .then(Commands.argument("force_ask", bool()).executes(cmd -> {
                                                                var players = getPlayers(cmd, "target");
                                                                var targetIp = getString(cmd, "targetIp");
                                                                var title = getComponent(cmd, "title");
                                                                var forceAsk = getBool(cmd, "force_ask");
                                                                return runDebugCommand(players, targetIp, title, forceAsk);
                                                            })
                                                    )
                                            )
                                    )
                            )
            );
        }
    }

    private static int runDebugCommand(Collection<ServerPlayer> players, String targetIp, Component title, boolean forceAsk) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(targetIp);
        buf.writeComponent(title);
        buf.writeInt(forceAsk ? ServerSwitchy.FLAG_FORCE_CONFIRMATION : 0);

        var packet = new ClientboundCustomPayloadPacket(ServerSwitchy.CHANNEL_NAME, buf);
        for (var player : players) {
            player.connection.send(packet);
        }

        return players.size();
    }
}
