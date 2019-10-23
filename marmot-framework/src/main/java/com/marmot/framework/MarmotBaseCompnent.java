package com.marmot.framework;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.marmot.common.component.IMarmotComponent;

@Service
public class MarmotBaseCompnent implements IMarmotComponent{

	@Override
	public void initComponent(ApplicationContext applicationContext) {
		
	}

}
