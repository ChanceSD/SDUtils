package me.chancesd.sdutils.library;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.bukkit.plugin.Plugin;

/**
 * Thanks to https://www.spigotmc.org/resources/dependencymanager.62275/
 * DotRar for the original source.
 *
 */
public enum InjectorUtils {
	INSTANCE;
	private URLClassLoader classLoader;
	private Method addUrlMethod;

	private void ensureInitiated(final Plugin pl) throws NoSuchMethodException {
		classLoader = (URLClassLoader) pl.getClass().getClassLoader();

		final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		method.setAccessible(true);

		this.addUrlMethod = method;
	}

	public void loadJar(final Plugin pl, final File src) throws MalformedURLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		ensureInitiated(pl);
		addUrlMethod.invoke(classLoader, src.toURI().toURL());
	}
}