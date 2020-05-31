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

package com.flippingutilities.ui.widgets;

import com.flippingutilities.FlippingItem;
import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.OfferInfo;
import com.flippingutilities.ui.UIUtilities;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ColorUtil;

public class TradeActivityTimer
{
	private static final String SPACER = "          ";
	private static final int FONT_ID = 495;

	@Setter
	private Widget slotWidget;
	private ClientThread clientThread;
	private FlippingPlugin plugin;
	private ScheduledExecutorService executor;

	private Widget slotStateWidget;
	private Widget itemIconWidget;

	private String slotStateString;

	private FlippingItem flippingItem;

	public TradeActivityTimer(Widget slotWidget, ClientThread clientThread, FlippingPlugin plugin, ScheduledExecutorService executor)
	{
		this.slotWidget = slotWidget;
		this.clientThread = clientThread;
		this.plugin = plugin;
		this.executor = executor;

		slotStateString = slotWidget.getChild(16).getText();

		executor.scheduleAtFixedRate(() -> clientThread.invokeLater(() -> updateTimer(false)), 10, 1000, TimeUnit.MILLISECONDS);
	}

	public void updateTimer(boolean isReloading)
	{
		if (slotWidget == null || slotWidget.isHidden())
		{
			return;
		}

		slotStateWidget = slotWidget.getChild(16);
		itemIconWidget = slotWidget.getChild(18);

		if (isReloading)
		{
			System.out.println(slotStateWidget.getText());
			slotStateString = slotStateWidget.getText();
		}

		if (slotStateString.equals("Empty"))
		{
			resetToEmpty();
			return;
		}

		List<FlippingItem> tradeList = new ArrayList<>(plugin.getTradesForCurrentView());

		if (flippingItem == null)
		{
			findItemFromWidget(tradeList);
		}

		ArrayList<OfferInfo> itemHistory = flippingItem.getSaleList(flippingItem.getIntervalHistory(Instant.EPOCH), slotStateString.equals("Buy"));

		if (!itemHistory.isEmpty())
		{
			Collections.reverse(itemHistory); //As newest offers are last in the list
			Instant lastRecordedTime = itemHistory.get(0).getTime();

			clientThread.invoke(() -> setText(UIUtilities.formatDuration(lastRecordedTime)));
			slotStateWidget.setFontId(FONT_ID);
			slotStateWidget.setXTextAlignment(0);
		}
	}

	private void setText(String timeString)
	{
		slotStateWidget.setText("  <html>" + slotStateString + SPACER + ColorUtil.wrapWithColorTag(timeString, Color.WHITE) + "</html>");
	}

	public void findItemFromWidget(List<FlippingItem> tradeList)
	{
		int widgetItemId = itemIconWidget.getItemId();

		for (FlippingItem item : tradeList)
		{
			if (item.getItemId() == widgetItemId)
			{
				flippingItem = item;
				break;
			}
		}
	}

	private void resetToEmpty()
	{
		slotStateWidget.setText("Empty");
		slotStateWidget.setFontId(496);
		slotStateWidget.setXTextAlignment(1);
	}
}
