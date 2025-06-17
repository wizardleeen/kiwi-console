package org.kiwi.console.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;


public class ReflectionUtils {

    public static Class<?> getRawClass(Type type) {
        return switch (type) {
            case Class<?> klass -> klass;
            case ParameterizedType parameterizedType -> getRawClass(parameterizedType.getRawType());
            case WildcardType wildcardType -> getRawClass(wildcardType.getUpperBounds()[0]);
            case TypeVariable<?> typeVariable -> getRawClass(typeVariable.getBounds()[0]);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

}
