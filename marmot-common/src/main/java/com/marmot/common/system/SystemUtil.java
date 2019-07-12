package com.marmot.common.system;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class SystemUtil {
	
	
    public static final String FILE_SEPARATOR = "file.separator";

    public static final String CATALINA_BASE = "catalina.base";

    public static final String FILENAME = "server.xml";
	
	private static String ip;
	
	
	public static String getLocalIp(){
		return ip;
	}
	
	static {
		ip = findIp();
	}

	 private static String findIp() {
	        Map<String, String> data = new HashMap<>();
	        try {
	            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
	            NetworkInterface netInterface;
	            while (allNetInterfaces.hasMoreElements()) {
	                netInterface = allNetInterfaces.nextElement();
	                if (netInterface.isVirtual() || netInterface.isLoopback() || !netInterface.isUp()
	                        || netInterface.getName().startsWith("vir") || netInterface.getName().startsWith("docker")
	                        || netInterface.getName().startsWith("contiv")) {
	                    continue;
	                }
	                String realIp = findRealIp(netInterface);
	                if (realIp != null) {
	                    data.put(netInterface.getName(), realIp);
	                }
	            }
	        } catch (Throwable e) {
	        }
	        if (data.isEmpty()) {
	            return null;
	        }
	        if (data.size() == 1) {
	            return data.values().iterator().next();
	        }
	        // 多个网卡优先eth0
	        String ip = data.remove("eth0");
	        if (ip != null) {
	            return ip;
	        }
	        return data.values().iterator().next();
	    }
	 
	 private static String findRealIp(NetworkInterface networkInterface) {
	        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
	        List<String> ips = new ArrayList<>();
	        InetAddress inetAddress = null;
	        while (addresses.hasMoreElements()) {
	            inetAddress = addresses.nextElement();
	            if (inetAddress == null || !(inetAddress instanceof Inet4Address)) {
	                continue;
	            }
	            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
	                continue;
	            }
	            if (inetAddress.isSiteLocalAddress()) {
	                ips.add(inetAddress.getHostAddress());
	            }
	        }
	        Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
	        if (subInterfaces != null) {
	            NetworkInterface netInterface;
	            while (subInterfaces.hasMoreElements()) {
	                netInterface = subInterfaces.nextElement();
	                // 虚拟接口不会有多个ip了，此处不用递归
	                InetAddress subAddress = netInterface.getInetAddresses().nextElement();
	                ips.remove(subAddress.getHostAddress());
	            }
	        }
	        // vip接口配置方式：keepalive的配置方式。在真实ip在第一位，程序读取默认在末尾。
	        /**
	         * eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast
	         * state UP qlen 1000<br>
	         * link/ether 1e:00:99:00:00:c1 brd ff:ff:ff:ff:ff:ff<br>
	         * inet 10.110.13.142/22 brd 10.110.15.255 scope global eth0<br>
	         * valid_lft forever preferred_lft forever<br>
	         * inet 10.110.15.216/32 scope global eth0<br>
	         * valid_lft forever preferred_lft forever<br>
	         */
	        return (ips.size() > 0 ? ips.get(ips.size() - 1) : null);
	    }
	 
	 /**
	     * 文件名分隔符
	     * 
	     * @return
	     */
	    public static String fileSeparator() {
	        return System.getProperty(FILE_SEPARATOR);
	    }

	 
	 /**
	     * 获取tomcat启动端口
	     * 
	     * @return
	     */
	    public static int getConnectorPort() {
	        String filepath = System.getProperty(CATALINA_BASE) + fileSeparator() + "conf" + fileSeparator() + FILENAME;

	        SAXReader reader = new SAXReader();
	        Document doc = null;;
	        try {
	            doc = reader.read(new File(filepath));

	            Element root = doc.getRootElement();
	            Element service = root.element("Service");
	            Element connector = service.element("Connector");
	            String value = connector.attributeValue("port");

	            return Integer.parseInt(value);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        return 0;
	    }

}
