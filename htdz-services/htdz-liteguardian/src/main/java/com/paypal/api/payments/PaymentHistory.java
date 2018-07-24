package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;





import java.util.List;




public class PaymentHistory extends PayPalModel {

	/**
	 * A list of Payment resources
	 */
	private List<Payment> payments;

	/**
	 * Number of items returned in each range of results. Note that the last results range could have fewer items than the requested number of items. Maximum value: 20.
	 */
	private int count;

	/**
	 * Identifier of the next element to get the next range of results.
	 */
	private String nextId;

	/**
	 * Default Constructor
	 */
	public PaymentHistory() {
	}

}
