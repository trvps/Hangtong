package com.htdz.device.controllers;


import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htdz.common.LogManager;
import com.htdz.def.data.Errors;



@RestController
public class APIController {
	
	
	@RequestMapping("/info")
	public String info() {
		return "Device APIController";
	}
}
