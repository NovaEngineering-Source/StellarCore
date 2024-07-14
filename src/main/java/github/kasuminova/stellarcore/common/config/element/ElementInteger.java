package github.kasuminova.stellarcore.common.config.element;

import com.github.bsideup.jabel.Desugar;
import net.minecraftforge.common.config.Config;

import java.lang.reflect.Field;

@Desugar
public record ElementInteger(Object object, Field field) implements ConfigElementPrimitive<Integer> {

    @Override
    public Integer get() throws IllegalAccessException {
        return field.getInt(object);
    }

    public void set(final Integer newValue) throws IllegalAccessException {
        field.setInt(object, newValue);
    }

    public int getMin() {
        if (field.isAnnotationPresent(Config.RangeInt.class)) {
            return field.getAnnotation(Config.RangeInt.class).min();
        }
        return Integer.MIN_VALUE;
    }

    public int getMax() {
        if (field.isAnnotationPresent(Config.RangeInt.class)) {
            return field.getAnnotation(Config.RangeInt.class).max();
        }
        return Integer.MAX_VALUE;
    }

}
