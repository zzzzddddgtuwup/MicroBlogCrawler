package m.sina;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static m.sina.HtmlUtils.HTMLGet;

/**
 * Created by zzzzddddgtuwup on 2/12/15.
 */

public class mSinaAnalysis {
    public EmbeddedNeo4j db;

    public mSinaAnalysis() {
        this.db = new EmbeddedNeo4j();
    }

    private void decodeCardGroup(JSONArray cardGroup, Node author,String tweetId) throws JSONException {
        for (int i = cardGroup.length() - 1; i >= 0; i--) {
            JSONObject jsonObj = cardGroup.getJSONObject(i);
            JSONObject user = jsonObj.getJSONObject("user");
            addRepostRelationInGraph(user.get("id").toString(), user.getString("screen_name"),
                    ParseUtils.removeTag(jsonObj.getString("text")), jsonObj.getString("created_at"), author, tweetId);
        }
    }

    public void getAllRepostForTweet(String url,String tweetId) throws IOException{
        Node author;
        author = db.findById("0");
        if(author==null){
            try (Transaction tx = db.graphDb.beginTx()) {
                author = db.graphDb.createNode();
                author.setProperty("id", "0");
                tx.success();
            }
        }
        String firstPage = HTMLGet(url + "page=1");
        int maxPage = ParseUtils.getPageNum(firstPage);
        System.out.println("Maxpage is " + maxPage);
        for (int i = maxPage; i >= 2; i--) {
            String html = HTMLGet(url + "page=" + i);
            System.out.println("current page is " + i);
            try{
                decodeCardGroup(new JSONArray(html).getJSONObject(0).getJSONArray("card_group"), author,tweetId);

            }catch(JSONException e){
                e.printStackTrace();
                ++i;
            }

        }
        JSONArray cardGroup = new JSONArray(firstPage).getJSONObject(1).getJSONArray("card_group");
        decodeCardGroup(cardGroup, author,tweetId);
    }

    private void addRepostRelationInGraph(String id, String screen_name, String text, String time, Node author, String tweetId) {
        String[] sp = text.split("//@");
        System.out.print(id + " ");
        Node current;

        //get current user node
        current = db.findByScreenName(screen_name);

        //create user node if not exists
        if (current == null) {
            db.createNode(id, screen_name);
        }
        System.out.println(tweetId + " " + text);
        if (sp.length == 1) {
            //if repost from original author exists
            boolean createRel = updateRelationship(author,current,time, tweetId);
            if(createRel) {
                System.out.println("add to original weibo");
            }else{
                System.out.println("relationship already exists");
            }
        } else {
            int i;
            for (i = 1; i < sp.length; i++) {
                int index = sp[i].indexOf(':');
                if(index==-1){
                    index = sp[i].indexOf('：');
                }
                String prevName = sp[i].substring(0, index);
                Node prevUser = db.findByScreenName(prevName);
                if (prevUser != null) {
                    boolean createRel = updateRelationship(prevUser,current,time, tweetId);
                    if(createRel) {
                        System.out.println("add link from " + prevName + " to " + screen_name);
                    }else{
                        System.out.println("relationship already exists");
                    }
                    break;
                }
            }
            if (i == sp.length) {
                boolean createRel = updateRelationship(author,current,time,tweetId);
                if(createRel) {
                    System.out.println("add to original weibo");
                }else{
                    System.out.println("relationship already exists");
                }
            }
        }
    }

    private boolean updateRelationship(Node start, Node end, String time, String tweetId){
        String currentTimePro = Tools.dateToStr(Tools.convertCalendar(time));
        Relationship relationship = db.findRelByNodesAndTweetId(start, end, tweetId);
        if (relationship == null) {
            db.addRelWithTimeAndTweetId(start, end, currentTimePro, tweetId);
            return true;
        } else {
            String timeString = db.getRelTime(relationship);
            if(Tools.timeBefore(currentTimePro, timeString)){
                db.updateRelTime(relationship,timeString);
            }
            return false;
        }
    }

    public void testOnAuthor() throws IOException {
        String[] tweetIdList = {"3812299523807086","3812215948115720","3811770344993173","3804135025546875",
                "3808949927299183","3814093612320822","3813714534926457","3813473794438142","3813009615146746",
                "3812917860883160"};
        //test.db.cleanDb();
        db.createDb();
//        for(String id:tweetIdList){
//            String url = "http://m.weibo.cn/single/rcList?format=cards&id=" + id +"&type=repost&hot=0&";
//            test.getAllRepostForTweet(url,id);
//        }
        db.shutDown();
        //test.login();
    }

//    public void getTopicTweet(String topic) throws UnsupportedEncodingException {
//        String url = "http://m.weibo.cn/page/pageJson?containerid="+URLEncoder.encode("100103type=1&q=#","UTF-8")
//                +topic +URLEncoder.encode("#","UTF-8");
//        System.out.println(url);
//    }

    public static void main(String[] args) throws IOException, JSONException {
        mSinaAnalysis test = new mSinaAnalysis();
    }
}