package me.indexyz.minecraft.ae2qio;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

@Mod(AppliedQio.ID)
public class AppliedQio {
    public static final String ID = "appqio";

    public AppliedQio() {

    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ID, path);
    }
}
