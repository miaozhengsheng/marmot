package com.marmot.zk.enums;

public enum EnumPermissions {


    READ(1 << 0), WRITE(1 << 1), CREATE(1 << 2), DELETE(1 << 3), ADMIN(1 << 4);

    private int perssions;

    EnumPermissions(int perssions) {
        this.setPerssions(perssions);
    }

    public int getPerssions() {
        return perssions;
    }

    public void setPerssions(int perssions) {
        this.perssions = perssions;
    }

}
