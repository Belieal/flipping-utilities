package com.flippingutilities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * This class is responsible for storing the trades on disk.
 */
@Slf4j
public class TradePersister
{

	//this is in {user's home directory}/.runelite/flipping
	public static final File PARENT_DIRECTORY = new File(RuneLite.RUNELITE_DIR, "flipping");

	//this is {user's home directory}/.runelite/flipping/trades.json
	public static final File TRADE_DATA_FILE = new File(PARENT_DIRECTORY, "trades.json");

	/**
	 * Ensures that {user's home directory}/.runelite/flipping/trades.json exists
	 *
	 * @throws IOException an exception that should be handled in flipping plugin
	 */
	public void setup() throws IOException
	{
		if (!PARENT_DIRECTORY.exists())
		{
			log.info("flipping directory doesn't exist yet so it's being created");
			if (!PARENT_DIRECTORY.mkdir())
			{
				throw new IOException("unable to create parent directory!");
			}
		}
		else
		{
			log.info("flipping directory already exists");
		}

		if (!TRADE_DATA_FILE.exists())
		{
			log.info("trades.json doesn't exist yet so it's being created");
			if (!TRADE_DATA_FILE.createNewFile())
			{
				throw new IOException("unable to create trades.json!");
			}
		}
		else
		{
			log.info("trades.json already exists");
		}
	}

	/**
	 * Loads the json as a byte array, converts it into a string, then converts it into hashmap
	 * that associates a display name to an AccountData object. If there is no previous trades
	 * stored, it just returns an empty hashmap.
	 *
	 * @return all of user's trade related data such as their trades and the last offers for each slot
	 * to helps screen out duplicate events.
	 * @throws IOException handled in flipping plugin
	 */
	public Map<String, AccountData> loadTrades() throws IOException
	{
		String tradesJson = new String(Files.readAllBytes(TRADE_DATA_FILE.toPath()));

		final Gson gson = new Gson();
		Type type = new TypeToken<Map<String, AccountData>>()
		{
		}.getType();
		Map<String, AccountData> accountData = gson.fromJson(tradesJson, type);

		return accountData == null ? new HashMap<>() : accountData;
	}

	/**
	 * Stores the user's trade related data in a file located at {user's home directory}/.runelite/flipping/trades.json
	 *
	 * @param accountDataCache all of user's accounts' trades and last offers for their slots.
	 * @throws IOException handled in flipping plugin
	 */
	public void storeTrades(Map<String, AccountData> accountDataCache) throws IOException
	{
		final Gson gson = new Gson();
		final String json = gson.toJson(accountDataCache);
		TRADE_DATA_FILE.delete();
		TRADE_DATA_FILE.createNewFile();
		Files.write(TRADE_DATA_FILE.toPath(), json.getBytes());
	}

}
