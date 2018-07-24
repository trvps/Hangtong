package com.htdz.def.interfaces;


import java.util.Map;

import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;


public interface RegService {
	public RPCResult hanleHttpRequest(RouteInfo ri, 
										String path, 
										Map<String, String> headers, 
										Map<String, String[]> params, 
										byte[] reqBody);
	
	public RPCResult hanleRegService (String method, 
										Map<String, String> params);
}
