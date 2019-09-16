package com.marmot.zk.enums;

public enum EnumMethodAuthType {

    READ("read"), WRITE("write"), NOAUTH("noauth");
    private String methodType;

    EnumMethodAuthType(String methodType) {
        this.setMethodType(methodType);
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }


}
