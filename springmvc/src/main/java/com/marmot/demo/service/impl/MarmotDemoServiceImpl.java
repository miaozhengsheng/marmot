package com.marmot.demo.service.impl;

import org.springframework.stereotype.Service;

import com.marmot.demo.service.IMarmotDemoService;
@Service
public class MarmotDemoServiceImpl implements IMarmotDemoService {

	@Override
	public String hello() throws Exception {
		return "Hello marmot";
	}

}
