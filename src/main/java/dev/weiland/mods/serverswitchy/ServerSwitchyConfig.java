package dev.weiland.mods.serverswitchy;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ServerSwitchy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerSwitchyConfig {

    public static class Client {

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> dontAskAgainSwitches;

        Client(ForgeConfigSpec.Builder builder) {
            dontAskAgainSwitches = builder.push("client")
                    .defineListAllowEmpty(List.of("dontAskAgainSwitches"), List::of, t -> true);

            builder.pop();
        }

        private String getKeyFor(String fromIp, String toIp) {
            return fromIp + '|' + toIp;
        }

        public boolean shouldAskForSwitch(String fromIp, String toIp) {
            return !dontAskAgainSwitches.get().contains(getKeyFor(fromIp, toIp));
        }

        public void dontAskAgainForSwitch(String fromIp, String toIp) {
            var key = getKeyFor(fromIp, toIp);
            if (!dontAskAgainSwitches.get().contains(key)) {
                dontAskAgainSwitches.set(
                        ImmutableList.<String>builder()
                                .addAll(dontAskAgainSwitches.get())
                                .add(key)
                                .build()
                );
                dontAskAgainSwitches.save();
            }
        }
    }

    public static class Common {

        public final ForgeConfigSpec.BooleanValue enableDebugCommand;

        Common(ForgeConfigSpec.Builder builder) {
            enableDebugCommand = builder.push("common")
                    .define("enableDebugCommand", false);
            builder.pop();
        }

    }

    static final ForgeConfigSpec clientSpec;
    public static final ServerSwitchyConfig.Client CLIENT;

    static final ForgeConfigSpec commonSpec;
    public static final ServerSwitchyConfig.Common COMMON;

    static {
        var clientSpecPair = new ForgeConfigSpec.Builder().configure(ServerSwitchyConfig.Client::new);
        clientSpec = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();

        var commonSpecPair = new ForgeConfigSpec.Builder().configure(ServerSwitchyConfig.Common::new);
        commonSpec = commonSpecPair.getRight();
        COMMON = commonSpecPair.getLeft();
    }

}
