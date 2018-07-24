package com.htdz.reg.controllers;


import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htdz.common.BaseController;
import com.htdz.common.LogManager;


@RestController
public class APIController {

	@RequestMapping("/info")
	public String info() {
		return "Reg APIController";
	}
	
	
	@RequestMapping("/reg/test")
	public String test(HttpServletRequest request) {
		String print = BaseController.printRequestHeader(request);
		LogManager.info(print);
		
		byte[] reqBody = BaseController.readRequestBody(request);
		if (reqBody != null)
			try {
				print += "\r\n" + new String(reqBody, "utf-8");
			} catch (UnsupportedEncodingException e) {
				LogManager.exception(e.getMessage(), e);
			}
		
		return print;
	}
}
