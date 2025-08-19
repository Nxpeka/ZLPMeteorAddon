package com.vlib.zlpaddon;

import com.mojang.brigadier.Command;
import com.vlib.zlpaddon.commands.EyeOfGodCommand;
import com.vlib.zlpaddon.hud.EyeOfGodHud;
import com.vlib.zlpaddon.modules.EyeOfGodModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public class ZlpAddon extends MeteorAddon {
    static  {
        LogUtils.configureRootLoggingLevel(Level.DEBUG);
    }
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("zlp");
    public static final HudGroup HUD_GROUP = new HudGroup("zlp");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        Modules.get().add(new EyeOfGodModule());
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
