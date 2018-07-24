package com.htdz.def.data;

import java.io.Serializable;

import com.alibaba.fastjson.JSONObject;
import com.htdz.common.utils.DataUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResult implements Serializable {
	private static final long serialVersionUID = 1L;

	public Integer code = Errors.ERR_FAILED;
	public String msg = null;
	public Object data = null;

	// 成功
	private static ApiResult success = new ApiResult(Errors.ERR_SUCCESS, Errors.getErrorMsg(Errors.ERR_SUCCESS),
			Errors.success());
	// 失败
	private static ApiResult failed = new ApiResult(Errors.ERR_FAILED, Errors.getErrorMsg(Errors.ERR_FAILED),
			Errors.failed());

	public static ApiResult success() {
		return success;
	}

	public static ApiResult failed() {
		return failed;
	}

	@Override
	public String toString() {
		JSONObject jsonobject = new JSONObject();
		jsonobject.put("code", code);
		jsonobject.put("msg", msg);
		jsonobject.put("data", resultToString());

		return jsonobject.toString();
	}

	@Data
	private static class ApiResultInfo {
		public Integer code;
		public String what;
		public Object ret;
	}

	public ApiResultInfo ApiResultRenameMember() {
		ApiResultInfo apiResultInfo = new ApiResultInfo();

		apiResultInfo.code = code;
		apiResultInfo.what = msg;
		apiResultInfo.ret = data;
		return apiResultInfo;
	}

	public String resultToString() {
		try {
			if (data != null) {
				if (data instanceof byte[])
					return DataUtil.bytesToHexString((byte[]) data, null, null, ", ", true);
				else
					return data.toString();
			}
		} catch (Exception e) {
		}

		return "";
	}

	public byte[] resultToBytes() {
		try {
			if (data != null) {
				if (data instanceof byte[])
					return (byte[]) data;
				else
					return data.toString().getBytes("utf-8");
			}
		} catch (Exception e) {
		}

		return null;
	}
}
