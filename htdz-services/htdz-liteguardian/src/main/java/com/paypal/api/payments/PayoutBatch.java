package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;





import java.util.List;




public class PayoutBatch extends PayPalModel {

	/**
	 * A batch header. Includes the generated batch status.
	 */
	private PayoutBatchHeader batchHeader;

	/**
	 * An array of items in a batch payout.
	 */
	private List<PayoutItemDetails> items;

	/**
	 *
	 */
	private List<Links> links;

	/**
	 * Default Constructor
	 */
	public PayoutBatch() {
	}

	/**
	 * Parameterized Constructor
	 */
	public PayoutBatch(PayoutBatchHeader batchHeader, List<PayoutItemDetails> items) {
		this.batchHeader = batchHeader;
		this.items = items;
	}
}
