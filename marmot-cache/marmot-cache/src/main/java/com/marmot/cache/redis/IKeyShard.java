package com.marmot.cache.redis;

public interface IKeyShard {


    public Shard getShard(String key);

    public static class Shard {
        String host;
        int port;

        public Shard(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "Shard [host=" + host + ", port=" + port + "]";
        }

    }


}
