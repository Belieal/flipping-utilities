package com.flippingutilities.ui.offereditor;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.Option;
import com.flippingutilities.ui.uiutilities.CustomColors;
import com.flippingutilities.ui.uiutilities.Icons;
import com.flippingutilities.ui.uiutilities.UIUtilities;
import com.flippingutilities.utilities.InvalidOptionException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@Slf4j
public class OptionPanel extends JPanel {
    private FlippingPlugin plugin;
    @Getter
    private Option option;
    private JLabel resultingValueLabel;
    private JPanel errorTextPanel;
    private JPanel resultingValuePanel;
    private JLabel dotIcon;
    private JLabel keyLabel;
    private boolean isHighlighted;
    private Icon lastIcon;

    public OptionPanel(Option option, FlippingPlugin plugin) {
        this.option = option;
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBackground(CustomColors.DARK_GRAY);
        setBorder(new EmptyBorder(5, 0, 5, 5));

        setResultingValuePanel();

        errorTextPanel = new JPanel();
        errorTextPanel.setBackground(CustomColors.DARK_GRAY);
        dotIcon = createDotIcon();

        add(resultingValuePanel, BorderLayout.SOUTH);
        add(createBodyPanel(), BorderLayout.CENTER);
        add(dotIcon, BorderLayout.WEST);
    }

    public JPanel createBodyPanel() {
        JPanel body = new JPanel();
        body.setBackground(CustomColors.DARK_GRAY);

        JTextField keyInputField = new JTextField(2);
        keyInputField.setPreferredSize(new Dimension(30, 25));
        keyInputField.setText(option.getKey());
        keyInputField.addActionListener(e -> {
            option.setKey(keyInputField.getText());
            keyLabel.setText(keyInputField.getText());
        });
        keyInputField.setToolTipText("Press enter after inputting a key to save your changes");


        JComboBox propertiesSelector = new JComboBox(option.isQuantityOption()? new String[]{Option.REMAINING_LIMIT, Option.GE_LIMIT, Option.CASHSTACK}:new String[]{Option.MARGIN_BUY, Option.MARGIN_SELL, Option.LAST_BUY, Option.LAST_SELL});
        propertiesSelector.setPreferredSize(new Dimension(85, 25));
        propertiesSelector.addActionListener(e -> {
            if (propertiesSelector.getSelectedItem() != null) {
                option.setProperty((String) propertiesSelector.getSelectedItem());
                setResultingValue();
            }
        });
        propertiesSelector.setSelectedItem(option.getProperty());


        JTextField optionalEditor = new JTextField(5);
        optionalEditor.setPreferredSize(new Dimension(30, 25));
        optionalEditor.setText(option.getModifier());
        optionalEditor.addActionListener(e -> {
            option.setModifier(optionalEditor.getText());
            setResultingValue();
        });
        optionalEditor.setToolTipText("press enter after inputting something to save your changes");

        body.add(keyInputField);
        body.add(propertiesSelector);
        body.add(optionalEditor);

        return body;
    }

    private void setResultingValuePanel() {
        resultingValuePanel = new JPanel();
        resultingValuePanel.setBackground(CustomColors.DARK_GRAY);

        JLabel pressText = new JLabel("press", JLabel.CENTER);
        pressText.setFont(FontManager.getRunescapeSmallFont());

        keyLabel = new JLabel(option.getKey(), JLabel.CENTER);
        keyLabel.setFont(FontManager.getRunescapeSmallFont());
        keyLabel.setForeground(CustomColors.VIBRANT_YELLOW);

        JLabel descLabel = new JLabel("to set quantity to", JLabel.CENTER);
        descLabel.setFont(FontManager.getRunescapeSmallFont());

        resultingValueLabel = new JLabel("", JLabel.CENTER);
        resultingValueLabel.setFont(FontManager.getRunescapeSmallFont());
        resultingValueLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);

        resultingValuePanel.add(pressText);
        resultingValuePanel.add(keyLabel);
        resultingValuePanel.add(descLabel);
        resultingValuePanel.add(resultingValueLabel);
    }

    private JLabel createDotIcon() {
        dotIcon = new JLabel(Icons.GRAY_DOT);
        lastIcon = dotIcon.getIcon();
        dotIcon.setBorder(new EmptyBorder(8, 15, 0, 0));
        dotIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (dotIcon.getIcon().equals(Icons.DELETE_ICON)) {
                    plugin.getFlippingPanel().getOfferEditorContainerPanel().deleteOption(option);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                lastIcon = dotIcon.getIcon();
                dotIcon.setIcon(Icons.DELETE_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                dotIcon.setIcon(lastIcon);
            }
        });
        return dotIcon;
    }

    private void setResultingValue() {
        plugin.getClientThread().invokeLater(() -> {
            String errorMessage = null;
            int val = 0;
            try {
                val = plugin.calculateOptionValue(option);
            } catch (InvalidOptionException e) {
                errorMessage = e.getMessage();
            }
            String finalErrorMessage = errorMessage;
            int finalVal = val;
            SwingUtilities.invokeLater(() -> {
                remove(errorTextPanel);
                add(resultingValuePanel, BorderLayout.SOUTH);
                dotIcon.setIcon(Icons.GRAY_DOT);
                if (finalErrorMessage != null) {
                    showError(finalErrorMessage);
                } else {
                    resultingValueLabel.setText(String.valueOf(finalVal));
                }
                revalidate();
                repaint();
            });
        });
    }

    private void showError(String msg) {
        remove(resultingValuePanel);
        String labelText = UIUtilities.wrappedText(msg, 150);
        JLabel errorTextLabel = new JLabel(labelText, JLabel.CENTER);
        errorTextPanel.removeAll();
        errorTextLabel.setFont(FontManager.getRunescapeSmallFont());
        errorTextLabel.setForeground(CustomColors.TOMATO);
        errorTextPanel.add(errorTextLabel);
        add(errorTextPanel, BorderLayout.SOUTH);
        dotIcon.setIcon(Icons.RED_DOT);
    }

    public void highlight() {
        isHighlighted = true;
        dotIcon.setIcon(Icons.GREEN_DOT);
    }

    public void deHighlight() {
        if (isHighlighted) {
            dotIcon.setIcon(Icons.GRAY_DOT);
            isHighlighted = false;
        }
    }
}
