package com.paypal.api.payments;

import java.util.List;


public class Transaction extends TransactionBase {

	/**
	 * Financial transactions related to a payment.
	 */
	private List<Transaction> transactions;

	/**
	 * Default Constructor
	 */
	public Transaction() {
	}

}
