package de.evosec.simpleawaitility;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SimpleAwaitility {

	public static SimpleAwaitility await() {
		return new SimpleAwaitility();
	}

	private SimpleAwaitility() {
	}

	private long pollInterval = TimeUnit.MILLISECONDS.toMillis(100);
	private long pollDelay = TimeUnit.MILLISECONDS.toMillis(100);
	private long atMost = TimeUnit.SECONDS.toMillis(10);
	private List<Class<?>> ignoredExceptions = emptyList();

	public SimpleAwaitility atMost(long timeout, TimeUnit unit) {
		this.atMost = unit.toMillis(timeout);
		return this;
	}

	public SimpleAwaitility pollDelay(long timeout, TimeUnit unit) {
		this.pollDelay = unit.toMillis(timeout);
		return this;
	}

	public SimpleAwaitility pollInterval(long timeout, TimeUnit unit) {
		this.pollInterval = unit.toMillis(timeout);
		return this;
	}

	public SimpleAwaitility ignoredExceptions(Class<?>... exception) {
		this.ignoredExceptions = Arrays.asList(exception);
		return this;
	}

	public void until(Callable<Boolean> callable) {
		ScheduledExecutorService executor =
		        Executors.newSingleThreadScheduledExecutor();
		CompletableFuture<Void> completionFuture = new CompletableFuture<>();

		ScheduledFuture<?> checkFuture = executor.scheduleWithFixedDelay(
		    () -> checkForCompletion(callable, completionFuture), pollDelay,
		    pollInterval, TimeUnit.MILLISECONDS);

		try {
			completionFuture.get(atMost, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException
		        | TimeoutException e) {
			throw new IllegalStateException(e);
		} finally {
			checkFuture.cancel(true);
			executor.shutdown();
		}
	}

	private void checkForCompletion(Callable<Boolean> callable,
	        CompletableFuture<Void> completionFuture) {
		try {
			if (callable.call()) {
				completionFuture.complete(null);
			}
		} catch (Exception e) {
			if (ignoredExceptions.stream()
			    .noneMatch(c -> c.isAssignableFrom(e.getClass()))) {
				completionFuture.completeExceptionally(e);
			}
		}
	}

}
