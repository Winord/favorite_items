package winord.favoriteitems.compat.nemosinventorysorting;

import net.minecraft.world.inventory.AbstractContainerMenu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class NemoReflect {
    private static Field startIndexField;
    private static Method endIndexMethod;

    private NemoReflect() { }

    public static int startIndexOf(Object sortButton) {
        try {
            if (startIndexField == null) {
                startIndexField = findField(sortButton.getClass(), "startIndex");
                startIndexField.setAccessible(true);
            }
            return (Integer) startIndexField.get(sortButton);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nemo's Inventory Sorting compat: cannot read startIndex", e);
        }
    }

    public static int endIndexOf(Object sortButton, AbstractContainerMenu menu) {
        try {
            if (endIndexMethod == null) {
                endIndexMethod = findMethod(sortButton.getClass(), "calculateEndIndex");
                endIndexMethod.setAccessible(true);
            }
            return (Integer) endIndexMethod.invoke(sortButton, menu);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nemo's Inventory Sorting compat: cannot read the end index", e);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (; type != null; type = type.getSuperclass()) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name) throws NoSuchMethodException {
        for (; type != null; type = type.getSuperclass()) {
            for (var method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 1) return method;
            }
        }
        throw new NoSuchMethodException(name);
    }
}
