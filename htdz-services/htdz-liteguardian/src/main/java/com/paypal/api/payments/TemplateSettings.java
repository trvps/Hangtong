package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;


public class TemplateSettings extends PayPalModel {

	/**
	 * The field name (for any field in template_data) for which the corresponding display preferences will be mapped to.
	 */
	private String fieldName;

	/**
	 * Settings metadata for each field.
	 */
	private TemplateSettingsMetadata displayPreference;

	/**
	 * Default Constructor
	 */
	public TemplateSettings() {
	}

}
