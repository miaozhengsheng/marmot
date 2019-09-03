package com.marmot.framework;



import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;

import com.marmot.common.system.SystemUtil;
import com.marmot.common.zk.EnumZKNameSpace;
import com.marmot.common.zk.ZKConstants;
import com.marmot.common.zk.ZKUtil;

public class MarmotDiaptcherServlet extends DispatcherServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doDispatch(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		super.doDispatch(request, response);
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
	    // 将当前的IP注册到ZK服务器上
		String ip = SystemUtil.getLocalIp();
		int port  = SystemUtil.getConnectorPort();
		ZKUtil.getZkClient().setTempNodeData(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("base-project/"+ip+":7777"));
	}

	@Override
	public void destroy() {
		super.destroy();
		System.out.println("服务停止中。。。。。");
		// 删除注册到ZK上的服务节点
		ZKUtil.getZkClient().deleteNormalNode(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("base-project"));
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
