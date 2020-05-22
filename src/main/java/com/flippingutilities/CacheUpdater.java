package com.flippingutilities;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Updates the cache in real time as files are changed in the directory being monitored. It monitors the directory
 * where the accounts' data is stored and fires a callback when it detects a change for an account. The reason it
 * accepts a callback is so that this class is not tied to any specific component's way of handling a file change. This
 * decoupling allows the cache updater to be used easily by any component that wishes to fire an action when a file
 * for an account is changed.
 */
@Slf4j
public class CacheUpdater
{

	ScheduledExecutorService executor;

	List<Consumer<String>> callbacks = new ArrayList<>();

	boolean isBeingShutdown = false;

	Future realTimeUpdateTask;

	Map<String, Long> lastEvents = new HashMap<>();


	public CacheUpdater()
	{
		this.executor = Executors.newSingleThreadScheduledExecutor();
	}

	public void registerCallback(Consumer<String> callback) {
		callbacks.add(callback);
	}

	public void start()
	{
		realTimeUpdateTask = executor.schedule(this::updateCacheRealTime, 1000, TimeUnit.MILLISECONDS);
	}

	public void stop()
	{
		isBeingShutdown = true;
		realTimeUpdateTask.cancel(true);
	}

	public void updateCacheRealTime()
	{
		try
		{
			log.info("monitoring directory for changes!");

			WatchService watchService = FileSystems.getDefault().newWatchService();

			Path path = TradePersister.PARENT_DIRECTORY.toPath();

			path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

			WatchKey key;
			while ((key = watchService.take()) != null)
			{
				for (WatchEvent<?> event : key.pollEvents())
				{
					log.info("change in directory for {} with event: {}", event.context(), event.kind());
					if (!isDuplicateEvent(event.context().toString()))
					{
						log.info("not duplicate event, firing callbacks");
						callbacks.forEach(callback -> callback.accept(event.context().toString()));
					}
					else
					{
						log.info("duplicate event, not firing callbacks");
					}

				}
				//put the key back in the queue so we can take out more events when they occur
				key.reset();
			}
		}

		catch (IOException e)
		{
			log.info("io exception in updateCacheRealTime. Error = {}", e);
			if (!isBeingShutdown)
			{
				log.info("Scheduling task again.");
				realTimeUpdateTask = executor.schedule(this::updateCacheRealTime, 1000, TimeUnit.MILLISECONDS);
			}
			return;

		}

		catch (InterruptedException e)
		{
			if (!isBeingShutdown)
			{
				log.info("InterruptedException in updateCacheRealTime. Scheduling task again. Error = {}", e);
				realTimeUpdateTask = executor.schedule(this::updateCacheRealTime, 1000, TimeUnit.MILLISECONDS);
			}
			return;
		}

	}

	private boolean isDuplicateEvent(String fileName)
	{
		long lastModified = TradePersister.lastModified(fileName);
		if (lastEvents.containsKey(fileName))
		{
			long prevModificationTime = lastEvents.get(fileName);
			if (prevModificationTime == lastModified)
			{
				return true;
			}
			else
			{
				lastEvents.put(fileName, lastModified);
				return false;
			}

		}
		else
		{
			lastEvents.put(fileName, lastModified);
			return false;
		}

	}
}