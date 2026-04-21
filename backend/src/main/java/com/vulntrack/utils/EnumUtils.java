package com.vulntrack.utils;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.lang.reflect.Field;

public final class EnumUtils {

    private EnumUtils() {}

    /**
     * Converts a String value to the correct Enum instance based on the Entity's field definition.
     * Supports both ORDINAL (int) and STRING (name) storage.
     */
    public static <E extends Enum<E>> E convertToEnum(Class<?> entityClass, String fieldName, Object value) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            
            // Check if the field is actually an Enum
            if (!field.getType().isEnum()) {
                throw new IllegalArgumentException("Field " + fieldName + " is not an Enum");
            }

            @SuppressWarnings("unchecked")
            Class<E> enumClass = (Class<E>) field.getType();

            // Check how it's stored in DB (Ordinal vs String)
            Enumerated enumeratedAnnotation = field.getAnnotation(Enumerated.class);
            EnumType storageType = (enumeratedAnnotation != null) ? enumeratedAnnotation.value() : EnumType.STRING;

            if (storageType == EnumType.ORDINAL) {
                // If stored as Ordinal, value might be a Number or a String representing a number
                int ordinal = Integer.parseInt(value.toString());
                return enumClass.getEnumConstants()[ordinal];
            } else {
                // If stored as String, match by Name
                return Enum.valueOf(enumClass, value.toString().toUpperCase());
            }

        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in entity");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for enum field " + fieldName, e);
        }
    }
}