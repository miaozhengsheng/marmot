package com.marmot.common.util;

public class TraceId {


    private String uniqueId;

    private int serialNum;

    public TraceId() {
        this.uniqueId = "bs." + System.currentTimeMillis() + "";
    }

    public TraceId(String traceId) {
        this.uniqueId = traceId;
    }

    public void iterate() {
        this.serialNum++;
    }

    @Override
    public String toString() {
        if (serialNum == 0) {
            return uniqueId;
        } else {
            return uniqueId + "." + serialNum;
        }
    }


}
