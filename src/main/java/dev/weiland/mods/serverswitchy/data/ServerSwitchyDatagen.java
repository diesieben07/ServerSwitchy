package dev.weiland.mods.serverswitchy.data;

import dev.weiland.mods.serverswitchy.LanguageConstants;
import dev.weiland.mods.serverswitchy.ServerSwitchy;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(modid = ServerSwitchy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerSwitchyDatagen {

    @SubscribeEvent
    public static void register(GatherDataEvent event) {
        if (event.includeClient()) {
            event.getGenerator().addProvider(new LanguageProviderImpl(event.getGenerator(), "en_us"));
        }
    }

    private static class LanguageProviderImpl extends LanguageProvider {

        public LanguageProviderImpl(DataGenerator gen, String locale) {
            super(gen, ServerSwitchy.MOD_ID, locale);
        }

        @Override
        protected void addTranslations() {
            add(LanguageConstants.SCREEN_TITLE, "Switch to %s");
            add(LanguageConstants.SWITCH_QUESTION, "The server suggests you switch servers to\n\n%s\n\nDo you want to connect to the server at the IP %s?");
            add(LanguageConstants.YES_DONT_ASK_AGAIN, "Yes and do not ask again");
        }
    }


}
