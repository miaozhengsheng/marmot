package com.marmot.zk.bean;
import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.ZooDefs;

import com.marmot.zk.enums.EnumPermissions;

public class AclDto {


    private String userName;

    private List<EnumPermissions> permissions;

    public AclDto(String userName, int perm) {
        this.userName = userName;
        permissions = new ArrayList<EnumPermissions>();
        if ((perm & ZooDefs.Perms.ADMIN) == ZooDefs.Perms.ADMIN) {
            permissions.add(EnumPermissions.ADMIN);
        }
        if ((perm & ZooDefs.Perms.WRITE) == ZooDefs.Perms.WRITE) {
            permissions.add(EnumPermissions.WRITE);
        }
        if ((perm & ZooDefs.Perms.DELETE) == ZooDefs.Perms.DELETE) {
            permissions.add(EnumPermissions.DELETE);
        }
        if ((perm & ZooDefs.Perms.READ) == ZooDefs.Perms.READ) {
            permissions.add(EnumPermissions.READ);
        }
        if ((perm & ZooDefs.Perms.CREATE) == ZooDefs.Perms.CREATE) {
            permissions.add(EnumPermissions.CREATE);
        }
    }

    public String getUserName() {
        return userName;
    }

    public List<EnumPermissions> getPermissions() {
        return permissions;
    }

    public Integer getNumber4Perm() {
        int perm = 0;
        for (EnumPermissions enumPermissions : permissions) {
            perm = perm | enumPermissions.getPerssions();
        }
        return perm;
    }

    @Override
    public String toString() {
        return "AclDto [userName=" + userName + ", permissions=" + permissions + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AclDto other = (AclDto) obj;
        if (userName == null) {
            if (other.userName != null)
                return false;
        } else if (!userName.equals(other.userName))
            return false;
        return true;
    }

}
