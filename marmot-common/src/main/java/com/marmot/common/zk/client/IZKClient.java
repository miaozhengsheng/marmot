package com.marmot.common.zk.client;

import java.util.List;

import org.apache.zookeeper.data.Stat;

import com.marmot.common.zk.EnumZKNameSpace;

public interface IZKClient {

	/**
	 * 创建ZK节点 父节点不存在则创建
	 * 
	 * @param space
	 * @param path
	 */
	public void createNode(EnumZKNameSpace space, String path);

	/**
	 * 判断节点是否存在
	 * 
	 * @param namespace
	 * @param path
	 * @return
	 */
	boolean exists(EnumZKNameSpace namespace, String path);

	/**
	 * @param namespace
	 * @param path
	 * @return
	 */
	boolean setTempNodeData(EnumZKNameSpace namespace, String path);

	/**
	 * 删除临时节点
	 * 
	 * @param namespace
	 * @param path
	 * @return
	 */
	public boolean deleteTempNode(EnumZKNameSpace namespace, String path);
	/**
	 * 删除普通节点
	 * @param namespace
	 * @param path
	 * @return
	 */
	public boolean deleteNormalNode(EnumZKNameSpace namespace, String path);

	/**
	 * 得到节点的状态
	 * @param fullPath
	 * @return
	 */
	public Stat getStat(String fullPath);
	
	/**
	 * @param nameSpace
	 * @param path
	 * @return
	 * @throws Exception 
	 */
	public List<String> listSubNodes(EnumZKNameSpace nameSpace,String path) throws Exception;

}
