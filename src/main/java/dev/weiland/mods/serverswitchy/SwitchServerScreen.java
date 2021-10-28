package dev.weiland.mods.serverswitchy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class SwitchServerScreen extends Screen {

    @Nullable
    private final String fromIp;
    private final String targetIp;
    private final boolean connectImmediately;

    private final Component question;

    private boolean hasTriedConnecting;
    private MultiLineLabel questionLabel;

    protected SwitchServerScreen(@Nullable String fromIp, String targetIp, Component targetName, boolean connectImmediately) {
        super(new TranslatableComponent(LanguageConstants.SCREEN_TITLE, targetIp));
        this.fromIp = fromIp;
        this.targetIp = targetIp;

        this.question = new TranslatableComponent(LanguageConstants.SWITCH_QUESTION, targetName);
        this.connectImmediately = connectImmediately;
    }

    @Override
    protected void init() {
        super.init();
        if (this.hasTriedConnecting) {
            minecraft.setScreen(null);
            return;
        }
        if (this.connectImmediately) {
            this.connect(true);
            return;
        }

        var yes = new TranslatableComponent("gui.yes");
        var yesDontAskAgain = new TranslatableComponent(LanguageConstants.YES_DONT_ASK_AGAIN);
        var cancel = new TranslatableComponent("gui.cancel");

        var yesWidth = font.width(yes);
        var yesDontAskAgainWidth = font.width(yesDontAskAgain);
        var cancelWidth = font.width(cancel);

        var btnsWidth = IntStream.of(yesWidth, yesDontAskAgainWidth, cancelWidth).max().orElseThrow() + 40;

        addRenderableWidget(
                new Button((width / 2) - btnsWidth - 10, 140, btnsWidth, 20, yes, btn -> connect(false))
        );

        addRenderableWidget(
                new Button((width / 2) + 10, 140, btnsWidth, 20, yesDontAskAgain, btn -> connect(true))
        );

        addRenderableWidget(
                new Button((width / 2) - btnsWidth - 10, 170, btnsWidth * 2 + 20, 20, cancel, btn -> cancel())
        );

        questionLabel = MultiLineLabel.create(font, question, width - 200);
    }

    private void cancel() {
        minecraft.setScreen(null);
    }

    private void connect(boolean dontAskAgain) {
        if (dontAskAgain && this.fromIp != null) {
            ServerSwitchyConfig.CLIENT.dontAskAgainForSwitch(this.fromIp, this.targetIp);
        }
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        minecraft.clearLevel(new ProgressScreen(false));
        hasTriedConnecting = true;
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(this.targetIp), null);
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderDirtBackground(0);
        questionLabel.renderCentered(poseStack, this.width / 2, 70);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

}
