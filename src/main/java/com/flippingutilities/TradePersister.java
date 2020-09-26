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

package com.flippingutilities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * This class is responsible for handling all the IO related tasks for persisting trades. This class should contain
 * any logic that pertains to reading/writing to disk. This includes logic related to whether it should reload things
 * again, etc.
 */
@Slf4j
public class TradePersister
{

	//this is in {user's home directory}/.runelite/flipping
	public static final File PARENT_DIRECTORY = new File(RuneLite.RUNELITE_DIR, "flipping");

	public static final File OLD_FILE = new File(PARENT_DIRECTORY, "trades.json");

	/**
	 * Creates flipping directory if it doesn't exist and partitions trades.json into individual files
	 * for each account, if it exists.
	 *
	 * @throws IOException handled in FlippingPlugin
	 */
	public static void setup() throws IOException
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
			log.info("flipping directory already exists so it's not being created");
			if (OLD_FILE.exists())
			{
				log.info("trades.json exists and is being partitioned into separate files to match the new way of storing" +
					"trades");
				partitionOldFile(OLD_FILE);
				OLD_FILE.delete();

			}
		}

	}

	/**
	 * Reads the data from trades.json and creates separate files for each account to conform with the
	 * new way of saving data. It also sets the "madeBy" field on every OfferEvent object in the trade lists
	 * as the old trades (in trades.json) would not have that field.
	 *
	 * @param f the old trades.json file.
	 */
	private static void partitionOldFile(File f) throws IOException
	{
		String tradesJson = new String(Files.readAllBytes(OLD_FILE.toPath()));

		final Gson gson = new Gson();
		Type type = new TypeToken<Map<String, AccountData>>()
		{
		}.getType();
		Map<String, AccountData> accountData = gson.fromJson(tradesJson, type);

		//they have no data to partition
		if (!accountData.containsKey(FlippingPlugin.ACCOUNT_WIDE))
		{
			return;
		}

		//the account wide list might have different entries than the account specific lists. We want to use the
		//list which has the most data. For example, it might be the case that a user has cleared one of their
		//account's trade list but they haven't cleared the account wide list, which still has all the data.
		Map<String, List<FlippingItem>> accountWideData = accountData.get(FlippingPlugin.ACCOUNT_WIDE).
			getTrades().
			stream().
			collect(Collectors.groupingBy(FlippingItem::getFlippedBy));

		for (String displayName : accountData.keySet())
		{
			if (displayName.equals(FlippingPlugin.ACCOUNT_WIDE))
			{
				continue;
			}

			AccountData accountSpecificData = accountData.get(displayName);

			if (accountWideData.containsKey(displayName))
			{
				//if the account wide list has more items for that display name
				if (accountWideData.get(displayName).size() > accountSpecificData.getTrades().size())
				{
					accountSpecificData.setTrades(accountWideData.get(displayName));
				}
			}

			//sets the madeBy field on each offer as its required in the process for constructing the account wide tradelist.
			//Every new offer that comes in (After this update) already gets it set, but the old offers won't have it and
			//I don't want to have to delete all the user's data, so i am just making it conform to the new format.
			accountSpecificData.getTrades().forEach(item -> item.getHistory().getCompressedOfferEvents().forEach(offer ->
				offer.setMadeBy(item.getFlippedBy())));

			try
			{
				storeTrades(displayName, accountSpecificData);
			}
			catch (IOException e)
			{
				log.info("error while partitioning trades.json into files for each account. error = {}, display name = {}.",
					e, displayName);
			}
		}
	}

	/**
	 * loads each account's data from the parent directory located at {user's home directory}/.runelite/flipping/
	 * Each account's data is stored in separate file in that directory and is named {displayName}.json
	 *
	 * @return a map of display name to that account's data
	 * @throws IOException handled in FlippingPlugin
	 */
	public static Map<String, AccountData> loadAllTrades() throws IOException
	{
		Map<String, AccountData> accountsData = new HashMap<>();
		for (File f : PARENT_DIRECTORY.listFiles())
		{
			String displayName = f.getName().split("\\.")[0];
			log.info("loading data for {}", displayName);
			AccountData accountData = loadFromFile(f);
			if (accountData == null)
			{
				log.info("data for {} is null for some reason, setting it to a empty AccountData object", displayName);
				accountData = new AccountData();
			}

			accountsData.put(displayName, accountData);
		}

		return accountsData;
	}

	public static AccountData loadTrades(String displayName) throws IOException
	{
		log.info("loading data for {}", displayName);
		File accountFile = new File(PARENT_DIRECTORY, displayName + ".json");
		AccountData accountData = loadFromFile(accountFile);
		if (accountData == null)
		{
			log.info("data for {} is null for some reason, setting it to a empty AccountData object", displayName);
			accountData = new AccountData();
		}
		return accountData;
	}

	private static AccountData loadFromFile(File f) throws IOException
	{
		String accountDataJson = new String(Files.readAllBytes(f.toPath()));
		final Gson gson = new Gson();
		Type type = new TypeToken<AccountData>()
		{
		}.getType();
		AccountData accountData = gson.fromJson(accountDataJson, type);
		cleanAccountData(accountData);
		return accountData;
	}

	/**
	 * stores trades for an account in {user's home directory}/.runelite/flipping/{account's display name}.json
	 *
	 * @param displayName display name of the account the data is associated with
	 * @param data        the trades and last offers of that account
	 * @throws IOException
	 */
	public static void storeTrades(String displayName, AccountData data) throws IOException
	{
		log.info("storing trades for {}", displayName);
		File accountFile = new File(PARENT_DIRECTORY, displayName + ".json");
		final Gson gson = new Gson();
		final String json = gson.toJson(data);
		Files.write(accountFile.toPath(), json.getBytes());
	}

	public static long lastModified(String fileName)
	{
		return new File(PARENT_DIRECTORY, fileName).lastModified();
	}

	public static void deleteFile(String fileName)
	{
		File accountFile = new File(PARENT_DIRECTORY, fileName);
		if (accountFile.exists())
		{
			if (accountFile.delete())
			{
				log.info("{} deleted", fileName);
			}
			else
			{
				log.info("unable to delete {}", fileName);
			}
		}
	}

	//might need to add some stuff to this now that i removed valid flipping offer and added isvalidflippingpanelitem
	private static void cleanAccountData(AccountData accountData) {
		//a bug led to items not having their last active times be updated. This bug is fixed
		//but the null value remains in the user's items, so this sets it.
		for (FlippingItem item: accountData.getTrades()) {
			if (item.getLatestActivityTime() == null) {
				item.setLatestActivityTime(Instant.now());
			}

			for (OfferEvent offerEvent: item.getHistory().getCompressedOfferEvents()) {
				if (offerEvent.getMadeBy() == null) {
					offerEvent.setMadeBy(item.getFlippedBy());
				}
			}
		}
	}
}
