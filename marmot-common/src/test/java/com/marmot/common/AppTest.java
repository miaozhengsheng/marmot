package com.marmot.common;

import java.util.List;

import org.apache.commons.lang.math.RandomUtils;

import com.marmot.common.zk.EnumZKNameSpace;
import com.marmot.common.zk.ZKConstants;
import com.marmot.common.zk.ZKUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
    
    public static void main(String[] args) throws Exception {
    	// 获取方法所在的工程的名称
    			String projectName = "base-project";

    			// 查询zk上该项目注册的服务
    			
    			List<String> listSubNodes = ZKUtil.getZkClient().listSubNodes(EnumZKNameSpace.PROJECT,ZKConstants.getProjectRpcNode(projectName));
    			
    			if(listSubNodes==null||listSubNodes.isEmpty()){
    				throw new Exception("无可用的节点");
    			}
    			
    			int nextInt = RandomUtils.nextInt(listSubNodes.size());
    			
    			String targetAddr = listSubNodes.get(nextInt);
    			
    			String[] split = targetAddr.split(":");
    			
    			String ip = split[0];
    			int port = Integer.parseInt(split[1]);
    			
    			System.out.println("注册的服务的IP为："+ip+" 端口为:"+port);
	}
}
