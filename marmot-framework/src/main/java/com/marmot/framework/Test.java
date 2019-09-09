package com.marmot.framework;


import com.marmot.common.proxy.RpcCallProxy;
import com.marmot.remote.service.IRemoteService;


public class Test {


    public static void main(String[] args) throws Exception {
    	
    	IRemoteService proxy = RpcCallProxy.getProxy(IRemoteService.class);
    	
    	for(int i=0;i<10;i++){
    		proxy.del();
    	}
    	
    }
}
