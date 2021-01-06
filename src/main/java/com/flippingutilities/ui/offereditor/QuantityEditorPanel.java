package com.flippingutilities.ui.offereditor;

import com.flippingutilities.controller.FlippingPlugin;
import com.flippingutilities.model.Option;

import java.util.List;
import java.util.stream.Collectors;

public class QuantityEditorPanel extends AbstractOfferEditorPanel{
    public QuantityEditorPanel(FlippingPlugin plugin) {
        super(plugin);
    }

    @Override
    public List<Option> getOptions() {
        return plugin.getOptionsForCurrentView().stream().filter(option -> option.isQuantityOption()).collect(Collectors.toList());
    }

    @Override
    public void addOptionPanel() {
        plugin.getOptionsForCurrentView().add(0, Option.defaultQuantityOption());
        rebuild(getOptions());
    }

    @Override
    public void onTemplateClicked() {
        plugin.getOptionsForCurrentView().add(new Option("p", Option.GE_LIMIT, "+0", true));
        plugin.getOptionsForCurrentView().add(new Option("l", Option.REMAINING_LIMIT, "+0", true));
        plugin.getOptionsForCurrentView().add(new Option("o", Option.CASHSTACK, "+0", true));
    }
}
