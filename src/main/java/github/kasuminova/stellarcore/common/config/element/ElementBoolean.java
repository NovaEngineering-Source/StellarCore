package github.kasuminova.stellarcore.common.config.element;

import com.github.bsideup.jabel.Desugar;

import java.lang.reflect.Field;

@Desugar
public record ElementBoolean(Object object, Field field) implements ConfigElementPrimitive<Boolean> {

    public Boolean get() throws IllegalAccessException {
        return field.getBoolean(object);
    }

    public void set(final Boolean newValue) throws IllegalAccessException {
        field.setBoolean(object, newValue);
    }

}
