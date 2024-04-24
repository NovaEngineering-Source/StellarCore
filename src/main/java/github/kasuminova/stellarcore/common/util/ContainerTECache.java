package github.kasuminova.stellarcore.common.util;

import github.kasuminova.stellarcore.StellarCore;
import github.kasuminova.stellarcore.common.config.StellarCoreConfig;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContainerTECache {

    private static final Map<Class<? extends Container>, Function<Container, List<TileEntity>>> CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<Class<?>, List<Field>>> CLASS_FILED_TYPE_CACHE = new ConcurrentHashMap<>();

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
        if (StellarCoreConfig.DEBUG.enableDebugLog && !availableFields.isEmpty()) {
            StringBuilder sb = new StringBuilder("[DEBUG] Registered TileEntity container, available fields: \n");
            for (Iterator<Field> it = availableFields.iterator(); it.hasNext(); ) {
                final Field field = it.next();
                sb.append(field.getType().getName()).append(": ").append(field.getName());
                if (it.hasNext()) {
                    sb.append(";\n");
                }
            }
            StellarCore.log.info(sb.toString());
        }
        CACHE.put(cClass, func);
        return func;
    }

    private static <T> T safeGetField(final Object instance, final Field field, final Class<T> type) {
        Object obj;
        try {
            obj = field.get(instance);
            if (type.isInstance(obj)) {
                return type.cast(obj);
            } else if (StellarCoreConfig.DEBUG.enableDebugLog) {
                StellarCore.log.warn("[DEBUG] Field {} {} is not assignable to {}", field.getType(), field.getName(), type.getName());
            }
        } catch (Error | Exception e) {
            StellarCore.log.warn(e);
        }
        return null;
    }

    public static List<Field> scanTileEntityField(final Class<? extends Container> containerClass) {
        return scanTileEntityFieldRecursive(containerClass, TileEntity.class);
    }

    private static List<Field> scanTileEntityFieldRecursive(Class<?> aClass, Class<?> target) {
        Map<Class<?>, List<Field>> cachedFieldMap = CLASS_FILED_TYPE_CACHE.computeIfAbsent(aClass, v -> new ConcurrentHashMap<>());
        List<Field> fieldCache = cachedFieldMap.get(target);
        if (fieldCache != null) {
            return fieldCache;
        }

        List<Field> teFields = new ArrayList<>();

        // 遍历当前类的声明字段
        try {
            Field[] fields = aClass.getDeclaredFields();
            if (StellarCoreConfig.DEBUG.enableDebugLog) {
                StellarCore.log.info("[DEBUG] Scanning fields for class {}, required: {}", aClass.getName(), target.getName());
            }

            for (Field field : fields) {
                boolean assignable = target.isAssignableFrom(field.getType());
                if (StellarCoreConfig.DEBUG.enableDebugLog) {
                    StellarCore.log.info("[DEBUG] Field: {} {} (targetAssignable = {})", field.getType().getName(), field.getName(), assignable);
                }

                if (assignable) {
                    field.setAccessible(true);
                    teFields.add(field);
                }
            }
            // 某些模组的黑魔法会导致扫 Field 的时候出现奇怪的问题，特此点名 AE2UEL 的 ContainerCraftConfirm。
        } catch (Error | Exception ignored) {
        }

        // 检查是否有父类，如果有，则递归遍历父类
        try {
            Class<?> superClass = aClass.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                List<Field> parentFields = scanTileEntityFieldRecursive(superClass, target);
                teFields.addAll(parentFields);
            }
        } catch (Error | Exception ignored) {
        }

        cachedFieldMap.put(target, teFields);
        return teFields;
    }

}
