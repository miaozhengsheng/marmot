package com.marmot.common.constants;

public class Const {


    public static final String CLIENT_IDS = "clientId";// 访问的系统client_id，多个逗号隔开
    public static final String CURRENT_USER_ID = "currentUserId";// 访问者用户id
    @Deprecated
    public static final String CURRENT_USER_KIND = "currentUserKind";// 访问者用户类型
    public static final String ORIGINAL_IP = "originalIp";// 请求原始ip
    public static final String BI_INFO = "biInfo";// BI 信息
    public static final String DEVICE_UUID = "deviceUuid";// app设备id
    public static final String CAT = "cat";// CAT 数据
    // public static final String RANDOM = "random"; // 随机数，FIXME 逐渐去掉，改为TRACEID
    public static final String TRACEID = "traceId"; // 唯一跟踪id
    public static final String TIME_RIVER = "timeRiver"; // 时间秒表，纪录rpc请求中的关键节点
    public static final String INITIATE_URL = "initiateUrl"; // 请求发起url
    public static final String VERSION = "version";// 版本信息
    public static final String UPLOAD_FILE = "MULTIPART_FORMDATA";// 上传文件
    public static final String TRANSMIT_EXTEND = "transmitExtend";// 自定义服务透传扩展字段
    public static final String CURRENT_URL = "currentUrl";// 当前请求url
    public static final String CLIENT_IP = "clientIp";// 当前请求ip
    public static final String AREA = "area"; // 当前请求所在逻辑区

    public static final String DATA = "data";

    /**
     * 接口命名空间
     * <p>
     * 例如：/RPC/IUserService/getUserDtoByUserId<br>
     */
    public static final String NAMESPACE_API = "RPC";

    /**
     * 内部接口命令空间
     * <p>
     * 例如： <br>
     * /RPC/define/IUserService/getUserDtoByUserId<br>
     */
    public static final String NAMESPACE_API_DEFINE = "define";

    /**
     * 内部监控命令空间
     * <p>
     * 例如： <br>
     * /monitor/http.do<br>
     */
    public static final String NAMESPACE_MONITOR = "monitor";

    /**
     * 配置文件定义Package日志级别命名空间
     */
    public static final String NAMESPACE_LOG_LEVEL = "LOG.LEVEL";

    /**
     * 自定义log4j appender日志命名空间
     */
    public static final String NAMESPACE_LOG4J_CATEGORY = "marmot.log4j.category.";

    /**
     * PC端 ajax和APP端 json 请求url后缀标示
     */
    public static final String JSON_SUFFIX = ".json";

    /**
     * RPC请求配置抛异常时是否打印请求参数，默认不打印。支持3种状态:BIZ、ALL、NOT<br>
     * NOT 代表不打印<br>
     * BIZ 代表出现BizException才打印<br>
     * ALL 代表全部异常都打印<br>
     */
    public static final String LOG_PARAM_WHEN_EXCEPTION = "LOG.PARAM.WHEN.EXCEPTION";


}
