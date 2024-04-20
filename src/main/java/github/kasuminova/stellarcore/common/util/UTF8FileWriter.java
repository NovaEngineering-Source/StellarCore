package github.kasuminova.stellarcore.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class UTF8FileWriter extends OutputStreamWriter {

    public UTF8FileWriter(File file) throws IOException {
        super(new FileOutputStream(file), StandardCharsets.UTF_8);
    }

}
