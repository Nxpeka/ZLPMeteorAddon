package com.vlib.zlpaddon.hud;

import com.vlib.zlpaddon.ZlpAddon;
import com.vlib.zlpaddon.dto.request.ZlpMapPlayersDTO;
import com.vlib.zlpaddon.models.MinecraftPlayerModel;
import com.vlib.zlpaddon.modules.EyeOfGodModule;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EyeOfGodHud extends HudElement {

    public static final HudElementInfo<EyeOfGodHud> INFO = new HudElementInfo<>(ZlpAddon.HUD_GROUP, "EyeOfGodHud", "HUD for EyeOfGodModel.", EyeOfGodHud::new);


    private final List<Pair<String, MinecraftPlayerModel>> pairs = new ArrayList<>();

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("flat-color")
        .description("Color for flat color mode.")
        .defaultValue(new SettingColor(225, 255, 255))
        .build()
    );


    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(0, 0, 0, 50))
        .build()
    );


    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal alignment.")
        .defaultValue(Alignment.Auto)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the element.")
        .defaultValue(0)
        .build()
    );
    private final Setting<Boolean> customScale = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    public EyeOfGodHud() {
        super(INFO);
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    @Override
    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth() - border.get() * 2, width, alignment);
    }


    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }


    @Override
    public void tick(HudRenderer renderer) {
        if (mc.player == null || isInEditor()) {
            setSize(renderer.textWidth("No players selected", shadow.get(), getScale()), renderer.textHeight(shadow.get(), getScale()));
            return;
        }

        double width = 0;
        double height = 0;

        pairs.clear();

        Modules modules = Modules.get();
        EyeOfGodModule eyeOfGodModule = modules.get(EyeOfGodModule.class);
        ConcurrentHashMap<String, MinecraftPlayerModel> players = eyeOfGodModule.getOnlinePlayers();

        for(Map.Entry<String, MinecraftPlayerModel> entry : players.entrySet()) {
            ZlpMapPlayersDTO.PositionDTO position = entry.getValue().getPosition();
            String text = entry.getValue().getName() + ": " + String.format("%.0f, %.0f, %.0f", position.getX(), position.getY(), position.getZ());
            pairs.add(new ObjectObjectImmutablePair<>(entry.getKey(), entry.getValue()));
            width = Math.max(width, renderer.textWidth(text, shadow.get(), getScale()));
            height += renderer.textHeight(shadow.get(), getScale());
        }

        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = this.x + border.get();
        double y = this.y + border.get();

        if (mc.player == null || isInEditor()) {
            renderer.text("EyeOfGod zlpMap", x, y, color.get(), shadow.get());
            return;
        }

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }


        for (Pair<String, MinecraftPlayerModel> pair : pairs) {
            ZlpMapPlayersDTO.PositionDTO position = pair.right().getPosition();
            String cords = String.format("%.0f, %.0f, %.0f", position.getX(), position.getY(), position.getZ());

            String text = pair.right().getName() + " " + pair.right().getDimension() + ": " + cords;
            renderer.text(text, x + alignX(renderer.textWidth(text, shadow.get(), getScale()), alignment.get()), y, color.get(), shadow.get(), getScale());
            y += renderer.textHeight(shadow.get(), getScale());
        }
    }

}
