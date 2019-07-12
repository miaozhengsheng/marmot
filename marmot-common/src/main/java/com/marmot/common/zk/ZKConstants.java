package com.marmot.common.zk;

public class ZKConstants {
	
	public static final String ROOT = "/config";
	
	public static final String PUBLIC = "/public";
	
	public static final String RPC = "/rpc";
	
	public static final String RPC_REIGISTRY = "/registry";
	
	
	public static String getProjectRpcNode(String projectName){
		return RPC+RPC_REIGISTRY+"/"+projectName;
	}
	
	

}
