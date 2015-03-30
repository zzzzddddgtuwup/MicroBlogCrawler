package m.sina;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static m.sina.HtmlUtils.HTMLGet;

public class sinaTopic {
    public EmbeddedNeo4j db;

    public sinaTopic() {
        this.db = new EmbeddedNeo4j();
    }

    public static void main(String[] args) throws Exception {
        String[] topics = {"#伊能静秦昊婚礼#", "#广州恒大VS石家庄永昌#",
                "#日本最美女高中生#", "#央视主播炮轰收视率#",
                "#男人下厨9大理由#", "#王学兵张博吸毒被拘#"};
        sinaTopic test = new sinaTopic();
        test.testOnTopic();
    }

    private static String topic = "#2015重庆国际马拉松#";

    public void testOnTopic() throws IOException, ParseException, InterruptedException {
//        System.out.println("开始搜索微博");
//        String url = "http://s.weibo.com/wb/" + URLEncoder.encode("#你女朋友掉色了#","utf-8") + "&xsort=time&timescope=custom:2015-03-23-02:2015-03-23-02&nodup=1";
//        String url = "http://s.weibo.com/wb/" + URLEncoder.encode("#2015重庆国际马拉松#","utf-8") + "&xsort=time&nodup=1";
//        String n_url = url + "&page=1";
//        String res = HtmlUtils.HTMLGet(n_url, HtmlUtils.getCookie());
//        System.out.println(getHtmlFromJs(res));

//        testAvailability("#李光耀去世#", "2015-03-23-18", "2015-03-24-02");
        db.cleanDb();
        db.createDb();
//        searchTopic("2015-03-21-18", "2015-03-25-08");
        searchSmallTopic();
        db.shutDown();
    }

    public void testAvailability(String startTime, String endTime) throws IOException, ParseException, InterruptedException {
        List<String> l = Tools.getTimeSeries(startTime, endTime);
        for (String time : l) {
            System.err.println("time is " + time);
            String url = "http://s.weibo.com/wb/" + URLEncoder.encode(topic, "utf-8") + "&xsort=time&timescope=custom:" + time + ":" + time + "&nodup=1";
            String firstPage = getHtmlFromJs(HtmlUtils.HTMLGet(url, HtmlUtils.getCookie()));
            System.out.println(getTotalResNumInTimeBlk(firstPage));
            Thread.sleep(1000 * 3);
        }
    }

    public void searchTopic(String startTime, String endTime) throws IOException, ParseException, InterruptedException {
        List<String> l = Tools.getTimeSeries(startTime, endTime);
        for (String time : l) {
            System.out.println("time is " + time);
            String url = "http://s.weibo.com/wb/" + URLEncoder.encode(topic, "utf-8") + "&xsort=time&timescope=custom:" + time + ":" + time + "&nodup=1";
            String firstPage = getHtmlFromJs(HtmlUtils.HTMLGet(url, HtmlUtils.getCookie()));
            int totalWeibo = getTotalResNumInTimeBlk(firstPage);
            //if the number is larger than 1000. Try get all original weibo first
            if (totalWeibo > 1000) {
                System.err.println("get all original weibo");
                String ori_url = url + "&scope=ori";
                GetAllPages(ori_url, true);
                //get all repost weibo
                GetAllPages(url, false);
            } else {
                GetAllPages(url, true);
            }
        }
    }

    public void searchSmallTopic() throws IOException, InterruptedException {
        String url = "http://s.weibo.com/wb/" + URLEncoder.encode(topic, "utf-8") + "&xsort=time&nodup=1";
        //get all repost weibo
        GetAllPages(url, true);
    }

    private void GetAllPages(String url, boolean goDeep) throws IOException, InterruptedException {
        String firstPage = getHtmlFromJs(HtmlUtils.HTMLGet(url, HtmlUtils.getCookie()));
        int maxpage = getMaxPage(firstPage);
        Thread.sleep(1000 * 3);
        for (int i = maxpage; i >= 2; i--) {
            System.out.println("current page is " + i);
            String n_url = url + "&page=" + i;
            String res = HtmlUtils.HTMLGet(n_url, HtmlUtils.getCookie());
            decodeHtml(getHtmlFromJs(res), goDeep);
            Thread.sleep(1000 * 3);
        }
        System.out.println("current page is 1");
        decodeHtml(firstPage, goDeep);
    }

    private String getHtmlFromJs(String s) {
        Matcher m = Pattern.compile("<script>STK && STK.pageletM && STK.pageletM.view\\((\\{\"pid\":\"pl_wb_feedlist\".*?)\\)</script>").matcher(s);
        String js = "";
        if (m.find()) {
            js = m.group();
            int indexLeft = js.indexOf("{");
            int indexRight = js.indexOf("}");
            String json = js.substring(indexLeft, indexRight + 1);
            return new JSONObject(json).getString("html");
        } else {
            return js;
        }
    }

    private void decodeHtml(String html, boolean deepOnOriginalWeibo) throws IOException, InterruptedException {
        //System.out.println(html);
        Document doc = Jsoup.parse(html);
        if (doc.select("div.pl_noresult").size() != 0) {
            System.err.println("No result");
            return;
        }
        Elements cards = doc.select("div.WB_cardwrap.S_bg2.clearfix");
        for (int i = cards.size() - 1; i >= 0; i--) {
            Element card = cards.get(i);
            Elements div = card.select("div[mid]");
            String mid = div.attr("mid");
            String name = card.select("a.name_txt").attr("nick-name");
            String text = card.select("div.feed_content.wbcon >p.comment_txt").text();
            String time = card.select("div.feed_from.W_textb > a[title]").text();
            time = Tools.dateToStr(Tools.convertCalendar(time));


            String uid = card.select("div.feed_content.wbcon > a").attr("usercard");
            if (uid == null || uid.isEmpty()) {
                uid = card.select("div.content.clearfix > div.feed_from.W_textb").select("a").first().attr("href");
                int last = uid.lastIndexOf('/');
                uid = ParseUtils.getNumber(uid.substring(0, last));
            } else {
                int index = uid.indexOf('&');
                uid = ParseUtils.getNumber(uid.substring(3, index));
            }

            System.out.println(
                    "user id is " + uid + "," +
                            "name:" + name + "," +
                            "text:" + text + "," +
                            "time:" + time
            );

            if (div.hasAttr("isforward") && div.attr("isforward").equals("1")) {
                String ori_text = card.select("div.comment_info").select("p.comment_txt").text();
                if (ori_text.contains(topic)) {
                    continue;
                }
                String originalUser = card.select("a.W_texta.W_fb").attr("nick-name");
                if (originalUser == null || originalUser.isEmpty()) {
                    System.out.println("weibo is deleted");
                    continue;
                }
                text += "//@" + originalUser + ":";
                int end = text.indexOf("//@");
                if (!text.substring(0, end).contains(topic)) {
                    continue;
                }
                String originalTime = card.select("div.comment_info").select("div.feed_from.W_textb > a[date]").text();
                originalTime = Tools.dateToStr(Tools.convertCalendar(originalTime));
                String originalId = card.select("a.W_texta.W_fb").attr("usercard");
                originalId = originalId.substring(3);
                addToGraph(name, uid, time, text, originalUser, originalId, originalTime);

                String repost = card.select("ul.feed_action_info.feed_action_row4 > li").get(1).text();
                repost = repost.replaceAll("转发", "");
                //if the tweet has been reposted, go deep to search
                if (repost.length() != 0) {
                    System.out.println("mid is " + mid);
                    Node current = db.findByScreenName(name);
                    String url = "http://m.weibo.cn/single/rcList?format=cards&id=" + mid + "&type=repost&hot=0&";
                    getAllRepostForTweet(url, current);
//                    Thread.sleep(1000 * 10);
                }
            } else {
                Node current = db.findByScreenName(name);
                if (current == null) {
                    System.out.println("create user node");
                    current = db.createNode(uid, name, time);
                }else if(!db.hasNodeProperty(current,"time")){
                    db.nodeAddTime(current,time);
                    System.out.println("add time to node");
                }
                if (deepOnOriginalWeibo) {
                    String repost = card.select("ul.feed_action_info.feed_action_row4 > li").get(1).text();
                    repost = repost.replaceAll("转发", "");
                    //if the tweet has been reposted, go deep to search
                    if (repost.length() != 0) {
                        System.out.println("mid is " + mid);
                        String url = "http://m.weibo.cn/single/rcList?format=cards&id=" + mid + "&type=repost&hot=0&";
                        getAllRepostForTweet(url, current);
//                        Thread.sleep(1000 * 10);
                    }
                }
            }
        }
    }

    private void addToGraph(String screen_name, String id, String time, String text, String ori_name, String ori_id, String ori_time) {
        System.out.println(",original time:" + ori_time + ",original id:" + ori_id);

        List<String> l = Tools.deleteDuplicate(text);
        //get current user node
        Node current = db.findByScreenName(screen_name);

        int i;
        for (i = 0; i < l.size(); i++) {
            String prevName = l.get(i);
            if (prevName.equals(screen_name)) continue;

            Node prevUser = db.findByScreenName(prevName);

            if (prevUser != null) {
                //create user node if not exists
                if (current == null) {
                    System.out.println("create user node");
                    current = db.createNode(id, screen_name);
                }
                boolean createRel = updateRelationship(prevUser, current, time);
                if (createRel) {
                    System.out.println("add link from " + prevName + " to " + screen_name);
                } else {
                    System.out.println("relationship already exists");
                }
                break;
            }
        }
        if (i == l.size()) {
            //repost from self and the original tweet does not has hashtag
            if (ori_name.equals(screen_name)) {
                if (current == null) {
                    System.out.println("create current_User");
                    db.createNode(id, screen_name, time);
                }
            } else {
                System.out.println("create ori_User");
                Node ori = db.createNode(ori_id, ori_name);
                if (current == null) {
                    System.out.println("create user node");
                    current = db.createNode(id, screen_name);
                }
                boolean createRel = updateRelationship(ori, current, time);
                if (createRel) {
                    System.out.println("add link from " + ori_name + " to " + screen_name);
                } else {
                    System.out.println("relationship already exists");
                }
            }
        }

    }

    private int getMaxPage(String html) {
        //System.out.println(html);
        Document doc = Jsoup.parse(html);
        if (!doc.select("p.noresult_tit").isEmpty()) {
            return 0;
        }
        if (doc.select("div.WB_cardwrap.S_bg2.relative").select("li").isEmpty()) {
            return 1;
        }
        Element li = doc.select("div.WB_cardwrap.S_bg2.relative").select("li").last();
        String text = li.text();
        String result = text.substring(1, text.length() - 1);
//        System.out.println(result);
        return Integer.parseInt(result);
    }

    private int getTotalResNumInTimeBlk(String html) {
        Document doc = Jsoup.parse(html);
        System.out.println("##################");
        String search_num = doc.select("div.search_num > span").text();
        if (search_num == null || search_num.isEmpty()) {
            System.out.println("only one page");
        } else {
            System.out.println(search_num);
        }
        System.out.println("##################");
        if (search_num != null && !search_num.trim().equals("")) {
            String result = ParseUtils.getNumber(search_num);
            return Integer.parseInt(result);
        } else {
            return 0;
        }
    }

    private boolean updateRelationship(Node start, Node end, String time) {
        Relationship relationship = db.findRelByNodes(start, end);
        if (relationship == null) {
            db.addRelWithTime(start, end, time);
            return true;
        } else {
            String timeString = db.getRelTime(relationship);
            if (Tools.timeBefore(time, timeString)) {
                db.updateRelTime(relationship, timeString);
            }
            return false;
        }
    }

    public void getAllRepostForTweet(String url, Node author) throws IOException, InterruptedException {
        String firstPage = HTMLGet(url + "page=1");
        int maxPage = ParseUtils.getPageNum(firstPage);
        System.out.println("Maxcard is " + maxPage);
        for (int i = maxPage; i >= 2; i--) {
            String html = HTMLGet(url + "page=" + i);
            System.out.println("current card page is " + i);
            try {
                JSONObject jo = new JSONArray(html).getJSONObject(0);
                if (jo.getString("mod_type").equals("mod/empty")) {
                    continue;
                }
                decodeCardGroup(jo.getJSONArray("card_group"), author);
            } catch (JSONException e) {
                e.printStackTrace();
                Thread.sleep(1000 * 5);
                ++i;
            }
            Thread.sleep(500);
        }
        try {
            JSONArray cardGroup = new JSONArray(firstPage).getJSONObject(1).getJSONArray("card_group");
            decodeCardGroup(cardGroup, author);
        } catch (JSONException e) {
            e.printStackTrace();
            Thread.sleep(1000 * 5);
        }
    }

    private void decodeCardGroup(JSONArray cardGroup, Node author) throws JSONException {
        for (int i = cardGroup.length() - 1; i >= 0; i--) {
            JSONObject jsonObj = cardGroup.getJSONObject(i);
            JSONObject user = jsonObj.getJSONObject("user");
            addRepostRelationInGraph(user.get("id").toString(), user.getString("screen_name"),
                    ParseUtils.removeTag(jsonObj.getString("text")), jsonObj.getString("created_at"), author);
        }
    }

    //only for repost tweet
    private void addRepostRelationInGraph(String id, String screen_name, String text, String time, Node author) {
        time = Tools.dateToStr(Tools.convertCalendar(time));
        System.out.println(
                "user id is " + id + "," +
                        "name:" + screen_name + "," +
                        "text:" + text + "," +
                        "time:" + time
        );

        List<String> l = Tools.deleteDuplicate(text);
        //get current user node
        Node current = db.findByScreenName(screen_name);

        //create user node if not exists
        if (current == null) {
            current = db.createNode(id, screen_name);
        }
        if (l.size() == 0) {
            boolean createRel = updateRelationship(author, current, time);
            if (createRel) {
                System.out.println("add to original weibo");
            } else {
                System.out.println("relationship already exists");
            }
        } else {
            int i;
            for (i = 0; i < l.size(); i++) {
                String prevName = l.get(i);
                if (prevName.equals(screen_name)) continue;

                Node prevUser = db.findByScreenName(prevName);
                if (prevUser != null) {
                    boolean createRel = updateRelationship(prevUser, current, time);
                    if (createRel) {
                        System.out.println("add link from " + prevName + " to " + screen_name);
                    } else {
                        System.out.println("relationship already exists");
                    }
                    break;
                }
            }
            if (i == l.size() && !db.getNodeName(author).equals(screen_name)) {
                boolean createRel = updateRelationship(author, current, time);
                if (createRel) {
                    System.out.println("add to original weibo");
                } else {
                    System.out.println("relationship already exists");
                }
            }
        }
    }
}