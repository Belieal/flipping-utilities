package com.flippingutilities.ui.offereditor;

import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.model.Option;
import com.flippingutilities.ui.uiutilities.CustomColors;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class OptionPanel extends JPanel {
    private Option option;
    private int optionNumber;

    public OptionPanel(Option option, int optionNumber, FlippingItem item) {
        this.option = option;
        this.optionNumber = optionNumber;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(230, 60));
        setBackground(CustomColors.DARK_GRAY);
        setBorder(new EmptyBorder(5,0,5,0));

        JPanel body = new JPanel();
        body.setBackground(CustomColors.DARK_GRAY);

        JTextField keyInputField =  new JTextField(2);
        keyInputField.setText(option.getKey().orElse(""));

        JComboBox propertiesSelector = new JComboBox(new String[]{"rem limit", "ge limit"});
        propertiesSelector.setSelectedItem(option.getProperty().orElse("rem limit"));

        JTextField optionalEditor = new JTextField(5);
        optionalEditor.setText(option.getChange().orElse("+0"));

        JPanel resultingValuePanel = new JPanel();
        resultingValuePanel.setBorder(new EmptyBorder(3,0,0,0));
        resultingValuePanel.setBackground(CustomColors.DARK_GRAY);

        JLabel pressText = new JLabel("press", JLabel.CENTER);
        pressText.setFont(FontManager.getRunescapeSmallFont());

        JLabel keyLabel = new JLabel("b", JLabel.CENTER);
        keyLabel.setFont(FontManager.getRunescapeSmallFont());
        keyLabel.setForeground(CustomColors.VIBRANT_YELLOW);

        JLabel descLabel = new JLabel("to set quantity to", JLabel.CENTER);
        descLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel resultingValue = new JLabel("16", JLabel.CENTER);
        resultingValue.setFont(FontManager.getRunescapeSmallFont());
        resultingValue.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);

        resultingValuePanel.add(pressText);
        resultingValuePanel.add(keyLabel);
        resultingValuePanel.add(descLabel);
        resultingValuePanel.add(resultingValue);

        body.add(keyInputField);
        body.add(propertiesSelector);
        body.add(optionalEditor);

        JLabel optionNumberLabel = new JLabel(String.valueOf(optionNumber) + ")", JLabel.CENTER);
        optionNumberLabel.setBorder(new EmptyBorder(17,5,0,0));
        optionNumberLabel.setFont(FontManager.getRunescapeBoldFont());
        //optionNumberLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);


        add(body, BorderLayout.CENTER);
        add(resultingValuePanel, BorderLayout.SOUTH);
        add(optionNumberLabel, BorderLayout.WEST);

    }



}
