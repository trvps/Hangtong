package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;


/*

*/
public class AgreementStateDescriptor  extends PayPalModel {

	/**
	 * Reason for changing the state of the agreement.
	 */
	private String note;

	/**
	 * The amount and currency of the agreement.
	 */
	private Currency amount;

	/**
	 * Default Constructor
	 */
	public AgreementStateDescriptor() {
	}

	/**
	 * Parameterized Constructor
	 */
	public AgreementStateDescriptor(Currency amount) {
		this.amount = amount;
	}
}
