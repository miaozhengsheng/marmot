package com.marmot.framework;



import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.DispatcherServlet;

import com.marmot.common.system.SystemUtil;
import com.marmot.common.util.PropUtil;
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
		ZKUtil.getZkClient().setTempNodeData(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode(PropUtil.getInstance().get("project-name")+"/"+ip+":7777"));
	}

	@Override
	public void destroy() {
		super.destroy();
		System.out.println("服务停止中。。。。。");
		// 删除注册到ZK上的服务节点
		ZKUtil.getZkClient().deleteNormalNode(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("base-project"));
	}


}
