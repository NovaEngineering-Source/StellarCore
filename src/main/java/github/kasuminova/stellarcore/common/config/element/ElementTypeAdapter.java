package github.kasuminova.stellarcore.common.config.element;

import java.lang.reflect.Field;

public interface ElementTypeAdapter {

    ConfigElement get(Object object, Field field);

}
