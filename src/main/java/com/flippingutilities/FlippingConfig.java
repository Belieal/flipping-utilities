
package com.flippingutilities;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup(FlippingPlugin.CONFIG_GROUP)
public interface FlippingConfig extends Config
{
	@ConfigItem(
		keyName = "storeTradeHistory",
		name = "Store session trade history locally",
		description = "Store your trade history to have your previous trade data show up on new game sessions."
	)
	default boolean storeTradeHistory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "outOfDateWarning",
		name = "Set how long before prices are outdated",
		description = "Set how long before warning that prices are outdated"
	)
	@Units(Units.MINUTES)
	default int outOfDateWarning()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "roiGradientMax",
		name = "Set ROI gradient range limit",
		description = "Set the limit of the range before the gradient is bright green."
	)
	@Units(Units.PERCENT)
	default int roiGradientMax()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "marginCheckLoss",
		name = "Account for margin check loss",
		description = "Subtract the loss from margin checking the item when calculating the total profit"
	)
	default boolean marginCheckLoss()
	{
		return true;
	}
}
