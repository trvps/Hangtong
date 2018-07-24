package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;







public class OverrideChargeModel  extends PayPalModel {

	/**
	 * ID of charge model.
	 */
	private String chargeId;

	/**
	 * Updated Amount to be associated with this charge model.
	 */
	private Currency amount;

	/**
	 * Default Constructor
	 */
	public OverrideChargeModel() {
	}
}
