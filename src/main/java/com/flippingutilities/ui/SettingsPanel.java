package com.flippingutilities.ui;

import com.flippingutilities.FlippingPlugin;
import javax.swing.JPanel;

/**
 * The settings panel is a panel displayed in a modal when a user clicks on the settings button next to the account
 * selector dropdown. Its purpose is to allow modifying various features of the plugin. Perhaps in the future this
 * panel can subsume Runelite's config option for FlippingUtilities as the config option is not as easy to access as this
 * panel. This is broken out into its own class instead of residing in the MasterPanel because it will contain
 * enough distinct functionality from the MasterPanel that adding it in their will muddle the responsibility of the
 * master panel and hurt readability.
 */
public class SettingsPanel extends JPanel
{
	FlippingPlugin plugin;

	public SettingsPanel(FlippingPlugin plugin) {
		this.plugin = plugin;
	}
}
