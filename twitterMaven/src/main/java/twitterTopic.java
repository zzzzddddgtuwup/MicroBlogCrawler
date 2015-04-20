import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * Created by zzzzddddgtuwup on 4/19/15.
 */
public class twitterTopic {
    public String getFirstCursor(String topic) throws IOException {
        String url="";
        if(topic.contains("#")){
            topic = topic.replace("#","");
            url="https://twitter.com/hashtag/"+topic+"?f=realtime&src=tren";
        }else{
            url = "https://twitter.com/search?f=realtime&q="+topic+"&src=tren";
        }
        String firstPage = Utils.HTMLGet(url);
        Document doc = Jsoup.parse(firstPage);
        String cursor = doc.select("div.stream-container").attr("data-scroll-cursor");
        System.out.println(cursor);
        return cursor;
    }

    public String getOnePage(String cursor, String topic) throws IOException {
        String url = "https://twitter.com/i/search/timeline?f=realtime&" +
                "q="+ URLEncoder.encode(topic, "utf-8")+ "&src=tren&include_available_features=1&include_entities=1" +
                "&scroll_cursor="+cursor;
        return Utils.HTMLGet(url);
    }

    public void getTimeSeriesForTopic(String topic) throws IOException, InterruptedException {
        BufferedWriter out=new BufferedWriter(new FileWriter("timeSeries_"+topic+".txt"));

        String cursor = getFirstCursor(topic);
        boolean hasMore = true;

        while(hasMore){
            String jsonRes = getOnePage(cursor,topic);
            JSONObject response = new JSONObject(jsonRes);
            String userTimePair_string = decodeUserAndTime(response.getString("items_html"));
            cursor = response.getString("scroll_cursor");
            hasMore = response.getBoolean("has_more_items");
            System.out.println(userTimePair_string);
            out.write(userTimePair_string);
            Thread.sleep(5000);
        }
        out.close();
    }

    private String decodeUserAndTime(String html){
        Document doc = Jsoup.parse(html);
        Elements cards = doc.select("div.tweet.original-tweet.js-stream-tweet");
        StringBuilder sb = new StringBuilder();
        for(Element e: cards){
            String userName = e.attr("data-screen-name");
            String userId = e.attr("data-user-id");
            String time = e.select("div.content a.tweet-timestamp").attr("title");
            String data_time = e.select("div.content span._timestamp").attr("data-time-ms");
            sb.append(Utils.convert2String(Long.parseLong(data_time))).append('\t').append(userId).append("\n");
//                    .append(userName).append('\t')
//                    .append(time).append('\n');
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
//        new twitterTopic().getFirstCursor("#pappufarce");
        new twitterTopic().getTimeSeriesForTopic("#pappufarce");
    }
}
