package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;








public class FileAttachment extends PayPalModel {

	/**
	 * Name of the file attached.
	 */
	private String name;

	/**
	 * URL of the attached file that can be downloaded.
	 */
	private String url;

	/**
	 * Default Constructor
	 */
	public FileAttachment() {
	}

}
