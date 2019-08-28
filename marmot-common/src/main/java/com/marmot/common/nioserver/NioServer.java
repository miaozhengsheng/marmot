package com.marmot.common.nioserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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


public class NioServer {

	
	private static Selector selector =  null;
	
	private static ServerSocketChannel serverSocketChannel = null;
	
	public static void startServer(int port) throws IOException, ClassNotFoundException{
		
		selector = Selector.open();
		
		serverSocketChannel = ServerSocketChannel.open();
		
		serverSocketChannel.configureBlocking(false);
		
		InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
		serverSocketChannel.bind(inetSocketAddress);
		
		serverSocketChannel.register(selector, SelectionKey.OP_CONNECT);
		
		System.out.println("服务启动成功，开始接受RPC请求");
		
		acceptRequest();
		
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
	
	private static void acceptRequest() throws IOException, ClassNotFoundException{
		
		while(true){
		
			int cnt = selector.select(3*1000);
			if(cnt<=0){
				continue;
			}
			
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = selectedKeys.iterator();
			
			while(iterator.hasNext()){
				
				SelectionKey next = iterator.next();
				
				if(next.isAcceptable()){
					handleAccept(next);
				}
				
				if(next.isReadable()){
					handleRead(next);
				}
				if(next.isWritable()){
					handleWrite(next);
				}
				
			}
		}
	}
	
	
	private static void handleAccept(SelectionKey key) throws IOException{
		
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		
		SocketChannel accept = serverSocketChannel.accept();
		
		accept.configureBlocking(false);
		
		accept.register(selector, SelectionKey.OP_READ,new ArrayList<Object>());
	}
	
	private static void handleRead(SelectionKey key) throws IOException, ClassNotFoundException{
		
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
		socketChannel.read(byteBuffer);
		byteBuffer.flip();
		
		byte[] copyOf = Arrays.copyOf(byteBuffer.array(),byteBuffer.capacity());
		
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(copyOf));
		
		MarmotRpcBean rpcBean = (MarmotRpcBean)objectInputStream.readObject();
		
		System.out.println("接收到的请求信息为："+rpcBean);
		
		// 真正调用本地方法的地方
		
		key.attach(rpcBean);
		
		socketChannel.register(selector, SelectionKey.OP_WRITE);
	}
	

	private static void handleWrite(SelectionKey key)throws IOException{
		
		System.out.println("服务端，接收到写的事件。'");
		
		SocketChannel socketChannel = (SocketChannel)key.channel();
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
		
		  ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(byteOutputStream);
          
           oos.writeObject(key.attachment());
          
          byte[] resultArray  = byteOutputStream.toByteArray();
          
          byteBuffer.put(resultArray);
          
		byteBuffer.flip();
		
		socketChannel.write(byteBuffer);
		System.out.println("服务端向客户端写信息完毕。");
		socketChannel.register(selector, SelectionKey.OP_READ,new ArrayList<Object>());
		
	}
}
