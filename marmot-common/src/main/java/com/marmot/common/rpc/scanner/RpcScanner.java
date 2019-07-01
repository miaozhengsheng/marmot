package com.marmot.common.rpc.scanner;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;


public class RpcScanner {

	
	// 所有的rpc的接口生命类
	public static List<Class<?>> allRpcInterfaces = new ArrayList<Class<?>>();
	
	private static PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
	
	private static MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();
	
	public static void scanPackage(String packagePattern){
	
		initClass(packagePattern);
		
		
	}
	
	
	private static void initClass(String packagePattern){
		
		String resourcePath = ClassUtils.convertClassNameToResourcePath(packagePattern) + ".class";
        try {
            Resource[] resources = resolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + resourcePath);
            
            for(Resource resource:resources){
            	MetadataReader metadataReader = readerFactory.getMetadataReader(resource);
            	
            	String clazzName= metadataReader.getClassMetadata().getClassName();
            	
            	Class<?> clazz = Class.forName(clazzName);
            	
            	allRpcInterfaces.add(clazz);
            }
            
        }catch(Exception e){
        	e.printStackTrace();
        }
	}
	
}
