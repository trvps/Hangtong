package com.htdz.def.data;


import com.alibaba.fastjson.JSONObject;


public class Errors {
	public static final int ERR_SUCCESS 					= 0;	// 成功		
	public static final int ERR_FAILED 						= 1;	// 失败
	public static final int ERR_IPAUTHFAILED 				= 2;	// IP鉴权失败
	public static final int ERR_ACCTAUTHFAILED 			= 3;	// 账户鉴权失败
	public static final int ERR_SERVICEREFUSE				= 4;	// 服务被拒绝

	
	public static String success() {
		return jsonResponse(ERR_SUCCESS);
	}
	
	public static String success(String smsid) {
		return jsonResponse(ERR_SUCCESS, smsid);
	}
	
	public static String failed() {
		return jsonResponse(ERR_FAILED);
	}
	
	public static String ipAuthFailed() {
		return jsonResponse(ERR_IPAUTHFAILED);
	}
	
	public static String acctAuthFailed() {
		return jsonResponse(ERR_ACCTAUTHFAILED);
	}
	
	public static String serviceRefuse() {
		return jsonResponse(ERR_SERVICEREFUSE);
	}
	
	
	public static String jsonResponse(int err) {
		JSONObject jsonobject = new JSONObject();
		
		try {
			jsonobject.put("code", err);
			jsonobject.put("msg", getErrorMsg(err));
		} catch (Exception e) {
		}
		
		return jsonobject.toString();
	}
	
	public static String jsonResponse(int err, String smsid) {
		JSONObject jsonobject = new JSONObject();
		
		try {
			jsonobject.put("code", err);
			jsonobject.put("msg", getErrorMsg(err));
			jsonobject.put("smsid", smsid);
		} catch (Exception e) {
		}
		
		return jsonobject.toString();
	}
	
	public static String jsonResponseException(int err, String exception) {
		JSONObject jsonobject = new JSONObject();
		
		try {
			jsonobject.put("code", err);
			jsonobject.put("msg", getErrorMsg(err));
			jsonobject.put("exception", exception);
		} catch (Exception e) {
		}
		
		return jsonobject.toString();
	}
	
	public static String getErrorMsg(int err) {
		switch (err) {
			case ERR_SUCCESS:				return "成功";
			case ERR_FAILED:				return "失败";
			case ERR_IPAUTHFAILED:		return "IP鉴权失败";
			case ERR_ACCTAUTHFAILED:		return "账户鉴权失败";
			case ERR_SERVICEREFUSE:		return "服务被拒绝";
		}
		
		return "未知错误";
	}
}

