package com.marmot.remote.service;

import com.marmot.common.rpc.annotation.MarmotInterface;
import com.marmot.common.rpc.annotation.MarmotMethod;

@MarmotInterface
public interface IRemoteService {

	@MarmotMethod
	public void del();
}
