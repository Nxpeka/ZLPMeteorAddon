package com.vlib.zlpaddon.services;

import com.vlib.zlpaddon.hud.EyeOfGodHud;
import com.vlib.zlpaddon.modules.EyeOfGodModule;

public class EyeOfGodFactory {
    private EyeOfGodHud eyeOfGodHud = null;
    private EyeOfGodModule eyeOfGodModule = null;
    private static final EyeOfGodFactory INSTANCE = new EyeOfGodFactory();

    private EyeOfGodFactory() {
    }

    public static EyeOfGodFactory getINSTANCE() {
        return EyeOfGodFactory.INSTANCE;
    }


    public EyeOfGodHud getEyeOfGodHud() {
        if (eyeOfGodHud == null) {
            this.eyeOfGodHud = new EyeOfGodHud();
        }
        return this.eyeOfGodHud;
    }

    public EyeOfGodModule getEyeOfGodModule() {
        if (eyeOfGodModule == null) {
            this.eyeOfGodModule = new EyeOfGodModule();
        }
        return this.eyeOfGodModule;
    }
}
