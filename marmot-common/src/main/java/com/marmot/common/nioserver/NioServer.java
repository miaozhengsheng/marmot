package com.marmot.common.nioserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import com.marmot.common.rpc.bean.MarmotRpcBean;
import com.marmot.framework.SpringContextUtil;


public class NioServer {

	
	private static Selector selector =  null;
	
	private static ServerSocketChannel serverSocketChannel = null;
	
	public static void startServer(int port) throws IOException, ClassNotFoundException{
		
		selector = Selector.open();
		
		serverSocketChannel = ServerSocketChannel.open();
		
		serverSocketChannel.configureBlocking(false);
		
		InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
		serverSocketChannel.socket().bind(inetSocketAddress);
		
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		
		System.out.println("服务启动成功，开始接受RPC请求");
		
		
		new Thread(()->{
			try {
				acceptRequest();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
		
	}
	
	
	public static void stopServer(){
		try {
			selector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void acceptRequest() throws Exception{
		
		while(true){
		
			int cnt = selector.select(3*1000);
			if(cnt<=0){
				continue;
			}
			
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = selectedKeys.iterator();
			
			while(iterator.hasNext()){
				
				SelectionKey next = iterator.next();
				
				iterator.remove();
				
				if(!next.isValid()){
					System.out.println("接受的key已失效，需等待:"+next);
				}
				
				
				if(next.isAcceptable()){
					handleAccept(next);
				}
				
				
				if(next.isReadable()){
					handleRead(next);
				}
				if(next.isWritable()){
					System.out.println("处理请求结果");
					handleWrite(next);
				}
				
			}
		}
	}
	
	
	private static void handleAccept(SelectionKey key) throws IOException{
		
		
		System.out.println("服务端，收到接入的key："+key);
		SocketChannel socketChannel = ((ServerSocketChannel)key.channel()).accept();
		socketChannel.configureBlocking(false);
		// 注册读取事件
		socketChannel.register(selector,SelectionKey.OP_READ);
	}
	
	private static void handleRead(SelectionKey key) throws Exception{
		
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
		socketChannel.read(byteBuffer);
		byteBuffer.flip();
		
		if(byteBuffer.limit()<=0){
			return;
		}
		
		byte[] copyOf = Arrays.copyOf(byteBuffer.array(),byteBuffer.capacity());
		
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(copyOf));
		
		MarmotRpcBean rpcBean = (MarmotRpcBean)objectInputStream.readObject();
		
		System.out.println("接收到的请求信息为："+rpcBean);
		
		// 真正调用本地方法的地方
		
		String methodName = rpcBean.getMethodName();
		String clazzName = rpcBean.getClazzName();
		
		Class clazz = Class.forName(clazzName);
		
		Method method = clazz.getMethod(methodName, rpcBean.getParameterTypes());
		
		Object target = SpringContextUtil.getBean(clazz);
		
		if(target==null){
			throw new Exception("接口不存在");
		}
		
		Object result = method.invoke(target, rpcBean.getParamterValues());
		
		
		System.out.println("处理后的结果为:"+result);
		
		socketChannel.register(selector, SelectionKey.OP_WRITE,result);
	}
	

	private static void handleWrite(SelectionKey key)throws IOException{
		
		System.out.println("服务端，接收到写的事件。'");
		
		SocketChannel socketChannel = (SocketChannel)key.channel();
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
		
		  ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(byteOutputStream);
          
          System.out.println("开始向客户端输出结果:"+key.attachment());
          
           oos.writeObject(key.attachment());
          
          byte[] resultArray  = byteOutputStream.toByteArray();
          
          byteBuffer.put(resultArray);
          
		byteBuffer.flip();
		
		socketChannel.write(byteBuffer);
		System.out.println("服务端向客户端写信息完毕。");
		socketChannel.register(selector, SelectionKey.OP_READ,new ArrayList<Object>());
		
	}
}
