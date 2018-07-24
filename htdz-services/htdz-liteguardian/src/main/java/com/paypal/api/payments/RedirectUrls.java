package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;








public class RedirectUrls extends PayPalModel {

	/**
	 * Url where the payer would be redirected to after approving the payment. **Required for PayPal account payments.**
	 */
	private String returnUrl;

	/**
	 * Url where the payer would be redirected to after canceling the payment. **Required for PayPal account payments.**
	 */
	private String cancelUrl;

	/**
	 * Default Constructor
	 */
	public RedirectUrls() {
	}

}
