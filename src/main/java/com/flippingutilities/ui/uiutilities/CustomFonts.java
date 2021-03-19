package com.flippingutilities.ui.uiutilities;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;

import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.IOException;

@Slf4j
public class CustomFonts {
    public static Font SMALLER_RS_BOLD_FONT = FontManager.getRunescapeBoldFont().deriveFont(14.0F);
}
