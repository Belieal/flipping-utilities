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

package com.flippingutilities.model;

import com.flippingutilities.ui.widgets.TradeActivityTimer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemStats;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class AccountData
{
	private Map<Integer, OfferEvent> lastOffers = new HashMap<>();
	private List<FlippingItem> trades = new ArrayList<>();
	private Instant sessionStartTime = Instant.now();
	private Duration accumulatedSessionTime = Duration.ZERO;
	private Instant lastSessionTimeUpdate;
	private List<TradeActivityTimer> slotTimers;

	/**
	 * Resets all session related data associated with an account. This is called when the plugin first starts
	 * as that's when a new session is "started" and when a user wants to start a new session for an account.
	 */
	public void startNewSession()
	{
		sessionStartTime = Instant.now();
		accumulatedSessionTime = Duration.ZERO;
		lastSessionTimeUpdate = null;
	}

	/**
	 * Over time as we delete/add fields, we need to make sure the fields are set properly the first time the user
	 * loads their trades after the new update. This method serves as a way to sanitize the data. It also ensures
	 * that the FlippingItems have their non persisted fields set from history.
	 */
	public void prepareForUse(ItemManager itemManager)
	{
		for (FlippingItem item : trades)
		{
			//in case ge limits have been updated
			int tradeItemId = item.getItemId();
			ItemStats itemStats = itemManager.getItemStats(tradeItemId, false);
			int geLimit = itemStats != null ? itemStats.getGeLimit() : 0;

			item.setOfferMadeBy();
			item.setTotalGELimit(geLimit);
			item.syncState();
			//when this change was made the field will not exist and will be null
			if (item.getValidFlippingPanelItem() == null)
			{
				item.setValidFlippingPanelItem(true);
			}
		}
	}
}
