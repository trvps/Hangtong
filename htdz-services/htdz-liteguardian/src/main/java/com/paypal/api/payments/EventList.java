package com.paypal.api.payments;

import java.util.ArrayList;
import java.util.List;

import com.paypal.base.rest.PayPalModel;


public class EventList  extends PayPalModel {

	/**
	 * A list of Webhooks event resources
	 */
	private List<Event> events;

	/**
	 * Number of items returned in each range of results. Note that the last results range could have fewer items than the requested number of items.
	 */
	private int count;

	/**
	 * 
	 */
	private List<Links> links;

	/**
	 * Default Constructor
	 */
	public EventList() {
		events = new ArrayList<Event>();
	}

}

