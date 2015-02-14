package m.sina;

import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by zzzzddddgtuwup on 2/12/15.
 */
public class mSinaAnalysis {
    private List<String> cookies;
    private final String USER_AGENT = "Mozilla/5.0";
    private static final String USER_NAME = "derekzhang4484%40gmail.com";
    private static final String PASS_WORD = "zzzzddddgggg";

    public List<String> getCookies() {
        return cookies;
    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

    public void login() throws IOException {
        CookieHandler.setDefault(new CookieManager());
        String url = "https://passport.weibo.cn/sso/login";
        String postParams = "username=" + USER_NAME + "&password=" + PASS_WORD +
                "&savestate=1&ec=0&pagerefer=http%3A%2F%2Fpassport.sina.cn%2Fsso%2Flogout%3Fentry%3Dmweibo%26r%3Dhttp%253A%252F%252Fm.weibo.cn&entry=mweibo&loginfrom=&client_id=&code=&hff=&hfp=";
        URL obj = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();

        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4");
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        conn.setRequestProperty("origin", "https://passport.weibo.cn");
        conn.setRequestProperty("Host", "passport.weibo.cn");
        conn.setRequestProperty("Referer", "https://passport.weibo.cn/signin/login?entry=mweibo&res=wel&wm=3349&r=http%3A%2F%2Fm.weibo.cn%2F");

        conn.setDoOutput(true);
        conn.setDoInput(true);
        // Send post request
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(postParams);
        wr.flush();
        wr.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(
                conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        setCookies(conn.getHeaderFields().get("Set-Cookie"));
        System.out.println("cookie is " + getCookies());
        int responseCode = conn.getResponseCode();
        System.out.println("login responsecode is " + responseCode);
    }

    public String HTMLGet(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod("GET");
        conn.setUseCaches(false);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4");
        for (String cookie : this.cookies) {
            conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(
                conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //System.out.println(response.toString());
        return response.toString();
    }

    public int getPageNum(String firstPageJson) throws JSONException {
        JSONArray jsonArray = new JSONArray(firstPageJson);
        JSONObject jsonObj = jsonArray.getJSONObject(1);
        return jsonObj.getInt("maxPage");
    }

    private void decodeCardGroup(JSONArray cardGroup) throws JSONException {
        for (int i = 0; i < cardGroup.length(); i++) {
            JSONObject jsonObj = cardGroup.getJSONObject(i);
            JSONObject user = jsonObj.getJSONObject("user");
            System.out.println(user.get("id").toString() + " " + user.getString("screen_name") + " " +
                    removeTag(jsonObj.getString("text")) + " " + jsonObj.getString("created_at"));
        }
//        return cardGroup.length();
    }

    public void getAllRepostForTweet(String url) throws IOException, JSONException {
        String firstPage = HTMLGet(url + "page=1");
        int maxPage = getPageNum(firstPage);
        JSONArray cardGroup = new JSONArray(firstPage).getJSONObject(1).getJSONArray("card_group");
        decodeCardGroup(cardGroup);
        for (int i = 2; i < maxPage; i++) {
            String html = HTMLGet(url + "page=" + i);
            decodeCardGroup(new JSONArray(html).getJSONObject(0).getJSONArray("card_group"));
        }
    }

    //remove all tags and whitespace
    public String removeTag(String s) {
        return s.replaceAll("<\\/?[^>]*>", "").replaceAll(" ", "");
    }

    public static void main(String[] args) throws IOException, JSONException {
        mSinaAnalysis test = new mSinaAnalysis();
        test.login();
        test.getAllRepostForTweet("http://m.weibo.cn/single/rcList?format=cards&id=3808949927299183&type=repost&hot=0&");
//        String secondpage = test.HTMLGet("http://m.weibo.cn/single/rcList?format=cards&id=3808949927299183&type=repost&hot=0&page=2");
    }
}