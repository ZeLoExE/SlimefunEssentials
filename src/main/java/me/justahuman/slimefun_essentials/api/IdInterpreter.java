package me.justahuman.slimefun_essentials.api;

import me.justahuman.slimefun_essentials.SlimefunEssentials;
import me.justahuman.slimefun_essentials.client.SlimefunRegistry;
import me.justahuman.slimefun_essentials.client.RecipeComponent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.NotNull;

public interface IdInterpreter<T> {
    default T interpretId(@NotNull RecipeComponent component, @NotNull String id, @NotNull T def) {
        if (id.isBlank()) {
            return def;
        }

        if (!id.contains(":")) {
            SlimefunEssentials.LOGGER.error("Invalid Ingredient Id: {}", id);
            return def;
        }

        int damage = 0;
        if (id.contains("^")) {
            try {
                damage = Integer.parseInt(id.substring(id.indexOf('^') + 1));
                id = id.substring(0, id.indexOf('^'));
            } catch (Exception ignored) {}
        }

        float chance = 1;
        if (id.contains("%")) {
            try {
                chance = Float.parseFloat(id.substring(id.indexOf('%') + 1));
                id = id.substring(0, id.indexOf('%'));
            } catch (Exception ignored) {}
        }

        final String type = id.substring(0, id.indexOf(':'));
        final String count = id.substring(id.indexOf(':') + 1);
        int amount = 1;
        try {
            amount = Integer.parseInt(count);
        } catch (Exception ignored) {}

        // Slimefun Item
        if (SlimefunRegistry.hasItem(type)) {
            final ItemStack itemStack = SlimefunRegistry.getItemStack(type).copy();
            if (damage > 0) {
                itemStack.set(DataComponents.DAMAGE, damage);
            }
            return fromItemStack(chance, itemStack, amount, def);
        }
        // Complex Item
        else if (type.startsWith("?")) {
            int index = 0;
            try {
                index = Integer.parseInt(type.substring(1));
            } catch (Exception ignored) {}
            return fromItemStack(chance, component.getComplex().get(index), amount, def);
        }
        // Entity
        else if (type.startsWith("@")) {
            final boolean baby = type.startsWith("baby_", 1);
            final Identifier identifier = Identifier.tryParse("minecraft:" + type.substring(baby ? 6 : 1));
            if (identifier == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
                SlimefunEssentials.LOGGER.error("Invalid Ingredient Entity Id: {}", id);
                return def;
            }
            return fromEntityType(chance, BuiltInRegistries.ENTITY_TYPE.getValue(identifier), baby, amount, def);
        }
        // Fluid
        else if (type.startsWith("~")) {
            final Identifier identifier = Identifier.tryParse("minecraft:" + type.substring(1));
            if (identifier == null || !BuiltInRegistries.FLUID.containsKey(identifier)) {
                SlimefunEssentials.LOGGER.error("Invalid Ingredient Fluid Id: {}", id);
                return def;
            }
            return fromFluid(chance, FluidVariant.of(BuiltInRegistries.FLUID.getValue(identifier)), amount, def);
        }
        // Tag
        else if (type.startsWith("#")) {
            final Identifier identifier = Identifier.tryParse("minecraft:" + type.substring(1));
            if (identifier == null) {
                SlimefunEssentials.LOGGER.error("Invalid Ingredient Tag Id: {}", id);
                return def;
            }
            return fromTag(chance, TagKey.create(BuiltInRegistries.ITEM.key(), identifier), amount, def);
        }
        // Experience
        else if (type.equals("$")) {
            return fromEntityType(chance, EntityTypes.EXPERIENCE_ORB, false, amount, def);
        }
        // Item (Or Mistake)
        else {
            final Identifier identifier = Identifier.tryParse("minecraft:" + type.toLowerCase());
            if (identifier == null || !BuiltInRegistries.ITEM.containsKey(identifier)) {
                SlimefunEssentials.LOGGER.error("Invalid Ingredient Item Id: {}", id);
                return def;
            }

            final ItemStack itemStack = BuiltInRegistries.ITEM.getValue(identifier).getDefaultInstance().copy();
            if (damage > 0) {
                itemStack.set(DataComponents.DAMAGE, damage);
            }
            return fromItemStack(chance, itemStack, amount, def);
        }
    }
    
    T fromTag(float chance, TagKey<@NotNull Item> tagKey, int amount, T def);
    T fromItemStack(float chance, ItemStack itemStack, int amount, T def);
    T fromFluid(float chance, FluidVariant fluid, int amount, T def);
    T fromEntityType(float chance, EntityType<?> entityType, boolean baby, int amount, T def);
}