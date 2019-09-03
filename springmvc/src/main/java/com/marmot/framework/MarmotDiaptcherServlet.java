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
		
	    // ����ǰ��IPע�ᵽZK��������
		String ip = SystemUtil.getLocalIp();
		int port  = SystemUtil.getConnectorPort();
		ZKUtil.getZkClient().setTempNodeData(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("base-project/"+ip+":7777"));
	}

	@Override
	public void destroy() {
		super.destroy();
		System.out.println("����ֹͣ�С���������");
		// ɾ��ע�ᵽZK�ϵķ���ڵ�
		ZKUtil.getZkClient().deleteNormalNode(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("base-project"));
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
