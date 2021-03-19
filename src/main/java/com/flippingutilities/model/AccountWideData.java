package com.flippingutilities.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class AccountWideData {
    List<Option> options = new ArrayList<>();
    List<Section> sections = new ArrayList<>();

    public boolean setDefaults() {
        boolean setDefaults = false;
        if (options.isEmpty()) {
            setDefaults = true;
            setDefaultOptions();
        }
        if (sections.isEmpty()) {
            setDefaults = true;
            setDefaultFlippingItemPanelSections();
        }
        return setDefaults;
    }

    private void setDefaultOptions() {
        options.add(new Option("p", Option.MARGIN_SELL, "+0", false));
        options.add(new Option("l", Option.MARGIN_BUY, "+0", false));
        options.add(new Option("o", Option.LAST_BUY, "+0", false));
        options.add(new Option("u", Option.LAST_SELL, "+0", false));

        options.add(new Option("p", Option.GE_LIMIT, "+0", true));
        options.add(new Option("l", Option.REMAINING_LIMIT, "+0", true));
        options.add(new Option("o", Option.CASHSTACK, "+0", true));
    }

    private void setDefaultFlippingItemPanelSections() {
        Section importantSection = new Section("Important");
        Section otherSection = new Section("Other");

        importantSection.defaultExpanded = true;
        importantSection.showLabel(Section.LATEST_BUY_PRICE);
        importantSection.showLabel(Section.LATEST_SELL_PRICE);
        importantSection.showLabel(Section.PRICE_CHECK_BUY_PRICE);
        importantSection.showLabel(Section.PRICE_CHECK_SELL_PRICE);

        otherSection.showLabel(Section.PROFIT_EACH);
        otherSection.showLabel(Section.POTENTIAL_PROFIT);
        otherSection.showLabel(Section.REMAINING_GE_LIMIT);
        otherSection.showLabel(Section.ROI);
        otherSection.showLabel(Section.GE_LIMIT_REFRESH_TIMER);

        sections.add(importantSection);
        sections.add(otherSection);
    }

}
