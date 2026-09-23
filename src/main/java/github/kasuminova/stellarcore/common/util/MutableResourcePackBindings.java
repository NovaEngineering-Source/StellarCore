package github.kasuminova.stellarcore.common.util;

import com.github.bsideup.jabel.Desugar;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MutableResourcePackBindings {

    private volatile Snapshot snapshot = Snapshot.empty();

    private volatile ConcurrentMap<ResourceLocation, Boolean> discoverable = new ConcurrentHashMap<>();

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
        final Object2ObjectOpenHashMap<String, FallbackResourceManager> managers = new Object2ObjectOpenHashMap<>();
        final Binding[] bindings = state.bindings;
        for (final Binding binding : bindings) {
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
        return !(pack instanceof StellarCoreResourcePack)
            || ((StellarCoreResourcePack) pack).stellar_core$isMutableResourcePack();
    }

    public void beginFullReload() {
        applySnapshot(Snapshot.empty());
    }

    private void applySnapshot(final Snapshot next) {
        this.discoverable = new ConcurrentHashMap<>();
        this.snapshot = next;
    }

    public RecordPlan prepareRecordPack(final IResourcePack pack, final Set<String> namespaces) {
        final Binding[] current = snapshot.bindings;
        final Binding[] nextBindings = Arrays.copyOf(current, current.length + 1);
        nextBindings[current.length] = new Binding(pack, namespaces, isMutable(pack));
        return new RecordPlan(new Snapshot(nextBindings));
    }

    public void commit(final RecordPlan plan) {
        applySnapshot(plan.nextSnapshot);
    }

    public RefreshPlan refreshMutableNamespaces(final MetadataSerializer serializer) {
        final Snapshot current = snapshot;
        final Binding[] bindings = current.bindings;
        final Reference2ObjectOpenHashMap<IResourcePack, Set<String>> currentDomains =
            new Reference2ObjectOpenHashMap<>();
        for (final Binding binding : bindings) {
            if (!binding.mutable || currentDomains.containsKey(binding.pack)) {
                continue;
            }
            currentDomains.put(binding.pack, new ObjectLinkedOpenHashSet<>(binding.pack.getResourceDomains()));
        }

        final ObjectLinkedOpenHashSet<String> affected = new ObjectLinkedOpenHashSet<>();
        final Binding[] nextBindings = new Binding[bindings.length];
        for (int i = 0; i < bindings.length; i++) {
            final Binding binding = bindings[i];
            final Set<String> liveDomains = currentDomains.get(binding.pack);
            if (liveDomains == null) {
                nextBindings[i] = binding;
                continue;
            }
            final ObjectLinkedOpenHashSet<String> discovered = new ObjectLinkedOpenHashSet<>(liveDomains);
            discovered.removeAll(binding.namespaces);
            if (discovered.isEmpty()) {
                nextBindings[i] = binding;
                continue;
            }
            final ObjectLinkedOpenHashSet<String> updated = new ObjectLinkedOpenHashSet<>(binding.namespaces);
            updated.addAll(discovered);
            nextBindings[i] = new Binding(binding.pack, updated, true);
            affected.addAll(discovered);
        }
        return buildPlan(current, new Snapshot(nextBindings), serializer, affected);
    }

    
    public boolean canDiscover(final ResourceLocation location) {
        final Binding[] probes = snapshot.mutableProbes;
        if (probes.length == 0) {
            return false;
        }

        final ConcurrentMap<ResourceLocation, Boolean> answers = this.discoverable;
        final Boolean cached = answers.get(location);
        if (cached != null) {
            return cached;
        }

        final boolean discovered = probeMutablePacks(location, probes);
        
        
        answers.put(location, discovered);
        return discovered;
    }

    private static boolean probeMutablePacks(final ResourceLocation location, final Binding[] probes) {
        final String namespace = location.getNamespace();
        for (final Binding probe : probes) {
            if (!probe.namespaces.contains(namespace) && probe.pack.resourceExists(location)) {
                return true;
            }
        }
        return false;
    }

    public RefreshPlan discoverResource(final MetadataSerializer serializer,
                                        final ResourceLocation location) {
        final Snapshot current = snapshot;
        final Binding[] bindings = current.bindings;
        final String namespace = location.getNamespace();
        final Reference2BooleanOpenHashMap<IResourcePack> discoveries = new Reference2BooleanOpenHashMap<>();
        boolean anyDiscovered = false;
        for (final Binding binding : bindings) {
            if (!binding.mutable || binding.namespaces.contains(namespace)
                || discoveries.containsKey(binding.pack)) {
                continue;
            }
            final boolean exists = binding.pack.resourceExists(location);
            discoveries.put(binding.pack, exists);
            anyDiscovered |= exists;
        }
        if (!anyDiscovered) {
            return RefreshPlan.empty(current);
        }

        final Binding[] nextBindings = new Binding[bindings.length];
        for (int i = 0; i < bindings.length; i++) {
            final Binding binding = bindings[i];
            if (!discoveries.getBoolean(binding.pack) || binding.namespaces.contains(namespace)) {
                nextBindings[i] = binding;
                continue;
            }
            final ObjectLinkedOpenHashSet<String> updated = new ObjectLinkedOpenHashSet<>(binding.namespaces);
            updated.add(namespace);
            nextBindings[i] = new Binding(binding.pack, updated, true);
        }
        return buildPlan(
            current, new Snapshot(nextBindings), serializer, Collections.singleton(namespace)
        );
    }

    public void commit(final RefreshPlan plan) {
        applySnapshot(plan.nextSnapshot);
    }

    public Map<String, FallbackResourceManager> rebuildFallbackManagers(final MetadataSerializer serializer) {
        return buildFallbackManagers(snapshot, serializer, null);
    }

    public Map<String, FallbackResourceManager> rebuildFallbackManagers(final MetadataSerializer serializer,
                                                                       final Set<String> namespaceFilter) {
        return buildFallbackManagers(snapshot, serializer, namespaceFilter);
    }

    @Desugar
    private record Binding(IResourcePack pack, Set<String> namespaces, boolean mutable) {
    }

    private static final class Snapshot {
        private static final Binding[] NO_BINDINGS = new Binding[0];
        private static final Snapshot EMPTY = new Snapshot(NO_BINDINGS);

        private final Binding[] bindings;
        private final Binding[] mutableProbes;

        private Snapshot(final Binding[] bindings) {
            this.bindings = bindings;
            final Reference2ObjectOpenHashMap<IResourcePack, ObjectLinkedOpenHashSet<String>> shared =
                new Reference2ObjectOpenHashMap<>();
            for (final Binding binding : bindings) {
                if (!binding.mutable) {
                    continue;
                }
                final ObjectLinkedOpenHashSet<String> common = shared.get(binding.pack);
                if (common == null) {
                    shared.put(binding.pack, new ObjectLinkedOpenHashSet<>(binding.namespaces));
                    continue;
                }
                common.retainAll(binding.namespaces);
            }
            if (shared.isEmpty()) {
                this.mutableProbes = NO_BINDINGS;
                return;
            }
            final Binding[] probes = new Binding[shared.size()];
            int index = 0;
            for (Map.Entry<IResourcePack, ObjectLinkedOpenHashSet<String>> entry : shared.entrySet()) {
                probes[index++] = new Binding(entry.getKey(), entry.getValue(), true);
            }
            this.mutableProbes = probes;
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
            this.replacements = replacements.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(replacements);
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
