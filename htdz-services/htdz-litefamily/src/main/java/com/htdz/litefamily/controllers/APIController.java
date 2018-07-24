package com.htdz.litefamily.controllers;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class APIController {
	
	@RequestMapping("/info")
	public String info() {
		return "Reg APIController";
	}
}
