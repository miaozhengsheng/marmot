package com.marmot.cache.factory;

import com.marmot.cache.ICacheClient;

public interface ICachedClientFactory {
	public ICacheClient newInstance() throws Exception;
}
