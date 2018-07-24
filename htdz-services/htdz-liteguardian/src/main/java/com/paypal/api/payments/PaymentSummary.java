package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;








public class PaymentSummary extends PayPalModel {

	/**
	 * Total Amount paid/refunded via PayPal.
	 */
	private Currency paypal;

	/**
	 * Total Amount paid/refunded via other sources.
	 */
	private Currency other;

	/**
	 * Default Constructor
	 */
	public PaymentSummary() {
	}

}
