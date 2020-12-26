package com.flippingutilities;

import com.flippingutilities.model.Option;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

public class OptionManager {
    private List<Option> options;
    //allowed keys are any keys that can't be typed into the quantity/price editor. Not allowed keys are m,k,b
    private final Set<String> allowedKeys = new HashSet<>(Arrays.asList("p","l","m","o","n","i","j","u","h","v","y","g","c","t","f","x","r","d","z","e","s","w","a","q"));

    public Set<String> getAvailableKeys() {
        Set<String> usedKeys = options.stream().map(Option::getKey).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        return Sets.difference(allowedKeys, usedKeys).copyInto(new HashSet<>());
    }
}
