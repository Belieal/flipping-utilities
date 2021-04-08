package com.flippingutilities.ui.offereditor;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.Option;
import com.flippingutilities.ui.uiutilities.CustomColors;
import com.flippingutilities.ui.uiutilities.Icons;
import com.flippingutilities.utilities.InvalidOptionException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ColorUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
public class OptionPanel extends JPanel {
    private FlippingPlugin plugin;
    @Getter
    private Option option;
    private JLabel resultingValueLabel;
    private JLabel dotIcon;
    private boolean isHighlighted;
    private Icon lastIcon;

    public OptionPanel(Option option, FlippingPlugin plugin) {
        this.option = option;
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBackground(CustomColors.DARK_GRAY);
        setBorder(new EmptyBorder(5, 0, 5, 5));

        resultingValueLabel = new JLabel("", JLabel.CENTER);
        resultingValueLabel.setFont(FontManager.getRunescapeSmallFont());

        dotIcon = createDotIcon();

        add(resultingValueLabel, BorderLayout.SOUTH);
        add(createBodyPanel(), BorderLayout.CENTER);
        add(dotIcon, BorderLayout.WEST);
        setResultingValue();
    }

    public JPanel createBodyPanel() {
        JPanel body = new JPanel();
        body.setBackground(CustomColors.DARK_GRAY);

        JTextField keyInputField = new JTextField(2);
        //keyInputField.setPreferredSize(new Dimension(30, 25));
        keyInputField.setText(option.getKey());
        keyInputField.addActionListener(e -> {
            plugin.markAccountTradesAsHavingChanged(FlippingPlugin.ACCOUNT_WIDE);
            option.setKey(keyInputField.getText());
            setResultingValue();
        });
        keyInputField.setToolTipText("Press enter after inputting a key to save your changes");

        JComboBox propertiesSelector = new JComboBox(option.isQuantityOption()? Option.QUANTITY_OPTIONS: Option.PRICE_OPTIONS);
        propertiesSelector.setPreferredSize(new Dimension(88, 25));
        propertiesSelector.setSelectedItem(option.getProperty());
        propertiesSelector.addActionListener(e -> {
            if (propertiesSelector.getSelectedItem() != null) {
                plugin.markAccountTradesAsHavingChanged(FlippingPlugin.ACCOUNT_WIDE);
                option.setProperty((String) propertiesSelector.getSelectedItem());
                setResultingValue();
            }
        });

        JTextField optionalEditor = new JTextField(5);
        //optionalEditor.setPreferredSize(new Dimension(30, 25));
        optionalEditor.setText(option.getModifier());
        optionalEditor.addActionListener(e -> {
            plugin.markAccountTradesAsHavingChanged(FlippingPlugin.ACCOUNT_WIDE);
            option.setModifier(optionalEditor.getText());
            setResultingValue();
        });
        optionalEditor.setToolTipText("press enter after inputting something to save your changes");

        body.add(keyInputField);
        body.add(propertiesSelector);
        body.add(optionalEditor);

        return body;
    }

    private String createResultingValueText(int resultingValue) {
        String keyText = String.format("<span style='color:%s;'>%s</span>",ColorUtil.colorToHexCode(CustomColors.VIBRANT_YELLOW), option.getKey());
        String typeText = option.isQuantityOption()? "quantity":"price";
        String value = NumberFormat.getInstance(Locale.getDefault()).format(resultingValue);
        String valueText = String.format("<span style='color:%s;'>%s</span>", ColorUtil.colorToHexCode(ColorScheme.GRAND_EXCHANGE_PRICE), value);
        return String.format("<html><body width='170' style='text-align:center;'>Press %s to set %s to %s</body></html>",keyText,typeText,valueText);
    }

    private JLabel createDotIcon() {
        dotIcon = new JLabel(Icons.GRAY_DOT);
        lastIcon = dotIcon.getIcon();
        dotIcon.setBorder(new EmptyBorder(8, 12, 0, 0));
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
                dotIcon.setIcon(Icons.GRAY_DOT);
                if (finalErrorMessage != null) {
                    showError(finalErrorMessage);
                } else {
                    resultingValueLabel.setText(createResultingValueText(finalVal));
                }
                revalidate();
                repaint();
            });
        });
    }

    private void showError(String msg) {
        String color = ColorUtil.colorToHexCode(CustomColors.TOMATO);
        resultingValueLabel.setText(String.format("<html><body width='170'style='text-align:center;color:%s'>%s</body></html>", color,msg));
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
