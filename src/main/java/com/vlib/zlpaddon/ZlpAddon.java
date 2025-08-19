package com.vlib.zlpaddon;

import com.vlib.zlpaddon.commands.EyeOfGodCommand;
import com.vlib.zlpaddon.hud.EyeOfGodHud;
import com.mojang.logging.LogUtils;
import com.vlib.zlpaddon.services.EyeOfGodFactory;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class ZlpAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("zlp");
    public static final HudGroup HUD_GROUP = new HudGroup("zlp");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        EyeOfGodFactory eyeOfGodFactory = EyeOfGodFactory.getINSTANCE();

        Modules.get().add(eyeOfGodFactory.getEyeOfGodModule());
        Commands.add(new EyeOfGodCommand());

        // HUD
        Hud.get().register(EyeOfGodHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
