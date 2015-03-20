package m.sina;

import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.List;
import java.util.Random;

/**
 * Created by zzzzddddgtuwup on 3/4/15.
 */
public class HtmlUtils {
    private static final String USER_NAME = "derekzhang4484@gmail.com";
    private static final String PASS_WORD = "zzzzddddgggg1234";

    public static String login() throws IOException {
        System.err.println("开始登陆，获取tiket");
        // 设置微博用户名以及密码
        String ticket = requestAccessTicket(USER_NAME,PASS_WORD);
        if (ticket != "false") {
            System.out.println("获取成功:" + ticket);
            System.out.println("开始获取cookies");
            String cookies = sendGetRequest(ticket, null);
            System.out.println("cookies获取成功:" + cookies);
            return cookies;
        } else {
            System.err.println("ticket获取失败，请检查用户名或者密码是否正确!");
            return null;
        }
    }

    public static String sendGetRequest(String url, String cookies)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url)
                .openConnection();
        conn.setRequestProperty("Cookie", cookies);
        conn.setRequestProperty("Referer",
                "http://login.sina.com.cn/signup/signin.php?entry=sso");
        conn.setRequestProperty("User-Agent",generateRandomUserAgent());
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        BufferedReader read = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), "gbk"));
        String line = null;
        StringBuilder ret = new StringBuilder();
        while ((line = read.readLine()) != null) {
            ret.append(line).append("\n");
        }
        StringBuilder ck = new StringBuilder();
        try {
            for (String s : conn.getHeaderFields().get("Set-Cookie")) {
                ck.append(s.split(";")[0]).append(";");
            }

        } catch (Exception e) {
        }
        return ck.toString();
    }

    public static String requestAccessTicket(String username, String password)
            throws IOException {
        username = Base64.encodeBase64String(username.replace("@", "%40")
                .getBytes());
        HttpURLConnection conn = (HttpURLConnection) new URL(
                "https://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.15)")
                .openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Referer",
                "http://login.sina.com.cn/signup/signin.php?entry=sso");
        conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:34.0) Gecko/20100101 Firefox/34.0");
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        out.writeBytes(String
                .format("entry=sso&gateway=1&from=null&savestate=30&useticket=0&pagerefer=&vsnf=1&su=%s&service=sso&sp=%s&sr=1280*800&encoding=UTF-8&cdult=3&domain=sina.com.cn&prelt=0&returntype=TEXT",
                        URLEncoder.encode(username), password));
        out.flush();
        out.close();
        BufferedReader read = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), "gbk"));
        String line = null;
        StringBuilder ret = new StringBuilder();
        while ((line = read.readLine()) != null) {
            ret.append(line).append("\n");
        }
        String res = null;
        try {
            res = ret.substring(ret.indexOf("https:"),
                    ret.indexOf(",\"https:") - 1).replace("\\", "");
        } catch (Exception e) {
            res = "false";
        }
        return res;
    }

    public static String HTMLGet(String url, String cookies) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod("GET");
        conn.setUseCaches(false);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:34.0) Gecko/20100101 Firefox/34.0");
        conn.setRequestProperty("Cookie", cookies);
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4");
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

    public static String HTMLGet(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setRequestMethod("GET");
        conn.setUseCaches(false);
        conn.setRequestProperty("User-Agent", generateRandomUserAgent());
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4");
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

    private static String generateRandomUserAgent(){
        String[] array = {
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/22.0.1207.1 Safari/537.1",
                "Mozilla/5.0 (X11; CrOS i686 2268.111.0) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.57 Safari/536.11",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.6 (KHTML, like Gecko) Chrome/20.0.1092.0 Safari/536.6",
                "Mozilla/5.0 (Windows NT 6.2) AppleWebKit/536.6 (KHTML, like Gecko) Chrome/20.0.1090.0 Safari/536.6",
                "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/19.77.34.5 Safari/537.1",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.9 Safari/536.5",
                "Mozilla/5.0 (Windows NT 6.0) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1063.0 Safari/536.3",
                "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1063.0 Safari/536.3",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_0) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1063.0 Safari/536.3",
                "Mozilla/5.0 (Windows NT 6.2) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1062.0 Safari/536.3",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1062.0 Safari/536.3",
                "Mozilla/5.0 (Windows NT 6.2) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1061.1 Safari/536.3",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1061.1 Safari/536.3",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1061.1 Safari/536.3",
                "Mozilla/5.0 (Windows NT 6.2) AppleWebKit/536.3 (KHTML, like Gecko) Chrome/19.0.1061.0 Safari/536.3",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.24 (KHTML, like Gecko) Chrome/19.0.1055.1 Safari/535.24",
                "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.24 (KHTML, like Gecko) Chrome/19.0.1055.1 Safari/535.24",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:34.0) Gecko/20100101 Firefox/34.0"
        };
        int max = array.length;
        Random random = new Random();
        int s = random.nextInt(max-1);
        return array[s];
    }

    public static void main(String[] args) throws IOException {
        String res = HTMLGet("http://s.weibo.com/wb/%2523MH370%2523&xsort=time&nodup=1&page=2");
        System.out.println(res);
    }
}
