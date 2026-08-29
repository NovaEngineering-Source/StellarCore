package github.kasuminova.stellarcore.mixin.util;

import java.io.File;

/** Exposes an AbstractResourcePack root to the FolderResourcePack path-check Mixin. */
public interface StellarCoreAbstractResourcePackAccessor {

    File stellar_core$getResourcePackFile();
}
