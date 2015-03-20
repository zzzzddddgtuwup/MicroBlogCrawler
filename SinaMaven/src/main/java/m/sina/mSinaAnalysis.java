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

    public int getPageNum(String firstPageJson) throws JSONException {
        JSONArray jsonArray = new JSONArray(firstPageJson);
        JSONObject jsonObj = jsonArray.getJSONObject(1);
        return jsonObj.getInt("maxPage");
    }

    private void decodeCardGroup(JSONArray cardGroup, Node author,String tweetId) throws JSONException {
        for (int i = cardGroup.length() - 1; i >= 0; i--) {
            JSONObject jsonObj = cardGroup.getJSONObject(i);
            JSONObject user = jsonObj.getJSONObject("user");
            addRepostRelationInGraph(user.get("id").toString(), user.getString("screen_name"),
                    removeTag(jsonObj.getString("text")), jsonObj.getString("created_at"), author, tweetId);
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
        int maxPage = getPageNum(firstPage);
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

    @Deprecated
    public void getAllIndegreeAndOutdegree(Map<Integer,Integer> indegreeMap, Map<Integer,Integer> outdegreeMap){
        ExecutionEngine engine = new ExecutionEngine(db.graphDb);
        ExecutionResult result;
        try (Transaction tx = db.graphDb.beginTx()) {
            result = engine.execute("match n return n");
            Iterator<Node> n_column = result.columnAs("n");
            while (n_column.hasNext()) {
                Node temp = n_column.next();
                int indegree = temp.getDegree(Direction.INCOMING);
                int outdegree = temp.getDegree(Direction.OUTGOING);
                if(indegreeMap.containsKey(indegree)){
                    indegreeMap.put(indegree, indegreeMap.get(indegree) + 1);
                }else{
                    indegreeMap.put(indegree,1);
                }
                if(outdegreeMap.containsKey(outdegree)){
                    outdegreeMap.put(outdegree,outdegreeMap.get(outdegree)+1);
                }else{
                    outdegreeMap.put(outdegree,1);
                }
            }
        }
    }
    //remove all tags and whitespace
    public String removeTag(String s) {
        return s.replaceAll("<\\/?[^>]*>", "").replaceAll(" ", "");
    }

    public void outputTimeSeriesData(String[] tweetIdList) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(db.graphDb);
        ExecutionResult result;
        for(String id:tweetIdList){
            int count = 0;
            String prevTime = null;
            BufferedWriter out=new BufferedWriter(new FileWriter("timeSeries_"+id+".txt"));
            String statement = "match (n) - [r{tweetId:\"" + id + "\"}]->(m) return r order by r.time";
            try (Transaction tx = db.graphDb.beginTx()) {
                result = engine.execute(statement);
                Iterator<Relationship> n_column = result.columnAs("r");
                while(n_column.hasNext()) {
                    Relationship rel = n_column.next();
                    String currentTime = (String)rel.getProperty("time");
                    if(prevTime==null){
                        prevTime = currentTime;
                    }else{
                        if(!prevTime.equals(currentTime)){
                            out.write(prevTime + "\t" + count + '\n');
                            prevTime = currentTime;
                        }
                    }
                    count++;
                }
                out.write(prevTime + "\t" + count + '\n');
            }
            out.close();
        }
    }

    public void outputDegreeData(String[] tweetIdList) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(db.graphDb);
        ExecutionResult result;
        for(String id:tweetIdList){
            Map<Integer,Integer> indegreeMap = new TreeMap<>();
            Map<Integer,Integer> outdegreeMap = new TreeMap<>();

            Map<Long,Integer> nMap = new HashMap<>();
            Map<Long,Integer> mMap = new HashMap<>();
            String statement = "match (n) - [r{tweetId:\"" + id + "\"}]->(m) return n,m";
            try (Transaction tx = db.graphDb.beginTx()) {
                result = engine.execute(statement);

                for ( Map<String, Object> row : result )
                {
                    for ( Map.Entry<String, Object> column : row.entrySet() )
                    {
                        String col_name = column.getKey();
                        if(col_name.equals("n")){
                            Node temp = (Node) column.getValue();
                            Long nid = temp.getId();
                            if(nMap.containsKey(nid)){
                                nMap.put(nid,nMap.get(nid)+1);
                            }else{
                                nMap.put(nid,1);
                            }
                        }else if(col_name.equals("m")){
                            Node temp = (Node) column.getValue();
                            Long nid = temp.getId();
                            if(mMap.containsKey(nid)){
                                mMap.put(nid,mMap.get(nid)+1);
                            }else{
                                mMap.put(nid,1);
                            }
                        }
                    }
                }
            }

            for(Long k:nMap.keySet()){
                int outdegree = nMap.get(k);
                if(outdegreeMap.containsKey(outdegree)){
                    outdegreeMap.put(outdegree,outdegreeMap.get(outdegree)+1);
                }else{
                    outdegreeMap.put(outdegree,1);
                }
            }

            for(Long k:mMap.keySet()){
                int indegree = mMap.get(k);
                if(indegreeMap.containsKey(indegree)){
                    indegreeMap.put(indegree,indegreeMap.get(indegree)+1);
                }else{
                    indegreeMap.put(indegree,1);
                }
            }

            BufferedWriter out=new BufferedWriter(new FileWriter("indegreeReport_"+id+".txt"));
            for(Integer k:indegreeMap.keySet()){
                out.write(k + " " + indegreeMap.get(k) + '\n');
            }
            out.close();
            out=new BufferedWriter(new FileWriter("outdegreeReport_"+id+".txt"));
            for(Integer k:outdegreeMap.keySet()){
                out.write(k + " " + outdegreeMap.get(k) + '\n');
            }
            out.close();
        }
    }

    public void outputUserData(String[] tweetIdList) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(db.graphDb);
        ExecutionResult result;
        BufferedWriter out=new BufferedWriter(new FileWriter("userDataReport.txt"));
        String title = "name\t"+"totalDegree\t";
        for(int i=0;i<tweetIdList.length;i++){
            title += "in_" + i + '\t' + "out_" + i + '\t';
        }
        title+='\n';
        out.write(title);

        try (Transaction tx = db.graphDb.beginTx()) {
            result = engine.execute("match n return n");
            Iterator<Node> n_column = result.columnAs("n");
            while (n_column.hasNext()) {
                Node temp = n_column.next();
                Map<String,Integer> indegreeMap = new HashMap<>();
                Map<String,Integer> outdegreeMap = new HashMap<>();
                Iterator<Relationship> itr = temp.getRelationships(Direction.INCOMING).iterator();
                while(itr.hasNext()){
                    Relationship rel = itr.next();
                    String tweetId = (String)rel.getProperty("tweetId");
                    if(indegreeMap.containsKey(tweetId)){
                        indegreeMap.put(tweetId,indegreeMap.get(tweetId)+1);
                    }else{
                        indegreeMap.put(tweetId,1);
                    }
                }

                itr = temp.getRelationships(Direction.OUTGOING).iterator();
                while(itr.hasNext()){
                    Relationship rel = itr.next();
                    String tweetId = (String)rel.getProperty("tweetId");
                    if(outdegreeMap.containsKey(tweetId)){
                        outdegreeMap.put(tweetId,outdegreeMap.get(tweetId)+1);
                    }else{
                        outdegreeMap.put(tweetId,1);
                    }
                }

                String name = null;
                if(temp.getProperty("id").equals("0")){
                    name = "author";
                }else{
                    name = (String)temp.getProperty("name");
                }
                StringBuilder output = new StringBuilder().append(name).append("\t").append(temp.getDegree()).append('\t');
                for(String id:tweetIdList){
                    if(indegreeMap.containsKey(id)){
                        output.append(indegreeMap.get(id)).append('\t');
                    }else{
                        output.append(0).append('\t');
                    }
                    if(outdegreeMap.containsKey(id)){
                        output.append(outdegreeMap.get(id)).append('\t');
                    }else{
                        output.append(0).append('\t');
                    }
                }
                out.write(output.toString() + '\n');
            }
        }
        out.close();
    }

    public void testOnAuthor() throws IOException {
        String[] tweetIdList = {"3812299523807086","3812215948115720","3811770344993173","3804135025546875",
                "3808949927299183","3814093612320822","3813714534926457","3813473794438142","3813009615146746",
                "3812917860883160"};
        //test.db.cleanDb();
        db.createDb();
        outputUserData(tweetIdList);
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