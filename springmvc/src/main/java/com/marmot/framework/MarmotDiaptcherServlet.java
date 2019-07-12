package com.marmot.framework;



import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;

import com.marmot.common.rpc.bean.MarmotRpcBean;
import com.marmot.common.system.SystemUtil;
import com.marmot.common.util.JsonUtil;
import com.marmot.common.zk.EnumZKNameSpace;
import com.marmot.common.zk.ZKConstants;
import com.marmot.common.zk.ZKUtil;
import com.marmot.framework.remote.RemoteCallDealer;
import com.marmot.framework.remote.RequestOutput;
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
			
			String tmp = uri.replace("/RPC","");
			if(!tmp.endsWith("/")){
				tmp = tmp+"/";
			}
			
			MarmotRpcBean marmotRpcBean = RPCUtil.RPC_MAPPER.get(tmp);
			if(null==marmotRpcBean){
				throw new Exception("请求的接口不存在。"+uri);
			}
			try{
				Object result = RemoteCallDealer.deal(marmotRpcBean, request);
				
				RequestOutput resuOutput = new RequestOutput();
				resuOutput.setData(result);
				resuOutput.setMessage("ok");
				resuOutput.setStatus(1);
				response.getWriter().write(JsonUtil.toJson(resuOutput));
			}catch(Exception e){
				RequestOutput resuOutput = new RequestOutput();
				resuOutput.setMessage(e.getMessage());
				resuOutput.setStatus(0);
				response.getWriter().write(JsonUtil.toJson(resuOutput));
			}
			
			
			return;
		}
		
		super.doDispatch(request, response);
	}
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
	    // 将当前的IP注册到ZK服务器上
		
		String ip = SystemUtil.getLocalIp();
		int port  = SystemUtil.getConnectorPort();
		ZKUtil.getZkClient().setTempNodeData(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("test/"+ip+":"+port));
	}

	@Override
	public void destroy() {
		super.destroy();
		System.out.println("服务停止中。。。。。");
		// 删除注册到ZK上的服务节点
		ZKUtil.getZkClient().deleteTempNode(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("test")+"/"+SystemUtil.getLocalIp()+":"+SystemUtil.getConnectorPort());
	}

	/**
	 * 是否是RPC请求
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
