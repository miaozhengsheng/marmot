package com.marmot.server.service;

import org.springframework.stereotype.Service;

import com.marmot.demo.api.IMarmotDemoService;

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
