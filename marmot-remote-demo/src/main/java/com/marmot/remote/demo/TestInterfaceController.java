package com.marmot.remote.demo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/interface")
@Controller
public class TestInterfaceController implements InterfaceParameterController {

	@Override
	public void test1(String name,HttpServletResponse response) throws IOException {
		response.getWriter().write("test1:"+name);
	}

}
