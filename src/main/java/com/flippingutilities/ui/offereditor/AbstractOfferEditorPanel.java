package com.flippingutilities.ui.offereditor;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.Option;
import com.flippingutilities.ui.uiutilities.CustomColors;
import com.flippingutilities.ui.uiutilities.Icons;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOfferEditorPanel extends JPanel {
    JPanel optionsContainer;
    FlippingPlugin plugin;
    List<OptionPanel> optionPanels = new ArrayList<>();
    JPanel descriptionPanel;

    public abstract List<Option> getOptions();

    public abstract void addOptionPanel();

    public abstract void onTemplateClicked();

    public AbstractOfferEditorPanel(FlippingPlugin plugin) {
        this.plugin = plugin;
        setLayout(new BorderLayout());
        setBackground(CustomColors.DARK_GRAY);
        setBorder(new EmptyBorder(5,0,10,0));

        optionsContainer = new JPanel();
        optionsContainer.setLayout(new BoxLayout(optionsContainer, BoxLayout.Y_AXIS));
        optionsContainer.setBackground(CustomColors.DARK_GRAY);

        //i need thsi wrapper because when using the tab group and if one tab is larger than the other,
        // having the optionsContainer be in BorderLayout.CENTER causes the optionpanel to take up all the space. This is because
        //when the tabs are diff lenghts the shorter tab is forced to be the same size as teh larger tab and so the option
        //panel is stretched to fill in when its in BorderLayout.CENTER
        //the option panels to take up
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(createTitlePanel(), BorderLayout.NORTH);
        wrapper.add(optionsContainer, BorderLayout.CENTER);

        add(wrapper, BorderLayout.NORTH);
        rebuild(getOptions());
    }

    private JPanel createTitlePanel() {
        descriptionPanel = new JPanel();
        descriptionPanel.setBorder(new EmptyBorder(10,0,0,0));
        descriptionPanel.setBackground(CustomColors.DARK_GRAY);
        JLabel keyDescriptionLabel = new JLabel("<html><u>Key</u></html>", JLabel.CENTER);
        keyDescriptionLabel.setToolTipText("<html>The key you can press to trigger the option.<br> Make sure to hit enter after editing a key so that your changes are saved</html>");
        keyDescriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        JLabel propertyDescriptionLabel = new JLabel("<html><u>Property</u></html>",  JLabel.CENTER);
        propertyDescriptionLabel.setToolTipText("The property an option's value is dependent on");
        propertyDescriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        propertyDescriptionLabel.setBorder(new EmptyBorder(0,25,0,25));
        JLabel modifierDescriptionLabel = new JLabel("<html><u>Modifier</u></html>", JLabel.CENTER);
        modifierDescriptionLabel.setToolTipText("<html>Any of these symbols +,-,*, followed by a positive whole number.<br> Examples: +0, +10, -5, *2. Make sure to hit enter after editing a modifier to save your changes</html>");
        modifierDescriptionLabel.setFont(FontManager.getRunescapeSmallFont());

        descriptionPanel.add(keyDescriptionLabel);
        descriptionPanel.add(propertyDescriptionLabel);
        descriptionPanel.add(modifierDescriptionLabel);
        return descriptionPanel;
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
        plugin.getDataHandler().getAccountWideData().getOptions().remove(option);
        rebuild(getOptions());
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
                OptionPanel newPanel = new OptionPanel(option, plugin);
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
        welcomePanel.setBorder(new EmptyBorder(20,0,20,0));

        String text = "<html><div style=width:200;text-align: center;>" +
                "Click the plus icon above to create an option<br><br>" +
                "or<br><br>" +
                "get started quickly with multiple default options by pressing the button below!" +
                "</div></html>";

        JLabel descriptionText = new JLabel(text, JLabel.CENTER);
        descriptionText.setFont(FontManager.getRunescapeSmallFont());

        JLabel templateButton = new JLabel(Icons.TEMPLATE);
        templateButton.setBorder(new EmptyBorder(15,0,0,0));
        templateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onTemplateClicked();
                rebuild(getOptions());
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
}
