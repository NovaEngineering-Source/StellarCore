package github.kasuminova.stellarcore.common.config.element;

import net.minecraftforge.common.config.Config;

import java.lang.reflect.Field;

public interface ConfigElement {

    default String name() {
        return field().getAnnotation(Config.Name.class).value();
    }

    default String langKey() {
        Field field = field();
        if (field.isAnnotationPresent(Config.LangKey.class)) {
            return field.getAnnotation(Config.LangKey.class).value();
        }
        return field.getAnnotation(Config.Name.class).value();
    }

    Object object();

    Field field();

}
