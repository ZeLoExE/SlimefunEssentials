package me.justahuman.slimefun_essentials.mixins;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SlimefunEssentialsMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".jei.")) {
            return FabricLoader.getInstance().isModLoaded("jei") && classExists(targetClassName);
        }
        if (mixinClassName.contains(".rei.")) {
            return FabricLoader.getInstance().isModLoaded("roughlyenoughitems") && classExists(targetClassName);
        }
        if (mixinClassName.contains(".emi.")) {
            return FabricLoader.getInstance().isModLoaded("emi") && classExists(targetClassName);
        }
        if (mixinClassName.contains(".jade.")) {
            return FabricLoader.getInstance().isModLoaded("jade") && classExists(targetClassName);
        }
        return true;
    }

    private static boolean classExists(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().getResource(className.replace('.', '/') + ".class") != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
