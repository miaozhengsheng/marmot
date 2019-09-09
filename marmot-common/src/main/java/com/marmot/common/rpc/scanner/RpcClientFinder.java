package com.marmot.common.rpc.scanner;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.util.CollectionUtils;

import com.marmot.common.rpc.annotation.MarmotInterface;
import com.marmot.common.util.PropUtil;

public class RpcClientFinder {

	private static final RpcClientFinder instance = new RpcClientFinder();
	
	// 提供的服务
	private static final List<Class<?>> INTERFACECLASS = new ArrayList<Class<?>>();
	private static final List<File> INTERFACECLASS_JARFILE = new ArrayList<File>();

	private static final Map<Class<?>, String> REMOTE_INFO = new HashMap<Class<?>,String>();
	
	// 更具远程服务的名称 获取工程名称
	public static String getRemoteClient(Class<?> clazz){
		return REMOTE_INFO.get(clazz);
	}
	
	public static List<Class<?>> getLocalServiceClass(){
		return INTERFACECLASS;
	}
	
	
	private RpcClientFinder() {

	}

	public static RpcClientFinder getInstance() {
		return instance;
	}
	
	/**
     * 加载lib包下的所有客户端jar
     */
    public void load() {
    	
    	List<File> remoteClientFiles = loadLib();
    	
        if (CollectionUtils.isEmpty(remoteClientFiles)) {
        	remoteClientFiles =  loadClassPath();
        }

        // 本地要提供的服务
       initInterfaceClazz();
       // 有可能需要调用到的服务
       initRpcInterfaceClazz(remoteClientFiles);
    }
    
    private static void initInterfaceClazz(){
    	 if(!CollectionUtils.isEmpty(INTERFACECLASS_JARFILE)){
         	for(File file:INTERFACECLASS_JARFILE){
         		JarFile jarPath = null;
         		try {
 					 jarPath = new JarFile(file);
 					System.out.println(jarPath.getName());
 					Enumeration<JarEntry> entries = jarPath.entries();
 					if(null!=entries){
 						while(entries.hasMoreElements()){
 							JarEntry nextElement = entries.nextElement();
 							String jarFileName = nextElement.getName();
 							if(jarFileName.endsWith(".class")){
 								String clazzName = jarFileName.replace("/",".") ;
 								try {
 									clazzName = clazzName.substring(0,clazzName.indexOf(".class"));
 									Class<?> clazz = Class.forName(clazzName);
 									if(clazz.isAnnotationPresent(MarmotInterface.class)){
 										INTERFACECLASS.add(clazz);
 									}
 									
 								} catch (ClassNotFoundException e) {
 									e.printStackTrace();
 								}
 							}
 							
 						}
 					}
 					
 				} catch (IOException e) {
 					e.printStackTrace();
 				}finally{
 					try {
						jarPath.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
 				}
         	}
         }
    }
    
    private static void initRpcInterfaceClazz(List<File> remoteClientFiles){
   	 if(!CollectionUtils.isEmpty(remoteClientFiles)){
        	for(File file:remoteClientFiles){
        		JarFile jarPath = null;
        		try {
					jarPath = new JarFile(file);
					String fullPath = jarPath.getName();
					System.out.println("远程客户端："+fullPath);
					String fileName = fullPath.substring(fullPath.lastIndexOf("\\")+1);
					
					String projectName = fileName.substring(0,fileName.indexOf("-client"));
					System.out.println("远程客户端的项目名称为:"+projectName);
					
					Enumeration<JarEntry> entries = jarPath.entries();
					if(null!=entries){
						while(entries.hasMoreElements()){
							JarEntry nextElement = entries.nextElement();
							String jarFileName = nextElement.getName();
							if(jarFileName.endsWith(".class")){
								String clazzName = jarFileName.replace("/",".") ;
								try {
									clazzName = clazzName.substring(0,clazzName.indexOf(".class"));
									Class<?> clazz = Class.forName(clazzName);
									if(clazz.isAnnotationPresent(MarmotInterface.class)){
										REMOTE_INFO.put(clazz, projectName);
									}
									
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
								}
							}
							
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}finally{
					try {
						jarPath.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
        	}
        }
   }
    
    private List<File> loadLib() {
    	List<File> remoteClientFiles = new ArrayList<File>();
    	
        URL location = RpcClientFinder.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath = location.getPath();
        if (filePath.endsWith(".jar")) {
            // 截取路径中的jar包名
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        }
        File file = new File(filePath);
        File[] listFiles = file.listFiles(new FilenameFilter() {
            // 过滤并找出客户端jar
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".jar") && isInnerClient(name)) {
                    return true;
                }
                return false;
            }
        });
        if (listFiles != null) {
            for (File jarFile : listFiles) {
                boolean include = include(jarFile);
                if (include) {
                	String fileName = jarFile.getName();
                	fileName = fileName.substring(0,fileName.indexOf("-client"));
                	if(fileName.equals(PropUtil.getInstance().get("project-name"))){
                		INTERFACECLASS_JARFILE.add(jarFile);
                	}else if(fileName.startsWith("marmot")){
                		remoteClientFiles.add(jarFile);
                	}
                	System.out.println(jarFile.getName());
                    System.out.println("ServiceMetadataManager include jar: " + jarFile);
                } else {
                	 System.out.println("ServiceMetadataManager try inclue jar: " + jarFile
                            + ", because did not find the ServiceMetadata and ingore");
                }
            }
        }
        return remoteClientFiles;
    }
    
    
    /**
     * 判断是否是内部服务客户端
     * 
     * @param name
     * @return
     */
    private boolean isInnerClient(String name) {
        if (name.startsWith("marmot-")) {
            if (name.indexOf("-client-") != -1) {
                return true;
            }
        }
        return false;
    }
    
    private List<File> loadClassPath() {
    	
    	List<File> remoteClientFiles = new ArrayList<File>();
    	
        String paths = System.getProperty("java.class.path");
        String[] array = paths.split("\\" + File.pathSeparator);
        for (String path : array) {
            File jarFile = new File(path);
            if (path.endsWith(".jar") && isInnerClient(jarFile.getName())) {
                boolean include = include(jarFile);
                if (include) {
                	String fileName = jarFile.getName();
                	if(fileName.substring(0,fileName.indexOf("-client")).equals(PropUtil.getInstance().get("project-name"))){
                		INTERFACECLASS_JARFILE.add(jarFile);
                	}else if(fileName.endsWith("-client")&&fileName.startsWith("-marmot")){
                		remoteClientFiles.add(jarFile);
                	}
                } else {
                	System.out.println("ServiceMetadataManager try inclue jar: " + jarFile
                            + ", because did not find the ServiceMetadata and ingore");
                }
            }
        }
        
        return remoteClientFiles;
    }
    
    public boolean include(File jarFile) {
    	
    	return true;
    }
}
