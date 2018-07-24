package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;





import java.util.List;




public class Notification extends PayPalModel {

	/**
	 * Subject of the notification.
	 */
	private String subject;

	/**
	 * Note to the payer.
	 */
	private String note;

	/**
	 * Indicates whether to send a copy of the email to the merchant.
	 */
	private Boolean sendToMerchant;

	/**
	 * Applicable for invoices created with Cc emails. If this field is not in the body, all the cc email addresses added as part of the invoice shall be notified else this field can be used to limit the list of email addresses. Note: additional email addresses are not supported.
	 */
	private List<String> ccEmails;

	/**
	 * Default Constructor
	 */
	public Notification() {
	}
}
