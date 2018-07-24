package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;








public class PayerInfo extends PayPalModel {

	/**
	 * Email address representing the payer. 127 characters max.
	 */
	private String email;

	/**
	 * External Remember Me id representing the payer
	 */
	private String externalRememberMeId;

	/**
	 * @deprecated use {@link #buyerAccountNumber} instead
	 */
	@Deprecated
	private String accountNumber;

	/**
	 * Account Number representing the Payer
	 */
	private String buyerAccountNumber;

	/**
	 * Salutation of the payer.
	 */
	private String salutation;

	/**
	 * First name of the payer.
	 */
	private String firstName;

	/**
	 * Middle name of the payer.
	 */
	private String middleName;

	/**
	 * Last name of the payer.
	 */
	private String lastName;

	/**
	 * Suffix of the payer.
	 */
	private String suffix;

	/**
	 * PayPal assigned encrypted Payer ID.
	 */
	private String payerId;

	/**
	 * Phone number representing the payer. 20 characters max.
	 */
	private String phone;

	/**
	 * Phone type
	 */
	private String phoneType;

	/**
	 * Birth date of the Payer in ISO8601 format (yyyy-mm-dd).
	 */
	private String birthDate;

	/**
	 * Payer’s tax ID. Only supported when the `payment_method` is set to `paypal`.
	 */
	private String taxId;

	/**
	 * Payer’s tax ID type. Allowed values: `BR_CPF` or `BR_CNPJ`. Only supported when the `payment_method` is set to `paypal`.
	 */
	private String taxIdType;

	/**
	 * Two-letter registered country code of the payer to identify the buyer country.
	 */
	private String countryCode;

	/**
	 * Billing address of the Payer.
	 */
	private Address billingAddress;

	/**
	 * @deprecated  Use shipping address present in purchase unit or at root level of checkout Session.
	 */
	@Deprecated
	private ShippingAddress shippingAddress;

	/**
	 * Default Constructor
	 */
	public PayerInfo() {
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getExternalRememberMeId() {
		return externalRememberMeId;
	}

	public void setExternalRememberMeId(String externalRememberMeId) {
		this.externalRememberMeId = externalRememberMeId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getBuyerAccountNumber() {
		return buyerAccountNumber;
	}

	public void setBuyerAccountNumber(String buyerAccountNumber) {
		this.buyerAccountNumber = buyerAccountNumber;
	}

	public String getSalutation() {
		return salutation;
	}

	public void setSalutation(String salutation) {
		this.salutation = salutation;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getPayerId() {
		return payerId;
	}

	public void setPayerId(String payerId) {
		this.payerId = payerId;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPhoneType() {
		return phoneType;
	}

	public void setPhoneType(String phoneType) {
		this.phoneType = phoneType;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(String birthDate) {
		this.birthDate = birthDate;
	}

	public String getTaxId() {
		return taxId;
	}

	public void setTaxId(String taxId) {
		this.taxId = taxId;
	}

	public String getTaxIdType() {
		return taxIdType;
	}

	public void setTaxIdType(String taxIdType) {
		this.taxIdType = taxIdType;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public Address getBillingAddress() {
		return billingAddress;
	}

	public void setBillingAddress(Address billingAddress) {
		this.billingAddress = billingAddress;
	}

	public ShippingAddress getShippingAddress() {
		return shippingAddress;
	}

	public void setShippingAddress(ShippingAddress shippingAddress) {
		this.shippingAddress = shippingAddress;
	}

}
