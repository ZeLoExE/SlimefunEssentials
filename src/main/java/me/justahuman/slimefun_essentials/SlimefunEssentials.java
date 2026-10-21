package me.justahuman.slimefun_essentials;

import me.justahuman.slimefun_essentials.api.DisplayComponentType;
import me.justahuman.slimefun_essentials.api.RecipeDisplay;
import me.justahuman.slimefun_essentials.client.RecipeCategory;
import me.justahuman.slimefun_essentials.client.SlimefunRegistry;
import me.justahuman.slimefun_essentials.client.payload.ClientConfigPayload;
import me.justahuman.slimefun_essentials.client.payload.ComponentTypePayload;
import me.justahuman.slimefun_essentials.client.payload.ItemsPayload;
import me.justahuman.slimefun_essentials.client.payload.LoadingStatePayload;
import me.justahuman.slimefun_essentials.client.payload.RecipeCategoriesPayload;
import me.justahuman.slimefun_essentials.client.payload.RecipeDisplayPayload;
import me.justahuman.slimefun_essentials.compat.cloth_config.ConfigScreen;
import me.justahuman.slimefun_essentials.config.ModConfig;
import me.justahuman.slimefun_essentials.utils.Payloads;
import me.justahuman.slimefun_essentials.utils.CompatUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class SlimefunEssentials implements ClientModInitializer {
    public static final String MOD_ID = "slimefun_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "slimefun"));

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(LoadingStatePayload.TYPE, LoadingStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ItemsPayload.TYPE, ItemsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ComponentTypePayload.TYPE, ComponentTypePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RecipeCategoriesPayload.TYPE, RecipeCategoriesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RecipeDisplayPayload.TYPE, RecipeDisplayPayload.CODEC);
        // 客户端 → 服务端：上报 receiveServerPayloads 配置
        PayloadTypeRegistry.serverboundPlay().register(ClientConfigPayload.TYPE, ClientConfigPayload.CODEC);
        ModConfig.loadConfig();

        if (ModConfig.resourcePackFeatures()) {
            ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id("reload_listener"), (ResourceManagerReloadListener) manager -> {
                SlimefunRegistry.loadResources(manager);
                // 资源包加载完成后，若不接收服务端 Payload，则尝试从缓存补全并直接标记完成
                if (!ModConfig.receiveServerPayloads()) {
                    SlimefunRegistry.loadFromCache();
                    Payloads.markMetExpected();
                } else if (SlimefunRegistry.loadFromCacheIfMissing()) {
                    // 缓存作为资源包模式的补充：若资源包未提供数据且缓存存在，立即用缓存显示
                    Payloads.markMetExpected();
                }
            });
        } else if (!ModConfig.receiveServerPayloads()) {
            // 既无资源包模式也不接收 Payload：仅依赖缓存
            // 此时无 reload listener，需在登录后从缓存加载
        }

        ClientPlayNetworking.registerGlobalReceiver(LoadingStatePayload.TYPE, (payload, context) -> {
            // 服务端即将发送 Payload，清除缓存中可能过期的同 type 数据
            if (!ModConfig.receiveServerPayloads()) {
                return;
            }
            Payloads.expect(payload);
        });
        ClientPlayNetworking.registerGlobalReceiver(ItemsPayload.TYPE, (payload, context) -> {});
        ClientPlayNetworking.registerGlobalReceiver(ComponentTypePayload.TYPE, (payload, context) -> {});
        ClientPlayNetworking.registerGlobalReceiver(RecipeCategoriesPayload.TYPE, (payload, context) -> {});
        ClientPlayNetworking.registerGlobalReceiver(RecipeDisplayPayload.TYPE, (payload, context) -> {});

        // 登录后上报配置给服务端；若不接收 Payload，直接从缓存加载并标记完成
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sender.sendPacket(new ClientConfigPayload(ModConfig.receiveServerPayloads()));
            if (!ModConfig.receiveServerPayloads()) {
                client.execute(() -> {
                    SlimefunRegistry.loadFromCache();
                    Payloads.markMetExpected();
                });
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RecipeCategory.clear();
            DisplayComponentType.clear();
            RecipeDisplay.clear();
            SlimefunRegistry.reset();
            Payloads.reset();
        });

        if (CompatUtils.isClothConfigLoaded()) {
            final KeyMapping configKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping("slimefun_essentials.key_bind.open_config", GLFW.GLFW_KEY_F6, CATEGORY));
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (configKeyMapping.consumeClick()) {
                    client.gui.setScreen(ConfigScreen.buildScreen(client.gui.screen()));
                }
            });
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.toLowerCase(Locale.ROOT));
    }
}