package com.marmot.cache.enums;

public enum EnumExist {
	/**
     * 只在键不存在时，才对键进行设置操作
     */
    NX,
    /**
     * 只在键已经存在时，才对键进行设置操作
     */
    XX
}
