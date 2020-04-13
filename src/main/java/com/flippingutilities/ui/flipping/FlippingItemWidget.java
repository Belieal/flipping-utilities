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

package com.flippingutilities.ui.flipping;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.VarClientStr;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;

@Slf4j
public class FlippingItemWidget
{
	private final Widget parent;
	private final Client client;
	private int value;
	private Widget text;

	@Inject
	public FlippingItemWidget(Widget parent, Client client)
	{
		this.parent = parent;
		this.client = client;
	}

	private void init()
	{
		if (parent == null)
		{
			return;
		}

		text = parent.createChild(-1, WidgetType.TEXT);

		text.setTextColor(0x800000);
		text.setFontId(FontID.QUILL_8);
		text.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		text.setOriginalX(0);
		text.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		text.setOriginalY(5);
		text.setOriginalHeight(20);
		text.setXTextAlignment(WidgetTextAlignment.CENTER);
		text.setYTextAlignment(WidgetTextAlignment.CENTER);
		text.setWidthMode(WidgetSizeMode.MINUS);
		text.revalidate();
		text.setHasListener(true);
		text.setOnMouseRepeatListener((JavaScriptCallback) ev -> text.setTextColor(0xFFFFFF));
		text.setOnMouseLeaveListener((JavaScriptCallback) ev -> text.setTextColor(0x800000));
	}

	public void showWidget(String mode, int value)
	{
		init();

		switch (mode)
		{
			case ("setQuantity"):
				text.setText("Set to remaining GE limit: " + value);
				text.setAction(1, "Set quantity");
				text.setOnOpListener((JavaScriptCallback) ev ->
				{
					client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(value + "*");
					client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(value));
				});
				break;
			case ("setSellPrice"):
				text.setText("Set to latest sell price: " + String.format("%,d", value) + " gp");
				text.setAction(1, "Set price");
				text.setOnOpListener((JavaScriptCallback) ev ->
				{
					client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(value + "*");
					client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(value));
				});
				break;
			case ("setBuyPrice"):
				text.setText("Set to latest buy price: " + String.format("%,d", value) + " gp");
				text.setAction(1, "Set price");
				text.setOnOpListener((JavaScriptCallback) ev ->
				{
					client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(value + "*");
					client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(value));
				});
				break;
			case ("reset"):
				text.setText("");
		}
	}
}
