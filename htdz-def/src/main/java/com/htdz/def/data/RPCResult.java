package com.htdz.def.data;


import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.alibaba.fastjson.JSONObject;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class RPCResult implements Serializable {
	private static final long serialVersionUID = 1L;

	// RPC业务层面的通信结果码，不是用户业务逻辑层面的结果码
	private Integer rpcErrCode = Errors.ERR_SUCCESS;
	// RPC业务层面的通信结果描述，不是用户业务逻辑层面的结果描述
	private String rpcErrMsg = Errors.getErrorMsg(Errors.ERR_SUCCESS);

	// 用户业务逻辑处理结果
	private Object rpcResult = null;

	public static RPCResult success() {
		return new RPCResult(Errors.ERR_SUCCESS,
				Errors.getErrorMsg(Errors.ERR_SUCCESS), null);
	}

	public static RPCResult failed() {
		return new RPCResult(Errors.ERR_FAILED,
				Errors.getErrorMsg(Errors.ERR_FAILED), null);
	}

	public static RPCResult ipAuthFailed() {
		return new RPCResult(
				Errors.ERR_IPAUTHFAILED,
				Errors.getErrorMsg(Errors.ERR_IPAUTHFAILED), null);
	}

	public static RPCResult serviceRefuse() {
		return new RPCResult(
				Errors.ERR_SERVICEREFUSE,
				Errors.getErrorMsg(Errors.ERR_SERVICEREFUSE), null);
	}

	public String toString() {
		JSONObject jsonobject = new JSONObject();
		jsonobject.put("code", rpcErrCode);
		jsonobject.put("msg", rpcErrMsg);
		jsonobject.put("data", resultToString());

		return jsonobject.toString();
	}

	public String resultToString() {
		try {
			if (rpcResult != null) {
				if (rpcResult instanceof byte[])
					return DataUtil.bytesToString((byte[]) rpcResult);
				else
					return rpcResult.toString();
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return "";
	}

	public byte[] resultToBytes() {
		try {
			if (rpcResult != null) {
				if (rpcResult instanceof byte[])
					return (byte[]) rpcResult;
				else
					return rpcResult.toString().getBytes("utf-8");
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return null;
	}
}
