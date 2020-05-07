package com.flippingutilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;


@Data
public class AccountData
{
	private Map<Integer, OfferInfo> lastOffers = new HashMap<>();
	private List<FlippingItem> trades = new ArrayList<>();
}
