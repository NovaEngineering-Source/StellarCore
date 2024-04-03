package github.kasuminova.stellarcore.common.util;

import github.kasuminova.stellarcore.StellarCore;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContainerTECache {

    private static final Map<Class<? extends Container>, Function<Container, List<TileEntity>>> CACHE = new ConcurrentHashMap<>();

    public static List<TileEntity> getTileEntityList(final Container container) {
        Function<Container, List<TileEntity>> func = CACHE.get(container.getClass());
        return func == null ? register(container.getClass()).apply(container) : func.apply(container);
    }

    public static Function<Container, List<TileEntity>> register(final Class<? extends Container> cClass) {
        List<Field> availableFields = scanTileEntityField(cClass);
        Function<Container, List<TileEntity>> func = (container) -> {
            if (container == null) {
                return Collections.emptyList();
            }
            return availableFields.stream()
                    .map(field -> safeGetField(container, field, TileEntity.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        };
        CACHE.put(cClass, func);
        return func;
    }

    private static <T> T safeGetField(final Object instance, final Field field, final Class<T> type) {
        Object obj;
        try {
            obj = field.get(instance);
            if (type.isInstance(obj)) {
                return type.cast(obj);
            }
        } catch (IllegalAccessException e) {
            StellarCore.log.warn(e);
        }
        return null;
    }

    public static List<Field> scanTileEntityField(final Class<? extends Container> containerClass) {
        return scanTileEntityFieldRecursive(containerClass, TileEntity.class);
    }

    private static List<Field> scanTileEntityFieldRecursive(Class<?> aClass, Class<?> target) {
        List<Field> teFields = new ArrayList<>();

        // 遍历当前类的声明字段
        for (Field field : aClass.getDeclaredFields()) {
            if (target.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                teFields.add(field);
            }
        }

        // 检查是否有父类，如果有，则递归遍历父类
        Class<?> superClass = aClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            List<Field> parentFields = scanTileEntityFieldRecursive(superClass, target);
            teFields.addAll(parentFields);
        }

        return teFields;
    }

}
