package com.marmot.remote.demo;


import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
@RequestMapping("/testcontroller")
public  class TestAbstractController extends AbstractParamterController{

	@RequestMapping("/test2")
	@Override
	public void test2(String name,HttpServletResponse response) throws IOException {
		System.out.println("test2:"+name);
		response.getWriter().write("test2:"+name);
	}

	
	
}
