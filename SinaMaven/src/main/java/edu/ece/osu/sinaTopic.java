package edu.ece.osu;

import m.sina.EmbeddedNeo4j;
import m.sina.HtmlUtils;
import m.sina.Tools;
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

public class sinaTopic {
    public EmbeddedNeo4j db;

    public sinaTopic() {
        this.db = new EmbeddedNeo4j();
    }

    public static void main(String[] args) throws Exception {
        String[] topics = {"#广州恒大VS石家庄永昌#","#日本最美女高中生#","#央视主播炮轰收视率#","#男人下厨9大理由#","#王学兵张博吸毒被拘#"};
        sinaTopic test = new sinaTopic();
        test.testOnTopic();
    }

    public void testOnTopic() throws IOException, ParseException, InterruptedException {
        String cookies = HtmlUtils.login();
        System.out.println("开始搜索微博");
        db.cleanDb();
        db.createDb();
        searchTopic(cookies, "#王学兵张博吸毒被拘#", "2015-03-10-18", "2015-03-12-05");
        db.shutDown();
    }
    public void searchTopic(String cookies,String topic,String startTime, String endTime) throws IOException, ParseException, InterruptedException {
        List<String> l = Tools.getTimeSeries(startTime,endTime);
        for(String time:l){
            System.out.println("time is " + time);
            String url = "http://s.weibo.com/wb/" + URLEncoder.encode(topic,"utf-8") + "&xsort=time&timescope=custom:"+time+":"+time+"&nodup=1";
            String firstPage = getHtmlFromJs(HtmlUtils.HTMLGet(url, cookies));
            int maxpage = getMaxPage(firstPage);
            Thread.sleep(1000*30);
            if(maxpage==0){
                System.out.println("没有结果");
                continue;
            }else if(maxpage==1){
                System.out.println("有一页结果");
            }
            for(int i = maxpage;i >= 2;i--){
                System.out.println("current page is " + i);
                String n_url = url + "&page="+i;
                String res = HtmlUtils.HTMLGet(n_url, cookies);
                decodeHtml(getHtmlFromJs(res));
                Thread.sleep(1000*30);
            }
            decodeHtml(firstPage);
        }
    }

    private String getHtmlFromJs(String s){
        Matcher m = Pattern.compile("<script>STK && STK.pageletM && STK.pageletM.view\\((\\{\"pid\":\"pl_wb_feedlist\".*?)\\)</script>").matcher(s);
        String js =  "";
        if(m.find()){
            js = m.group();
            int indexLeft = js.indexOf("{");
            int indexRight = js.indexOf("}");
            String json =  js.substring(indexLeft,indexRight+1);
            return new JSONObject(json).getString("html");
        }else{
            return js;
        }
    }

    private void decodeHtml(String html){
        //System.out.println(html);
        Document doc = Jsoup.parse(html);
        Elements cards = doc.select("div.WB_cardwrap.S_bg2.clearfix");
        for(int i = cards.size()-1;i>=0;i--){
            Element card = cards.get(i);
            Elements div = card.select("div[mid]");
//            String mid = div.attr("mid");
            String name = card.select("a.name_txt").attr("nick-name");
            String text = card.select("div.feed_content.wbcon >p.comment_txt").text();
            String time = card.select("div.feed_from.W_textb > a[title]").text();
            time = Tools.dateToStr(Tools.convertCalendar(time));
            String uid = card.select("div.feed_content.wbcon > a").attr("usercard");
            int index = uid.indexOf("&");
            String originalTime = null;
            String originalId = null;
            if(div.hasAttr("isforward")&&div.attr("isforward").equals("1")){
                String originalUser = card.select("a.W_texta.W_fb").attr("nick-name");
                text += "//@"+originalUser+":";
                originalTime = card.select("div.comment_info").select("div.feed_from.W_textb > a[date]").text();
                originalTime = Tools.dateToStr(Tools.convertCalendar(originalTime));
                originalId = card.select("a.W_texta.W_fb").attr("usercard");
                originalId = originalId.substring(3);
                //System.out.println("has forward from" + originalUser);
            }
            addToGraph(name,uid.substring(0,index),time,text,originalTime,originalId);
        }
    }

    private void addToGraph(String screen_name, String id, String time, String text,String ori_time,String ori_id){
        System.out.println(
                "user id is " + id + "," +
                        "name:" + screen_name + "," +
                        "text:" + text + "," +
                        "time:" + time
        );
        if(ori_time!=null){
            System.out.println(",original time:" + ori_time + ",original id:" + ori_id);
        }

        List<String> l = Tools.deleteDuplicate(text);
        //get current user node
        Node current = db.findByScreenName(screen_name);

        if(l.size()==0){
            if(current == null){
                System.out.println("create user node");
                db.createNode(id, screen_name, time);
            }
        }else{
            for(int i = 0; i < l.size();i++){
                String prevName = l.get(i);
                if(prevName.equals(screen_name)) continue;

                Node prevUser = db.findByScreenName(prevName);
                if(i==l.size()-1 && prevUser==null){
                    System.out.println("create prevUser");
                    prevUser = db.createNode(ori_id,prevName,ori_time);
                }
                if (prevUser != null) {
                    //create user node if not exists
                    if (current == null) {
                        current = db.createNode(id, screen_name);
                    }
                    boolean createRel = updateRelationship(prevUser,current,time);
                    if(createRel) {
                        System.out.println("add link from " + prevName + " to " + screen_name);
                    }else{
                        System.out.println("relationship already exists");
                    }
                    break;
                }
            }
        }

    }
    private int getMaxPage(String html){
        //System.out.println(html);
        Document doc = Jsoup.parse(html);
        if(!doc.select("p.noresult_tit").isEmpty()){
            return 0;
        }
        if(doc.select("div.WB_cardwrap.S_bg2.relative").select("li").isEmpty()){
            return 1;
        }
        Element li = doc.select("div.WB_cardwrap.S_bg2.relative").select("li").last();
        String text = li.text();
        String result = text.substring(1,text.length()-1);
//        System.out.println(result);
        System.out.println("##################");
        String search_num = doc.select("div.search_num > span").text();
        System.out.println(search_num);
        System.out.println("##################");
        return Integer.parseInt(result);
    }

    private boolean updateRelationship(Node start, Node end, String time){
        Relationship relationship = db.findRelByNodes(start, end);
        if (relationship == null) {
            db.addRelWithTime(start, end, time);
            return true;
        } else {
            String timeString = db.getRelTime(relationship);
            if(Tools.timeBefore(time, timeString)){
                db.updateRelTime(relationship,timeString);
            }
            return false;
        }
    }
}