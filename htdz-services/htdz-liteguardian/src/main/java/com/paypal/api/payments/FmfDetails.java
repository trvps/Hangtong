package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;







public class FmfDetails extends PayPalModel {

	/**
	 * Type of filter.
	 */
	private String filterType;

	/**
	 * Filter Identifier.
	 */
	private String filterId;

	/**
	 * Name of the filter
	 */
	private String name;

	/**
	 * Description of the filter.
	 */
	private String description;
}
