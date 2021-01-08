package com.flippingutilities.model;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public static final String MARGIN_BUY = "marg buy";
    public static final String MARGIN_SELL = "marg sell";
    String key;
    String property;
    String modifier;
    boolean isQuantityOption = true;

    public static Option defaultQuantityOption() {
        return new Option("", Option.GE_LIMIT, "+0", true);
    }

    public static Option defaultPriceOption() {
        return new Option("", Option.MARGIN_SELL,"+0", false);
    }
}
