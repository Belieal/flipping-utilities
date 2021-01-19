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
        return plugin.getDataHandler().getAccountWideData().getOptions().stream().filter(option -> option.isQuantityOption()).collect(Collectors.toList());
    }

    @Override
    public void addOptionPanel() {
        plugin.getDataHandler().getAccountWideData().getOptions().add(0,Option.defaultQuantityOption());
        rebuild(getOptions());
    }

    @Override
    public void onTemplateClicked() {
        List<Option> options = plugin.getDataHandler().getAccountWideData().getOptions();
        options.add(new Option("p", Option.GE_LIMIT, "+0", true));
        options.add(new Option("l", Option.REMAINING_LIMIT, "+0", true));
        options.add(new Option("o", Option.CASHSTACK, "+0", true));
    }
}
