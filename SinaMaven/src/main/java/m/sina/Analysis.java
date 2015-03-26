package m.sina;

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

/**
 * Created by zzzzddddgtuwup on 3/23/15.
 */
public class Analysis {
    public EmbeddedNeo4j db;

    public Analysis(){
        this.db = new EmbeddedNeo4j();
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

    public void outputTimeSeriesDataForTopic() throws IOException {
        ExecutionEngine engine = new ExecutionEngine(db.graphDb);
        ExecutionResult result;
        BufferedWriter out=new BufferedWriter(new FileWriter("timeSeries_topic.txt"));
        String statement = "match (n) - [r]->(m) return r";
        try (Transaction tx = db.graphDb.beginTx()) {
            result = engine.execute(statement);
            Iterator<Relationship> n_column = result.columnAs("r");
            while(n_column.hasNext()) {
                Relationship rel = n_column.next();
                String currentTime = (String)rel.getProperty("time");
                out.write(currentTime+'\n');
            }
        }

        statement = "match (n) where has (n.time) return n";
        try (Transaction tx = db.graphDb.beginTx()) {
            result = engine.execute(statement);
            Iterator<Node> n_column = result.columnAs("n");
            while(n_column.hasNext()) {
                Node node = n_column.next();
                String currentTime = (String)node.getProperty("time");
                out.write(currentTime+'\n');
            }
        }
        out.close();
    }

    public void outputDegreeDataFortopic() throws IOException {
        ExecutionEngine engine = new ExecutionEngine(db.graphDb);
        ExecutionResult result;

        String statement = "match n return n";
        Map<Integer,Integer> indegreeMap = new TreeMap<>();
        Map<Integer,Integer> outdegreeMap = new TreeMap<>();
        try (Transaction tx = db.graphDb.beginTx()) {
            result = engine.execute(statement);
            Iterator<Node> n_column = result.columnAs("n");
            while(n_column.hasNext()) {
                Node node = n_column.next();
                int indegree = node.getDegree(Direction.INCOMING);
                int outdegree = node.getDegree(Direction.OUTGOING);
                if(indegreeMap.containsKey(indegree)){
                    indegreeMap.put(indegree,indegreeMap.get(indegree)+1);
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
        BufferedWriter out=new BufferedWriter(new FileWriter("indegree_topic.txt"));
        for(Integer k:indegreeMap.keySet()){
            out.write(k + " " + indegreeMap.get(k) + '\n');
        }
        out.close();
        out=new BufferedWriter(new FileWriter("outdegree_topic.txt"));
        for(Integer k:outdegreeMap.keySet()){
            out.write(k + " " + outdegreeMap.get(k) + '\n');
        }
        out.close();
    }

    public static void main(String[] args) throws IOException {
        Analysis test = new Analysis();
        test.db.createDb();
//        test.outputTimeSeriesDataForTopic();
        test.outputDegreeDataFortopic();
        test.db.shutDown();
    }
}
