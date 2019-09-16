package com.marmot.framework;



import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.DispatcherServlet;

import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.utils.ZookeeperFactory;


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
		
	}

	@Override
	public void destroy() {
		super.destroy();
		try {
			ZookeeperFactory.useDefaultZookeeper().deleteNode(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("base-project"));
		} catch (ZookeeperException e) {
			e.printStackTrace();
		}
	}


}
