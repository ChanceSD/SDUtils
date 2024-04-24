package me.chancesd.sdutils.utils;

import java.lang.reflect.InvocationTargetException;

public class ReflectionUtil {

	public static Object invokeMethods(final Object object, final String... methods)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Object result = object;
		for (final String method : methods) {
			result = result.getClass().getDeclaredMethod(method).invoke(result);
		}
		return result;
	}

}
