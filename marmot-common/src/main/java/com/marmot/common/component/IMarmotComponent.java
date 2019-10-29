package com.marmot.common.component;

import org.springframework.context.ApplicationContext;

public interface IMarmotComponent {

	
	/**
	 * spring 上下文成功启动后 会调用的方法， 用来启动框架的组件
	 * @param applicationContext
	 */
	public void initComponent(ApplicationContext applicationContext);
	
	/**
	 * spring 上下文销毁之前 会调用改方法 ，用来销毁框架组件占用的资源 如：链接 、打开的文件等
	 * @param applicationContext
	 */
	public void distory(ApplicationContext applicationContext);
	
	
	/**决定的 spring 上下文启动后 ，框架组件的启动顺序，
	 * 
	 * <100 框架占用，请勿使用,自定义的插件请放在100之后 （需等待框架组件加载完成后）
	 * 数字越大 调用顺序越靠后
	 * @return
	 */
	public int order();
	
}
