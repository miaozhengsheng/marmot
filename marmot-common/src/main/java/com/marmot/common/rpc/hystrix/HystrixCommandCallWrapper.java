package com.marmot.common.rpc.hystrix;


import java.lang.reflect.Method;
import com.netflix.hystrix.HystrixCommand;

public class HystrixCommandCallWrapper extends HystrixCommand<Object> {
	
	
	// 加载本地所有的可能调用的RPC服务
	
	protected Method method;
    protected Object[] args;

    public HystrixCommandCallWrapper(Method method, Object[] args) {
        super(null,null);
        this.method = method;
        this.args = args;
    }

	@Override
	protected Object run() throws Exception {
		return null;
	}

}
