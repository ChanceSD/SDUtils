package me.chancesd.sdutils.command;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Asynchronous argument loader for command arguments that require complex loading or validation.
 * 
 * <p>ArgumentLoader validates and loads command arguments asynchronously. The validator returns
 * an Optional context object that is passed to the loader, eliminating duplicate expensive operations.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Validator performs expensive operation once, returns context
 * ArgumentLoader.of(
 *     name -> {
 *         UUID uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
 *         boolean exists = player.isOnline() || storage.userExists(uuid);
 *         return exists ? Optional.of(uuid) : Optional.empty();
 *     },
 *     (name, uuid) -> playerManager.getOrLoadOffline(uuid),
 *     "§cPlayer not found!"
 * )
 * }</pre>
 * 
 * @param <T> the type of object this loader produces
 */
public class ArgumentLoader<T> {
	private final Function<String, Optional<?>> contextValidator;
	private final BiFunction<String, Object, CompletableFuture<T>> contextLoader;
	private final Function<String, String> validationErrorMessage;

	private ArgumentLoader(
			final Function<String, Optional<?>> contextValidator,
			final BiFunction<String, Object, CompletableFuture<T>> contextLoader,
			final Function<String, String> validationErrorMessage) {
		this.contextValidator = contextValidator;
		this.contextLoader = contextLoader;
		this.validationErrorMessage = validationErrorMessage;
	}

	/**
	 * Creates an ArgumentLoader with context-passing validation.
	 * The validator performs expensive operations once and returns a context object
	 * that is passed to the loader, avoiding duplicate work.
	 * 
	 * @param <T>              the type of object the loader produces
	 * @param <C>              the type of context object passed from validator to loader
	 * @param contextValidator function that validates and returns Optional of context if valid
	 * @param loader           function that receives input and context to load the object
	 * @param errorMessage     function that generates error message for invalid input
	 * @return a new ArgumentLoader instance
	 */
	public static <T, C> ArgumentLoader<T> of(
			final Function<String, Optional<C>> contextValidator,
			final BiFunction<String, C, T> loader,
			final Function<String, String> errorMessage) {
		// Wrap the context validator to return Optional<?>
		final Function<String, Optional<?>> wrappedValidator = contextValidator::apply;
		
		// Wrap the loader to work with Object context and execute asynchronously
		final BiFunction<String, Object, CompletableFuture<T>> wrappedLoader = 
			(input, ctx) -> CompletableFuture.supplyAsync(() -> {
				@SuppressWarnings("unchecked")
				final C typedContext = (C) ctx;
				return loader.apply(input, typedContext);
			});
		
		return new ArgumentLoader<>(wrappedValidator, wrappedLoader, errorMessage);
	}
	
	/**
	 * Creates an ArgumentLoader with context-passing validation using a static error message.
	 * 
	 * @param <T>              the type of object the loader produces
	 * @param <C>              the type of context object passed from validator to loader
	 * @param contextValidator function that validates and returns Optional of context if valid
	 * @param loader           function that receives input and context to load the object
	 * @param errorMessage     static error message to show when validation fails
	 * @return a new ArgumentLoader instance
	 */
	public static <T, C> ArgumentLoader<T> of(
			final Function<String, Optional<C>> contextValidator,
			final BiFunction<String, C, T> loader,
			final String errorMessage) {
		return of(contextValidator, loader, input -> errorMessage);
	}

	/**
	 * Validates the input and returns a context object if valid.
	 * 
	 * @param input the input string to validate
	 * @return Optional containing the context if valid, empty if invalid
	 */
	Optional<?> validateAndGetContext(final String input) {
		return contextValidator.apply(input);
	}
	
	/**
	 * Validates the input string.
	 * 
	 * @param input the input string to validate
	 * @return true if valid, false if invalid
	 */
	public boolean isValid(final String input) {
		return contextValidator.apply(input).isPresent();
	}
	
	/**
	 * Loads the object asynchronously using a previously validated context.
	 * 
	 * @param input   the input string to load from
	 * @param context the context object returned by the validator
	 * @return a CompletableFuture that will complete with the loaded object
	 */
	CompletableFuture<T> loadWithContext(final String input, final Object context) {
		return contextLoader.apply(input, context);
	}

	/**
	 * Gets the validation error message for the given input.
	 * 
	 * @param input the input string that failed validation
	 * @return the error message to display to the user
	 */
	public String getValidationError(final String input) {
		return validationErrorMessage.apply(input);
	}
}
