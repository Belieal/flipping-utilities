package com.flippingutilities.ui.offereditor;

import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.model.FlippingItem;
import com.flippingutilities.model.Option;
import com.flippingutilities.ui.uiutilities.CustomColors;
import com.flippingutilities.ui.uiutilities.Icons;
import lombok.extern.slf4j.Slf4j;
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
    private final Set<String> allowedKeys = new HashSet<>(Arrays.asList("p","l","m","o","n","i","j","u","h","v","y","g","c","t","f","x","r","d","z","e","s","w","a","q"));
    FlippingPlugin plugin;
    List<OptionPanel> optionPanels = new ArrayList<>();

    public OfferEditorPanel(FlippingPlugin plugin, FlippingItem item) {
        this.item = item;
        this.plugin = plugin;
        setLayout(new BorderLayout());
        setBackground(CustomColors.DARK_GRAY);

        optionsContainer = new JPanel();
        optionsContainer.setLayout(new BoxLayout(optionsContainer, BoxLayout.Y_AXIS));
        optionsContainer.setBackground(CustomColors.DARK_GRAY);

        plugin.getOptionsForCurrentView().forEach(option -> {
            OptionPanel newPanel = new OptionPanel(option, item, plugin);
            optionPanels.add(newPanel);
            optionsContainer.add(newPanel);
        });

        add(createTitlePanel(), BorderLayout.NORTH);
        add(optionsContainer, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(5,0,5,0));
        titlePanel.setBackground(CustomColors.DARK_GRAY);

        JLabel titleText = new JLabel("Quantity Editor", JLabel.CENTER);
        titleText.setFont(FontManager.getRunescapeBoldFont());
        titleText.setBorder(new EmptyBorder(0,15,0,0));

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

        titlePanel.add(titleText, BorderLayout.CENTER);
        titlePanel.add(plusIconLabel, BorderLayout.EAST);
        return titlePanel;
    }

    private void addOptionPanel() {
        Option newOption = Option.defaultOption();
        OptionPanel newPanel = new OptionPanel(newOption,item,plugin);
        plugin.getOptionsForCurrentView().add(newOption);
        optionPanels.add(newPanel);
        optionsContainer.add(newPanel);
        revalidate();
        repaint();
    }

    public void highlightPressedOption(String key) {
        for (OptionPanel panel: optionPanels) {
            String keyInOption = panel.getOption().getKey();
            if (key.equals(keyInOption)) {
                panel.highlight();
            }
            else {
                panel.deHighlight();
            }
        }
    }

//    public Set<String> getAvailableKeys() {
//        Set<String> usedKeys = options.stream().map(Option::getKey).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
//        return Sets.difference(allowedKeys, usedKeys).copyInto(new HashSet<>());
//    }
}
