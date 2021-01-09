package com.flippingutilities;

import com.flippingutilities.controller.FlippingPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FlippingPlugin.class);
		RuneLite.main(args);
	}
}