import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;

public class parserTest {
	private List<String> cookies;
	private HttpsURLConnection conn;

	private final String USER_AGENT = "Mozilla/5.0";
	private static final String USER_NAME = "zzzzddddgtuwup@gmail.com";
	private static final String USER_SCREEN_NAME_STRING = "zzzzddddgtuwup";
	private static final String PASS_WORD = "ggggddddzzzz1234";
	private String authenticity_tokenString;

	public List<String> getCookies() {
		return cookies;
	}

	public void setCookies(List<String> cookies) {
		this.cookies = cookies;
	}

	public boolean testIfLogin(String html) {
		Document doc = Jsoup.parse(html);
		Elements head = doc.getElementsByTag("head");
		Elements title = head.get(0).getElementsByTag("title");
		if (title.get(0).text().equals("Login on Twitter")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean testGetJson(String html) {
		return html.charAt(0) == '{';
	}

	public String getFormParams(String html, String username, String password)
			throws UnsupportedEncodingException {

		// System.out.println("Extracting form's data...");

		Document doc = Jsoup.parse(html);
		Elements loginform = doc.getElementsByClass("js-signin");
		Elements inputElements = loginform.get(0).getElementsByTag("input");
		Map<String, String> map = new HashMap<String, String>();
		for (Element inputElement : inputElements) {
			String key = inputElement.attr("name");
			String value = inputElement.attr("value");

			if (key.equals("session[username_or_email]"))
				value = username;
			else if (key.equals("session[password]"))
				value = password;
			map.put(URLEncoder.encode(key, "UTF-8"),
					URLEncoder.encode(value, "UTF-8"));
		}
		this.authenticity_tokenString = map.get("authenticity_token");
		// build parameters list
		StringBuilder result = new StringBuilder();
		result.append("session%5Busername_or_email%5D=")
				.append(map.get("session%5Busername_or_email%5D")).append('&')
				.append("session%5Bpassword%5D=")
				.append(map.get("session%5Bpassword%5D")).append('&')
				.append("authenticity_token=")
				.append(map.get("authenticity_token")).append('&')
				.append("scribe_log=").append(map.get("scribe_log"))
				.append('&').append("redirect_after_login=")
				.append(map.get("redirect_after_login")).append('&')
				.append("authenticity_token=")
				.append(map.get("authenticity_token"));
		return result.toString();
	}

	private String GetPageContent(String url) throws Exception {
		long start = System.currentTimeMillis();
		URL obj = new URL(url);
		conn = (HttpsURLConnection) obj.openConnection();

		// default is GET
		conn.setRequestMethod("GET");

		conn.setUseCaches(false);

		// act like a browser
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		if (cookies != null) {
			for (String cookie : this.cookies) {
				// System.out.println("cookie is " + cookie.split(";", 1)[0]);
				conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
			}
		}
//		int responseCode = conn.getResponseCode();
//		System.out.println("\nSending 'GET' request to URL : " + url);
//		System.out.println("Response Code : " + responseCode);
//		System.out.println("==============================");
		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// Get the response cookies
		setCookies(conn.getHeaderFields().get("set-cookie"));
		return response.toString();
	}

	private String sendPost(String url, String postParams, String referer)
			throws Exception {

		URL obj = new URL(url);
		conn = (HttpsURLConnection) obj.openConnection();

		// Acts like a browser
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Host", "twitter.com");
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Language",
				"en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4");
		for (String cookie : this.cookies) {
			// System.out.println("post cookie is " + cookie.split(";", 1)[0]);
			conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
		}
		// conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty("origin", "https://twitter.com");
		// conn.setRequestProperty("Referer", "https://twitter.com/login?"+
		// refererSuffix);
		conn.setRequestProperty("Referer", referer);
		conn.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length",
				Integer.toString(postParams.length()));

		conn.setDoOutput(true);
		conn.setDoInput(true);
		// Send post request
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(postParams);
		wr.flush();
		wr.close();

		// System.out.println("header is "+ conn.getHeaderFields());

		int responseCode = conn.getResponseCode();
		if (responseCode != 200)
			return null;
		// System.out.println("\nSending 'POST' request to URL : " + url);
		// System.out.println("Post parameters : " + postParams);
		// System.out.println("Response Code : " + responseCode);
		// System.out.println("==============================");

		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		setCookies(conn.getHeaderFields().get("set-cookie"));
		return response.toString();
	}

	public void login() throws Exception {
		CookieHandler.setDefault(new CookieManager());
		String url = "https://twitter.com/login";
		String html = GetPageContent(url);
		String paramsList = getFormParams(html, USER_NAME, PASS_WORD);
		html = sendPost("https://twitter.com/sessions", paramsList, url);
		// System.out.println(html);
	}

	@Deprecated
	public List<String> getFollowersfromSpider(String currentUser)
			throws Exception {
		CookieHandler.setDefault(new CookieManager());
		String url = "https://twitter.com/" + currentUser + "/followers";
		String html = GetPageContent(url);
		if (testIfLogin(html)) {
			// if need login
			String paramsList = getFormParams(html, USER_NAME, PASS_WORD);
			String[] paramsArray = paramsList.split("&");
			String refererSuffix = null;
			for (String s : paramsArray) {
				if (s.contains("redirect_after_login")) {
					refererSuffix = s;
				}
			}
			String referer = "https://twitter.com/login?" + refererSuffix;
			html = sendPost("https://twitter.com/sessions", paramsList, referer);
		}
		return getFollowersFromHtml(html);
	}

	@Deprecated
	public List<String> getFollowersFromHtml(String html) {
		Document doc = Jsoup.parse(html);
		Elements timeline = doc.getElementsByClass("GridTimeline");
		Elements screenNames = timeline.get(0)
				.getElementsByClass("ProfileCard");

		List<String> resultList = new ArrayList<>();
		for (Element e : screenNames) {
			resultList.add(e.attr("data-screen-name"));
		}
		return resultList;
	}

	public List<TwitterUser> getUserInfoFromJsonHtml(String html) {
		Document doc = Jsoup.parse(html);
		Elements screenNames = doc.getElementsByClass("ProfileCard");
		List<TwitterUser> resultList = new ArrayList<>();
		for (Element e : screenNames) {
			// if the twitter is not protected
			if (e.getElementsByClass("ProfileNameTruncated-badges").size() == 0) {
				resultList.add(new TwitterUser(e.attr("data-screen-name"), e
						.attr("data-user-id")));
			}
		}
		return resultList;
	}

	public String sendAjaxRequest(String url) throws Exception {
		URL obj = new URL(url);
		conn = (HttpsURLConnection) obj.openConnection();

		// default is GET
		conn.setRequestMethod("GET");
		conn.setUseCaches(false);

		// act like a browser
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept",
				"application/json, text/javascript, */*; q=0.01");
		conn.setRequestProperty("Accept-Language",
				"en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4");
		if (cookies != null) {
			for (String cookie : this.cookies) {
				// System.out.println("cookie is " + cookie.split(";", 1)[0]);
				conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
			}
		}
		int responseCode = conn.getResponseCode();
		if (responseCode != 200){
			System.out.println(responseCode);
			return null;
		}
			
		// System.out.println("\nSending ajax request to URL : " + url);
		// System.out.println("Response Code : " + responseCode);
		// System.out.println("==============================");
		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// Get the response cookies
		setCookies(conn.getHeaderFields().get("set-cookie"));
		return response.toString();
	}

	public List<TwitterUser> getFollowers(String currentUser) throws Exception {
		String cursor = "-1";
		List<TwitterUser> result = new ArrayList<>();
		while (!cursor.equals("0")) {
			List<TwitterUser> tmpList = new ArrayList<>();
			cursor = twitterAjaxDBInterface(currentUser, "followers", cursor,
					tmpList);
			result.addAll(tmpList);
		}
		return result;
	}

	public List<TwitterUser> getFollowing(String currentUser) throws Exception {
		String cursor = "-1";
		List<TwitterUser> result = new ArrayList<>();
		while (!cursor.equals("0")) {
			List<TwitterUser> tmpList = new ArrayList<>();
			cursor = twitterAjaxDBInterface(currentUser, "following", cursor,
					tmpList);
			result.addAll(tmpList);
		}
		return result;
	}

	public void addUsersUnderOrg(String currentUser, EmbeddedNeo4j db)
			throws Exception {
		String cursor = "-1";
		int count = 0;
		while (!cursor.equals("0")) {
			count++;
			if (count % 30 == 0)
				System.out.println(count*18);
			List<TwitterUser> tmpList = new ArrayList<>();
			cursor = twitterAjaxDBInterface(currentUser, "followers", cursor,
					tmpList);
			if (!tmpList.isEmpty()) {
				try (Transaction tx = db.graphDb.beginTx()) {
					for (TwitterUser u : tmpList) {
						Node tmp = db.graphDb.createNode();
						tmp.setProperty("name", u.name);
						tmp.setProperty("id", u.userId);
					}
					tx.success();
				}
			}
		}
	}

	public void destroyAllFollowingForDeveloper() throws Exception {
		String cursor = "-1";
		while (!cursor.equals("0")) {
			List<TwitterUser> tmpList = new ArrayList<>();
			cursor = twitterAjaxDBInterface(USER_SCREEN_NAME_STRING,
					"following", cursor, tmpList);
			if (!tmpList.isEmpty()) {
				for (TwitterUser u : tmpList) {
					unFollowOneUserUnderOrg(u.userId);
				}
			}
		}
	}

	/*
	 * get followers/following under currentUser. 18 one time
	 */
	public String twitterAjaxDBInterface(String currentUser,
			String instruction, String cursor, List<TwitterUser> list)
			throws Exception {

		String url = "https://twitter.com/"
				+ currentUser
				+ "/"
				+ instruction
				+ "/users?cursor="
				+ cursor
				+ "&cursor_index=&cursor_offset=&include_available_features=1&include_entities=1&is_forward=true";
		String response = sendAjaxRequest(url);
		// System.out.println(response);
		if(response==null){
			System.out.println(url);
			return cursor;
		}
		if (testGetJson(response)) {
			JSONObject jsonObject = new JSONObject(response);
			String html = jsonObject.getString("items_html");
			// System.out.println(html);
			cursor = jsonObject.getString("cursor");
			List<TwitterUser> userList = getUserInfoFromJsonHtml(html);
			list.addAll(userList);
		} else {
			login();
		}
		return cursor;
	}

	/*
	 * follow one user under one group. So that I can reduce the times to build
	 * the graph. I only need to consider the common followers
	 */
	public boolean followOneUserUnderOrg(String userId, String currentUser)
			throws Exception {
		String paramListString = "authenticity_token="
				+ authenticity_tokenString + "&challenges_passed=false"
				+ "&handles_challenges=1" + "&impression_id="
				+ "&inject_tweet=false" + "&user_id=" + userId;
		String url = "https://twitter.com/i/user/follow";
		String referer = "https://twitter.com/" + currentUser + "/followers";
		String response = sendPost(url, paramListString, referer);
		if (response == null) {
			return false;
		}
		JSONObject jsonObject = new JSONObject(response);
		return jsonObject.getString("new_state").equals("following");
	}

	public void unFollowOneUserUnderOrg(String userId) throws Exception {
		String paramListString = "authenticity_token="
				+ authenticity_tokenString + "&challenges_passed=false"
				+ "&handles_challenges=1" + "&impression_id="
				+ "&inject_tweet=false" + "&user_id=" + userId;
		String url = "https://twitter.com/i/user/unfollow";
		String referer = "https://twitter.com/following";
		sendPost(url, paramListString, referer);
	}
	
	public int getFollowersNum(String user) throws Exception{
		String url = "https://twitter.com/" + user;
		long start = System.currentTimeMillis();
		String html = GetPageContent(url);
		long end = System.currentTimeMillis();
		System.out.println("network " + (end - start) + "ms");
		Document doc = Jsoup.parse(html);
		Elements followingTitle = doc.getElementsByClass("ProfileNav-item--followers");
		if(followingTitle.size()==0){
			return 0;
		}else{
			Elements num = followingTitle.get(0).getElementsByClass("ProfileNav-value");
			return Integer.parseInt(num.get(0).ownText().replace(",", ""));
		}
	}
	
	public void getRelationships(EmbeddedNeo4j db) throws Exception{
		ExecutionEngine engine = new ExecutionEngine(db.graphDb);
		ExecutionResult result;
		int total = 0;
		login();
		System.out.println("login done");
		try (Transaction tx = db.graphDb.beginTx()) {
			result = engine.execute("match n return n.name");
			Iterator<String> name_col = result.columnAs("n.name");
			for(String name:IteratorUtil.asIterable(name_col)){
				int follower_num = getFollowersNum(name);
				total+=follower_num;
				System.out.println(name +" has "+follower_num+" followers");
				if (follower_num>2000) {
					System.out.println("=====================");
				}
			}
		}
		System.out.println("total followers are "+total);
	}
	public static void main(String[] args) throws Exception {
		parserTest test = new parserTest();

		EmbeddedNeo4j db = new EmbeddedNeo4j();
		
//		db.cleanDb();
		db.createDb();
		test.getRelationships(db);
		CookieHandler.setDefault(new CookieManager());
//		long start = System.currentTimeMillis();
		// List<TwitterUser> followersList =
		// test.getFollowers("OhioStAthletics");

		// System.out.println(followersList.size());
		// System.out.println(followersList);

		// List<TwitterUser> followingList =
		// test.getFollowing("OhioStAthletics");
		// System.out.println(followingList.size());
		// System.out.println(followingList);

//		test.addUsersUnderOrg("ohiounion", db);
		// test.destroyAllFollowingForDeveloper();
//		long end = System.currentTimeMillis();
//		System.out.println((end - start)/1000 + "s");

		db.shutDown();
	}
}
