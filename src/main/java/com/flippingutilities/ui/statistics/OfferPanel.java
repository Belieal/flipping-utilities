package com.flippingutilities.ui.statistics;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.model.OfferEvent;
import com.flippingutilities.ui.uiutilities.CustomColors;
import com.flippingutilities.ui.uiutilities.Icons;
import com.flippingutilities.ui.uiutilities.TimeFormatters;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class OfferPanel extends JPanel {
    private JLabel title;
    private String offerDescription;
    private OfferEvent offer;

    public OfferPanel(OfferEvent offer, FlippingItem item, FlippingPlugin plugin) {
        setLayout(new BorderLayout());
        this.offer = offer;

        this.offerDescription = getOfferDescription();
        this.title = new JLabel(QuantityFormatter.formatNumber(offer.getCurrentQuantityInTrade()) + " " + offerDescription
                + " " + "(" + TimeFormatters.formatDurationTruncated(offer.getTime()) + " ago)", SwingConstants.CENTER);

        title.setOpaque(true);
        title.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        title.setFont(FontManager.getRunescapeSmallFont());
        title.setForeground(offer.isBuy() ? CustomColors.OUTDATED_COLOR : ColorScheme.GRAND_EXCHANGE_PRICE);

        JPanel body = new JPanel(new DynamicGridLayout(3, 0, 0, 2));
        body.setBorder(new EmptyBorder(0, 2, 1, 2));

        JLabel priceLabel = new JLabel("Price:");
        JLabel priceVal = new JLabel(QuantityFormatter.formatNumber(offer.getPrice()) + " gp", SwingConstants.RIGHT);

        JLabel totalPriceLabel = new JLabel("Total:");
        JLabel totalPriceVal = new JLabel(QuantityFormatter.formatNumber(offer.getPrice() * offer.getCurrentQuantityInTrade()) + " gp", SwingConstants.RIGHT);

        JLabel[] descriptions = {priceLabel, totalPriceLabel};
        JLabel[] vals = {priceVal, totalPriceVal};

        for (int i = 0; i < descriptions.length; i++) {
            JLabel descriptionLabel = descriptions[i];
            JLabel valLabel = vals[i];

            descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
            descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            valLabel.setFont(FontManager.getRunescapeSmallFont());
            valLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.add(descriptionLabel, BorderLayout.WEST);
            infoPanel.add(valLabel, BorderLayout.EAST);
            body.add(infoPanel);
        }

        JPanel deleteIconPanel = new JPanel(new BorderLayout());
        JLabel trashIcon = new JLabel(Icons.TRASH_CAN_OFF);
        trashIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (plugin.getAccountCurrentlyViewed().equals(FlippingPlugin.ACCOUNT_WIDE)) {
                    JOptionPane.showMessageDialog(null, "You cannot delete offers in the Accountwide view");
                    return;
                }
                //Display warning message
                final int result = JOptionPane.showOptionDialog(new JLabel(Icons.TRASH_CAN_ON), "Are you sure you want to delete this offer?",
                        "Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                        null, new String[]{"Yes", "No"}, "No");

                //If the user pressed "Yes"
                if (result == JOptionPane.YES_OPTION) {
                    item.invalidateOffers(Collections.singletonList(offer));
                    plugin.getStatPanel().rebuild(plugin.getTradesForCurrentView());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                trashIcon.setIcon(Icons.TRASH_CAN_ON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                trashIcon.setIcon(Icons.TRASH_CAN_OFF);
            }
        });

        deleteIconPanel.add(trashIcon, BorderLayout.CENTER);
        body.add(deleteIconPanel);

        add(title, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }


    public void updateTimeDisplay() {
        title.setText(QuantityFormatter.formatNumber(offer.getCurrentQuantityInTrade()) + " " + offerDescription
                + " " + "(" + TimeFormatters.formatDurationTruncated(offer.getTime()) + " ago)");
    }

    private String getOfferDescription() {
        if (offer.isBuy() && offer.isMarginCheck()) {
            return "Insta Bought";
        } else if (offer.isBuy() && !offer.isMarginCheck()) {
            return "Bought";
        } else if (!offer.isBuy() && offer.isMarginCheck()) {
            return "Insta Sold";
        } else if (!offer.isBuy() && !offer.isMarginCheck()) {
            return "Sold";
        } else {
            return "";
        }
    }
}
