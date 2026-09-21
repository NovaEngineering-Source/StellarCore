package github.kasuminova.stellarcore.client.model;

import net.minecraftforge.client.model.IModel;

import java.util.concurrent.CompletableFuture;

/** Shared in-flight model load state for one resource generation. */
public final class ModelLoadFlight {

    public final Thread owner;
    public final long generation;
    public final CompletableFuture<IModel> future = new CompletableFuture<>();

    public ModelLoadFlight(final Thread owner, final long generation) {
        this.owner = owner;
        this.generation = generation;
    }
}
