package com.flippingutilities.ui.offereditor;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.Option;
import com.flippingutilities.ui.uiutilities.CustomColors;
import com.flippingutilities.ui.uiutilities.FastTabGroup;
import com.flippingutilities.ui.uiutilities.Icons;
import net.runelite.client.ui.components.materialtabs.MaterialTab;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OfferEditorContainerPanel extends JPanel {
    public static final String QUANTITY_EDITOR = "quantity";
    public static final String PRICE_EDITOR = "price";
    AbstractOfferEditorPanel quantityEditorPanel;
    AbstractOfferEditorPanel priceEditorPanel;
    FastTabGroup tabGroup;
    MaterialTab quantityEditorTab;
    MaterialTab priceEditorTab;

    public OfferEditorContainerPanel(FlippingPlugin plugin) {
        setLayout(new BorderLayout());
        JPanel mainDisplay = new JPanel();

        quantityEditorPanel = new QuantityEditorPanel(plugin);
        priceEditorPanel = new PriceEditorPanel(plugin);

        add(createTitlePanel(mainDisplay), BorderLayout.NORTH);
        add(mainDisplay, BorderLayout.CENTER);
    }

    private JPanel createTitlePanel(JPanel mainDisplay) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(5,0,5,0));
        titlePanel.setBackground(CustomColors.DARK_GRAY);

        tabGroup = new FastTabGroup(mainDisplay);
        quantityEditorTab = new MaterialTab(QUANTITY_EDITOR, tabGroup, quantityEditorPanel);
        priceEditorTab = new MaterialTab(PRICE_EDITOR, tabGroup, priceEditorPanel);
        tabGroup.addTab(quantityEditorTab);
        tabGroup.addTab(priceEditorTab);

        JLabel plusIconLabel = new JLabel(Icons.PLUS_ICON);
        plusIconLabel.setBorder(new EmptyBorder(0,0,0,8));
        plusIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                addOptionPanel();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                plusIconLabel.setIcon(Icons.PLUS_ICON_OFF);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                plusIconLabel.setIcon(Icons.PLUS_ICON);
            }
        });

        JLabel helpIconLabel = new JLabel(Icons.HELP);
        helpIconLabel.setBorder(new EmptyBorder(0,8,0,0));
        helpIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(helpIconLabel, createHelpText());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                helpIconLabel.setIcon(Icons.HELP_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                helpIconLabel.setIcon(Icons.HELP);

            }
        });

        titlePanel.add(tabGroup, BorderLayout.CENTER);
        titlePanel.add(helpIconLabel, BorderLayout.WEST);
        titlePanel.add(plusIconLabel, BorderLayout.EAST);
        return titlePanel;
    }

    private void addOptionPanel() {
        AbstractOfferEditorPanel editorPanel = (AbstractOfferEditorPanel) tabGroup.getCurrentlySelectedTab();
        editorPanel.addOptionPanel();
        revalidate();
        repaint();
    }

    private String createHelpText() {
        return "<html><body width='500'>"
                + "<h1> What is the quantity editor?</h1>"
                + "The quantity editor helps you input quantities much faster when buying items. It does so by allowing you to map a key to a certain value. This mapping is referred to as an Option."
                + "<h1> Options </h1>"
                + "As stated above, options are the term for this key to value mapping.<br><br>"
                + "Options have three inputs: The key you press to trigger the option, the property the option's value is based on, and an optional" +
                " modifier that will change the value however you want."
                + "<h2> Properties</h2>"
                + "Properties refer to the text in the dropdown box of an option<br>"
                + "<ul> <li> ge limit: selecting this will make the option's value dependent on the ge limit of the item</li>"
                + "<li> rem limit: selecting this will make the option's value dependent on the remaining ge limit of the item </li>"
                + "<li> cashstack: selecting this will make the option's value the max amount of items you can buy with the cash in your inventory </li></ul>"
                + "<h2> Modifers </h2>"
                + "Modifers are the text in the third box (last box) of an option."
                + " Modifiers are any of these symbols +,-,* followed by a positive whole number. For example, all of these are valid: +0, +10, *2, -15, etc<br><br>"
                + "Once you input a modifier, press enter to save it"
                + "<h1> Getting Started! </h1>"
                + "Press the plus button to create an option<br><br>"
                + "Input a key into the first box and press enter to save the key<br><br>"
                + "Change the default property in the dropdown box if you want to<br><br>"
                + "Add an additional modifier if you are not satisfied with the option's value<br><br>"
                + "Now, instead of typing a quantity for an item when you are buying it, just press the key associated with the option!<br><br>"
                + "You can create as many options as you want and you can also edit existing options by changing their keys, properties, or modifiers";
    }

    public boolean currentlyViewingQuantityEditor() {
        return tabGroup.getLastSelectedTab().equals(QUANTITY_EDITOR);
    }

    public void highlightPressedOption(String key) {
        AbstractOfferEditorPanel editorPanel = (AbstractOfferEditorPanel) tabGroup.getCurrentlySelectedTab();
        editorPanel.highlightPressedOption(key);
    }

    public void deleteOption(Option option) {
        AbstractOfferEditorPanel editorPanel = (AbstractOfferEditorPanel) tabGroup.getCurrentlySelectedTab();
        editorPanel.deleteOption(option);
        revalidate();
        repaint();
    }

    public void selectQuantityEditor() {
        tabGroup.select(quantityEditorTab);
    }

    public void selectPriceEditor() {
        tabGroup.select(priceEditorTab);
    }

}
