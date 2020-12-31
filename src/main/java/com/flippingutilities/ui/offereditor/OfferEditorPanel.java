package com.flippingutilities.ui.offereditor;

import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.model.Option;
import com.flippingutilities.ui.uiutilities.CustomColors;
import com.flippingutilities.ui.uiutilities.Icons;
import com.flippingutilities.ui.uiutilities.UIUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

@Slf4j
public class OfferEditorPanel extends JPanel {
    FlippingItem item;
    JPanel optionsContainer;
    FlippingPlugin plugin;
    List<OptionPanel> optionPanels = new ArrayList<>();
    JPanel descriptionPanel;

    public OfferEditorPanel(FlippingPlugin plugin, FlippingItem item) {
        this.item = item;
        this.plugin = plugin;
        setLayout(new BorderLayout());
        setBackground(CustomColors.DARK_GRAY);
        setBorder(new EmptyBorder(5,0,10,0));

        optionsContainer = new JPanel();
        optionsContainer.setLayout(new BoxLayout(optionsContainer, BoxLayout.Y_AXIS));
        optionsContainer.setBackground(CustomColors.DARK_GRAY);

        add(createTitlePanel(), BorderLayout.NORTH);
        add(optionsContainer, BorderLayout.CENTER);
        rebuild(plugin.getOptionsForCurrentView());
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(5,0,5,0));
        titlePanel.setBackground(CustomColors.DARK_GRAY);

        JLabel titleText = new JLabel("Quantity Editor", JLabel.CENTER);
        titleText.setFont(FontManager.getRunescapeBoldFont());
        //titleText.setBorder(new EmptyBorder(0,15,0,0));

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
        descriptionPanel = new JPanel();
        descriptionPanel.setBorder(new EmptyBorder(10,35,0,22));
        descriptionPanel.setBackground(CustomColors.DARK_GRAY);
        JLabel keyDescriptionLabel = new JLabel("Key", JLabel.CENTER);
        keyDescriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        JLabel propertyDescriptionLabel = new JLabel("Property",  JLabel.CENTER);
        propertyDescriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        propertyDescriptionLabel.setBorder(new EmptyBorder(0,25,0,27));
        JLabel modifierDescriptionLabel = new JLabel("Modifier", JLabel.CENTER);
        modifierDescriptionLabel.setFont(FontManager.getRunescapeSmallFont());

        descriptionPanel.add(keyDescriptionLabel);
        descriptionPanel.add(propertyDescriptionLabel);
        descriptionPanel.add(modifierDescriptionLabel);

        titlePanel.add(titleText, BorderLayout.CENTER);
        titlePanel.add(plusIconLabel, BorderLayout.EAST);
        titlePanel.add(helpIconLabel, BorderLayout.WEST);
        titlePanel.add(descriptionPanel, BorderLayout.SOUTH);
        return titlePanel;
    }

    private void addOptionPanel() {
        plugin.getOptionsForCurrentView().add(0, Option.defaultOption());
        rebuild(plugin.getOptionsForCurrentView());
    }

    public void highlightPressedOption(String key) {
        boolean alreadyHighlightedOne = false;
        for (OptionPanel panel: optionPanels) {
            String keyInOption = panel.getOption().getKey();
            //in case a user has put multiple options with the same key, don't highlight multiple.
            if (key.equals(keyInOption) && !alreadyHighlightedOne) {
                panel.highlight();
                alreadyHighlightedOne = true;
            }
            else {
                panel.deHighlight();
            }
        }
    }

    public void deleteOption(Option option) {
        plugin.getOptionsForCurrentView().remove(option);
        rebuild(plugin.getOptionsForCurrentView());
    }

    public void rebuild(List<Option> options) {
        SwingUtilities.invokeLater(() -> {
            optionsContainer.removeAll();
            if (options.isEmpty()) {
                descriptionPanel.setVisible(false);
                optionsContainer.add(createWelcomePanel());
            }
            else {
                descriptionPanel.setVisible(true);
            }
            optionPanels.clear();
            for (Option option:options) {
                OptionPanel newPanel = new OptionPanel(option, item, plugin);
                optionPanels.add(newPanel);
                optionsContainer.add(newPanel);
            }
            revalidate();
            repaint();
        });
    }

    private JPanel createWelcomePanel() {
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(CustomColors.DARK_GRAY);
        welcomePanel.setBorder(new EmptyBorder(20,0,0,0));

        String text = "<html><div style=width:200;text-align: center;>" +
                "Click the plus icon above to create an option<br><br>" +
                "or<br><br>" +
                "get started quickly with multiple default options by pressing the button below!" +
                "</div></html>";

        JLabel descriptionText = new JLabel(text, JLabel.CENTER);
        descriptionText.setFont(FontManager.getRunescapeSmallFont());

        JLabel templateButton = new JLabel(Icons.TEMPLATE);
        templateButton.setBorder(new EmptyBorder(10,0,0,0));
        templateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                plugin.getOptionsForCurrentView().add(new Option("p", Option.GE_LIMIT, "+0"));
                plugin.getOptionsForCurrentView().add(new Option("l", Option.REMAINING_LIMIT, "+0"));
                plugin.getOptionsForCurrentView().add(new Option("o", Option.CASHSTACK, "+0"));
                rebuild(plugin.getOptionsForCurrentView());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                templateButton.setIcon(Icons.TEMPLATE_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                templateButton.setIcon(Icons.TEMPLATE);
            }
        });

        welcomePanel.add(descriptionText, BorderLayout.NORTH);

        welcomePanel.add(templateButton, BorderLayout.CENTER);
        return welcomePanel;
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
}
