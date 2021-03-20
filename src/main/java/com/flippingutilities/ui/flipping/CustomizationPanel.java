package com.flippingutilities.ui.flipping;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.Section;
import com.flippingutilities.ui.uiutilities.CustomColors;
import com.flippingutilities.ui.uiutilities.UIUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

@Slf4j
public class CustomizationPanel extends JPanel {
    FlippingPlugin plugin;
    List<Section> sections;

    public CustomizationPanel(FlippingPlugin plugin) {
        this.plugin = plugin;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.BLACK.darker());
    }

    private JPanel createSectionPanel(Section section) {
        JPanel sectionPanel = new JPanel();
        sectionPanel.setBackground(Color.BLACK.darker());
        sectionPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel("Section name: " + section.getName());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(FontManager.getRunescapeBoldFont());

        sectionPanel.add(titleLabel);
        sectionPanel.add(Box.createVerticalStrut(10));

        for (String labelName : Section.possibleLabels) {
            JPanel sectionLabelPanel = new JPanel(new BorderLayout());
            sectionLabelPanel.setBackground(Color.BLACK.darker());
            JLabel sectionLabel = new JLabel(labelName);
            sectionLabel.setFont(FontManager.getRunescapeSmallFont());
            JToggleButton sectionLabelToggle = UIUtilities.createToggleButton();
            sectionLabelToggle.setSelected(section.isShowingLabel(labelName));
            sectionLabelPanel.add(sectionLabel, BorderLayout.WEST);
            sectionLabelPanel.add(sectionLabelToggle, BorderLayout.EAST);
            sectionPanel.add(sectionLabelPanel);
            sectionPanel.add(Box.createVerticalStrut(3));
        }
        return sectionPanel;
    }

    public void rebuild(List<Section> sections) {
        this.sections = sections;
        removeAll();
        sections.forEach(section -> add(createSectionPanel(section)));
        revalidate();
        repaint();

    }
}
