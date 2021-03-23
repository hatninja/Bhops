package smol.bhops.config;

import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import smol.bhops.Bhops;

@Config(name = Bhops.MOD_ID)
public class BhopsConfig implements ConfigData {
    public boolean showSpeed = true;
    public boolean enableBhops = true;
    public float sv_maxvelocity = 2.0F; //Maximum speed.
    public float sv_friction = 0.5F;
    public float sv_accelerate = 0.1F; //Ground acceleration.
    public float sv_airaccelerate = 0.2F; //Air acceleration.
    public float sv_maxairspeed = 0.1F; //Maximum speed you can move in air without influence. Also determines how fast you gain bhop speed.
    public float maxSpeedMul = 2.2F; //How much to multiply default game's movementSpeed by.
}