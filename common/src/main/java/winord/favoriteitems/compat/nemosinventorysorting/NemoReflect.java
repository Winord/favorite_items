package winord.favoriteitems.compat.nemosinventorysorting;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class NemoReflect {
    private static Field startIndexField;
    private static Method endIndexMethod;
    private static Method slotIndexMethod;

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

    public static int endIndexOf(Object button) {
        try {
            if (endIndexMethod == null) {
                endIndexMethod = findMethod(button.getClass(), "getEndIndex");
                endIndexMethod.setAccessible(true);
            }
            return (Integer) endIndexMethod.invoke(button);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nemo's Inventory Sorting compat: cannot read getEndIndex", e);
        }
    }

    public static int slotIndexOf(Object slotItem) {
        try {
            if (slotIndexMethod == null || slotIndexMethod.getDeclaringClass() != slotItem.getClass()) {
                slotIndexMethod = slotItem.getClass().getMethod("slotIndex");
            }
            return (Integer) slotIndexMethod.invoke(slotItem);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nemo's Inventory Sorting compat: cannot read SlotItem.slotIndex", e);
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
            try {
                return type.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(name);
    }
}
