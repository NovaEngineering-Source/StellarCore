package github.kasuminova.stellarcore.common.util;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.ImmutableList;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MutableResourcePackBindings {

    private Snapshot snapshot = Snapshot.empty();

    private static RefreshPlan buildPlan(final Snapshot current,
                                         final Snapshot next,
                                         final MetadataSerializer serializer,
                                         final Set<String> affected) {
        if (affected.isEmpty()) {
            return RefreshPlan.empty(current);
        }
        return new RefreshPlan(buildFallbackManagers(next, serializer, affected), next);
    }

    private static Map<String, FallbackResourceManager> buildFallbackManagers(
        final Snapshot state,
        final MetadataSerializer serializer,
        final Set<String> namespaceFilter) {
        final Map<String, FallbackResourceManager> managers = new LinkedHashMap<>();
        for (Binding binding : state.bindings) {
            for (String namespace : binding.namespaces) {
                if (namespaceFilter != null && !namespaceFilter.contains(namespace)) {
                    continue;
                }
                FallbackResourceManager manager = managers.get(namespace);
                if (manager == null) {
                    manager = new FallbackResourceManager(serializer);
                    managers.put(namespace, manager);
                }
                manager.addResourcePack(binding.pack);
            }
        }
        return managers;
    }

    private static boolean isMutable(final IResourcePack pack) {
        // Packs outside StellarCore's mixins (e.g. Resource-Loader's NormalResourceLoader) may
        // expose dynamically generated domains; treat them as mutable so their domains
        // participate in namespace refresh and late discovery.
        return !(pack instanceof StellarCoreResourcePack)
            || ((StellarCoreResourcePack) pack).stellar_core$isMutableResourcePack();
    }

    private static Set<String> immutableCopy(final Set<String> namespaces) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(namespaces));
    }

    public void beginFullReload() {
        snapshot = Snapshot.empty();
    }

    public RecordPlan prepareRecordPack(final IResourcePack pack, final Set<String> namespaces) {
        final List<Binding> nextBindings = new ArrayList<>(snapshot.bindings);
        nextBindings.add(new Binding(pack, namespaces, isMutable(pack)));
        return new RecordPlan(new Snapshot(nextBindings));
    }

    public void commit(final RecordPlan plan) {
        snapshot = plan.nextSnapshot;
    }

    public RefreshPlan refreshMutableNamespaces(final MetadataSerializer serializer) {
        final Snapshot current = snapshot;
        final Map<IResourcePack, Set<String>> currentDomains = new IdentityHashMap<>();
        final Set<String> affected = new LinkedHashSet<>();
        for (Binding binding : current.bindings) {
            if (!binding.mutable || currentDomains.containsKey(binding.pack)) {
                continue;
            }
            currentDomains.put(binding.pack, immutableCopy(binding.pack.getResourceDomains()));
        }

        final List<Binding> nextBindings = new ArrayList<>(current.bindings.size());
        for (Binding binding : current.bindings) {
            final Set<String> liveDomains = currentDomains.get(binding.pack);
            if (liveDomains == null) {
                nextBindings.add(binding);
                continue;
            }
            final Set<String> discovered = new LinkedHashSet<>(liveDomains);
            discovered.removeAll(binding.namespaces);
            if (discovered.isEmpty()) {
                nextBindings.add(binding);
                continue;
            }
            final Set<String> updated = new LinkedHashSet<>(binding.namespaces);
            updated.addAll(discovered);
            nextBindings.add(new Binding(binding.pack, updated, true));
            affected.addAll(discovered);
        }
        return buildPlan(current, new Snapshot(nextBindings), serializer, affected);
    }

    public RefreshPlan discoverResource(final MetadataSerializer serializer,
                                        final ResourceLocation location) {
        final Snapshot current = snapshot;
        final String namespace = location.getNamespace();
        final Map<IResourcePack, Boolean> discoveries = new IdentityHashMap<>();
        for (Binding binding : current.bindings) {
            if (binding.mutable && !binding.namespaces.contains(namespace)
                && !discoveries.containsKey(binding.pack)) {
                discoveries.put(binding.pack, binding.pack.resourceExists(location));
            }
        }
        if (!discoveries.containsValue(Boolean.TRUE)) {
            return RefreshPlan.empty(current);
        }

        final List<Binding> nextBindings = new ArrayList<>(current.bindings.size());
        for (Binding binding : current.bindings) {
            if (!Boolean.TRUE.equals(discoveries.get(binding.pack))
                || binding.namespaces.contains(namespace)) {
                nextBindings.add(binding);
                continue;
            }
            final Set<String> updated = new LinkedHashSet<>(binding.namespaces);
            updated.add(namespace);
            nextBindings.add(new Binding(binding.pack, updated, true));
        }
        return buildPlan(
            current, new Snapshot(nextBindings), serializer, Collections.singleton(namespace)
        );
    }

    public void commit(final RefreshPlan plan) {
        snapshot = plan.nextSnapshot;
    }

    public Map<String, FallbackResourceManager> rebuildFallbackManagers(final MetadataSerializer serializer) {
        return buildFallbackManagers(snapshot, serializer, null);
    }

    @Desugar
    private record Binding(IResourcePack pack, Set<String> namespaces, boolean mutable) {
    }

    @Desugar
    private record Snapshot(List<Binding> bindings) {
        private static final Snapshot EMPTY = new Snapshot(Collections.emptyList());

        private Snapshot(final List<Binding> bindings) {
            this.bindings = ImmutableList.copyOf(bindings);
        }

        private static Snapshot empty() {
            return EMPTY;
        }
    }

    public static final class RecordPlan {
        private final Snapshot nextSnapshot;

        private RecordPlan(final Snapshot nextSnapshot) {
            this.nextSnapshot = nextSnapshot;
        }
    }

    public static final class RefreshPlan {
        private final Map<String, FallbackResourceManager> replacements;
        private final Snapshot nextSnapshot;

        private RefreshPlan(final Map<String, FallbackResourceManager> replacements,
                            final Snapshot nextSnapshot) {
            this.replacements = Collections.unmodifiableMap(new LinkedHashMap<>(replacements));
            this.nextSnapshot = nextSnapshot;
        }

        static RefreshPlan empty(final Snapshot snapshot) {
            return new RefreshPlan(Collections.emptyMap(), snapshot);
        }

        public boolean isEmpty() {
            return replacements.isEmpty();
        }

        public Map<String, FallbackResourceManager> replacements() {
            return replacements;
        }

        public Set<String> namespaces() {
            return replacements.keySet();
        }
    }
}
