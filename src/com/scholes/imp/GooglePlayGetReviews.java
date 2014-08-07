package com.scholes.imp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.scholes.GetReviews;
import com.scholes.util.ExcelExport;

public class GooglePlayGetReviews implements GetReviews {
	
	private Properties androidParams = null;
	private String androidParamsPath = "params.properties";
	private String postURL;
	private String reviewType;
	private String fromPageNum;
	private String ID;
	private String reviewSortOrder;
	private String XHR;
	private String outputFilePath;
	HttpClient httpClient = new DefaultHttpClient();
	private String cookies;
	private final String USER_AGENT = "Mozilla/5.0";
	String loginUrl ;
	private boolean useProxy;
	private boolean enablePeriod;
	private SimpleDateFormat fmt;
	private String dateFormat;
	private Date startDate;
	private String lastEntryAuthorId;
	private int continueAttempts = 3;
	public GooglePlayGetReviews() {
		super();
		
		this.androidParams = new Properties();
		try {
			FileInputStream temp = new FileInputStream(androidParamsPath);
			androidParams.load(temp);
			temp.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		initParams();
		// make sure cookies is turn on
        CookieHandler.setDefault(new CookieManager());
		//loginAndSaveCookie();
	}

	private void initParams(){
		if(!androidParams.isEmpty()){
			postURL = androidParams.getProperty("GOOGLEPLAY_POST_URL");
			reviewType = androidParams.getProperty("GOOGLEPLAY_REVIEW_TYPE");
			fromPageNum = androidParams.getProperty("GOOGLEPLAY_FROM_PAGE_NUM");
			ID = androidParams.getProperty("GOOGLEPLAY_ID");
			reviewSortOrder = androidParams.getProperty("GOOGLEPLAY_REVIEW_SORT_ORDER");
			XHR = androidParams.getProperty("GOOGLEPLAY_XHR");
			outputFilePath = androidParams.getProperty("GOOGLEPLAY_OUTPUTFILE_PATH");
			enablePeriod = Boolean.valueOf(androidParams.getProperty("GOOGLEPLAY_ENABLE_PERIOD"));
			if(this.enablePeriod){
				dateFormat = androidParams.getProperty("GOOGLEPLAY_DATE_FORMAT");
				String startDateStr = androidParams.getProperty("GOOGLEPLAY_START_DATE");
				String endDateStr = androidParams.getProperty("GOOGLEPLAY_END_DATE");
				if(!("".equals(dateFormat) || "".equals(startDateStr) || "".equals(endDateStr))){
					try {
						fmt = new SimpleDateFormat(dateFormat);
						startDate = fmt.parse(startDateStr);
					} catch (ParseException e) {
						this.enablePeriod = false;
						e.printStackTrace();
					}
				}else{
					this.enablePeriod = false;
				}
				
			}
			loginUrl = androidParams.getProperty("GOOGLEPLAY_LOGIN_URL");
		}
	}
	
	private void loginAndSaveCookie(){
		HttpPost loginpost = new HttpPost(loginUrl);
        List<NameValuePair> params = null;
		try {
			 String html = this.GetPageContent(loginUrl);
			params = getFormParams(html,"htsdeploy@gmail.com","hsbc123456");
		} catch (UnsupportedEncodingException e2) {
			e2.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
//        params.add(new BasicNameValuePair("Email", "select.zhenyu.from@gmail.com"));   
//        params.add(new BasicNameValuePair("Passwd", "scholes616"));   
//        params.add(new BasicNameValuePair("PersistentCookie", "yes"));
//        params.add(new BasicNameValuePair("service", "googleplay"));
//        params.add(new BasicNameValuePair("continue", "https://play.google.com/store"));
     // add header
		
        loginpost.setHeader("Host", "accounts.google.com");
        loginpost.setHeader("User-Agent", USER_AGENT);
        loginpost.setHeader("Accept", 
             "text/html,application/xhtml+xml,application/xml;q=0.9,*;q=0.8");
        loginpost.setHeader("Accept-Language", "en-US,en;q=0.5");
        loginpost.setHeader("Cookie", getCookies());
        loginpost.setHeader("Connection", "keep-alive");
        loginpost.setHeader("Referer", "https://accounts.google.com/ServiceLoginAuth");
        loginpost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        try {   
        	loginpost.setEntity(new UrlEncodedFormEntity(params));   
        	HttpResponse hr = httpClient.execute(loginpost);
        	System.out.println(hr);
        } catch (UnsupportedEncodingException e1) {  
            e1.printStackTrace();   
        } catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			loginpost.releaseConnection();
		}
	}

	private JSONArray convertTOJSONOBject(String str) throws JSONException{
		str = str.substring(str.indexOf("["));
		JSONArray array = new JSONArray(str);
		return array;
	}
	
	public static void main(String[] args){
		GooglePlayGetReviews obj = new GooglePlayGetReviews();
		try {
			obj.exportAllData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void exportAllData() throws Exception{
		String rawData = "";
		ExcelExport<Entry> excelExport = new ExcelExport<Entry>(outputFilePath);
		int pageNum = Integer.valueOf(fromPageNum);
		List<Entry> allEntries = new ArrayList(0);
		while(!"".equals(rawData = rawReviewsData(pageNum))){
			
			List<Entry> entries = readEntries(rawData);
			if(entries.get(0) != null && entries.get(0).authorId.equals(lastEntryAuthorId)){
				break;
			}else{
				lastEntryAuthorId = entries.get(0).authorId;
				allEntries.addAll(entries);
			}
			
			System.out.println(entries);
			System.out.println("Page : "+pageNum);
			pageNum++;
		}
		excelExport.createSheet("Google Play", Entry.getHeaders(), 0, allEntries);
		excelExport.closeWorkbook();
	}
	
	
	@Override
	public String rawReviewsData(int pageNum) {
		HttpPost httppost = null;
		String responseBody = "";
		try {
			StringBuffer sb = new StringBuffer(postURL);
			sb.append("?reviewType="+reviewType)
			.append("&pageNum="+pageNum)
			.append("&id="+ID)
			.append("&reviewSortOrder="+reviewSortOrder)
			.append("&xhr="+XHR);
			
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
			if(useProxy){
				String pacUrl = androidParams.getProperty("PROXY_PAC");
				//px = ProxyUtil.getProxyInfoFromPAC(pacUrl, sb.toString());
				String username = androidParams.getProperty("PROXY_USER");
				String password = androidParams.getProperty("PROXY_PASSWORD");
				String proxyIp = androidParams.getProperty("PROXY_IP");
				int proxyPort = Integer.valueOf(androidParams.getProperty("PROXY_PORT"));
				if(sb.toString().startsWith("http://")){
					sb = sb.delete(0, 7);
				}else if(sb.toString().startsWith("https://")){
					sb = sb.delete(0, 8);
				}
				HttpHost target = new HttpHost(sb.toString(), 80, "http");
		        HttpHost proxy = new HttpHost(proxyIp, proxyPort);
		        
				CredentialsProvider credsProvider = new BasicCredentialsProvider();
		        credsProvider.setCredentials(
		                new AuthScope(proxyIp, proxyPort),
		                new UsernamePasswordCredentials(username, password));
		        CloseableHttpClient httpclient = HttpClients.custom()
		                .setDefaultCredentialsProvider(credsProvider).build();
		        
		        RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
		        int startIndexOfRelativeAddress = sb.indexOf("/");
	            HttpGet httpget = new HttpGet(sb.substring(startIndexOfRelativeAddress));
	            httpget.setConfig(config);
//	            httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	            System.out.println("Executing request with Proxy " + httpget.getRequestLine() + " to " + target + " via " + proxy);
	            responseBody = httpclient.execute(target, httpget, responseHandler);
			}else{
				HttpClient httpClient = new DefaultHttpClient();
				httppost = new HttpPost(sb.toString());
				System.out.println("executing request "+httppost.getURI());
				responseBody = httpClient.execute(httppost, responseHandler);
			}

			//handle json
			JSONArray jsonObj = convertTOJSONOBject(responseBody);
			if(jsonObj.getJSONArray(0).length() > 2){
				String obj = jsonObj.getJSONArray(0).getString(2);
				System.out.println("---------------------------------");
				System.out.println(obj);
				System.out.println("---------------------------------");
				return obj;
			}else{
				return "";
			}
		} catch (Exception e) {
			if(e instanceof ClientProtocolException){
				System.out.println(e.getMessage() +" : End Page");
			}else{
				e.printStackTrace();
			}
		}finally{
			if(httppost != null){
				httppost.releaseConnection();
			}
		}
		return "";
	}
	
	private List<Entry> readEntries(String obj){
		// create an instance of HtmlCleaner
		HtmlCleaner cleaner = new HtmlCleaner();
		 
		// take default cleaner properties
		CleanerProperties props = cleaner.getProperties();
		 
		// customize cleaner's behaviour with property setters
		props.setOmitDoctypeDeclaration(true);
		props.setOmitHtmlEnvelope(true);
		props.setOmitXmlDeclaration(true);
		 
		// Clean HTML taken from simple string, file, URL, input stream, 
		// input source or reader. Result is root node of created 
		// tree-like structure. Single cleaner instance may be safely used
		// multiple times.
		TagNode node = cleaner.clean(obj);
		List<Entry> entriesList = new ArrayList<Entry>(0);
		TagNode[]  entryNodes = node.getElementsByAttValue("class", "single-review", false, false);
		if(entryNodes != null && entryNodes.length > 0){
			for(TagNode singleReview : entryNodes){
				String authorId = "";
				String authorName;
				String reviewDate;
				String starRating;
				String reivewTitle;
				String reviewBody;
				TagNode[] authorNames = singleReview.getElementsByAttValue("class", "author-name", true, false);
				authorName = authorNames[0].getText().toString();
				
				try {
					authorId = (singleReview.getElementsHavingAttribute("data-userid", false))[0].getAttributeByName("data-userid");
				} catch (Exception e) {
					System.out.println("It is Google User , so no user id : "+authorName);
					//<a href="/store/people/details?id=100438274427121954519">subhamay sur</a>
					try {
						TagNode[] nodes = authorNames[0].getElementsHavingAttribute("href", true);
						String href = nodes[0].getAttributeByName("href");
						authorId = href.substring(href.indexOf("id=")+3);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				
				TagNode[] reviewDates = singleReview.getElementsByAttValue("class", "review-date", true, false);
				reviewDate = reviewDates[0].getText().toString();
				//check periods
				if(this.enablePeriod && !"".equals(reviewDate)){
					try {
						String dateStr = reviewDate.substring(0, this.dateFormat.length());
						Date reviewDate2 = fmt.parse(dateStr);
						if(reviewDate2.before(startDate)){
							break;
						}
					} catch (ParseException e) {
						e.printStackTrace();
						continue;
					}
				}
				
				TagNode[] starRatings = singleReview.getElementsByAttValue("class", "current-rating", true, false);
				starRating = starRatings[0].getAttributeByName("style");
				starRating = convertStarRatings(starRating);
				TagNode[] reviewBodys = singleReview.getElementsByAttValue("class", "review-body", true, false);
				reviewBody = reviewBodys[0].getText().toString();
				
				reivewTitle = reviewBodys[0].getElementsByAttValue("class", "review-title", false, false)[0].getText().toString();
				entriesList.add(new Entry(authorId,authorName,reviewDate,starRating,reivewTitle,reviewBody));
			}
		}
		return entriesList;
	}
	
	private String convertStarRatings(String str){
		if(str == null) return "";
		String ret = "";
		if(str.contains("100%")){
			ret = "5";
		}else if(str.contains("80%")){
			ret = "4";
		}else if(str.contains("60%")){
			ret = "3";
		}else if(str.contains("40%")){
			ret = "2";
		}else if(str.contains("20%")){
			ret = "1";
		}
		
		return ret;
	}
	
	public static class Entry{
		public final String authorId;
		public final String authorName;
		public final String reviewDate;
		public final String starRating;
		public final String reivewTitle;
		public final String reviewBody;
		public Entry(String authorId, String authorName, String reviewDate, String starRating,
				String reivewTitle, String reviewBody) {
			super();
			this.authorId = authorId;
			this.authorName = authorName;
			this.reviewDate = reviewDate;
			this.starRating = starRating;
			this.reivewTitle = reivewTitle;
			this.reviewBody = reviewBody;
		}
		@Override
		public String toString() {
			return this.authorId+" <> "+this.authorName+" <> "+this.reviewDate+" <> "+this.starRating+" <> "+this.reivewTitle+" <> "+this.reviewBody+"\r\n";
		}
		
		public static String[] getHeaders(){
			return new String[]{"Author-Id","Author","Review-Date","Rating","Review-Title","Review-Body"};
		}
		
	}



	public String getCookies() {
		return cookies;
	}

	public void setCookies(String cookies) {
		this.cookies = cookies;
	}
	 public List<NameValuePair> getFormParams(
             String html, String username, String password)
                        throws UnsupportedEncodingException {
 
        System.out.println("Extracting form's data...");
 
        Document doc = Jsoup.parse(html);
        // Google form id
        Element loginform = doc.getElementById("gaia_loginform");
        Elements inputElements = loginform.getElementsByTag("input");
        List<NameValuePair> paramList = new ArrayList<NameValuePair>();
 
        for (Element inputElement : inputElements) {
                String key = inputElement.attr("name");
                String value = inputElement.attr("value");
 
                if (key.equals("Email"))
                        value = username;
                else if (key.equals("Passwd"))
                        value = password;
 
                paramList.add(new BasicNameValuePair(key, value));
 
        }
 
        return paramList;
  }
	 
 private String GetPageContent(String url) throws Exception {
	 
        HttpGet request = new HttpGet(url);
 
//        request.setHeader("User-Agent", USER_AGENT);
//        request.setHeader("Accept",
//                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
//        request.setHeader("Accept-Language", "en-US,en;q=0.5");
 
        HttpResponse response = httpClient.execute(request);
        int responseCode = response.getStatusLine().getStatusCode();
 
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
 
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));
 
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
                result.append(line);
        }
 
        // set cookies
        setCookies(response.getFirstHeader("Set-Cookie") == null ? "" : 
                     response.getFirstHeader("Set-Cookie").toString());
 
        return result.toString();
 
  }

}
