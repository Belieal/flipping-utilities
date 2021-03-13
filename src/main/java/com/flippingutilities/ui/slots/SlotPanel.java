package com.flippingutilities.ui.slots;

import com.flippingutilities.model.OfferEvent;
import com.flippingutilities.ui.uiutilities.CustomColors;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ThinProgressBar;
import net.runelite.client.util.QuantityFormatter;


import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SlotPanel extends JPanel {
    public int itemId;
    private OfferEvent offerEvent;
    private ThinProgressBar progressBar = new ThinProgressBar();
    private JLabel itemName = new JLabel();
    private JLabel itemIcon = new JLabel();
    private JLabel price = new JLabel();
    private JLabel state = new JLabel();
    private JLabel action = new JLabel();
    private JLabel timer = new JLabel();
    private Component verticalGap;


    public SlotPanel(Component verticalGap) {
        this.verticalGap = verticalGap;
        setVisible(false);
        setLayout(new BorderLayout());
        setBackground(CustomColors.DARK_GRAY);
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 5, 0, ColorScheme.DARKER_GRAY_COLOR.darker()),
                new EmptyBorder(10, 10, 0, 10)
        ));
        price.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        price.setFont(FontManager.getRunescapeSmallFont());
        state.setFont(FontManager.getRunescapeSmallFont());
        action.setFont(FontManager.getRunescapeBoldFont());
        action.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
        timer.setFont(FontManager.getRunescapeSmallFont());

        progressBar.setForeground(CustomColors.DARK_GRAY);
        progressBar.setMaximumValue(100);
        progressBar.setValue(0);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        progressBar.setMinimumSize(new Dimension(0, 10));
        progressBar.setPreferredSize(new Dimension(0, 10));
        progressBar.setSize(new Dimension(0, 10));
        add(itemIcon, BorderLayout.WEST);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(0,0,10,0));
        topPanel.setBackground(CustomColors.DARK_GRAY);
        topPanel.add(action, BorderLayout.WEST);
        topPanel.add(timer, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);


        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(CustomColors.DARK_GRAY);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(itemName);
        centerPanel.add(Box.createVerticalStrut(3));
        centerPanel.add(state);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(new EmptyBorder(10, 20,5,20));
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBackground(CustomColors.DARK_GRAY);
        bottomPanel.add(progressBar);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(price);
        add(bottomPanel, BorderLayout.SOUTH);

    }

    public void updateTimer(String timeString) {
        if (offerEvent == null || offerEvent.isCausedByEmptySlot() || timeString == null) {
            return;
        }
        timer.setText(timeString);
    }

    public boolean shouldUpdate(OfferEvent newOfferEvent) {
        return offerEvent == null || !offerEvent.isDuplicate(newOfferEvent);
    }

    public void update(BufferedImage itemImage, String name, OfferEvent newOfferEvent) {
        if (newOfferEvent.isCausedByEmptySlot()) {
            setVisible(false);
            verticalGap.setVisible(false);
            itemId = 0;
            itemIcon.setIcon(null);
            itemName.setText("");
            progressBar.setMaximumValue(0);
            progressBar.setValue(100);
            progressBar.setForeground(CustomColors.DARK_GRAY);
            progressBar.setBackground(CustomColors.DARK_GRAY);
            offerEvent = null;
        } else {
            setVisible(true);
            verticalGap.setVisible(true);
            offerEvent = newOfferEvent;
            itemId = newOfferEvent.getItemId();
            if (name != null) {
                itemName.setText(name);
            }
            if (itemImage != null) {
                itemIcon.setIcon(new ImageIcon(itemImage));
            }
            String stateText = QuantityFormatter.quantityToRSDecimalStack(newOfferEvent.getCurrentQuantityInTrade()) + " / "
                    + QuantityFormatter.quantityToRSDecimalStack(newOfferEvent.getTotalQuantityInTrade());

            action.setText(newOfferEvent.isBuy()? "Buy":"Sell");
            state.setText(stateText);
            price.setText(QuantityFormatter.formatNumber(newOfferEvent.getListedPrice()) + " coins");
            progressBar.setMaximumValue(newOfferEvent.getTotalQuantityInTrade());
            progressBar.setValue(newOfferEvent.getCurrentQuantityInTrade());
            if (newOfferEvent.isCancelled()) {
                progressBar.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
            }
            else if (newOfferEvent.isComplete()) {
                progressBar.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
            }
            else {
                progressBar.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
            }
        }
    }


}
