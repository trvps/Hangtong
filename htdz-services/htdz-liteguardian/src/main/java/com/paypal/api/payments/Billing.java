package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;



public class Billing extends PayPalModel {

	/**
	 * Identifier of the instrument in PayPal Wallet
	 */
	private String billingAgreementId;

	/**
	 * Selected installment option for issuer based installments (BR and MX).
	 */
	private InstallmentOption selectedInstallmentOption;

	/**
	 * Default Constructor
	 */
	public Billing() {
	}

}
