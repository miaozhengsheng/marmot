package com.marmot.common.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.marmot.common.other.GzipUtil;
import com.marmot.common.other.StopWatch;

public class HttpSendClient {
	


    private HttpClient httpClient;

    public HttpSendClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Post 方式请求 ，并返回HTTP_STATUS_CODE码
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return int
     * @throws ClientProtocolException
     * @throws IOException
     */
    public int postReturnHttpCode(String address, Map<String, Object> params) throws ClientProtocolException,
            IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        HttpPost httpPost = new HttpPost(address);
        try {
            List<NameValuePair> data = buildPostData(params);
            httpPost.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            stopWatch.stop();
            return httpResponse.getStatusLine().getStatusCode();
        } finally {
            httpPost.abort();
            stopWatch.log();
        }
    }

    /**
     * Get 方式请求 ，并返回HTTP_STATUS_CODE码
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return int
     * @throws ClientProtocolException
     * @throws IOException
     */
    public int getReturnHttpCode(String address, Map<String, Object> params) throws ClientProtocolException,
            IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        String paramsStr = buildGetData(params);
        HttpGet httpGet = new HttpGet(address + paramsStr);
        try {
            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpGet);
            stopWatch.stop();
            return httpResponse.getStatusLine().getStatusCode();
        } finally {
            httpGet.abort();
            stopWatch.log();
        }
    }

    /**
     * 回调接口
     */
    public static interface CallBack<T> {
        /**
         * 回调方法
         * 
         * @param String response 响应内容
         * @return T 转换的对象，自己封装
         */
        public T call(String response);

    }

    /**
     * Post 方式请求 ，并执行回调接口，返回 T 对象
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @param CallBack<T> callback 回调接口
     * @param T
     * @throws ClientProtocolException
     * @throws IOException
     */
    public <T> T post(String address, Map<String, Object> params, CallBack<T> callback) throws ClientProtocolException,
            IOException {
        String ret = post(address, params);
        return callback.call(ret);
    }

    /**
     * Get 方式请求 ，并执行回调接口，返回 T 对象
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @param CallBack<T> callback 回调接口
     * @param T
     * @throws ClientProtocolException
     * @throws IOException
     */
    public <T> T get(String address, Map<String, Object> params, CallBack<T> callback) throws ClientProtocolException,
            IOException {
        String ret = get(address, params);
        return callback.call(ret);
    }

    /**
     * Post 方式请求
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String post(String address, Map<String, Object> params) throws ClientProtocolException, IOException {
        HttpPost httpPost = new HttpPost(address);
        try {
            List<NameValuePair> data = buildPostData(params);
            httpPost.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity, HTTP.UTF_8);
        } finally {
            // keepalive自动释放链接
            // httpPost.abort();
        }
    }

    /**
     * Post Ajax 方式请求
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String postAjax(String address, Map<String, Object> params) throws ClientProtocolException, IOException {
        HttpPost httpPost = new HttpPost(address);
        try {
            List<NameValuePair> data = buildPostData(params);
            httpPost.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            httpPost.setHeader("X-Requested-With", "XMLHttpRequest");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity, HTTP.UTF_8);
        } finally {
            // keepalive自动释放链接
            // httpPost.abort();
        }
    }

    /**
     * Post 方式请求（服务于RPC）
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String rpcPost(String address, Map<String, Object> params) throws ClientProtocolException, IOException {
        Transaction t = Cat.newTransaction("Post", "common");
        int pos = address.indexOf("?random=");
        String api = address;
        if (pos != -1) {
            api = address.substring(0, pos);
            String random = address.substring(pos + 8);
            if (params == null) {
                params = new HashMap<String, Object>();
            }
            params.put("random", random);
        }
        URI uri = getURI(api);
        HttpPost httpPost = new HttpPost(uri);
        try {
            t.setStatus(Message.SUCCESS);
            List<NameValuePair> data = buildPostData(params);
            httpPost.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            // HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpResponse httpResponse = httpClient.execute(getHttpHost(uri), httpPost);
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity, HTTP.UTF_8);
        } finally {
            // keepalive自动释放链接
            // httpPost.abort();
            t.complete();
        }

    }

    private ConcurrentMap<String, URI> uriCache = new ConcurrentHashMap<String, URI>();

    private URI getURI(String address) {
        URI uri = uriCache.get(address);
        if (uri == null) {
            uriCache.put(address, uri = URI.create(address));
        }
        return uri;
    }

    private ConcurrentMap<URI, HttpHost> httpHostCache = new ConcurrentHashMap<URI, HttpHost>();

    private HttpHost getHttpHost(URI uri) {
        HttpHost httpHost = httpHostCache.get(uri);
        if (httpHost == null) {
            httpHostCache.put(uri, httpHost = URIUtils.extractHost(uri));
        }
        return httpHost;
    }

    /**
     * Post Body请求
     * 
     * @param address
     * @param body
     * @param charset
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String postBody(String address, String body, String charset) throws ClientProtocolException, IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        HttpPost httpPost = new HttpPost(address);
        try {
            httpPost.setEntity(new StringEntity(body, charset));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            stopWatch.stop();
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity, HTTP.UTF_8);
        } finally {
            // keepalive自动释放链接
            // httpPost.abort();
            stopWatch.log();
        }
    }

    /**
     * Put 方式请求
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String put(String address, Map<String, Object> params) throws ClientProtocolException, IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        HttpPut httpPut = new HttpPut(address);
        try {
            List<NameValuePair> data = buildPostData(params);
            httpPut.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            httpPut.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpPut);
            stopWatch.stop();
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity, HTTP.UTF_8);
        } finally {
            // keepalive自动释放链接
            // httpPost.abort();
            stopWatch.log();
        }
    }

    /**
     * Delete 方式请求
     * 
     * @param String address 请求地址
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String delete(String address) throws ClientProtocolException, IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        HttpDelete httpDelete = new HttpDelete(address);
        try {
            httpDelete.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpDelete);
            stopWatch.stop();
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity, HTTP.UTF_8);
        } finally {
            // keepalive自动释放链接
            // httpPost.abort();
            stopWatch.log();
        }
    }

    /**
     * Post Gzip压缩方式请求
     * 
     * @param String address 请求地址
     * @param String json json串
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String postGzip(String address, String json) throws ClientProtocolException, IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        HttpPost httpPost = new HttpPost(address);
        try {
            httpPost.setEntity(new ByteArrayEntity(GzipUtil.compressString2byte(json)));
            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
            httpPost.setHeader("Accept-Encoding", "gzip");

            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            stopWatch.stop();
            return uncompress(httpResponse);
        } finally {
            httpPost.abort();
            stopWatch.log();
        }
    }

    /**
     * Get 方式请求
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String get(String address, Map<String, Object> params) throws ClientProtocolException, IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        String paramsStr = buildGetData(params);
        HttpGet httpGet = new HttpGet(address + paramsStr);
        try {
            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpGet);
            stopWatch.stop();
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toString(entity, HTTP.UTF_8);
        } finally {
            // keepalive自动释放链接
            // httpGet.abort();
            stopWatch.log();
        }
    }

    /**
     * Post 压缩发送请求，解压缩接收返回数据. If both gzip and deflate compression will be
     * accepted in the HTTP response. please choose the method
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String postWithCompression(String address, Map<String, Object> params) throws ClientProtocolException,
            IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        HttpPost httpPost = new HttpPost(address);
        try {
            List<NameValuePair> data = buildPostData(params);
            httpPost.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            httpPost.setHeader("Accept-Encoding", "gzip,deflate");

            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            stopWatch.stop();
            return uncompress(httpResponse);
        } finally {
            httpPost.abort();
            stopWatch.log();
        }
    }

    /**
     * Get 压缩发送请求，解压缩接收返回数据. If both gzip and deflate compression will be
     * accepted in the HTTP response. please choose the method
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @return String 响应内容
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String getWithCompression(String address, Map<String, Object> params) throws ClientProtocolException,
            IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        String paramsStr = buildGetData(params);
        HttpGet httpGet = new HttpGet(address + paramsStr);
        try {
            httpGet.setHeader("Accept-Encoding", "gzip,deflate");

            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpGet);
            stopWatch.stop();
            return uncompress(httpResponse);
        } finally {
            httpGet.abort();
            stopWatch.log();
        }
    }

    /**
     * Post 压缩发送请求，解压缩接收返回数据. If both gzip and deflate compression will be
     * accepted in the HTTP response. please choose the method
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @param ResponseParser responseParser 自定义解析类
     * @return <T> T 返回对象
     * @throws ClientProtocolException
     * @throws IOException
     */
    public <T> T postWithCompression(String address, Map<String, Object> params, ResponseParser responseParser)
            throws ClientProtocolException, IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        HttpPost httpPost = new HttpPost(address);
        try {
            List<NameValuePair> data = buildPostData(params);
            httpPost.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            httpPost.setHeader("Accept-Encoding", "gzip,deflate");

            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            stopWatch.stop();
            return uncompress(httpResponse, responseParser);
        } finally {
            httpPost.abort();
            stopWatch.log();
        }
    }

    /**
     * Get 压缩发送请求，解压缩接收返回数据. If both gzip and deflate compression will be
     * accepted in the HTTP response. please choose the method
     * 
     * @param String address 请求地址
     * @param Map<String, Object> params 请求参数
     * @param ResponseParser responseParser 自定义解析类
     * @return <T> T 返回对象
     * @throws ClientProtocolException
     * @throws IOException
     */
    public <T> T getWithCompression(String address, Map<String, Object> params, ResponseParser responseParser)
            throws ClientProtocolException, IOException {
        StopWatch stopWatch = new StopWatch("HttpSendClient", true);
        String paramsStr = buildGetData(params);
        HttpGet httpGet = new HttpGet(address + paramsStr);
        try {
            httpGet.setHeader("Accept-Encoding", "gzip,deflate");

            stopWatch.stop();
            HttpResponse httpResponse = httpClient.execute(httpGet);
            stopWatch.stop();
            return uncompress(httpResponse, responseParser);
        } finally {
            httpGet.abort();
            stopWatch.log();
        }
    }

    /**
     * 下载远程文件
     * 
     * @param url 远程地址
     * @param os 输出流
     * @throws ClientProtocolException
     * @throws IOException
     */
    public void download(String url, final OutputStream os) throws ClientProtocolException, IOException {
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            InputStream content = entity.getContent();
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = content.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            content.close();
        } finally {
            httpGet.abort();
        }
    }

    /**
     * 验证HTTP_STAUSE_CODE 是否成功
     * 
     * @param int
     * @return boolean
     * 
     */
    public boolean isOK(int code) {
        return code == HttpStatus.SC_OK;
    }

    /**
     * 关闭HTTPCLIENT
     */
    public synchronized void shutdown() {
        httpClient.getConnectionManager().shutdown();
    }

    private List<NameValuePair> buildPostData(Map<String, Object> params) {
        if (params == null || params.size() == 0) {
            return new ArrayList<NameValuePair>(0);
        }
        List<NameValuePair> ret = new ArrayList<NameValuePair>(params.size());
        for (String key : params.keySet()) {
            Object p = params.get(key);
            if (key != null && p != null) {
                NameValuePair np = new BasicNameValuePair(key, p.toString());
                ret.add(np);
            }
        }
        return ret;
    }

    private String buildGetData(Map<String, Object> params) {
        StringBuilder builder = new StringBuilder();
        if (params != null && params.size() != 0) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key == null || key.trim().length() == 0 || value == null) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append("&");
                } else {
                    builder.append("?");
                }
                builder.append(key).append("=").append(value);
            }
        }
        return builder.toString();
    }

    private String uncompress(HttpResponse httpResponse) throws ParseException, IOException {
        int ret = httpResponse.getStatusLine().getStatusCode();
        if (!isOK(ret)) {
            return null;
        }

        // Read the contents
        String respBody = null;
        HttpEntity entity = httpResponse.getEntity();
        String charset = EntityUtils.getContentCharSet(entity);
        if (charset == null) {
            charset = "UTF-8";
        }

        // "Content-Encoding"
        Header contentEncodingHeader = entity.getContentEncoding();
        if (contentEncodingHeader != null) {
            String contentEncoding = contentEncodingHeader.getValue();
            if (contentEncoding.contains("gzip")) {
                respBody = EntityUtils.toString(new GzipDecompressingEntity(entity), charset);
            } else if (contentEncoding.contains("deflate")) {
                respBody = EntityUtils.toString(new DeflateDecompressingEntity(entity), charset);
            }
        } else {
            // "Content-Type"
            Header contentTypeHeader = entity.getContentType();
            if (contentTypeHeader != null) {
                String contentType = contentTypeHeader.getValue();
                if (contentType != null) {
                    if (contentType.startsWith("application/x-gzip-compressed")) {
                        respBody = EntityUtils.toString(new GzipDecompressingEntity(entity), charset);
                    } else if (contentType.startsWith("application/x-deflate")) {
                        respBody = EntityUtils.toString(new DeflateDecompressingEntity(entity), charset);
                    }
                }
            }
        }
        return respBody;
    }

    private <T> T uncompress(HttpResponse httpResponse, ResponseParser responseParser) throws IllegalStateException,
            IOException {
        int ret = httpResponse.getStatusLine().getStatusCode();
        if (!isOK(ret)) {
            return null;
        }

        // Read the contents
        HttpEntity entity = httpResponse.getEntity();
        String charset = EntityUtils.getContentCharSet(entity);
        if (charset == null) {
            charset = "UTF-8";
        }

        InputStream respBody = entity.getContent();

        // "Content-Encoding"
        Header contentEncodingHeader = entity.getContentEncoding();
        if (contentEncodingHeader != null) {
            String contentEncoding = contentEncodingHeader.getValue();
            if (contentEncoding.contains("gzip")) {
                respBody = new GZIPInputStream(respBody);
            } else if (contentEncoding.contains("deflate")) {
                // respBody = new InflaterInputStream(respBody);
                respBody = (new DeflateDecompressingEntity(entity)).getContent();
            }
        } else {
            // "Content-Type"
            Header contentTypeHeader = entity.getContentType();
            if (contentTypeHeader != null) {
                String contentType = contentTypeHeader.getValue();
                if (contentType != null) {
                    if (contentType.startsWith("application/x-gzip-compressed")) {
                        respBody = new GZIPInputStream(respBody);
                    } else if (contentType.startsWith("application/x-deflate")) {
                        // respBody = new InflaterInputStream(respBody);
                        respBody = (new DeflateDecompressingEntity(entity)).getContent();
                    }
                }
            }
        }
        return responseParser.processResponse(respBody, charset);
    }



}
