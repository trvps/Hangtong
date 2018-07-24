package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;








public class Phone extends PayPalModel {

	/**
	 * Country code (from in E.164 format)
	 */
	private String countryCode;

	/**
	 * In-country phone number (from in E.164 format)
	 */
	private String nationalNumber;

	/**
	 * Phone extension
	 */
	private String extension;

	/**
	 * Default Constructor
	 */
	public Phone() {
	}

	/**
	 * Parameterized Constructor
	 */
	public Phone(String countryCode, String nationalNumber) {
		this.countryCode = countryCode;
		this.nationalNumber = nationalNumber;
	}
}
