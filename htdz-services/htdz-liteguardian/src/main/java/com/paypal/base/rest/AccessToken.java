package com.paypal.base.rest;

public class AccessToken {

    private String accessToken;
    private long expires = 0;
    
    public AccessToken (String accessToken, long expires){
    	this.accessToken = accessToken;
    	this.expires = expires;
    }

    public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public long getExpires() {
		return expires;
	}

	public void setExpires(long expires) {
		this.expires = expires;
	}

	/**
     * Specifies how long this token can be used for placing API calls. The
     * remaining lifetime is given in seconds.
     *
     * @return remaining lifetime of this access token in seconds
     */
    public long expiresIn() {
        return expires - new java.util.Date().getTime() / 1000;
    }

}
