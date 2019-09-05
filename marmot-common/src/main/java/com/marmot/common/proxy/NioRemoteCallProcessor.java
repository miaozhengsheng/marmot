package com.marmot.common.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import com.marmot.common.rpc.bean.MarmotRpcBean;

public class NioRemoteCallProcessor {

	private String methodName;

	private String clazzName;

	private Object[] paramterValues;

	public NioRemoteCallProcessor(String methodName, String clazzName,
			Object[] paramterValues) {
		this.methodName = methodName;
		this.clazzName = clazzName;
		this.paramterValues = paramterValues;
	}

	public Object callRemote(String ip, int port) throws IOException {

		Class[] paramterTypes = null;

		if (paramterValues != null) {

			paramterTypes = new Class[paramterValues.length];
			for (int i = 0; i < paramterValues.length; i++) {
				paramterTypes[i] = paramterValues[i].getClass();
			}
		}
		MarmotRpcBean rpcBean = new MarmotRpcBean(methodName, clazzName,
				paramterValues, paramterTypes);

		Selector selector = Selector.open();

		SocketChannel socketChannel = SocketChannel.open();

		InetSocketAddress inetAddress = new InetSocketAddress(ip, port);
		socketChannel.configureBlocking(Boolean.FALSE);
		socketChannel.connect(inetAddress);

		socketChannel.register(selector, SelectionKey.OP_CONNECT);
		
		

		while (true) {

			int keyCnt = selector.select(3 * 1000);

			if (keyCnt <= 0) {
				continue;
			}

			Set<SelectionKey> keys = selector.selectedKeys();

			Iterator<SelectionKey> iterator = keys.iterator();

			while (iterator.hasNext()) {

				SelectionKey next = iterator.next();

				iterator.remove();

				if (!next.isValid()) {
					continue;
				}

				if (next.isConnectable()) {
					doConnect(next,selector);
				}

				if (next.isReadable()) {
					try {
						Object result = doRead(next,selector);
						return result;
					} catch (Exception e) {
						e.printStackTrace();
					}finally{
						selector.close();
						socketChannel.close();
					}
				}

				if (next.isWritable()) {
					doWrite(next, rpcBean,selector);
				}
			}

		}

	}

	private Object doRead(SelectionKey next,Selector selector) throws Exception {

		SocketChannel channel = (SocketChannel) next.channel();

		ByteBuffer buffer = ByteBuffer.allocate(1024);

		channel.read(buffer);

		buffer.flip();

		byte[] byteArray = Arrays.copyOf(buffer.array(), buffer.capacity());

		channel.register(selector, SelectionKey.OP_WRITE);

		ObjectInputStream objectInputStream = new ObjectInputStream(
				new ByteArrayInputStream(byteArray));

		return objectInputStream.readObject();

	}

	private void doWrite(SelectionKey next, MarmotRpcBean rpcBean,
			Selector selector) throws IOException {
		SocketChannel socketChannel = (SocketChannel) next.channel();

		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(byteOutputStream);

		oos.writeObject(rpcBean);

		byte[] resultArray = byteOutputStream.toByteArray();

		byteBuffer.put(resultArray);
		byteBuffer.flip();

		socketChannel.write(byteBuffer);

		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	private void doConnect(SelectionKey next, Selector selector) {

		SocketChannel socketChannel = (SocketChannel) next.channel();

		if (!socketChannel.isConnectionPending()) {
			return;
		}

		try {
			socketChannel.configureBlocking(false);
			socketChannel.finishConnect();
			socketChannel.register(selector, SelectionKey.OP_WRITE);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
