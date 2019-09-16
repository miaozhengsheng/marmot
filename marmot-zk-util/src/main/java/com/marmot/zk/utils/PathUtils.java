package com.marmot.zk.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;

public class PathUtils {

	// 约定创建的节点名称为大小写英文字母、数字、下划线、中划线组成。字符全部为半角，第一个字符不能是中划线（框架保留）；节点最大长度100，最大层级为15
	private static Pattern pattern = Pattern.compile("([/][A-Za-z0-9_]([A-Za-z0-9_:\\-\\.&&[^/]]){0,99}){1,15}");
	private static String pathCheckErrorInfo = "node path is invalid(Can only contain letters, numbers, underline, minus;"
	            + " maximum number of levels(15); nodename's maximum length(100). path=";
	
	private PathUtils() throws IllegalAccessException{
		throw new IllegalAccessException("can not instance PathUtils");
	}
	
	public static String joinPath(EnumZKNameSpace nameSpace,String path){
		
		String joinedPath = null;
		if(null==nameSpace){
			return path;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(nameSpace.getNamespace());
		if(StringUtils.isNotBlank(path)){
			if(path.charAt(0)!='/'){
				builder.append("/");
			}
			builder.append(path.trim());
		}
		
		// 去掉最后的空格
		joinedPath = removeLastSlash(path.toString());
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
    
    /**
     * 创建更新节点时，验证节点名是否为空。如果为空返回false<br>
     * 
     * @param path 相对路径
     * @return true/false
     */
    public static boolean isEmptyPath(String path) {
        return StringUtils.isBlank(path) || "/".equals(path.trim());
    }
    /**
     * 验证节点路径。<br>
     * 约定创建的节点名为大小写英文字母、数字、下划线、中划线组成，字符全部为半角，第一个字符不能是下划线和中划线（先保留）;<br>
     * 而且最大深度和节点字符数不能超过最大限制(默认不超过15层级节点，节点名字符数不超过100)
     * 
     * @param path 节点全路径
     * @throws IllegalArgumentException if the path is invalid
     */
    public static void checkNodePath(String path) throws IllegalArgumentException {
        // 匹配失败
        if (ZKConstants.ZK_ROOT_NODE.equals(path)) {
            return;
        }
        if (!pattern.matcher(path).matches()) {
            throw new IllegalArgumentException(pathCheckErrorInfo + path);
        }
    }
    
    /**
     * 获取当前工程的客户端ID（来自conf.properties文件的project.client_id）
     * 
     * @return 当前工程的客户端ID
     */
    private static final String CLIENT_ID_KEY = "project.client_id";

    // 禁止使用ClientUtil.getClientId(),避免与框架互相调用
    public static String getCurrentClientId() {
        return PropUtil.getInstance().get(CLIENT_ID_KEY);
    }
}
