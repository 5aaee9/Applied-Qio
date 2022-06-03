package me.indexyz.minecraft.ae2qio.mixin;

import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.parts.misc.StorageBusPart;
import me.indexyz.minecraft.ae2qio.QioStorageAdapter;
import mekanism.common.tile.qio.TileEntityQIODashboard;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StorageBusPart.class)
public class MixinStorageBusQioSupport {
    @Inject(method = "getInventoryWrapper(Lnet/minecraft/tileentity/TileEntity;)Lappeng/api/storage/IMEInventory", at = @At("TAIL"), cancellable = true)
    private void init(TileEntity target, CallbackInfoReturnable<IMEInventory<IAEItemStack>> info) {
        if (target instanceof TileEntityQIODashboard) {
            var dashboard = (TileEntityQIODashboard) target;
            var adapter = new QioStorageAdapter<>(dashboard, ((StorageBusPart)(Object)this).getSide().getFacing().getOpposite());

            info.setReturnValue(adapter);
        }
    }
}
