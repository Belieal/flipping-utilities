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
        return plugin.getOptions().stream().filter(option -> option.isQuantityOption()).collect(Collectors.toList());
    }

    @Override
    public void addOptionPanel() {
        plugin.addOption(Option.defaultQuantityOption());
        rebuild(getOptions());
    }

    @Override
    public void onTemplateClicked() {
        plugin.addOption(new Option("o", Option.CASHSTACK, "+0", true));
        plugin.addOption(new Option("l", Option.REMAINING_LIMIT, "+0", true));
        plugin.addOption(new Option("p", Option.GE_LIMIT, "+0", true));
    }
}
