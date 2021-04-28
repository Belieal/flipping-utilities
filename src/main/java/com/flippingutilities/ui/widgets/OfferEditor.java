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

import com.flippingutilities.ui.uiutilities.Icons;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.VarClientStr;
import net.runelite.api.widgets.*;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;

public class OfferEditor {
    private final Client client;
    private Widget bottomText;
    private Widget nonWikiText;
    private Widget wikiText;

    public OfferEditor(Widget parent, Client client) {
        this.client = client;

        if (parent == null) {
            return;
        }

        bottomText = parent.createChild(-1, WidgetType.TEXT);
        nonWikiText = parent.createChild(-1, WidgetType.TEXT);
        wikiText = parent.createChild(-1, WidgetType.TEXT);

        prepareTextWidget(nonWikiText, WidgetTextAlignment.LEFT, WidgetPositionMode.ABSOLUTE_TOP, 5, 10);
        prepareTextWidget(wikiText, WidgetTextAlignment.LEFT, WidgetPositionMode.ABSOLUTE_TOP, 20, 10);
        prepareTextWidget(bottomText, WidgetTextAlignment.CENTER, WidgetPositionMode.ABSOLUTE_BOTTOM, 5, 0);
    }


    private void prepareTextWidget(Widget widget, int xAlignment, int yMode, int yOffset, int xOffset) {
        widget.setTextColor(0x800000);
        widget.setFontId(FontID.VERDANA_11_BOLD);
        widget.setYPositionMode(yMode);
        widget.setOriginalX(xOffset);
        widget.setOriginalY(yOffset);
        widget.setOriginalHeight(20);
        widget.setXTextAlignment(xAlignment);
        widget.setWidthMode(WidgetSizeMode.MINUS);
        widget.setHasListener(true);
        widget.setOnMouseRepeatListener((JavaScriptCallback) ev -> widget.setTextColor(0xFFFFFF));
        widget.setOnMouseLeaveListener((JavaScriptCallback) ev -> widget.setTextColor(0x800000));
        widget.revalidate();
    }

    public void showQuantityWidgets(int quantity) {
        bottomText.setText("OR click this to use the quantity editor hotkeys!");
        bottomText.setAction(1, "pic");
        bottomText.setOnOpListener((JavaScriptCallback) ev -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, Icons.QUANTITY_EDITOR_PIC);
            });
        });

        nonWikiText.setText("set to remaining GE limit: " + quantity);
        nonWikiText.setAction(1, "Set quantity");
        nonWikiText.setOnOpListener((JavaScriptCallback) ev ->
        {
            client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(quantity + "*");
            client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(quantity));
        });
    }

    public void showInstaSellPrices(int instaSellPrice, int wikiInstaSellPrice) {
        bottomText.setText("OR click this to use the price editor hotkeys!");
        bottomText.setAction(1, "pic");
        bottomText.setOnOpListener((JavaScriptCallback) ev -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, Icons.PRICE_EDITOR_PIC);
            });
        });

        if (instaSellPrice != 0) {
            nonWikiText.setText("set to insta sell: " + String.format("%,d", instaSellPrice) + " gp");
            nonWikiText.setAction(0, "Set price");
            nonWikiText.setOnOpListener((JavaScriptCallback) ev ->
            {
                client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(instaSellPrice + "*");
                client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(instaSellPrice));
            });
        } else {
            nonWikiText.setText("no sell tracked");
        }

        if (wikiInstaSellPrice != 0) {
            wikiText.setText("set to wiki insta sell: " + String.format("%,d", wikiInstaSellPrice) + " gp");
            wikiText.setAction(1, "Set wiki price");
            wikiText.setOnOpListener((JavaScriptCallback) ev ->
            {
                client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(wikiInstaSellPrice + "*");
                client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(wikiInstaSellPrice));
            });
        } else {
            wikiText.setText("No wiki data");
        }
    }

    public void showInstaBuyPrices(int instaBuyPrice, int wikiInstaBuyPrice) {
        bottomText.setText("OR click this to use the price editor hotkeys!");
        bottomText.setAction(1, "pic");
        bottomText.setOnOpListener((JavaScriptCallback) ev -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, Icons.PRICE_EDITOR_PIC);
            });
        });

        if (instaBuyPrice != 0) {
            nonWikiText.setText("set to insta buy: " + String.format("%,d", instaBuyPrice) + " gp");
            nonWikiText.setAction(0, "Set price");
            nonWikiText.setOnOpListener((JavaScriptCallback) ev ->
            {
                client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(instaBuyPrice + "*");
                client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(instaBuyPrice));
            });
        } else {
            nonWikiText.setText("no buy tracked");
        }

        if (wikiInstaBuyPrice != 0) {
            wikiText.setText("set to wiki insta buy: " + String.format("%,d", wikiInstaBuyPrice) + " gp");
            wikiText.setAction(1, "Set wiki price");
            wikiText.setOnOpListener((JavaScriptCallback) ev ->
            {
                client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(wikiInstaBuyPrice + "*");
                client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(wikiInstaBuyPrice));
            });
        } else {
            wikiText.setText("No wiki data");
        }
    }

}