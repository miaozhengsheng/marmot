package com.marmot.zk.client.exception;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class ZookeeperException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	

    
    /**错误描述*/
    private String msg;
    
    public ZookeeperException() {
        super();
    }
    public ZookeeperException(Throwable t) {
        super(t);
    }
    public ZookeeperException(String msg) {
        super(msg);
        this.msg = msg;
    }
    public ZookeeperException(String msg, Throwable t) {
        super(msg,t);
        this.msg = msg;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }


}
