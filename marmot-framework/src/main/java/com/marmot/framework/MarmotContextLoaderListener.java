package com.marmot.framework;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;

import com.marmot.common.component.IMarmotComponent;

public class MarmotContextLoaderListener extends ContextLoaderListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event);
		// 启动框架的模块
		initFrameworkCompnent();
	}
	

	private void initFrameworkCompnent() {
		
		Map<String, IMarmotComponent> beansOfType = super.getCurrentWebApplicationContext().getBeansOfType(IMarmotComponent.class);
		
		if(beansOfType==null||beansOfType.isEmpty()){
			return;
		}
		
		Iterator<Entry<String, IMarmotComponent>> iterator = beansOfType.entrySet().iterator();
		
		IMarmotComponent[] components = new IMarmotComponent[beansOfType.size()];
		int i=0;
		while(iterator.hasNext()){
			
			Entry<String, IMarmotComponent> next = iterator.next();
			components[i++]=next.getValue();
		}
		
		Arrays.parallelSort(components, new Comparator<IMarmotComponent>() {

			@Override
			public int compare(IMarmotComponent o1, IMarmotComponent o2) {
				return o1.order()>o2.order()?1:o1.order()==o2.order()?0:-1;
			}
		});
		
		for(IMarmotComponent component:components){
			System.out.println(component.order());
			component.initComponent(super.getCurrentWebApplicationContext());
		}
	}


	@Override
	public void contextDestroyed(ServletContextEvent event) {
		
		// 在spring 上下文停止之前 将框架提供的服务组件停止掉
		Map<String, IMarmotComponent> beansOfType = super.getCurrentWebApplicationContext().getBeansOfType(IMarmotComponent.class);
		if(beansOfType!=null&& !beansOfType.isEmpty()){
			
			Iterator<Entry<String, IMarmotComponent>> iterator = beansOfType.entrySet().iterator();
			
			while(iterator.hasNext()){
				Entry<String, IMarmotComponent> next = iterator.next();
				
				IMarmotComponent marmotComponent = next.getValue();
				
				marmotComponent.distory(super.getCurrentWebApplicationContext());
			}
		}
		
		super.contextDestroyed(event);
	}

	

}
