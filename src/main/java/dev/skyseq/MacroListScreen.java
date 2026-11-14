package dev.skyseq;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Main screen: list of keybinds plus global switches. */
public final class MacroListScreen extends Screen {

    private final Screen parent;
    private int page = 0;
    private int armedDelete = -1;

    public MacroListScreen(Screen parent) {
        super(Component.literal("SkySeq"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cw = Math.min(320, this.width - 20);
        int left = (this.width - cw) / 2;
        int top = 28;
        int rowH = 24;

        this.addRenderableWidget(new StringWidget(left, 10, cw, 9, this.title, this.font));

        int perPage = Math.max(1, (this.height - 84 - top) / rowH);
        int count = Config.macros.size();
        int pages = Math.max(1, (count + perPage - 1) / perPage);
        page = Mth.clamp(page, 0, pages - 1);

        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= count) {
                break;
            }
            Macro m = Config.macros.get(idx);
            int y = top + i * rowH;

            this.addRenderableWidget(Button.builder(
                    Component.literal(m.enabled ? "ON" : "OFF").withStyle(m.enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                    b -> {
                        m.enabled = !m.enabled;
                        Config.save();
                        this.rebuildWidgets();
                    }).bounds(left, y, 40, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal(label(m)), b -> {
                this.minecraft.setScreenAndShow(new MacroEditScreen(this, m));
            }).bounds(left + 44, y, cw - 88, 20).build());

            boolean armed = armedDelete == idx;
            this.addRenderableWidget(Button.builder(
                    Component.literal(armed ? "Sure?" : "X").withStyle(armed ? ChatFormatting.RED : ChatFormatting.WHITE),
                    b -> {
                        if (armedDelete == idx) {
                            Config.macros.remove(idx);
                            armedDelete = -1;
                            Config.save();
                        } else {
                            armedDelete = idx;
                        }
                        this.rebuildWidgets();
                    }).bounds(left + cw - 40, y, 40, 20).build());
        }

        // Pager
        int pagerY = this.height - 74;
        this.addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            page--;
            armedDelete = -1;
            this.rebuildWidgets();
        }).bounds(left, pagerY, 60, 20).build()).active = page > 0;
        this.addRenderableWidget(new StringWidget(left + 60, pagerY + 6, cw - 120, 9,
                Component.literal("Page " + (page + 1) + " / " + pages + "  (" + count + " keybinds)"), this.font));
        this.addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            page++;
            armedDelete = -1;
            this.rebuildWidgets();
        }).bounds(left + cw - 60, pagerY, 60, 20).build()).active = page < pages - 1;

        // Global switches
        int toggleY = this.height - 50;
        int tw = (cw - 8) / 3;
        this.addRenderableWidget(Button.builder(Component.literal("Mod: " + (Config.enabled ? "ON" : "OFF")), b -> {
            Config.enabled = !Config.enabled;
            Config.save();
            this.rebuildWidgets();
        }).bounds(left, toggleY, tw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Strict: " + (Config.strictMode ? "ON" : "OFF")), b -> {
            Config.strictMode = !Config.strictMode;
            Config.save();
            this.rebuildWidgets();
        }).bounds(left + tw + 4, toggleY, tw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Status: " + (Config.showStatus ? "ON" : "OFF")), b -> {
            Config.showStatus = !Config.showStatus;
            Config.save();
            this.rebuildWidgets();
        }).bounds(left + (tw + 4) * 2, toggleY, tw, 20).build());

        // Bottom row
        int bottomY = this.height - 26;
        this.addRenderableWidget(Button.builder(Component.literal("+ New keybind"), b -> {
            Macro m = new Macro();
            m.steps.add(new Step(Step.Type.COMMAND, "", 150));
            Config.macros.add(m);
            Config.save();
            this.minecraft.setScreenAndShow(new MacroEditScreen(this, m));
        }).bounds(left, bottomY, cw / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(left + cw / 2 + 2, bottomY, cw / 2 - 2, 20).build());
    }

    private static String label(Macro m) {
        String name = m.name == null || m.name.isBlank() ? "(unnamed)" : m.name;
        return name + "  [" + m.bindingName() + "]  " + m.mode.label + ", " + m.steps.size() + " steps";
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }

    @Override
    public void removed() {
        Config.save();
    }
}
