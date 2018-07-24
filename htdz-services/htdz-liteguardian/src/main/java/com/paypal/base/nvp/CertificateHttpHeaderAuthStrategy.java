package com.paypal.base.nvp;

import java.util.HashMap;
import java.util.Map;

import com.paypal.base.AbstractCertificateHttpHeaderAuthStrategy;
import com.paypal.base.Constants;
import com.paypal.base.credential.CertificateCredential;
import com.paypal.base.credential.TokenAuthorization;
import com.paypal.base.exception.OAuthException;
import com.paypal.base.util.OAuthSignature;

/**
 * <code>CertificateHttpHeaderAuthStrategy</code> is an implementation of
 * {@link AuthenticationStrategy} which acts on {@link CertificateCredential}
 * and retrieves them as HTTP headers
 * 
 */
public class CertificateHttpHeaderAuthStrategy extends
		AbstractCertificateHttpHeaderAuthStrategy {

	/**
	 * CertificateHttpHeaderAuthStrategy
	 * 
	 * @param endPointUrl
	 */
	public CertificateHttpHeaderAuthStrategy(String endPointUrl) {
		super(endPointUrl);
	}

	/**
	 * Processing for {@link TokenAuthorization} under
	 * {@link CertificateCredential}
	 * 
	 * @param credential
	 *            {@link CertificateCredential} instance
	 * @param tokenAuth
	 *            {@link TokenAuthorization} instance
	 * @return Map of HTTP headers
	 * @throws OAuthException
	 */
	protected Map<String, String> processTokenAuthorization(
			CertificateCredential credential, TokenAuthorization tokenAuth)
			throws OAuthException {
		Map<String, String> headers = new HashMap<String, String>();
		String authString = OAuthSignature.getFullAuthString(
				credential.getUserName(), credential.getPassword(),
				tokenAuth.getAccessToken(), tokenAuth.getTokenSecret(),
				OAuthSignature.HTTPMethod.POST, endPointUrl, null);
		headers.put(Constants.PAYPAL_AUTHORIZATION_PLATFORM_HEADER, authString);
		return headers;
	}

}
