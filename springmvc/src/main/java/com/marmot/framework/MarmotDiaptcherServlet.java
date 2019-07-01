package com.marmot.framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;

import com.marmot.common.rpc.bean.MarmotRpcBean;
import com.marmot.framework.util.RPCUtil;

public class MarmotDiaptcherServlet extends DispatcherServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doDispatch(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		String uri = request.getRequestURI();
		
		boolean isRpc = isRpcRequest(uri);
		if(isRpc){
			System.out.println("����RPC���󣬵�������");
			
			
			String tmp = uri.replace("/RPC","");
			if(!tmp.endsWith("/")){
				tmp = tmp+"/";
			}
			
			MarmotRpcBean marmotRpcBean = RPCUtil.RPC_MAPPER.get(tmp);
			if(null==marmotRpcBean){
				throw new Exception("����Ľӿڲ����ڡ�"+uri);
			}
			
			return;
		}
		
		super.doDispatch(request, response);
	}
	
	
	/**
	 * �Ƿ���RPC����
	 * @param url
	 * @return
	 */
	private boolean isRpcRequest(String url){
		
		if(StringUtils.isEmpty(url)){
			return false;
		}
		
		if(url.startsWith("/RPC")){
			return true;
		}
		
		return false;
	}
	

}
