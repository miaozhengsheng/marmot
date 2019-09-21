package com.marmot.cache.dynamic;

import java.util.Set;

public interface IDynamicLoadHandle {
	 public void invoke(Set<String> config) throws Exception;
}
