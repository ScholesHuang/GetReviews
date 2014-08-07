package com.scholes;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		getWebSiteViaProxy();
	}
	
	private static String getWebSite(){
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://itunes.apple.com/en/rss/customerreviews/id=565993818/page=1/xml");
		System.out.println("executing request "+httppost.getURI());
		 ResponseHandler<String> responseHandler = new ResponseHandler<String>(){
				@Override
				public String handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if(status >= 200 && status <300){
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity,"UTF-8") : null;
					}else{
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}
			};
		String responseBody = null;
		try {
			responseBody = httpClient.execute(httppost, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Page details : "+responseBody);
		return responseBody;
	}
	
	private static String getWebSiteViaProxy(){
		// TODO Auto-generated method stub
		String proxyIp = "intpxy1.hk.hsbc";
		int proxyPort = 8080;
		HttpHost target = new HttpHost("itunes.apple.com", 80, "http");
        HttpHost proxy = new HttpHost(proxyIp, proxyPort);
        
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(proxyIp, proxyPort),
                new UsernamePasswordCredentials("43551416", "schabc680^*)"));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();
        
        RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        HttpGet httpget = new HttpGet("/en/rss/customerreviews/id=565993818/page=1/xml");
        httpget.setConfig(config);
//		        httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        System.out.println("Executing request with Proxy " + httpget.getRequestLine() + " to " + target + " via " + proxy);
        ResponseHandler<String> responseHandler = new ResponseHandler<String>(){
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if(status >= 200 && status <300){
					HttpEntity entity = response.getEntity();
					return entity != null ? EntityUtils.toString(entity,"UTF-8") : null;
				}else{
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}
		};
        String responseBody = null;
		try {
			responseBody = httpclient.execute(target, httpget, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Page details : "+responseBody);
		return responseBody;
		
	}
}
