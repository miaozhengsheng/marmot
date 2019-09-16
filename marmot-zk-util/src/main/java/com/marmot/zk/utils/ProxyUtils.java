package com.marmot.zk.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.log4j.Logger;

import com.google.common.collect.Sets;
import com.marmot.zk.client.IZKClient;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.enums.EnumMethodAuthType;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.listener.Listener;
import com.marmot.zk.listener.NewNodeChildListener;
import com.marmot.zk.listener.NewNodeListener;
import com.marmot.zk.listener.Operation4internal;

public class ProxyUtils {

    // 需要写鉴权的方法
    private static Set<String> methodName4writeAuth = Sets.newHashSet();
    // 需要读鉴权的方法
    private static Set<String> methodName4ReadAuth = Sets.newHashSet();
    // 日志
    private static Logger logger = Logger.getLogger(ProxyUtils.class);

    // 若要修改zkClient的参数名，请通知所有获取真实类的人修改反射代码
    public static IZKClient getProxy4ZKClient(IZKClient zkClient, CuratorFramework client,
            boolean isAuth) {

        // 初始化方法
        initMethodNames4Auth();
        // 初始化鉴权类
        ClientAuthUtil authUtil = new ClientAuthUtil(client, isAuth);

        IZKClient proxy4zkclient = (IZKClient) Proxy.newProxyInstance(
                zkClient.getClass().getClassLoader(), zkClient.getClass().getInterfaces(), new InvocationHandler() {

                    // 为方法代理鉴权
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        // 获取方法名
                        String methodName = method.getName();
                        try {
                            // 快速失败 检验链接是否可用
                            boolean isConnect = Operation4internal.isConnect(client);
                            if (!isConnect && "destroy".equals(methodName)) {
                                logger.warn("zookeeper client is closed");
                                return null;
                            }
                            if (!isConnect) {
                                throw new ZookeeperException("zookeeper client is not connected!");
                            }

                            if ((methodName4writeAuth.contains(methodName)
                                    || methodName4ReadAuth.contains(methodName))) {

                                // 开始鉴权
                                // 全路径
                                String fullPath = "";

                                if (args[0] instanceof Listener) {
                                    if (!(args[0] instanceof NewNodeListener)
                                            && !(args[0] instanceof NewNodeChildListener)) {
                                        // 用户自定义Listener异常
                                        throw new ZookeeperException(
                                                "please use NewNodeListener or NewNodeChildListener to Listener node!");
                                    }
                                    // 监听器鉴权
                                    fullPath = ((Listener) args[0]).listeningPath();
                                    // 获取全路径
                                    fullPath = PathUtils.removeLastSlash(fullPath);

                                } else {
                                    // 获取鉴权参数 需要鉴权的方法请按照标准定义参数
                                    EnumZKNameSpace namespace = (EnumZKNameSpace) args[0];
                                    String path = (String) args[1];

                                    // 获取全路径
                                    fullPath = PathUtils.joinPath(namespace, path);
                                }

                                // 校验路径
                                PathUtils.checkNodePath(fullPath);

                                // 客户端写鉴权
                                if (methodName4writeAuth.contains(methodName)) {
                                    boolean validateWrite = authUtil.validateWrite(fullPath);
                                    if (!validateWrite) {
                                        // 无写权限
                                        throw new ZookeeperException(
                                                "do not have write auth for this node,path=" + fullPath);
                                    }
                                }

                                // 客户端读鉴权
                                if (methodName4ReadAuth.contains(methodName)) {
                                    boolean validateRead = authUtil.validateRead(fullPath);
                                    if (!validateRead) {
                                        // 无读权限
                                        throw new ZookeeperException(
                                                "do not have read auth for this node,path=" + fullPath);
                                    }
                                }
                            }
                            // 执行方法
                            Object o = method.invoke(zkClient, args);
                            return o;
                        } catch (Throwable e) {
                            if (e instanceof InvocationTargetException) {
                                Throwable throwable = ((InvocationTargetException) e).getTargetException();
                                logger.error("【zookeeper】执行方法失败", throwable);
                                throw throwable;
                            }
                            logger.error("【zookeeper】执行方法失败", e);
                            throw e;
                        } finally {
                        }

                    }
                });
        return proxy4zkclient;
    }

    // 初始化需要客户端读写鉴权的方法
    private static void initMethodNames4Auth() {
        Method[] methods = IZKClient.class.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(BeforeMethod.class)) {
                BeforeMethod annotation = (BeforeMethod) method.getAnnotation(BeforeMethod.class);
                if (EnumMethodAuthType.WRITE == annotation.authType()) {
                    methodName4writeAuth.add(method.getName());
                }
                if (EnumMethodAuthType.READ == annotation.authType()) {
                    methodName4ReadAuth.add(method.getName());
                }
            }
        }
    }

}
