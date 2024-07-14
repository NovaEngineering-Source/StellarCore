package github.kasuminova.stellarcore.common.config.element;

public interface ConfigElementPrimitive<T> extends ConfigElement {

    T get() throws IllegalAccessException;

    void set(final T value) throws IllegalAccessException;

}
