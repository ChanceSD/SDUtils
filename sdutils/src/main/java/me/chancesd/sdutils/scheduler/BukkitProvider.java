package me.chancesd.sdutils.scheduler;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class BukkitProvider implements SchedulerProvider {

	@NotNull
	private final JavaPlugin plugin;

	public BukkitProvider(@NotNull final JavaPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void runPlatformAsync(final Runnable task) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
	}

	@Override
	public void runPlatformAsyncLater(final Runnable task, final long delay) {
		Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
	}

	@Override
	public void runPlatformAsyncTimer(final Runnable task, final long delay, final long period) {
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
	}

	@Override
	public SDTask runTask(final Runnable task) {
		return new WrappedBukkitTask(Bukkit.getScheduler().runTask(plugin, task));
	}

	@Override
	public void runTask(final Runnable task, final World world, final int x, final int z) {
		runTask(task);
	}

	@Override
	public SDTask runTask(final Runnable task, final Entity entity) {
		return runTask(task);
	}

	@Override
	public void runTaskLater(final Runnable task, final long delay) {
		Bukkit.getScheduler().runTaskLater(plugin, task, delay);
	}

	@Override
	public void runTaskLater(final Runnable task, final Entity entity, final long delay) {
		Bukkit.getScheduler().runTaskLater(plugin, task, delay);
	}

	@Override
	public SDTask runTaskTimer(final Runnable task, final Entity entity, final long delay, final long period) {
		return new WrappedBukkitTask(Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period));
	}

	@Override
	public void executeConsoleCommand(final String command) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
	}

	@Override
	public void executePlayerCommand(final Player player, final String command) {
		player.performCommand(command);
	}

	@Override
	public boolean isPrimaryThread() {
		return Bukkit.isPrimaryThread();
	}

	@Override
	public boolean isServerStopping() {
		return false;
	}

	@Override
	public CompletableFuture<Boolean> teleport(final Entity entity, final Location loc) {
		final CompletableFuture<Boolean> future = new CompletableFuture<>();
		if (isPrimaryThread()) {
			final boolean success = entity.teleport(loc);
			future.complete(success);
		} else {
			runTask(() -> {
				final boolean success = entity.teleport(loc);
				future.complete(success);
			});
		}
		return future;
	}

	@Override
	public void cancelAllTasks() {
		Bukkit.getScheduler().cancelTasks(plugin);
	}

}
