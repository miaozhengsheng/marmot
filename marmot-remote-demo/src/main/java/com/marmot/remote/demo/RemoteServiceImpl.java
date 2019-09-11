package com.marmot.remote.demo;

import org.springframework.stereotype.Service;

import com.marmot.remote.service.IRemoteService;
@Service
public class RemoteServiceImpl implements IRemoteService{

	@Override
	public void del() {
		System.out.println("del ........");
	}

}
