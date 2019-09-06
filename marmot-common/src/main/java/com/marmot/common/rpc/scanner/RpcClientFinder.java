package com.marmot.common.rpc.scanner;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.util.CollectionUtils;

import com.marmot.common.rpc.annotation.MarmotInterface;
import com.marmot.common.util.PropUtil;

public class RpcClientFinder {

	private static final RpcClientFinder instance = new RpcClientFinder();
	
	// 提供的服务
	private static final List<Class> INTERFACECLASS = new ArrayList<Class>();
	private static final List<File> INTERFACECLASS_JARFILE = new ArrayList<File>();
	// 需要调用的服务
	public static final List<Class> SERVERINTERFACES = new ArrayList<Class>();
	private static final List<File> SERVERINTERFACES_JARFILE = new ArrayList<File>();
	
	
	public static List<Class> getLocalServiceClass(){
		return INTERFACECLASS;
	}
	
	public static List<Class> getRemoteServiceClass(){
		
		return SERVERINTERFACES;
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
        if (loadLib() == 0) {
            loadClassPath();
        }

        // 本地要提供的服务
       initInterfaceClazz();
       // 有可能需要调用到的服务
       initRpcInterfaceClazz();
    }
    
    private static void initInterfaceClazz(){
    	 if(!CollectionUtils.isEmpty(INTERFACECLASS_JARFILE)){
         	for(File file:INTERFACECLASS_JARFILE){
         		
         		try {
 					JarFile jarPath = new JarFile(file);
 					
 					Enumeration<JarEntry> entries = jarPath.entries();
 					if(null!=entries){
 						while(entries.hasMoreElements()){
 							JarEntry nextElement = entries.nextElement();
 							String jarFileName = nextElement.getName();
 							if(jarFileName.endsWith(".class")){
 								String clazzName = jarFileName.replace("/",".") ;
 								try {
 									clazzName = clazzName.substring(0,clazzName.indexOf(".class"));
 									Class clazz = Class.forName(clazzName);
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
 				}
         	}
         }
    }
    
    private static void initRpcInterfaceClazz(){
   	 if(!CollectionUtils.isEmpty(SERVERINTERFACES_JARFILE)){
        	for(File file:SERVERINTERFACES_JARFILE){
        		JarFile jarPath = null;
        		try {
					jarPath = new JarFile(file);
					
					Enumeration<JarEntry> entries = jarPath.entries();
					if(null!=entries){
						while(entries.hasMoreElements()){
							JarEntry nextElement = entries.nextElement();
							String jarFileName = nextElement.getName();
							if(jarFileName.endsWith(".class")){
								String clazzName = jarFileName.replace("/",".") ;
								try {
									clazzName = clazzName.substring(0,clazzName.indexOf(".class"));
									Class clazz = Class.forName(clazzName);
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
        	}
        }
   }
    
    private int loadLib() {
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
        int loadCnt = 0;
        if (listFiles != null) {
            for (File jarFile : listFiles) {
                boolean include = include(jarFile);
                if (include) {
                	String fileName = jarFile.getName();
                	if(fileName.substring(0,fileName.indexOf("-client")).equals(PropUtil.getInstance().get("project-name"))){
                		INTERFACECLASS_JARFILE.add(jarFile);
                	}else{
                		SERVERINTERFACES_JARFILE.add(jarFile);
                	}
                	System.out.println(jarFile.getName());
                    System.out.println("ServiceMetadataManager include jar: " + jarFile);
                } else {
                	 System.out.println("ServiceMetadataManager try inclue jar: " + jarFile
                            + ", because did not find the ServiceMetadata and ingore");
                }
                loadCnt++;
            }
        }
        return loadCnt;
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
    
    private void loadClassPath() {
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
                	}else{
                		SERVERINTERFACES_JARFILE.add(jarFile);
                	}
                } else {
                	System.out.println("ServiceMetadataManager try inclue jar: " + jarFile
                            + ", because did not find the ServiceMetadata and ingore");
                }
            }
        }
    }
    
    public boolean include(File jarFile) {
    	
    	return true;
    }
}
