package dev.skyseq;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Pick what a step does. Shows every option with a short description. */
public final class StepTypeScreen extends Screen {

    private final Screen parent;
    private final Consumer<Step.Type> onPick;

    public StepTypeScreen(Screen parent, Consumer<Step.Type> onPick) {
        super(Component.literal("What should this step do?"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        int cw = Math.min(320, this.width - 20);
        int left = (this.width - cw) / 2;
        this.addRenderableWidget(new StringWidget(left, 10, cw, 9, this.title, this.font));

        Step.Type[] types = Step.Type.values();
        int rowH = Mth.clamp((this.height - 30 - 34) / types.length, 21, 26);
        for (int i = 0; i < types.length; i++) {
            Step.Type type = types[i];
            this.addRenderableWidget(Button.builder(Component.literal(type.label + "  -  " + type.help), b -> {
                this.minecraft.setScreenAndShow(parent);
                onPick.accept(type);
            }).bounds(left, 26 + i * rowH, cw, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left, this.height - 26, cw, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }
}
