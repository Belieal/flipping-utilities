package com.flippingutilities.ui.uiutilities;

import com.flippingutilities.model.FlippingItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.LinkBrowser;
import okhttp3.HttpUrl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
public class ItemLookUpPopup {
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final String[] PARENTHESIS_EXCLUDED_WORDS = {"empty", "sk", "lg", "dark", "dusk", "light", "unf", "uncharged", "wound", "full", "inactive"};

    /**
     * Builds the url required for opening the OSRS Exchange page for the item.
     * <p>
     * Example of a lobster's URL:
     * http://services.runescape.com/m=itemdb_oldschool/Runescape/viewitem?obj=6416
     *
     * @param itemId The item to be opened on the Exchange.
     * @return Returns the full URL for opening in the browser.
     */
    private static String buildOsrsExchangeUrl(int itemId)
    {
        String url = new HttpUrl.Builder()
                .scheme("http")
                .host("services.runescape.com")
                .addPathSegment("m=itemdb_oldschool")
                .addPathSegment("Runescape")
                .addPathSegment("viewitem")
                .addQueryParameter("obj", String.valueOf(itemId))
                .build()
                .toString();

        log.info("Opening OSRS Exchange: " + url);
        return url;
    }

    /**
     * Builds the url required for opening the Prices Runescaape wiki page for the item.
     * <p>
     * Example of a lobster's URL:
     * https://prices.runescape.wiki/osrs/item/379
     *
     * @param itemId The item to be opened on the Exchange.
     * @return Returns the full URL for opening in the browser.
     */
    private static String buildPricesRunescapeWiki(int itemId)
    {
        String url = new HttpUrl.Builder()
                .scheme("https")
                .host("prices.runescape.wiki")
                .addPathSegment("osrs")
                .addPathSegment("item")
                .addPathSegment(String.valueOf(itemId))
                .build()
                .toString();

        log.info("Opening Prices Runescape Wiki: " + url);
        return url;
    }

    /**
     * This method builds the https://platinumtokens.com (PT) url from the given itemName.
     * PT takes a slugged (Dragon dagger(p++) -> dragon-dagger-p-plus-plus) as its item query parameter.
     * This method therefore also slugs the item's name, however it's not perfect. There are some continuity errors
     * in the slug format used by the site (Rune armour set (sk) WON'T redirect to the item as the (sk) is
     * slugged as -sk even though every single other item on the website, with parentheses, are slugged like -s-k).
     * Thankfully, this just means the user will be directed to the base URL, so wouldn't be too disruptive for  the user.
     * <p>
     * Example of an item's url (Dragon dagger(p++)):
     * https://platinumtokens.com/item/dragon-dagger-p-plus-plus
     *
     * @param itemName The item's name to be opened on PT
     * @return Returns the URL for the item on PT
     */
    private static String buildPlatinumTokensUrl(String itemName)
    {
        //Determine if item name contains parentheses.
        String[] splitString = itemName.split("\\(");
        boolean containsParentheses = splitString.length != 1;
        if (containsParentheses)
        {
            if (Arrays.stream(PARENTHESIS_EXCLUDED_WORDS).parallel().noneMatch(itemName::contains))
            {
                //Every character inside parentheses need to be slugged.
                itemName = splitString[0] + splitString[1].replace("", "-");
            }
        }

        //'+' is slugged to "plus"
        itemName = itemName.replace("+", "plus");

        //All whitespaces are replaced with slugs
        String noWhitespace = WHITESPACE.matcher(itemName).replaceAll("-");
        //Normalize any characters not expected
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        //Remove all remaining parentheses or other symbols and check that we don't have any double slugs.
        String slug = NONLATIN.matcher(normalized).replaceAll("").replace("--", "-");

        if (containsParentheses)
        {
            if (Arrays.stream(PARENTHESIS_EXCLUDED_WORDS).parallel().noneMatch(itemName::contains))
            {
                //If we removed the parentheses earlier, we're guaranteed to have a trailing slug
                slug = slug.substring(0, slug.length() - 1);
            }
        }

        //Build the url
        String url = new HttpUrl.Builder()
                .scheme("https")
                .host("platinumtokens.com")
                .addPathSegment("item")
                .addPathSegment(slug.toLowerCase(Locale.ENGLISH))
                .build()
                .toString();

        log.info("Opening Platinum Tokens: " + url);
        return url;
    }

    public static JPopupMenu createGeTrackerLinksPopup(FlippingItem flippingItem)
    {
        final JMenuItem openOsrsGe = new JMenuItem("Open in OSRS Exchange");
        openOsrsGe.addActionListener(e -> LinkBrowser.browse(ItemLookUpPopup.buildOsrsExchangeUrl(flippingItem.getItemId())));

        //Opens the item's Platinum Tokens page
        final JMenuItem openPlatinumTokens = new JMenuItem("Open in PlatinumTokens.com");
        openPlatinumTokens.addActionListener(e -> LinkBrowser.browse(ItemLookUpPopup.buildPlatinumTokensUrl(flippingItem.getItemName())));

        //Opens the item's on Prices Runescape Wiki
        final JMenuItem openPricesRunescapeWiki = new JMenuItem("Open in prices.runescape.wiki.com");
        openPricesRunescapeWiki.addActionListener(e -> LinkBrowser.browse(ItemLookUpPopup.buildPricesRunescapeWiki(flippingItem.getItemID())));

        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
        popupMenu.add(openOsrsGe);
        popupMenu.add(openPlatinumTokens);
        popupMenu.add(openPricesRunescapeWiki);

        return popupMenu;
    }
}
