package me.justahuman.slimefun_essentials.mixins.jei;

import me.justahuman.slimefun_essentials.client.SlimefunItemStack;
import me.justahuman.slimefun_essentials.client.SlimefunRegistry;
import me.justahuman.slimefun_essentials.utils.Utils;
import mezz.jei.library.plugins.vanilla.ingredients.ItemStackHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = ItemStackHelper.class, remap = false)
public class ItemStackHelperMixin {
    @Inject(method = "getDisplayModId(Lnet/minecraft/world/item/ItemStack;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void slimefunEssentials$getDisplayModId(ItemStack ingredient, CallbackInfoReturnable<String> cir) {
        final String sfId = Utils.getSlimefunId(ingredient);
        if (sfId == null) {
            return;
        }
        final SlimefunItemStack slimefunItemStack = SlimefunRegistry.getSlimefunItem(sfId);
        if (slimefunItemStack != null) {
            cir.setReturnValue(slimefunItemStack.addon());
        }
    }
}
