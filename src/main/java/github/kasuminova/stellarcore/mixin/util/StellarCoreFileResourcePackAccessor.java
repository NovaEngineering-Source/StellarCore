package github.kasuminova.stellarcore.mixin.util;

import java.io.IOException;
import java.util.zip.ZipFile;

public interface StellarCoreFileResourcePackAccessor {

    ZipFile stellar_core$getResourcePackZipFile() throws IOException;
}
