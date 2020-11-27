# Flipping Utilities plugin for RuneLite
Flipping Utilities is a Runelite plugin that aids over 10000 players in making flipping more profitable and enjoyable. It calculates your profits in real time, allows you to quick search for your favorite items, lets you quickly input optimal prices according to your margin checks with just a button click, and a lot more!

**Disclaimer: This is not an OSBuddy Exchange replacement, as it does not send or receive price data from other users.**


## Support
If you notice any bugs or have any suggestions, let us know by making an [issue](https://github.com/Belieal/flipping-utilities/issues) or contact us on Discord at (gaymersriseup#4026) or (#Belieal6600)! I'm also happy to answer any questions that you may have. :)
 
# Table of Contents
- [Features](#features)
    + [The Flipping Panel](#the-flipping-panel)
    + [The Statistics Panel](#the-statistics-panel)
    + [Additional Features](#additional-features)
    
      - [GE limit tracking](#ge-limit-tracking--so-you-can-see-the-remaining-items-you-can-buy-in-the-limit-along-with-when-the-limit-expires)
      - [Time of latest margin check.](#time-of-latest-margin-check--so-that-you-can-make-sure-your-prices-are-up-to-date)
      - [Widgets](#widgets)
      
- [Potential future features](#potential-future-features)
- [Changelog](#changelog)
  * [v1.2](#v12)
  * [v1.3 - Statistics Tab update](#v13---statistics-tab-update)
    - [v1.3.1 - Multi-client support and GE tracking links](#v131---multi-client-support-and-ge-tracking-links)
    - [v1.3.2 - Account deletion and bug fixes](#v132---account-deletion-and-bug-fixes)


# Features
The plugin is divided into two panels, the flipping panel and statistics panel. Each panel shows different information about the flips you've made.

### The Flipping Panel
This panel is designed to be the main utility panel. It shows the information about your flips that is most relevant when you're actively flipping.

The panel is designed to be "hands-free". This means you can just margin check an item you want to flip and the panel will detect, store and display prices along with calculating the potential profits and tracking the GE limit.

![Demonstration gif](images/demo.gif)

You can use this information to quickly determine if the item is worth it to flip, when the gre

### The Statistics Panel
This panel serves as way to analyze the flips you've made over a period of time, that you specify. You will be able to see everything from the profits you've gained and the items you've flipped to the individual flip history for each item. It's all automatically recorded by the plugin and will be updated alongside your flipping.

The panel consists of various menu-layers. This means that all the menus within the panel are expandable and collapsible.

#### The Top Panel

<p align="center">
  <img src="https://i.imgur.com/xRU5qfY.png">
</p>

This panel allows you to specify the time span of flips you'd like to see the statistics of. Below that you're shown the general information about your flipping statistics. You can see the total profit (or loss) you've accrued during the time span, that you specified. You can click on this bar to expand or collapse the sub information that is shown below.

#### Item Panels

<p align="center">
  <img src="https://i.imgur.com/XaTiKcZ.png">
</p>

Below the top panel, you have the item panels. These are the panels that hold information about individual items, that you've flipped. These can also be clicked on to expand with detailed information about each item.

<p align="center">
  <img src="https://i.imgur.com/yaD2Ezu.png">
</p>

This shows the expanded state of an item panel. Notice that you can also access the trade history from here.

#### Trade History

<p align="center">
  <img src="https://i.imgur.com/yz9YSgg.png">
</p>

This is where you can view the individual flips that you've made along with margin checks. This panel is only updated after a flip is complete (one completed buy offer and one completed sell offer, their quantities bought/sold don't need to match).


## Additional Features

This plugin also features a lot of handy tooltips that will show you important information such as:
#### GE limit tracking, so you can see the remaining items you can buy in the limit along with when the limit expires.

<p align="center">
  <img src="https://i.imgur.com/DPT0AXv.png">
</p>

#### Time of latest margin check, so that you can make sure your prices are up-to-date.

<p align="center">
  <img src="https://i.imgur.com/E2xPEju.png">
</p>

#### Detailed descriptions of what texts and values mean

<p align="center">
  <img src="https://i.imgur.com/RCQEFIA.png">
</p>

#### Widgets

The Flipping widget seeks to help you set up your offers quickly! The widget will show up in the chatbox when you want to set the price of your item, if you've margin checked it beforehand.

<p align="center">
  <img src="https://i.imgur.com/mATjKuo.png">
</p>

Simply click the red text and press enter. 

## Icon Attributions
All icons were either made by Belieal or downloaded from the creators on www.flaticon.com below.
<div>Icons made by <a href="https://www.flaticon.com/authors/those-icons" title="Those Icons">Those Icons</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
<div>Icons made by <a href="https://www.flaticon.com/authors/pixel-perfect" title="Pixel perfect">Pixel perfect</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>


## Potential future features
* Optional notification when a GE limit expires.
* Favorited items.
* When setting up buy offers, provide an option to set quantity to only what you have gold for.
* Collapsing trades after the GE limit has been bought.
* Grouped checklist tab with import and export features so you can keep track of the items you like to flip most!

## Changelog

v1.0 - Plugin added!

v1.1 - GE limit tracking added.

v1.1.1 - 12 hour format for clocks and various bug fixes.

## v1.2

* Added a search bar

* Added margin freezing, so you can now flip items with a quantity of one without changing the buy or sell prices. (Big thanks to @Zumaad for developing this!)

* Added flipping widget that will show up in the chatbox when you want to set the price and quantity of your item, if you've margin checked it beforehand.

* Added individual removal of items, so you can now click the item icon to remove it.

* _Total_ profit has been rebranded to _potential_ profit to more accurately describe its intended meaning. (Thank you to @Telans for suggesting this)

* Added new config options, e.g. Calculate potential profit from remaining GE limit or total GE limit.

* Fixed an issue that caused prices to not show color.

* Fixed an issue that caused price colors to flicker.

* Various optimizations and smaller bug fixes.

## v1.3 - Statistics Tab update

This update added the statistics tab which contains a variety of useful stats about your flips! The panel features a large variety of sub information panels that tell you things such as:

* The amount of gp you've gained or lost along with the return on investment over the time span

* The total combined profits your flips accrued during the interval

* The hourly profit you've obtained during your session

* Statistics over every single item you've flipped

* View the flip history behind every item that you've flipped

... and much more!

This update also features all new multi-account support! You can now view the stats or flips your individual accounts have made separately. To turn it on, head to the config menu and check "Enable multi account tracking".

We've also added a whole new margin check detection system. This means we now determine if trades are margin checks by making sure that it completed instantly (2 game ticks, one for the RuneScape server to receive the trade offer and one for when it completes). We're confident that this will be the most accurate representation of a margin check detection system and thusly we've removed the old margin freezing feature, as it no longer serves a purpose. 

Lastly, we've added a ton of small bug fixes and general optimizations of the plugin. Unfortunately, we've had to disable the cloud sync feature, which means all your flips and stats are now stored locally. This was due to the amount of data we're requiring with this new statistics tab being too large for the way we synced RuneLite data. This local data is stored within your RuneLite directory (/flipping/trades.json). If you're a heavy user of this plugin, this may become a pretty large file so it is recommended that you reset your panels once in a while. This can be done by right-clicking the reset button in the top right of the panel.

This update has been long in the making and it definitely took a lot of work behind the scenes. Therefore, I couldn't have done it without the help from [Zumaad](https://github.com/zumaad), whose amazing work on the backend really made it all possible. Big thanks to you, my friend! ❤

## v1.3.1 - Multi-client support and GE tracking links!

This update adds all new multi-client support, GE tracking links and various smaller additions and bug fixes.

### Multi-client support

Previously, trade data would get overwritten whenever you had multiple clients open, rendering the plugin unable to properly store data. With this update, the plugin can now communicate, store and extract historical trade data between multiple simultaneous client sessions. Big thanks to [Zumaad](https://github.com/zumaad) for developing this! :)

### GE tracking links

By right-clicking an item on the plugin panel, you can now choose to open the item's respective OSRS GE or [Platinum Tokens](https://platinumtokens.com/) page. This makes it easy for you to check out the price movements on the items you want to flip! We're open to add more trackers, so long as the websites are respectable (No ads for private servers, cheats or RWT etc.) and that their owners give us permission to link to their sites.

There may be some failed URLs when it comes to Platinum Tokens, particularly with items containing parentheses, so be sure to let me know if you come across any! :)

#### The small stuff

- Added warnings when clicking the reset button.

- Added reset button on the session timer. You can now right-click the session timer to reset it.

- Added additional indicators and tooltip explanations for items with unknown total GE limits.

- Fixed flips showing the wrong prices when margin checks had uneven buy/sell offers.

- Fixed inconsistencies with the statistics panel that caused sorting on the stat panel to also affect the sorting on the flipping panel.

- Fixed errors causing trades not to register properly.

### v1.3.2 - Account deletion and bug fixes

- This update adds a new settings menu to the account selector. For now, you'll only be able to delete your account's data from here, but we plan on creating more things to include on this panel.

- We've significantly improved the accuracy of the session timer.

- Btw's will no longer be picked up by the account cacher since they stand alone and cannot be expected to use the GE.

- We're also fixing some critical bugs that caused issues with registering and storing trades. We've not fully found a fix for the bug yet, however we've significantly reduced its disruptive behavior.

*If you notice any bugs or have any suggestions, let us know by making an [issue](https://github.com/Belieal/flipping-utilities/issues) or PM us on Discord (#Belieal6600) or (gaymersriseup#4026)! I'm also happy to answer any questions that you may have. :)*
