package com.flippingutilities.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@Data
@AllArgsConstructor
public class Option {
    Optional<String> key;
    Optional<String> property;
    Optional<String> change;

    public static Option emptyOption() {
        return new Option(Optional.empty(), Optional.empty(), Optional.empty());
    }
}
