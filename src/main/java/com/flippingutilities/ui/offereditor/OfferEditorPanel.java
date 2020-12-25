package com.flippingutilities.ui.offereditor;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class OfferEditorPanel extends JPanel {

    public OfferEditorPanel() {
        setLayout(new BorderLayout());
        add(new JLabel("Offer Editor"));

        JPanel optionsContainer = new JPanel();
        optionsContainer.setLayout(new BoxLayout(optionsContainer, BoxLayout.Y_AXIS));

        JPanel optionPanel1 = new JPanel();
        optionPanel1.setPreferredSize(new Dimension(240, 70));

        JComboBox keySelector =  new JComboBox(new String[]{"a","b","c"});
        keySelector.setPreferredSize(new Dimension(45,25));
        //have a decription at the top, like press key below to set to blah blah, that way i wont have as much text on the option panel itself
        JComboBox propertiesSelector = new JComboBox(new String[]{"rem limit", "ge limit"});
        propertiesSelector.setPreferredSize(new Dimension(85, 25));

        JComboBox keySelector2 =  new JComboBox(new String[]{"a","b","c"});
        keySelector2.setPreferredSize(new Dimension(45,25));

        JComboBox keySelector3 =  new JComboBox(new String[]{"a","b","c"});
        //keySelector3.setPreferredSize(new Dimension(45,25));


        optionPanel1.add(keySelector);
        optionPanel1.add(propertiesSelector);
        optionPanel1.add(keySelector2);
        optionPanel1.add(keySelector3);

        optionsContainer.add(optionPanel1);

        add(optionsContainer, BorderLayout.CENTER);


    }
}
