package com.marmot.demo.service.impl;

import org.springframework.stereotype.Service;

import com.marmot.demo.service.IMarmotDemoService;
@Service
public class MarmotDemoServiceImpl implements IMarmotDemoService {

	@Override
	public String hello(String name) throws Exception {
		return "Hello marmot,"+name;
	}

	@Override
	public Integer add(Integer a, Integer b) throws Exception {
		return a+b;
	}

}
