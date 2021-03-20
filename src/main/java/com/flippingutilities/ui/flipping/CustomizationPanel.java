package com.flippingutilities.ui.flipping;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.Section;
import com.flippingutilities.ui.uiutilities.Icons;
import com.flippingutilities.ui.uiutilities.UIUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

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
        setBorder(new EmptyBorder(10,10,10,10));
    }

    private JPanel createSectionPanel(Section section) {
        JPanel sectionPanel = new JPanel();
        sectionPanel.setBackground(Color.BLACK.darker());
        sectionPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Section name: " + section.getName());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(FontManager.getRunescapeBoldFont());

        JPanel makeSectionDefaultExpandedPanel = new JPanel(new FlowLayout());
        makeSectionDefaultExpandedPanel.setBackground(Color.BLACK.darker());
        JLabel shouldMakeSectionExpandedLabel = new JLabel("expand section by default");
        shouldMakeSectionExpandedLabel.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), Font.ITALIC, FontManager.getRunescapeSmallFont().getSize()));

        JToggleButton toggleButton = UIUtilities.createToggleButton();
        toggleButton.setSelected(section.isDefaultExpanded());
        toggleButton.addItemListener(i ->
        {
            section.setDefaultExpanded(toggleButton.isSelected());
            onSectionChange();
        });
        makeSectionDefaultExpandedPanel.add(shouldMakeSectionExpandedLabel);
        makeSectionDefaultExpandedPanel.add(toggleButton);

        sectionPanel.add(titleLabel);
        sectionPanel.add(makeSectionDefaultExpandedPanel);
        sectionPanel.add(Box.createVerticalStrut(10));

        for (String labelName : Section.possibleLabels) {
            sectionPanel.add(createPanelForSectionLabel(section, labelName));
            sectionPanel.add(Box.createVerticalStrut(3));
        }
        return sectionPanel;
    }

    private boolean labelUsedInAnotherSection(Section section, String labelName) {
        for (Section otherSection: sections) {
            if (section != otherSection) {
                if (otherSection.isShowingLabel(labelName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JPanel createPanelForSectionLabel(Section section, String labelName) {
        boolean labelUsedInOtherSection = labelUsedInAnotherSection(section, labelName);
        JPanel sectionLabelPanel = new JPanel(new BorderLayout());
        sectionLabelPanel.setBackground(Color.BLACK.darker());
        JLabel sectionLabel = new JLabel(labelUsedInOtherSection? labelName + " (disabled: used in other section)" :labelName);
        sectionLabel.setFont(FontManager.getRunescapeSmallFont());
        sectionLabel.setForeground(labelUsedInOtherSection? ColorScheme.MEDIUM_GRAY_COLOR : Color.WHITE);
        JToggleButton sectionLabelToggle = UIUtilities.createToggleButton();
        sectionLabelToggle.setSelected(section.isShowingLabel(labelName));
        sectionLabelToggle.setEnabled(!labelUsedInOtherSection);
        sectionLabelToggle.setDisabledIcon(Icons.TOGGLE_OFF);

        sectionLabelToggle.addItemListener(i ->
        {
            section.showLabel(labelName, sectionLabelToggle.isSelected());
            onSectionChange();
        });

        sectionLabelPanel.add(sectionLabel, BorderLayout.WEST);
        sectionLabelPanel.add(sectionLabelToggle, BorderLayout.EAST);
        return sectionLabelPanel;
    }

    private void onSectionChange() {
        rebuild(sections);
        plugin.getFlippingPanel().rebuild(plugin.viewTradesForCurrentView());
        plugin.getDataHandler().markDataAsHavingChanged(FlippingPlugin.ACCOUNT_WIDE);
    }

    public void rebuild(List<Section> sections) {
        this.sections = sections;
        removeAll();
        sections.forEach(section -> add(createSectionPanel(section)));
        revalidate();
        repaint();

    }
}
