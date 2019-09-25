package com.marmot.remote.demo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;


public abstract class AbstractParamterController{
	@RequestMapping("/test1")
	public void test1(String name,HttpServletResponse response) throws IOException{
		System.out.println(name);
		response.getWriter().write("test1:"+name);
	}
	
	public abstract void test2(String name,HttpServletResponse response) throws IOException;
}
