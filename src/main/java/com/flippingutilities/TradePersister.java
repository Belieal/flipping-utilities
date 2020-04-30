package com.flippingutilities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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
	 * Loads the json as a byte array, converts it into a string, then converts it into flipping
	 * items. If there is no previous trades stores, it just returns an empty arraylist.
	 *
	 * @return the user's previous trades
	 * @throws IOException to be handled in flipping plugin
	 */
	public List<FlippingItem> loadTrades() throws IOException
	{
		String tradesJson = new String(Files.readAllBytes(TRADE_DATA_FILE.toPath()));

		final Gson gson = new Gson();
		Type type = new TypeToken<ArrayList<FlippingItem>>()
		{
		}.getType();
		List<FlippingItem> trades = gson.fromJson(tradesJson, type);

		return trades == null ? new ArrayList<>() : trades;
	}

	/**
	 * Stores the user's trades in a file located at {user's home directory}/.runelite/flipping/trades.json
	 *
	 * @param trades the user's trades
	 * @throws IOException to be handled in flipping plugin
	 */
	public void storeTrades(List<FlippingItem> trades) throws IOException
	{
		final Gson gson = new Gson();
		final String json = gson.toJson(trades);
		TRADE_DATA_FILE.delete();
		TRADE_DATA_FILE.createNewFile();
		Files.write(TRADE_DATA_FILE.toPath(), json.getBytes());
	}

}
