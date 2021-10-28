package dev.weiland.mods.serverswitchy;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;

import java.net.InetSocketAddress;
import java.util.Collection;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.arguments.ComponentArgument.getComponent;
import static net.minecraft.commands.arguments.ComponentArgument.textComponent;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;

@Mod(value = ServerSwitchy.MOD_ID)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerSwitchy {

    public static final String MOD_ID = "serverswitchy";
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation("serverswitchy", "main");
    public static final String PROTOCOL_VERSION = "1.0";

    public static final int FLAG_FORCE_CONFIRMATION = 0b0001;

    public ServerSwitchy() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ServerSwitchyConfig.clientSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ServerSwitchyConfig.commonSpec);
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = MOD_ID)
    public static class ForgeEventListener {
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

            var packet = new ClientboundCustomPayloadPacket(CHANNEL_NAME, buf);
            for (var player : players) {
                player.connection.send(packet);
            }

            return players.size();
        }
    }

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event) {
        var channel = NetworkRegistry.newEventChannel(
                CHANNEL_NAME,
                () -> PROTOCOL_VERSION,
                NetworkRegistry.acceptMissingOr(serverVersion -> serverVersion.startsWith("1.")),
                clientVersion -> true
        );
        channel.registerObject(ChannelEventHandler.class);
    }

    public static class ChannelEventHandler {

        @SubscribeEvent
        public static void onMessage(NetworkEvent.ServerCustomPayloadEvent event) {
            var connection = Minecraft.getInstance().getConnection();
            var remoteAddress = connection == null ? null : connection.getConnection().getRemoteAddress();
            String fromIp;
            if (remoteAddress instanceof InetSocketAddress address) {
                fromIp = address.getHostString() + ":" + address.getPort();
            } else {
                fromIp = null;
            }

            var targetServerIp = event.getPayload().readUtf(256);
            var targetServerName = event.getPayload().readComponent();
            var flags = event.getPayload().readInt();


            var askForSwitch = (flags & FLAG_FORCE_CONFIRMATION) != 0 || fromIp == null || ServerSwitchyConfig.CLIENT.shouldAskForSwitch(fromIp, targetServerIp);
            Minecraft.getInstance().setScreen(new SwitchServerScreen(fromIp, targetServerIp, targetServerName, !askForSwitch));
            event.getSource().get().setPacketHandled(true);
        }
    }

}
