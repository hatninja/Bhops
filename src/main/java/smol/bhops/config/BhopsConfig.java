package smol.bhops.config;

import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry;
import smol.bhops.Bhops;

@Config(name = Bhops.MOD_ID)
public class BhopsConfig implements ConfigData {
    public boolean showSpeed = true;
    public boolean enableBhops = true;
    public boolean exclusiveToPlayers = true;

    @ConfigEntry.Gui.Excluded
    public float sv_maxvelocity = 2.0F; //Maximum speed. (Now it's just for the speedometer colors)

    public float sv_friction = 0.5F; //Ground friction.
    public float sv_accelerate = 0.1F; //Ground acceleration.
    public float sv_airaccelerate = 0.2F; //Air acceleration.
    public float sv_maxairspeed = 0.08F; //Maximum speed you can move in air without influence. Also determines how fast you gain bhop speed.

    @ConfigEntry.Gui.Excluded
    public float sv_airspeedcutoff = 0.2F; //How fast to travel before applying sv_maxairspeed. Intended to be a fix for low-speed air control.

    public float maxSpeedMul = 2.2F; //How much to multiply default game's movementSpeed by.

}