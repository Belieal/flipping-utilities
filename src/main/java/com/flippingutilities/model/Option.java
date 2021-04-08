package com.flippingutilities.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
//need a no args constructor if you want field defaults to be respected when json is turned into object and its missing the field (when the field is
//newly added for example).
@NoArgsConstructor
public class Option {
    public static final String GE_LIMIT = "ge limit";
    public static final String REMAINING_LIMIT = "rem limit";
    public static final String CASHSTACK = "cashstack";
    public static final String LAST_BUY = "last buy";
    public static final String LAST_SELL = "last sell";
    public static final String INSTA_SELL = "insta sell";
    public static final String INSTA_BUY = "insta buy";
    public static final String WIKI_BUY = "wiki buy";
    public static final String WIKI_SELL = "wiki sell";
    public static final String[] QUANTITY_OPTIONS = new String[]{Option.REMAINING_LIMIT, Option.GE_LIMIT, Option.CASHSTACK};
    public static final String[] PRICE_OPTIONS = new String[]{Option.WIKI_BUY, Option.WIKI_SELL, Option.INSTA_SELL, Option.INSTA_BUY, Option.LAST_BUY, Option.LAST_SELL};
    String key;
    String property;
    String modifier;
    boolean isQuantityOption = true;

    public static Option defaultQuantityOption() {
        return new Option("", Option.GE_LIMIT, "+0", true);
    }

    public static Option defaultPriceOption() {
        return new Option("", Option.INSTA_BUY,"+0", false);
    }
}
