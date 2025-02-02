package me.chancesd.sdutils.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.chancesd.sdutils.utils.Log;

public class ScheduleUtils {

	private static ScheduledExecutorService executor;
	private static final boolean FOLIA_SUPPORT = checkFolia();
	private static final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();
	private static SchedulerProvider provider;

	private ScheduleUtils() {
	}

	public static void setupExecutor(final JavaPlugin plugin) {
		provider = FOLIA_SUPPORT ? new FoliaProvider(plugin) : new BukkitProvider(plugin);
		executor = Executors.newScheduledThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() / 2),
				new ThreadFactoryBuilder().setNameFormat("PvPManager Worker Thread - %d").build());
		// no point in having uncaught handler because exceptions are thrown silently in scheduled thread pool
	}

	public static void setExecutor(final ScheduledExecutorService executor) {
		ScheduleUtils.executor = executor;
	}

	public static void runAsync(final Runnable task) {
		executor.execute(new ExceptionRunnable(task));
	}

	public static ScheduledFuture<?> runAsyncLater(final Runnable task, final long delay, final TimeUnit unit) {
		return executor.schedule(new ExceptionRunnable(task), delay, unit);
	}

	public static ScheduledFuture<?> runAsyncTimer(final Runnable task, final long delay, final long period, final TimeUnit unit) {
		final ScheduledFuture<?> scheduledTask = executor.scheduleAtFixedRate(new ExceptionRunnable(task), delay, period, unit);
		scheduledTasks.add(scheduledTask);
		return scheduledTask;
	}

	public static <T> Future<T> runPlatformTask(final Supplier<T> task) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		provider.runTask(() -> {
			final T result = task.get();
			future.complete(result);
		});
		return future;
	}

	public static <T> Future<T> runPlatformTask(final Supplier<T> task, final World world, final int x, final int z) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		provider.runTask(() -> {
			final T result = task.get();
			future.complete(result);
		}, world, x, z);
		return future;
	}

	public static void runPlatformAsync(final Runnable task) {
		provider.runPlatformAsync(task);
	}

	public static void runPlatformAsyncTimer(final Runnable task, final long delay, final long period) {
		provider.runPlatformAsyncTimer(task, delay, period);
	}

	public static void runPlatformTask(final Runnable task) {
		provider.runTask(task);
	}

	public static void runPlatformTask(final Runnable task, @NotNull final Entity entity) {
		provider.runTask(task, entity);
	}

	public static void runPlatformTaskLater(final Runnable task, final Entity entity, final long delay) {
		provider.runTaskLater(task, entity, delay);
	}

	public static SDTask runPlatformTaskTimer(final SDCancellableTask task, final Entity entity, final long delay, final long period) {
		final SDTask sdTask = provider.runTaskTimer(task, entity, delay, period);
		task.setTask(sdTask);
		return sdTask;
	}

	public static void executeConsoleCommand(final String command) {
		provider.executeConsoleCommand(command);
	}

	public static void executePlayerCommand(final Player player, final String command) {
		provider.executePlayerCommand(player, command);
	}

	public static void ensureMainThread(final Runnable task) {
		if (provider.isPrimaryThread()) {
			task.run();
			return;
		}
		runPlatformTask(task);
	}

	public static void ensureMainThread(final Runnable task, @NotNull final Entity entity) {
		if (Bukkit.isPrimaryThread()) { // different from above method for untag on Folia
			task.run();
			return;
		}
		runPlatformTask(task, entity);
	}

	public static CompletableFuture<Boolean> teleport(final Entity entity, @NotNull final Location loc) {
		return provider.teleport(entity, loc);
	}

	public static void cancelAllTasks() {
		scheduledTasks.forEach(scheduledTask -> scheduledTask.cancel(false));
		executor.shutdown();
		provider.cancelAllTasks();
	}

	public static ExecutorService newBoundedCachedThreadPool(final int corePoolSize, final int maxPoolSize, final ThreadFactory threadFactory) {
		final BlockingQueue<Runnable> queue = new LinkedTransferQueue<Runnable>() {
			private static final long serialVersionUID = 4672233456178006928L;

			@Override
			public boolean offer(final Runnable e) {
				return tryTransfer(e);
			}
		};
		final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60, TimeUnit.SECONDS, queue, threadFactory);
		threadPool.setRejectedExecutionHandler((runnable, poolExecutor) -> {
			try {
				poolExecutor.getQueue().put(runnable);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		return threadPool;
	}

	public static boolean checkFolia() {
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServerInitEvent");
			Log.debug("Method " + Bukkit.class.getDeclaredMethod("getAsyncScheduler"));
			return true;
		} catch (final Throwable ignored) {
			return false;
		}
	}

	static class ExceptionRunnable implements Runnable {

		private final Runnable task;

		public ExceptionRunnable(final Runnable task) {
			this.task = task;
		}

		@Override
		public void run() {
			try {
				task.run();
			} catch (final Throwable e) {
				Log.severe(e.getMessage(), e);
			}
		}

	}

}
