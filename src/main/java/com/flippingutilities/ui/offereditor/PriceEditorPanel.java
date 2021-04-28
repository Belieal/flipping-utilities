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
        return plugin.getDataHandler().viewAccountWideData().getOptions().stream().filter(option -> !option.isQuantityOption()).collect(Collectors.toList());
    }

    @Override
    public void addOptionPanel() {
        plugin.getDataHandler().getAccountWideData().getOptions().add(0,Option.defaultPriceOption());
        rebuild(getOptions());
    }

    @Override
    public void onTemplateClicked() {
        List<Option> options = plugin.getDataHandler().getAccountWideData().getOptions();
        options.add(new Option("n", Option.WIKI_SELL, "+0", false));
        options.add(new Option("j", Option.WIKI_BUY, "+0", false));
        options.add(new Option("u", Option.LAST_SELL, "+0", false));
        options.add(new Option("o", Option.LAST_BUY, "+0", false));
        options.add(new Option("l", Option.INSTA_SELL, "+0", false));
        options.add(new Option("p", Option.INSTA_BUY, "+0", false));
    }
}
