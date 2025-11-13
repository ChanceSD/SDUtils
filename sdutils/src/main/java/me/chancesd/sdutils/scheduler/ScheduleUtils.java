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
import me.chancesd.sdutils.utils.Utils;

/**
 * Utility class for scheduling tasks across both regular Bukkit and Folia environments.
 * Provides methods for async operations, platform-specific tasks, and thread safety.
 * <p>
 * This class automatically detects if Folia is present and uses the appropriate scheduler
 * implementation. It also provides a standard way to handle exceptions in scheduled tasks.
 */
public class ScheduleUtils {

	private static ScheduledExecutorService executor;
	private static final boolean FOLIA_SUPPORT = checkFolia();
	private static final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();
	private static SchedulerProvider provider;

	private ScheduleUtils() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Sets up the executor service with a thread pool optimized for the current system.
	 * Must be called before using any other methods in this class.
	 *
	 * @param plugin The JavaPlugin instance that owns the tasks
	 */
	public static void setupExecutor(final JavaPlugin plugin) {
		provider = FOLIA_SUPPORT ? new FoliaProvider(plugin) : new BukkitProvider(plugin);
		executor = Executors.newScheduledThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() / 2),
				new ThreadFactoryBuilder().setNameFormat(plugin.getName() + " Worker Thread - %d").build());
		 // no point in having uncaught handler because exceptions are thrown silently in ScheduledExecutorService
	}

	/**
	 * Sets a custom executor service
	 *
	 * @param executor The executor service to use
	 */
	public static void setExecutor(final ScheduledExecutorService executor) {
		ScheduleUtils.executor = executor;
	}

	/**
	 * Runs a task asynchronously using the executor service
	 *
	 * @param task The task to run
	 */
	public static void runAsync(@NotNull final Runnable task) {
		executor.execute(new ExceptionRunnable(task));
	}

	/**
	 * Runs a task asynchronously after a delay
	 *
	 * @param task  The task to run
	 * @param delay The delay before execution
	 * @param unit  The time unit for the delay
	 * @return A ScheduledFuture representing the scheduled task, or null if the task could not be scheduled due to invalid parameters
	 * @throws IllegalArgumentException if delay is negative
	 */
	public static ScheduledFuture<?> runAsyncLater(@NotNull final Runnable task, final long delay, final TimeUnit unit) {
		if (delay < 0) {
			Log.warning("Cannot schedule task: delay cannot be negative (" + delay + ")");
			return null;
		}
		return executor.schedule(new ExceptionRunnable(task), delay, unit);
	}

	/**
	 * Runs a task asynchronously at fixed intervals
	 *
	 * @param task   The task to run
	 * @param delay  The initial delay before first execution
	 * @param period The period between successive executions
	 * @param unit   The time unit for delay and period
	 * @return A ScheduledFuture representing the scheduled task, or null if the task could not be scheduled due
	 *         to invalid parameters
	 */
	public static ScheduledFuture<?> runAsyncTimer(@NotNull final Runnable task, final long delay, final long period, final TimeUnit unit) {
		if (delay < 0) {
			Log.warning("Cannot schedule task: delay cannot be negative (" + delay + ")");
			return null;
		}
		if (period <= 0) {
			Log.warning("Cannot schedule task: period must be positive (" + period + " " + unit + "). Skipping task scheduling.");
			return null;
		}
		final ScheduledFuture<?> scheduledTask = executor.scheduleAtFixedRate(new ExceptionRunnable(task), delay, period, unit);
		scheduledTasks.add(scheduledTask);
		return scheduledTask;
	}

	/**
	 * Runs a task on the platform's main thread and returns a result.
	 * This will use the appropriate thread based on whether Folia is enabled.
	 * In Folia mode, this runs on the global region scheduler.
	 *
	 * @param <T>  The return type of the task
	 * @param task The task to run
	 * @return A Future that will be completed with the task's result
	 */
	public static <T> Future<T> runPlatformTask(final Supplier<T> task) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		provider.runTask(() -> {
			try {
				final T result = task.get();
				future.complete(result);
			} catch (final Throwable e) {
				future.completeExceptionally(e);
				Log.severe(e.getMessage(), e);
			}
		});
		return future;
	}

	/**
	 * Runs a task on the platform's main thread for a specific world region.
	 * In Folia, this runs on the region scheduler for the specified chunk.
	 * In standard Bukkit, this behaves the same as {@link #runPlatformTask(Supplier)}.
	 *
	 * @param <T>   The return type of the task
	 * @param task  The task to run
	 * @param world The world to run the task in
	 * @param x     The x-coordinate of the chunk
	 * @param z     The z-coordinate of the chunk
	 * @return A Future that will be completed with the task's result
	 */
	public static <T> Future<T> runPlatformTask(final Supplier<T> task, final World world, final int x, final int z) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		provider.runTask(() -> {
			try {
				final T result = task.get();
				future.complete(result);
			} catch (final Throwable e) {
				future.completeExceptionally(e);
				Log.severe(e.getMessage(), e);
			}
		}, world, x, z);
		return future;
	}

	/**
	 * Runs a task asynchronously using the platform's scheduler.
	 * In Folia, this uses the AsyncScheduler.
	 * In standard Bukkit, this uses BukkitScheduler.runTaskAsynchronously.
	 *
	 * @param task The task to run
	 */
	public static void runPlatformAsync(final Runnable task) {
		provider.runPlatformAsync(task);
	}

	/**
	 * Runs a task asynchronously after a delay using the platform's scheduler.
	 * In Folia, this uses the AsyncScheduler.
	 * In standard Bukkit, this uses BukkitScheduler.runTaskLaterAsynchronously.
	 *
	 * @param task  The task to run
	 * @param delay The delay before execution (in ticks)
	 */
	public static void runPlatformAsyncLater(final Runnable task, final long delay) {
		provider.runPlatformAsyncLater(task, delay);
	}

	/**
	 * Runs a task asynchronously at fixed intervals using the platform's scheduler.
	 * In Folia, this uses the AsyncScheduler.
	 * In standard Bukkit, this uses BukkitScheduler.runTaskTimerAsynchronously.
	 *
	 * @param task   The task to run
	 * @param delay  The initial delay before first execution (in ticks)
	 * @param period The period between successive executions (in ticks)
	 */
	public static void runPlatformAsyncTimer(final Runnable task, final long delay, final long period) {
		provider.runPlatformAsyncTimer(task, delay, period);
	}

	/**
	 * Runs a task on the platform's main thread.
	 * In Folia, this runs on the global region scheduler.
	 * In standard Bukkit, this uses BukkitScheduler.runTask.
	 *
	 * @param task The task to run
	 */
	public static void runPlatformTask(final Runnable task) {
		provider.runTask(task);
	}

	/**
	 * Runs a task on the platform's thread for a specific entity.
	 * In Folia, this runs on the entity's scheduler.
	 * In standard Bukkit, this behaves the same as {@link #runPlatformTask(Runnable)}.
	 *
	 * @param task   The task to run
	 * @param entity The entity to run the task for
	 */
	public static void runPlatformTask(final Runnable task, @NotNull final Entity entity) {
		provider.runTask(task, entity);
	}

	/**
	 * Runs a task on the platform's thread after a delay.
	 * In Folia, this runs on the global region scheduler.
	 * In standard Bukkit, this uses BukkitScheduler.runTaskLater.
	 *
	 * @param task  The task to run
	 * @param delay The delay before execution (in ticks)
	 */
	public static void runPlatformTaskLater(final Runnable task, final long delay) {
		provider.runTaskLater(task, delay);
	}

	/**
	 * Runs a task on the platform's thread for a specific entity after a delay.
	 * In Folia, this runs on the entity's scheduler.
	 * In standard Bukkit, this behaves like BukkitScheduler.runTaskLater.
	 *
	 * @param task   The task to run
	 * @param entity The entity to run the task for
	 * @param delay  The delay before execution (in ticks)
	 */
	public static void runPlatformTaskLater(final Runnable task, final Entity entity, final long delay) {
		provider.runTaskLater(task, entity, delay);
	}

	/**
	 * Runs a task on the platform's thread for a specific entity at fixed intervals.
	 * In Folia, this runs on the entity's scheduler.
	 * In standard Bukkit, this behaves like BukkitScheduler.runTaskTimer.
	 *
	 * @param task   The task to run
	 * @param entity The entity to run the task for
	 * @param delay  The initial delay before first execution (in ticks)
	 * @param period The period between successive executions (in ticks)
	 * @return An SDTask representing the scheduled task, which can be cancelled
	 */
	public static SDTask runPlatformTaskTimer(final SDCancellableTask task, final Entity entity, final long delay, final long period) {
		final SDTask sdTask = provider.runTaskTimer(task, entity, delay, period);
		task.setTask(sdTask);
		return sdTask;
	}

	/**
	 * Executes a command as the console.
	 * In Folia, this executes on the global region scheduler.
	 *
	 * @param command The command to execute
	 */
	public static void executeConsoleCommand(final String command) {
		provider.executeConsoleCommand(command);
	}

	/**
	 * Executes a command as a player
	 *
	 * @param player  The player to execute the command as
	 * @param command The command to execute
	 */
	public static void executePlayerCommand(final Player player, final String command) {
		provider.executePlayerCommand(player, command);
	}

	/**
	 * Ensures a task runs on the main thread.
	 * If already on the main thread, the task runs immediately.
	 * Otherwise, it's scheduled to run on the main thread.
	 *
	 * @param task The task to run
	 */
	public static void ensureMainThread(final Runnable task) {
		if (provider.isPrimaryThread()) {
			task.run();
			return;
		}
		runPlatformTask(task);
	}

	/**
	 * Ensures a task runs on the thread for a specific entity.
	 * If already on the primary thread, the task runs immediately and returns null.
	 * Otherwise, it's scheduled to run on the entity's thread and returns an SDTask.
	 *
	 * @param task   The task to run
	 * @param entity The entity to run the task for
	 * @return An SDTask representing the scheduled task, or null if run immediately
	 */
	public static SDTask ensureMainThread(final Runnable task, @NotNull final Entity entity) {
		if (Bukkit.isPrimaryThread()) { // different from above method for untag on Folia
			task.run();
			return null;
		}
		return provider.runTask(task, entity);
	}

	/**
	 * Teleports an entity to a location safely.
	 * In Folia, this uses entity.teleportAsync().
	 * In standard Bukkit, this wraps entity.teleport() in a CompletableFuture.
	 *
	 * @param entity The entity to teleport
	 * @param loc    The location to teleport to
	 * @return A CompletableFuture that will complete with true if teleport was successful
	 */
	public static CompletableFuture<Boolean> teleport(final Entity entity, @NotNull final Location loc) {
		return provider.teleport(entity, loc);
	}

	/**
	 * Teleports an entity to a location safely with error logging.
	 * In Folia, this uses entity.teleportAsync().
	 * In standard Bukkit, this wraps entity.teleport() in a CompletableFuture.
	 * Failed teleports will be logged with the provided error message.
	 *
	 * @param entity       The entity to teleport
	 * @param loc          The location to teleport to
	 * @param errorMessage The error message to log on failure, or null to disable logging
	 * @return A CompletableFuture that will complete with true if teleport was successful
	 */
	public static CompletableFuture<Boolean> teleport(final Entity entity, @NotNull final Location loc, @NotNull final String errorMessage) {
		final CompletableFuture<Boolean> future = provider.teleport(entity, loc);
		future.whenComplete((success, ex) -> {
			if (!success || ex != null) {
				Log.severe(errorMessage, ex);
			}
		});
		return future;
	}

	/**
	 * Cancels all scheduled tasks and shuts down the executor.
	 * This should be called when your plugin is disabled.
	 */
	public static void cancelAllTasks() {
		scheduledTasks.forEach(scheduledTask -> scheduledTask.cancel(false));
		scheduledTasks.clear();

		executor.shutdown();
		provider.cancelAllTasks();
	}

	/**
	 * Creates a new bounded cached thread pool that won't reject tasks.
	 * This pool will queue tasks when all threads are busy rather than rejecting them.
	 *
	 * @param corePoolSize  The minimum number of threads
	 * @param maxPoolSize   The maximum number of threads
	 * @param threadFactory The factory to create threads
	 * @return A new ExecutorService
	 */
	public static ExecutorService newBoundedCachedThreadPool(final int corePoolSize, final int maxPoolSize, final ThreadFactory threadFactory) {
		final BlockingQueue<Runnable> queue = new LinkedTransferQueue<>() {
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

	/**
	 * Wraps a CompletableFuture's thenRun to ensure the action runs on the main thread
	 * with proper exception logging. This is essential for operations that must run
	 * synchronously, such as firing Bukkit events.
	 *
	 * @param future The CompletableFuture to attach to
	 * @param action The action to run on the main thread after the future completes
	 * @return A new CompletableFuture with exception logging
	 */
	public static CompletableFuture<Void> thenRunSync(final CompletableFuture<?> future, final Runnable action) {
		final CompletableFuture<Void> result = new CompletableFuture<>();
		future.thenRun(() -> provider.runTask(new ExceptionRunnable(() -> {
			action.run();
			result.complete(null);
		}))).exceptionally(throwable -> {
			Log.severe("Exception in async chain before thenRunSync: " + throwable.getMessage(), throwable);
			result.completeExceptionally(throwable);
			return null;
		});
		return result;
	}

	private static boolean checkFolia() {
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServerInitEvent");
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			return true;
		} catch (final Throwable ignored) {
			return false;
		}
	}

	/**
	 * Checks if this server is running Folia
	 *
	 * @return true if Folia, false otherwise
	 */
	public static boolean isFolia() {
		return FOLIA_SUPPORT;
	}

	/**
	 * Checks if the server is stopping on Paper/Folia
	 *
	 * @return true if stopping, false otherwise
	 */
	public static boolean isServerStopping() {
		return Utils.isPaper() && provider.isServerStopping();
	}

	/**
	 * Simple wrapper that catches and logs exceptions from runnables
	 */
	public static class ExceptionRunnable implements Runnable {

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
