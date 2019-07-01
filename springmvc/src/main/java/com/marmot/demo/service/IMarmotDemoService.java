package com.marmot.demo.service;

import com.marmot.common.rpc.annotation.MarmotInterface;
import com.marmot.common.rpc.annotation.MarmotMethod;

@MarmotInterface
public interface IMarmotDemoService {
	
	@MarmotMethod
	public String hello()throws Exception;

}
