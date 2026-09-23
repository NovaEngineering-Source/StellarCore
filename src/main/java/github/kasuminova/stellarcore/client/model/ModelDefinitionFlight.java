package github.kasuminova.stellarcore.client.model;

import net.minecraft.client.renderer.block.model.ModelBlockDefinition;

import java.util.concurrent.CompletableFuture;

public final class ModelDefinitionFlight {

    public final Thread owner;
    public final CompletableFuture<ModelBlockDefinition> future = new CompletableFuture<>();

    public ModelDefinitionFlight(final Thread owner) {
        this.owner = owner;
    }
}
