package com.marmot.zk.enums;

public enum EnumZKNameSpace {
	
		PUBLIC("/config/public"), //
	    PROJECT("/config/project"), //
	    GROUP("/config/group"); //

	    private String namespace = null;

	    EnumZKNameSpace(String rootPath) {
	        namespace = rootPath;
	    }

	    public String getNamespace() {
	        return namespace;
	    }
}
