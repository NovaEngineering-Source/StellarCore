package github.kasuminova.stellarcore.common.config.element;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Category implements ConfigElement {

    private final Object object;
    private final Field field;
    private final List<ConfigElement> elements;

    public Category(final Object object, final Field field, final List<ConfigElement> elements) {
        this.object = object;
        this.field = field;
        this.elements = elements;
    }

    public Category(final Object object, final Field field) {
        this.object = object;
        this.field = field;
        this.elements = new ArrayList<>();
    }

    public Object getObject() {
        return object;
    }

    public void addElement(final ConfigElement element) {
        elements.add(element);
    }

    @Override
    public Object object() {
        return object;
    }

    @Override
    public Field field() {
        return field;
    }

}
