package com.marmot.cache.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

public class RedisHeartbeat {


    private static final Logger logger = Logger.getLogger(RedisHeartbeat.class);

    private String host;
    private int port;
    private String password;
    private Socket socket;
    private RedisOutputStream outputStream;
    private RedisInputStream inputStream;
    private static final int TIMEOUT = 500;
    private static final String MSG = "PING\r\n";
    private static final String RET = "PONG";

    private final AtomicBoolean status = new AtomicBoolean(true);
    private long lastFailTime = -1;
    private int failCnt = 0;
    private static final int RETRY = 3;// 连续3次

    // 释放连接重新打开连接周期，单位毫秒
    private static final int RELEASE_INTERVAL = 30 * 60 * 1000;// 30分钟
    private long lastConnectTime;

    private IHeartbeatReporting heartbeatReporting;

    public RedisHeartbeat(String host, int port, String password, IHeartbeatReporting heartbeatReporting) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.heartbeatReporting = heartbeatReporting;
        connect();
    }

    public void ping() {
        // 定期检查释放链接
        // if (needReleaseConnection()) {
        // return;
        // }

        // 不可用恢复检查
        if (status.get() == false && failCnt >= RETRY) {
            try {
                reset();
                // 上报
                if (heartbeatReporting != null) {
                    heartbeatReporting.report(host, port, true, System.currentTimeMillis());
                }
            } catch (Exception e) {
                failCnt++;
                log(e.getMessage());
            }
            return;
        }

        // 正常ping检查
        try {
            outputStream.write(MSG.getBytes());
            outputStream.flush();
            byte b = inputStream.readByte();
            if (b == Protocol.PLUS_BYTE) {
                if (RET.equals(inputStream.readLine())) {
                    failCnt = 0;
                    return;
                }
            }
        } catch (Exception e) {
            // 每次异常重新打开链接，为下次ping准备
            reset1();
        }
        // 异常计数
        failCnt++;

        if (status.get() == true) {
            if (failCnt >= RETRY) {
                // 连续3次异常置为不可用
                status.set(false);
                lastFailTime = System.currentTimeMillis();
                // 上报
                if (heartbeatReporting != null) {
                    heartbeatReporting.report(host, port, false, lastFailTime);
                }
                // 不可用状态，日志纪录
                log("连续三次心跳异常，置为不可用.");
            }
        }
    }

    private void reset() {
        disconnect();
        connect();
        status.set(true);
        // lastFailTime = -1;
        failCnt = 0;
    }

    private void reset1() {
        try {
            disconnect();
            connect();
        } catch (Exception e) {
        }
    }

    @SuppressWarnings("unused")
    private boolean needReleaseConnection() {
        if ((System.currentTimeMillis() - lastConnectTime) >= RELEASE_INTERVAL) {
            try {
                reset();
                return true;
            } catch (Exception e) {
                // 如果连接不上，并且在下次ping不检查，将检查交给心跳处理。保证周期只处理一次
                lastConnectTime = System.currentTimeMillis();
                return false;
            }
        }
        return false;
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException ex) {
                // nothing
            } finally {
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception e) {
                }
                socket = null;
            }
        }
    }

    private void connect() {
        if (!isConnected()) {
            try {
                socket = new Socket();
                socket.setReuseAddress(true);
                socket.setKeepAlive(true);
                socket.setTcpNoDelay(true);
                socket.setSoLinger(true, 0);
                socket.connect(new InetSocketAddress(host, port), 1000);
                socket.setSoTimeout(TIMEOUT);
                outputStream = new RedisOutputStream(socket.getOutputStream(), 8);
                inputStream = new RedisInputStream(socket.getInputStream(), 7);
                // auth
                if (password != null) {
                    if (!auth()) {
                        throw new JedisDataException(host + ":" + port + " NOAUTH Authentication required.");
                    }
                }
                lastConnectTime = System.currentTimeMillis();
            } catch (IOException ex) {
                throw new JedisConnectionException(host + ":" + port, ex);
            }
        }
    }

    private boolean auth() throws IOException {
        byte[][] args = new byte[1][];
        args[0] = SafeEncoder.encode(password);
        Protocol.sendCommand(outputStream, Command.AUTH, args);
        outputStream.flush();
        byte[] resp = (byte[]) Protocol.read(inputStream);
        if (resp != null && "OK".equals(SafeEncoder.encode(resp))) {
            return true;
        }
        return false;
    }

    private boolean isConnected() {
        return socket != null && socket.isBound() && !socket.isClosed() && socket.isConnected()
                && !socket.isInputShutdown() && !socket.isOutputShutdown();
    }

    public boolean getStatus() {
        return status.get();
    }

    public long getLastFailTime() {
        return lastFailTime;
    }

    private void log(String message) {
        logger.warn("RedisHeartbeat ping fail, state@" + toString() + ", " + message);
    }

    @Override
    public String toString() {
        return "[ip=" + host + ":" + port + ", status=" + status.get() + ", lastFailTime=" + lastFailTime
                + ", failCnt=" + failCnt + "]";
    }


}
