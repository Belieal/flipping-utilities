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

import javax.swing.*;

public class OfferEditor
{
    private final Client client;
    private Widget text;
    private Widget buttonText;
    private Widget picText;

    public OfferEditor(Widget parent, Client client)
    {
        this.client = client;

        if (parent == null)
        {
            return;
        }

        text = parent.createChild(-1, WidgetType.TEXT);
        buttonText = parent.createChild(-1, WidgetType.TEXT);
        picText = parent.createChild(-1, WidgetType.TEXT);

        prepareTextWidget(buttonText, WidgetPositionMode.ABSOLUTE_TOP, 5);
        prepareTextWidget(text, WidgetPositionMode.ABSOLUTE_BOTTOM, 18);
        prepareTextWidget(picText, WidgetPositionMode.ABSOLUTE_BOTTOM, 2);

        buttonText.setFontId(FontID.QUILL_8);
    }


    private void prepareTextWidget(Widget widget, int yMode, int originalY) {
        widget.setTextColor(0x800000);
        widget.setFontId(FontID.VERDANA_11_BOLD);
        widget.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
        widget.setOriginalX(0);
        widget.setYPositionMode(yMode);
        widget.setOriginalY(originalY);
        widget.setOriginalHeight(20);
        widget.setXTextAlignment(WidgetTextAlignment.CENTER);
        widget.setYTextAlignment(WidgetTextAlignment.CENTER);
        widget.setWidthMode(WidgetSizeMode.MINUS);
        widget.setHasListener(true);
        widget.setOnMouseRepeatListener((JavaScriptCallback) ev -> widget.setTextColor(0xFFFFFF));
        widget.setOnMouseLeaveListener((JavaScriptCallback) ev -> widget.setTextColor(0x800000));
        widget.revalidate();
    }

    public void update(String mode, int value)
    {
        switch (mode)
        {
            case ("quantity"):
                text.setText("OR use the quantity editor to set custom quantities quickly with just a key press!");
                text.setHasListener(false);

                picText.setText("Click this text to see where the quantity editor is!");
                picText.setAction(1, "pic");
                picText.setOnOpListener((JavaScriptCallback) ev -> {
                    SwingUtilities.invokeLater(()-> {
                        JOptionPane.showMessageDialog(null, Icons.QUANTITY_EDITOR_PIC);
                    });
                });

                buttonText.setText("click this to set to remaining GE limit: " + value);
                buttonText.setAction(1, "Set quantity");
                buttonText.setOnOpListener((JavaScriptCallback) ev ->
                {
                    client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(value + "*");
                    client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(value));
                });
                break;
            case ("setSellPrice"):
                text.setText("OR use the price editor to set custom prices quickly with just a key press!");
                text.setHasListener(false);

                picText.setText("Click this text to see where the price editor is!");
                picText.setAction(1, "pic");
                picText.setOnOpListener((JavaScriptCallback) ev -> {
                    SwingUtilities.invokeLater(()-> {
                        JOptionPane.showMessageDialog(null, Icons.PRICE_EDITOR_PIC);
                    });
                });

                if (value != 0) {
                    buttonText.setText("click this to set to latest margin sell price: " + String.format("%,d", value) + " gp");
                    buttonText.setAction(1, "Set price");
                    buttonText.setOnOpListener((JavaScriptCallback) ev ->
                    {
                        client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(value + "*");
                        client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(value));
                    });
                }

                break;
            case ("setBuyPrice"):
                text.setText("OR use the price editor to set custom prices quickly with just a key press!");
                text.setHasListener(false);

                picText.setText("Click this text to see where the price editor is!");
                picText.setAction(1, "pic");
                picText.setOnOpListener((JavaScriptCallback) ev -> {
                    SwingUtilities.invokeLater(()-> {
                        JOptionPane.showMessageDialog(null, Icons.PRICE_EDITOR_PIC);
                    });
                });
                if (value != 0) {
                    buttonText.setText("click this to set to latest margin buy price: " + String.format("%,d", value) + " gp");
                    buttonText.setAction(1, "Set price");
                    buttonText.setOnOpListener((JavaScriptCallback) ev ->
                    {
                        client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT).setText(value + "*");
                        client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(value));
                    });
                }

                break;
            case ("reset"):
                text.setText("");
        }
    }
}