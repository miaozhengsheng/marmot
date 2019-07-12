package com.marmot.common.zk;

import org.springframework.util.StringUtils;

import com.marmot.common.zk.client.IZKClient;
import com.marmot.common.zk.client.impl.ZKClientImpl;

public class ZKUtil {
	
	
	public static IZKClient getZkClient(){
		return ZKClientImpl.getInstance();
	}
	
	 /**
     * 拼接ZK节点path
     * 
     * @param namespace 命名空间EnumZKNameSpace类型
     * @param path 节点相对路径
     * @return 拼接完后的全路径
     */
    public static String joinPath(EnumZKNameSpace namespace, String path) {
        String joinedPath = null;
        if (namespace == null) {
            return path;
        }
        StringBuilder fullPathSB = new StringBuilder(namespace.getNamespace());
        if (!StringUtils.isEmpty(path)) {
            if (path.charAt(0) != '/') {
                fullPathSB.append("/");
            }
            fullPathSB.append(path.trim());
        }
        // 去掉路径最后的"/"符号
        joinedPath = removeLastSlash(fullPathSB.toString());
        return joinedPath;
    }

    /**
     * 去掉路径最后的"/"
     * 
     * @param path 节点相对路径
     * @return 去掉最后斜杠后的全路径
     */
    public static String removeLastSlash(String path) {
        if (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
