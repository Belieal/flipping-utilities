/*
 * Copyright (c) 2020, Belieal <https://github.com/Belieal>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flippingutilities.utilities;

import com.flippingutilities.db.TradePersister;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Updates the cache in real time as files are changed in the directory being monitored. It monitors the directory
 * where the accounts' data is stored and fires any registered callbacks when it detects a change for an account.
 * The reason it accepts callbacks is so that this class is not tied to any specific component's way of handling a file
 * change. This decoupling allows the cache updater to be used easily by any component that wishes to fire an action
 * when a file for an account is changed.
 */
@Slf4j
public class CacheUpdater
{

	ScheduledExecutorService executor;

	List<Consumer<String>> callbacks = new ArrayList<>();

	boolean isBeingShutdownByClient = false;

	Future realTimeUpdateTask;

	Map<String, Long> lastEvents = new HashMap<>();

	int requiredMinMsSinceLastUpdate = 5;
	int failureCount;
	int failureThreshold = 2;


	public CacheUpdater()
	{
		this.executor = Executors.newSingleThreadScheduledExecutor();
	}

	public void registerCallback(Consumer<String> callback)
	{
		callbacks.add(callback);
	}

	public void start()
	{
		realTimeUpdateTask = executor.schedule(this::updateCacheRealTime, 1000, TimeUnit.MILLISECONDS);
	}

	public void stop()
	{
		isBeingShutdownByClient = true;
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
				failureCount = 0;
			}
		}

		catch (IOException | InterruptedException e)
		{
			if (!isBeingShutdownByClient)
			{
				log.info("exception in updateCacheRealTime, Error = {}", e);
				onUnexpectedError();
			}

			else
			{
				onClientShutdown();
			}
		}

		catch (Exception e)
		{
			log.info("unknown exception in updateCacheRealTime, task is going to stop. Error = {}", e);
		}
	}

	private void onUnexpectedError()
	{
		log.info("Failure number: {} Error not caused by client shutdown", failureCount);
		failureCount++;
		if (failureCount > failureThreshold)
		{
			log.info("number of failures exceeds failure threshold, not scheduling task again");
			return;
		}

		else
		{
			log.info("failure count below threshold, scheduling task again");
			realTimeUpdateTask = executor.schedule(this::updateCacheRealTime, 1000, TimeUnit.MILLISECONDS);
		}
	}

	private void onClientShutdown()
	{
		log.info("shutting down cache updater due to the client shutdown");
	}

	private boolean isDuplicateEvent(String fileName)
	{
		long lastModified = TradePersister.lastModified(fileName);
		if (lastEvents.containsKey(fileName))
		{
			long prevModificationTime = lastEvents.get(fileName);
			long diffSinceLastModification = Math.abs(lastModified - prevModificationTime);
			if (diffSinceLastModification < requiredMinMsSinceLastUpdate)
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