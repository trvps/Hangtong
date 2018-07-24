package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;








public class Participant extends PayPalModel {

	/**
	 * The participant email address.
	 */
	private String email;

	/**
	 * The participant first name.
	 */
	private String firstName;

	/**
	 * The participant last name.
	 */
	private String lastName;

	/**
	 * The participant company business name.
	 */
	private String businessName;

	/**
	 * The participant phone number.
	 */
	private Phone phone;

	/**
	 * The participant fax number.
	 */
	private Phone fax;

	/**
	 * The participant website.
	 */
	private String website;

	/**
	 * Additional information, such as business hours.
	 */
	private String additionalInfo;

	/**
	 * The participant address.
	 */
	private InvoiceAddress address;

	/**
	 * Default Constructor
	 */
	public Participant() {
	}

	/**
	 * Parameterized Constructor
	 */
	public Participant(String email) {
		this.email = email;
	}
}
