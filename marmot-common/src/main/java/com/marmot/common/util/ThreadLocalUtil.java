package com.marmot.common.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.marmot.common.constants.Const;

public class ThreadLocalUtil extends ThreadLocal<Map<String, Object>> {

    @Override
    protected Map<String, Object> initialValue() {
        return new HashMap<String, Object>(15);
    }

    private static final ThreadLocalUtil instance = new ThreadLocalUtil();

    private ThreadLocalUtil() {
    }

    public static ThreadLocalUtil getInstance() {
        return instance;
    }

    public void remove() {
        this.get().clear();
    }

    public Object get(String key) {
        return this.get().get(key);
    }

    public void set(String key, Object value) {
        this.get().put(key, value);
    }

    public Map<String, Object> getAll() {
        return this.get();
    }

    public void set(Map<String, Object> map) {
        this.get().putAll(map);
    }

    public void setClientId(String[] clientIds) {
        set(Const.CLIENT_IDS, clientIds);
    }

    public String[] getClientId() {
        Object obj = get(Const.CLIENT_IDS);
        return (obj != null) ? (String[]) obj : new String[] {};
    }

    /**
     * 返回第一个请求的系统clientId
     * 
     * @return
     */
    public String getInitClientId() {
        String[] clientIdArray = getClientId();
        return (clientIdArray.length > 0) ? clientIdArray[0] : "";
    }

    /**
     * 返回最近一个请求的系统clientId
     * 
     * @return
     */
    public String getLastClientId() {
        String[] clientIdArray = getClientId();
        return (clientIdArray.length > 0) ? clientIdArray[clientIdArray.length - 1] : "";
    }

    /**
     * 注意：禁止业务使用
     * 
     * @param currentUserId
     */
    @Deprecated
    public void setCurrentUserId(String currentUserId) {
        set(Const.CURRENT_USER_ID, currentUserId);
    }

    public String getCurrentUserId() {
        Object obj = get(Const.CURRENT_USER_ID);
        return (obj != null) ? (String) obj : "0";
    }

    /**
     * 注意：即将下线
     * 
     * @param currentUserKind
     */
    @Deprecated
    public void setCurrentUserKind(String currentUserKind) {
        set(Const.CURRENT_USER_KIND, currentUserKind);
    }

    /**
     * 注意：即将下线
     * 
     * @return
     */
    @Deprecated
    public String getCurrentUserKind() {
        Object obj = get(Const.CURRENT_USER_KIND);
        return (obj != null) ? (String) obj : "";
    }

    public void setOriginalIP(String originalIP) {
        set(Const.ORIGINAL_IP, originalIP);
    }

    public String getOriginalIP() {
        Object obj = get(Const.ORIGINAL_IP);
        return (obj != null) ? (String) obj : "";
    }

    public void setBIInfo(String biInfo) {
        set(Const.BI_INFO, biInfo);
    }

    public String getBIInfo() {
        Object obj = get(Const.BI_INFO);
        return (obj != null) ? (String) obj : "";
    }

    public void setDeviceUuid(String deviceUuid) {
        set(Const.DEVICE_UUID, deviceUuid);
    }

    public String getDeviceUuid() {
        Object obj = get(Const.DEVICE_UUID);
        return (obj != null) ? (String) obj : "";
    }

    public void setExtend(Map<String, Object> map) {
        if (map != null && map.size() > 0) {
            set(Const.TRANSMIT_EXTEND, map);
        }
    }

    @SuppressWarnings("unchecked")
    public void setExtend(String key, Object value) {
        if (key != null && value != null) {
            Map<String, Object> map = (Map<String, Object>) get(Const.TRANSMIT_EXTEND);
            if (map == null) {
                set(Const.TRANSMIT_EXTEND, map = new HashMap<String, Object>());
            }
            map.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getExtend() {
        Map<String, Object> map = (Map<String, Object>) get(Const.TRANSMIT_EXTEND);
        if (map == null) {
            map = Collections.emptyMap();
        }
        return map;
    }

    public void setVersion(String version) {
        set(Const.VERSION, version);
    }

    public String getVersion() {
        Object obj = get(Const.VERSION);
        return (obj != null) ? (String) obj : "";
    }

    public <T> void setTraceId(T traceId) {
        set(Const.TRACEID, traceId);
    }

    public void setTraceIdString(String traceId) {
        setTraceId(new TraceId(traceId));
    }

    @SuppressWarnings("unchecked")
    public <T> T getTraceId() {
        Object obj = get(Const.TRACEID);
        return (obj != null) ? (T) obj : null;
    }

    public void setInitiateUrl(String initiateUrl) {
        set(Const.INITIATE_URL, initiateUrl);
    }

    public String getInitiateUrl() {
        Object obj = get(Const.INITIATE_URL);
        return (obj != null) ? (String) obj : "";
    }

    public void setCurrentUrl(String currentUrl) {
        set(Const.CURRENT_URL, currentUrl);
    }

    public String getCurrentUrl() {
        Object obj = get(Const.CURRENT_URL);
        return (obj != null) ? (String) obj : "";
    }

    public void setClientIp(String clientIp) {
        set(Const.CLIENT_IP, clientIp);
    }

    public String getClientIp() {
        Object obj = get(Const.CLIENT_IP);
        return (obj != null) ? (String) obj : "";
    }

    public void setArea(String area) {
        set(Const.AREA, area);
    }

    public String getArea() {
        Object obj = get(Const.AREA);
        return (obj != null) ? (String) obj : "";
    }

    // FIXME 框架内部不再使用，暂时兼容业务使用，后期去掉
    @Deprecated
    public String getRandom() {
        return "";
    }

}
