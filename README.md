# Flipping Utilities plugin for RuneLite
This external plugin seeks to help users with flipping in OSRS. It provides the latest prices and calculates profits according to the user's marginchecks. It also keeps track of how many remaining items you can buy in the 4-hour GE limit and when they will expire.

**Disclaimer: This is not an OSBuddy Exchange replacement, as it does not send or receive price data from other users.**

## How it works
The plugin is designed to be "hands-free". This means you can just margin check an item you want to flip and the plugin will detect, store and display prices along with calculating the profits and return on investments (ROI).

**The plugin will only record bought and sold trades with a quantity of one.** I hope to address this in a future update.

![](demo.gif)

You can use this box to quickly determine if the item is worth it to flip.

The plugin also detects when you are setting up an offer and will highlight that item in the panel such that it is easy to read the relevant information when setting up your offers. If you want to look at a specific item you've flipped before, you can easily just do a quick search on the search bar at the top of the panel that will then show the item below.

This plugin also features a lot of handy tooltips that will show you important information such as:
* GE limit tracking, so you can see the remaining items you can buy in the limit along with when the limit expires.
<p align="center">
  <img src="https://i.imgur.com/DPT0AXv.png">
</p>

* Time of latest margin check, so that you can make sure your prices are up-to-date.
<p align="center">
  <img src="https://i.imgur.com/E2xPEju.png">
</p>

* Text that function as buttons such as margin freezing.
<p align="center">
  <img src="https://i.imgur.com/Oi7mMq4.png">
</p>

Lastly, a widget has recently been added to help you set up your offers quickly! The widget will show up in the chatbox when you want to set the price of your item, if you've margin checked it beforehand.

<p align="center">
  <img src="https://i.imgur.com/mATjKuo.png">
</p>

Simply click the red text and press enter. 

## Potential future features
* Statistics tab that show how much profit you've earned during a time frame.
* Trade history
* Optional notification when a GE limit expires.
* Favorited items.
* When setting up buy offers, provide an option to set quantity to only what you have gold for.
* Collapsing trades after the GE limit has been bought.
* Grouped checklist tab with import and export features so you can keep track of the items you like to flip most!

## Support
This is my first RuneLite project/contribution and there are guaranteed to be mistakes. But I am determined to improve and better my skills so therefore, if you notice any bugs, have any suggestions or want to contribute to the plugin, I would appreciate it a lot if you opened an issue here or messaged me online. (RSN: Beliael) You can also contact me on Discord at Belieal#6600.

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

## v1.3 - Statistics tab update (Coming soon!)
