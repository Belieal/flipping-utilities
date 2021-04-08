package com.flippingutilities.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class AccountWideData {
    List<Option> options = new ArrayList<>();
    List<Section> flippingItemPanelSections = new ArrayList<>();
    boolean shouldMakeNewAdditions = true;

    public boolean setDefaults() {
        boolean changedData = changeOldPropertyNames();

        if (options.isEmpty()) {
            setDefaultOptions();
            shouldMakeNewAdditions = false;
            changedData = true;
        }
        //adding wiki options to users' existing options only once and making sure that its not added again by setting shouldMakeNewAdditions.
        //i need to use that flag so i don't add it multiple times in case a user deletes those options.
        boolean missingWikiOptions = options.stream().anyMatch(o -> o.getProperty().equals(Option.WIKI_BUY) || o.getProperty().equals(Option.WIKI_SELL));
        if (shouldMakeNewAdditions && missingWikiOptions) {
            options.add(0, new Option("n", Option.WIKI_SELL, "+0", false));
            options.add(0, new Option("j", Option.WIKI_BUY, "+0", false));
            shouldMakeNewAdditions = false;
            changedData = true;
        }

        if (flippingItemPanelSections.isEmpty()) {
            changedData = true;
            setDefaultFlippingItemPanelSections();
        }
        return changedData;
    }

    private boolean changeOldPropertyNames() {
        boolean changedOldNames = false;
        for (Option o : options) {
            if (o.getProperty().equals("marg sell")) {
                o.setProperty(Option.INSTA_BUY);
                changedOldNames = true;
            }
            if (o.getProperty().equals("marg buy")) {
                o.setProperty(Option.INSTA_SELL);
                changedOldNames = true;
            }
        }

        return changedOldNames;
    }

    private void setDefaultOptions() {
        options.add(new Option("n", Option.WIKI_SELL, "+0", false));
        options.add(new Option("j", Option.WIKI_BUY, "+0", false));
        options.add(new Option("p", Option.INSTA_BUY, "+0", false));
        options.add(new Option("l", Option.INSTA_SELL, "+0", false));
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
        importantSection.showLabel(Section.WIKI_BUY_PRICE, true);
        importantSection.showLabel(Section.WIKI_SELL_PRICE, true);
        importantSection.showLabel(Section.LATEST_BUY_PRICE, true);
        importantSection.showLabel(Section.LATEST_SELL_PRICE, true);
        importantSection.showLabel(Section.PRICE_CHECK_BUY_PRICE, true);
        importantSection.showLabel(Section.PRICE_CHECK_SELL_PRICE, true);

        otherSection.showLabel(Section.PROFIT_EACH, true);
        otherSection.showLabel(Section.POTENTIAL_PROFIT, true);
        otherSection.showLabel(Section.REMAINING_GE_LIMIT, true);
        otherSection.showLabel(Section.ROI, true);
        otherSection.showLabel(Section.GE_LIMIT_REFRESH_TIMER, true);

        flippingItemPanelSections.add(importantSection);
        flippingItemPanelSections.add(otherSection);
    }

}
