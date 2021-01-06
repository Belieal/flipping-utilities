package com.flippingutilities.ui.offereditor;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.Option;

import java.util.List;
import java.util.stream.Collectors;

public class PriceEditorPanel extends AbstractOfferEditorPanel{
    public PriceEditorPanel(FlippingPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<Option> getOptions() {
        return plugin.getOptionsForCurrentView().stream().filter(option -> !option.isQuantityOption()).collect(Collectors.toList());
    }

    @Override
    public void addOptionPanel() {
        plugin.getOptionsForCurrentView().add(0, Option.defaultPriceOption());
        rebuild(getOptions());
    }

    @Override
    public void onTemplateClicked() {
        plugin.getOptionsForCurrentView().add(new Option("p", Option.MARGIN_SELL, "+0", false));
        plugin.getOptionsForCurrentView().add(new Option("l", Option.MARGIN_BUY, "+0", false));
        plugin.getOptionsForCurrentView().add(new Option("o", Option.LAST_BUY, "+0", false));
        plugin.getOptionsForCurrentView().add(new Option("u", Option.LAST_SELL, "+0", false));
    }
}
