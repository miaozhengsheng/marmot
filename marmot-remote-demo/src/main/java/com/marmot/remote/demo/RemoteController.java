package com.marmot.remote.demo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.marmot.common.proxy.RpcCallProxy;
import com.marmot.demo.api.IMarmotDemoService;

@RestController
@RequestMapping("/remote")
public class RemoteController {


	@ResponseBody
	@RequestMapping("/call")
	public String remoteCall(String name) throws Exception{
		return RpcCallProxy.getProxy(IMarmotDemoService.class).hello(name);
	}
}

