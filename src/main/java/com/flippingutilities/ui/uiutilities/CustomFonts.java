package com.flippingutilities.ui.uiutilities;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;

import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.IOException;

@Slf4j
public class CustomFonts {
    public static Font RUNESCAPE_BOLD_FONT;

    static
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        try
        {
            Font font = Font.createFont(Font.TRUETYPE_FONT,
                    FontManager.class.getResourceAsStream("runescape.ttf"))
                    .deriveFont(Font.PLAIN, 16);
            ge.registerFont(font);

            Font boldFont = Font.createFont(Font.TRUETYPE_FONT,
                    FontManager.class.getResourceAsStream("runescape_bold.ttf"))
                    .deriveFont(Font.BOLD, 16);
            ge.registerFont(boldFont);

            RUNESCAPE_BOLD_FONT = StyleContext.getDefaultStyleContext()
                    .getFont(boldFont.getName(), Font.BOLD, 13);
            ge.registerFont(RUNESCAPE_BOLD_FONT);
        }
        catch (FontFormatException ex)
        {
            log.info("couldn't load font due to {}", ex);
            RUNESCAPE_BOLD_FONT = FontManager.getRunescapeBoldFont();
        }
        catch (IOException ex)
        {
            log.info("font file not found");
            RUNESCAPE_BOLD_FONT = FontManager.getRunescapeBoldFont();
        }
    }
}
