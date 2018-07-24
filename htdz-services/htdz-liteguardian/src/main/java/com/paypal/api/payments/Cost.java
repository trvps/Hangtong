package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;


public class Cost extends PayPalModel {

	/**
	 * Cost in percent. Range of 0 to 100.
	 */
	private float percent;

	/**
	 * The cost, as an amount. Valid range is from 0 to 1,000,000.
	 */
	private Currency amount;

	/**
	 * Default Constructor
	 */
	public Cost() {
	}
}
