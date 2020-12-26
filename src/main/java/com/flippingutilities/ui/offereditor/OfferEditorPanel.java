package com.flippingutilities.ui.offereditor;

import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.model.Option;
import com.flippingutilities.ui.uiutilities.CustomColors;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.grandexchange.GrandExchangeInputListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class OfferEditorPanel extends JPanel {

    public OfferEditorPanel() {
        setLayout(new BorderLayout());
        add(new JLabel("Offer Editor"));
        setBackground(CustomColors.DARK_GRAY);

        JPanel optionsContainer = new JPanel();
        optionsContainer.setLayout(new BoxLayout(optionsContainer, BoxLayout.Y_AXIS));
        optionsContainer.setBackground(CustomColors.DARK_GRAY);

        FlippingItem dummyItem = new FlippingItem(0,"",0,"");
        optionsContainer.add(new OptionPanel(Option.emptyOption(),1,dummyItem));

//        JPanel header = new JPanel();
//        header.setBackground(CustomColors.DARK_GRAY);
//        JLabel headerText = new JLabel("Press       to set to        with change");
//        headerText.setFont(FontManager.getRunescapeSmallFont());
//        header.add(headerText);
//
//        add(header, BorderLayout.NORTH);
        add(optionsContainer, BorderLayout.CENTER);
    }
}
