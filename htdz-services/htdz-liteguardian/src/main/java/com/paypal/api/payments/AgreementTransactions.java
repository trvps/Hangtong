package com.paypal.api.payments;

import com.paypal.base.rest.PayPalModel;


import java.util.ArrayList;
import java.util.List;


public class AgreementTransactions  extends PayPalModel {

	/**
	 * Array of agreement_transaction object.
	 */
	private List<AgreementTransaction> agreementTransactionList;

	/**
	 * Default Constructor
	 */
	public AgreementTransactions() {
		agreementTransactionList = new ArrayList<AgreementTransaction>();
	}
}
