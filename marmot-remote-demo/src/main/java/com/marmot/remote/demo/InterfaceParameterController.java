package com.marmot.remote.demo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;

public interface InterfaceParameterController {

	@RequestMapping("/test1")
	public void test1(String name,HttpServletResponse response) throws IOException;
}
