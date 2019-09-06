package com.marmot.framework;


import com.marmot.common.proxy.RpcCallProxy;
import com.marmot.demo.api.IMarmotDemoService;


public class Test {


    public static void main(String[] args) throws Exception {
    	
    	IMarmotDemoService proxy = RpcCallProxy.getProxy(IMarmotDemoService.class);
    	
    	for(int i=0;i<10;i++){
	    	Integer result = proxy.add(1, 2);
	    	
	    	System.out.println(result);
    	}
    	
    }
}
