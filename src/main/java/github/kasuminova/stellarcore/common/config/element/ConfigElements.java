package github.kasuminova.stellarcore.common.config.element;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigElements {

    private static final Map<Class<?>, ElementTypeAdapter> TYPE_ADAPTERS = new HashMap<>();

    static {
        registerTypeAdapter(boolean.class, ElementBoolean::new);
        registerTypeAdapter(Boolean.class, ElementBoolean::new);
        registerTypeAdapter(int.class,     ElementInteger::new);
        registerTypeAdapter(Integer.class, ElementInteger::new);
        registerTypeAdapter(double.class,  ElementDouble::new);
        registerTypeAdapter(Double.class,  ElementDouble::new);
    }

    public static ConfigElement getConfigElement(final Object object, final Field field) {
        if (object == null) {
            if (!Modifier.isStatic(field.getModifiers())) {
                return null;
            }
        }

        ElementTypeAdapter adapter = TYPE_ADAPTERS.get(field.getType());
        if (adapter != null) {
            if (Modifier.isFinal(field.getModifiers())) {
                return null;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                return adapter.get(null, field);
            }
            return adapter.get(object, field);
        }

        if (object != null) {
            Class<?> objectClass = object.getClass();
            if (objectClass.getSuperclass() == Object.class) {
                return new Category(object, field, getConfigElements(object, objectClass.getFields()));
            }
        } else {
            if (Modifier.isStatic(field.getModifiers())) {
                return new Category(null, field, getConfigElements(null, field.getType().getFields()));
            }
        }

        return null;
    }

    public static List<ConfigElement> getConfigElements(final Class<?> classObject) {
        return getConfigElements(null, classObject.getFields());
    }

    public static List<ConfigElement> getConfigElements(final Object object, final Field[] fields) {
        return Arrays.stream(fields)
                .map(field -> getConfigElement(object, field))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static void registerTypeAdapter(final Class<?> type, final ElementTypeAdapter adapter) {
        TYPE_ADAPTERS.put(type, adapter);
    }

}
