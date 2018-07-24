package com.paypal.api.payments;

import java.util.List;


public class TransactionBase extends CartBase {

	/**
	 * List of financial transactions (Sale, Authorization, Capture, Refund) related to the payment.
	 */
	private List<RelatedResources> relatedResources;

	/**
	 * Identifier to the purchase unit corresponding to this sale transaction.
	 */
	private String purchaseUnitReferenceId;


	/**
	 * Default Constructor
	 */
	public TransactionBase() {
	}
}
