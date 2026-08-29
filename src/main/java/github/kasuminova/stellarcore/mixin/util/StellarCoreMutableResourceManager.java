package github.kasuminova.stellarcore.mixin.util;

/**
 * Exposes the incremental mutable resource-pack namespace refresh required before model loading.
 * Resource generators may create namespace directories after the full reload captured pack domains.
 */
public interface StellarCoreMutableResourceManager {

    /**
     * Discovers newly exposed namespaces and atomically publishes rebuilt fallback managers for
     * affected namespaces without invoking resource reload listeners.
     */
    void stellar_core$refreshMutableResourcePackNamespaces();
}
