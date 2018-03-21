package com.convert.huobi.rest.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpUtilManager {

	private static HttpUtilManager instance = new HttpUtilManager();
	private static HttpClient client;
	private static long startTime = System.currentTimeMillis();
	public static PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

	private static ConnectionKeepAliveStrategy keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {

		@Override
		public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
			long keepAlive = super.getKeepAliveDuration(response, context);

			if (keepAlive == -1) {
				keepAlive = 5000;
			}
			return keepAlive;
		}

	};

	private HttpUtilManager() {
		HttpHost proxy = new HttpHost("192.168.1.115", 11111, "http");
		DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
		client = HttpClients.custom().setConnectionManager(cm).setKeepAliveStrategy(keepAliveStrat).setRoutePlanner(routePlanner).build();
	}

	public static void IdleConnectionMonitor() {

		if (System.currentTimeMillis() - startTime > 30000) {
			startTime = System.currentTimeMillis();
			cm.closeExpiredConnections();
			cm.closeIdleConnections(30, TimeUnit.SECONDS);
		}
	}

	private static RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(20000).setConnectTimeout(20000)
			.setConnectionRequestTimeout(20000).build();

	public static HttpUtilManager getInstance() {
		return instance;
	}

	public HttpClient getHttpClient() {
		return client;
	}

	private HttpPost httpPostMethod(String url) {
		return new HttpPost(url);
	}

	private HttpRequestBase httpGetMethod(String url) {
		return new HttpGet(url);
	}

	public String requestHttpGet(String url_prex, String url) throws HttpException, IOException {

		return requestHttpGet(url_prex, url, null);
	}

	public String requestHttpGet(String url_prex, String url, Map<String, String> param)
			throws HttpException, IOException {

		IdleConnectionMonitor();
		if (param != null) {
			url = url_prex + url + "?" + ParamUtils.getGetParam(param);
		} else {
			url = url_prex + url;
		}
		System.out.println("url=" + url);

		HttpRequestBase method = this.httpGetMethod(url);
		method.setConfig(requestConfig);
		method.addHeader("Content-Type", "application/x-www-form-urlencoded");
		HttpResponse response = client.execute(method);
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			return "";
		}
		InputStream is = null;
		String responseData = "";
		try {
			is = entity.getContent();
			responseData = IOUtils.toString(is, "UTF-8");
		} finally {
			if (is != null) {
				is.close();
			}
		}
		return responseData;
	}
	
	public String requestHttpPost(String url_prex, String url, Map<String, String> signMap)
			throws HttpException, IOException {
		return this.requestHttpPost(url_prex, url, signMap, null);
	}

	public String requestHttpPost(String url_prex, String url, Map<String, String> signMap, Map params)
			throws HttpException, IOException {

		IdleConnectionMonitor();
		url = url_prex + url + "?" + ParamUtils.getGetParam(signMap);
		HttpPost method = this.httpPostMethod(url);
		method.setConfig(requestConfig);
		String json = this.convertMap2Json(params);
		System.out.println(json);
		StringEntity strEntity = new StringEntity(json,"utf-8");		
		strEntity.setContentEncoding("UTF-8"); 
		strEntity.setContentType("application/json");  

		method.setEntity(strEntity);
		
		HttpResponse response = client.execute(method);
		
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			return "";
		}
		InputStream is = null;
		String responseData = "";
		try {
			is = entity.getContent();
			responseData = IOUtils.toString(is, "UTF-8");
		} finally {
			if (is != null) {
				is.close();
			}
		}
		return responseData;

	}
	private String convertMap2Json(Map params) {
		
		if (params == null) {
			return "{}";
		}
		
		List<String> keys = new ArrayList<String>(params.keySet());
		int keySize = keys.size();
		JSONObject jsonParam = new JSONObject(); 
		for (int i = 0; i < keySize; i++) {
			String key = keys.get(i);
			jsonParam.put(key, params.get(key));
		}
		return jsonParam.toString();
	}

}
