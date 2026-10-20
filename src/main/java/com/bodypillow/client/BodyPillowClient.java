package com.bodypillow.client;

import com.bodypillow.ModContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class BodyPillowClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(ModContent.PILLOW_ENTITY, PillowRenderer::new);
    }
}
