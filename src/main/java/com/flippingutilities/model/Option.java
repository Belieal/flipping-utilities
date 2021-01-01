package com.flippingutilities.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@Data
@AllArgsConstructor
public class Option {
    public static final String GE_LIMIT = "ge limit";
    public static final String REMAINING_LIMIT = "rem limit";
    public static final String CASHSTACK = "cashstack";
    String key;
    String property;
    String change;

    public static Option defaultOption() {
        return new Option("", Option.GE_LIMIT, "+0");
    }
}
