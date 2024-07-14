package github.kasuminova.stellarcore.common.config.element;

import com.github.bsideup.jabel.Desugar;

import java.lang.reflect.Field;

@Desugar
public record ElementDouble(Object object, Field field) implements ConfigElementPrimitive<Double> {

    public Double get() throws IllegalAccessException {
        return field.getDouble(object);
    }

    public void set(final Double newValue) throws IllegalAccessException {
        field.setDouble(object, newValue);
    }

}
