package com.flippingutilities.ui.slots;

import com.flippingutilities.model.OfferEvent;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SlotsPanel extends JPanel {
    private List<SlotPanel> slotPanels;
    private ItemManager itemManager;
    JLabel statusText = new JLabel();

    public SlotsPanel(ItemManager im) {
        itemManager = im;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        slotPanels = new ArrayList<>();
        JPanel slotPanelsContainer = new JPanel();
        slotPanelsContainer.setLayout(new BoxLayout(slotPanelsContainer, BoxLayout.Y_AXIS));

        for (int i = 0; i < 8; i++) {
            Component verticalGap = Box.createVerticalStrut(10);
            SlotPanel slotPanel = new SlotPanel(verticalGap);
            slotPanels.add(slotPanel);
            slotPanelsContainer.add(slotPanel);
            slotPanelsContainer.add(verticalGap);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(10,10,10,10));
        wrapper.add(slotPanelsContainer, BorderLayout.NORTH);

        JScrollPane jScrollPane = new JScrollPane(wrapper);
        jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));

        statusText.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        statusText.setBorder(new EmptyBorder(20,0,0,0));
        statusText.setFont(FontManager.getRunescapeSmallFont());
        statusText.setText("No currently active slots");
        add(statusText, BorderLayout.NORTH);
        add(jScrollPane, BorderLayout.CENTER);
    }

    public void updateTimerDisplays(int slotIndex, String time) {
        slotPanels.get(slotIndex).updateTimer(time);
    }


    public void update(OfferEvent newOfferEvent) {
        int slot = newOfferEvent.getSlot();
        SlotPanel slotPanel = slotPanels.get(slot);
        if (!slotPanel.shouldUpdate(newOfferEvent)) {
            return;
        }
        if (slotPanel.itemId == newOfferEvent.getItemId() || newOfferEvent.getItemId() == 0) {
            slotPanel.update(null, null, newOfferEvent);
        } else {
            ItemComposition itemComposition = itemManager.getItemComposition(newOfferEvent.getItemId());
            boolean shouldStack = itemComposition.isStackable() || newOfferEvent.getTotalQuantityInTrade() > 1;
            BufferedImage itemImage = itemManager.getImage(newOfferEvent.getItemId(), newOfferEvent.getTotalQuantityInTrade(), shouldStack);
            String itemName = itemComposition.getName();
            slotPanel.update(itemImage, itemName, newOfferEvent);
        }
        boolean activeSlots = slotPanels.stream().anyMatch(s -> s.itemId != 0);
        statusText.setVisible(!activeSlots);
    }
}
