package m.sina;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.impl.centrality.*;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPath;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPathDijkstra;
import org.neo4j.graphalgo.impl.util.DoubleAdder;
import org.neo4j.graphdb.*;

import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;


import java.io.*;
import java.util.*;

/**
 * Created by zzzzddddgtuwup on 3/27/15.
 */
public class Centrality {
    private EmbeddedNeo4j db;

    public Centrality() {
        this.db = new EmbeddedNeo4j();
    }

    public void shortestPath(Node startNode, Node endNode){
        try (Transaction tx = db.graphDb.beginTx()) {
            PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
                    PathExpanders.forTypeAndDirection( EmbeddedNeo4j.RelTypes.REPOST, Direction.OUTGOING ), 15 );
            Iterable<Path> paths = finder.findAllPaths( startNode, endNode );
            Path path = paths.iterator().next();
            System.out.println("path length is " + path.length());
            Iterator<Node> iterator = path.nodes().iterator();
            while(iterator.hasNext()){
                System.out.println(iterator.next().getProperty("name"));
            }
            tx.success();
        }
    }

//    private SingleSourceShortestPath<Integer> getSingleSourceShortestPath() {
//        return new SingleSourceShortestPathBFS(null,Direction.OUTGOING,EmbeddedNeo4j.RelTypes.REPOST);
//    }

    private Traverser BFSTraverse(final Node user){
        try ( Transaction tx = db.graphDb.beginTx() ) {
            TraversalDescription td = db.graphDb.traversalDescription()
                    .breadthFirst()
                    .relationships(EmbeddedNeo4j.RelTypes.REPOST, Direction.OUTGOING)
                    .evaluator(Evaluators.excludeStartPosition());
            return td.traverse(user);
        }
    }

    private Set<Node> getBFSNodes(
            final Node user )
    {
        Set<Node> set = new HashSet<>();
        set.add(user);
        try ( Transaction tx = db.graphDb.beginTx() ) {
            Traverser children = BFSTraverse(user);
            for (Path path : children) {
                set.add(path.endNode());
            }
        }
        return set;
    }

    protected SingleSourceShortestPath<Double> getSingleSourceShortestPath()
    {
        return new SingleSourceShortestPathDijkstra<Double>( 0.0, null,
                new CostEvaluator<Double>()
                {
                    public Double getCost( Relationship relationship,
                                           Direction direction )
                    {
                        return 1.0;
                    }
                }, new org.neo4j.graphalgo.impl.util.DoubleAdder(),
                new org.neo4j.graphalgo.impl.util.DoubleComparator(),
                Direction.OUTGOING, EmbeddedNeo4j.RelTypes.REPOST);

    }

    public void processBetweennessCentrality(Set<Node> nodeSet){
        try (Transaction tx = db.graphDb.beginTx()) {
            BetweennessCentrality<Double> betweennessCentrality = new BetweennessCentrality<>(
                    getSingleSourceShortestPath(),nodeSet);
            betweennessCentrality.calculate();
            for(Node node :  nodeSet) {
                node.setProperty("betweenness",betweennessCentrality.getCentrality(node));
                System.out.println(db.getNodeName(node) + " betweenness centrality is " + betweennessCentrality.getCentrality(node));
            }
            tx.success();
        }
    }

//    public void processConnectedComponentClosenessCentrality(Set<Node> nodeSet){
//        try (Transaction tx = db.graphDb.beginTx()) {
//            ClosenessCentrality<Double> closenessCentrality =
//                    new ClosenessCentrality<>( getSingleSourceShortestPath(),
//                            new DoubleAdder(), 0.0, nodeSet,
//                            new CostDivider<Double>()
//                            {
//                                public Double divideByCost( Double d, Double c )
//                                {
//                                    return d / c;
//                                }
//
//                                public Double divideCost( Double c, Double d )
//                                {
//                                    return c / d;
//                                }
//                            } );
//            for(Node node :  nodeSet) {
//                System.out.println(db.getNodeName(node) + " closeness centrality is " + closenessCentrality.getCentrality(node));
//            }
//            tx.success();
//        }
//    }

    public void processAllClosenessCentrality(){
        Set<Node> rootNodes = db.getAllNodeWithOutRel();

            for(Node root : rootNodes){
                double result = 0;
                int numOfChildren = 0;
                Traverser children = BFSTraverse(root);
                try ( Transaction tx = db.graphDb.beginTx() ) {
                for (Path path : children) {
                    numOfChildren++;
                    result += 1.0/path.length();
                    System.out.println("At depth " + path.length() + " => "
                            + path.endNode()
                            .getProperty( "name" ));
                }
                double closeness = result/numOfChildren;
                root.setProperty("closeness",closeness);
                tx.success();
                System.out.println("root is " + db.getNodeName(root) + ",closeness is "+closeness);
            }
        }
    }

    public void processEigenvectorCentrality(Set<Node> nodeSet, Set<Relationship> relationshipSet) {
        System.out.println("begin calulation");
        try (Transaction tx = db.graphDb.beginTx()) {
            EigenvectorCentrality eigenvectorCentrality = new EigenvectorCentralityPower(Direction.INCOMING, new CostEvaluator<Double>() {
                public Double getCost(Relationship relationship, Direction direction) {
                    return 1.0;
                }
            }, nodeSet, relationshipSet, 0.1);
            for (Node node : nodeSet) {
                System.out.println(db.getNodeName(node) + " eigenvector centrality is " + eigenvectorCentrality.getCentrality(node));
            }
            tx.success();
        }
    }

    public void generateAdjacentMatrix(Node[] node_arr) throws IOException {
        Map<Node,Integer> map = Tools.reverseArrayToHashMap(node_arr);
        BufferedWriter out=new BufferedWriter(new FileWriter("matrix.txt"));
        try (Transaction tx = db.graphDb.beginTx()) {
            for (int i = 0; i < node_arr.length; i++) {
                double[] tmp = new double[node_arr.length];
                for (Relationship neighbor : node_arr[i].getRelationships(Direction.OUTGOING)) {
                    tmp[map.get(neighbor.getEndNode())] = 1.0;
                }
                StringBuilder sb = new StringBuilder();
                for (double j : tmp) {
                    sb.append(j).append(" ");
                }
                out.write(sb.toString().substring(0, sb.length() - 1) + '\n');
            }
        }
        out.close();
    }

    public void processPagerankFromMatlab(Node[] node_arr) throws FileNotFoundException {
        BufferedReader in = new BufferedReader(new FileReader("pagerank.txt"));
        Scanner sc = new Scanner(in);
        int i = 0;
        try (Transaction tx = db.graphDb.beginTx()) {
            while (sc.hasNext()) {
                node_arr[i].setProperty("pagerank", sc.nextDouble());
                i++;
            }
            tx.success();
        }
    }

    public void processDegreeCentrality(Set<Node> nodeSet) {
        try (Transaction tx = db.graphDb.beginTx()) {
            for(Node node:nodeSet){
                int outdegree = node.getDegree(Direction.OUTGOING);
                node.setProperty("outdegree",outdegree);
            }
            tx.success();
        }
    }
    public static void main(String[] args) throws IOException {
        Centrality test = new Centrality();
        test.db.createDb();
        List<Node> nodelist = test.db.getAllNodes();

        Set<Relationship> relSet = test.db.getAllRels();
        test.processBetweennessCentrality(new HashSet<>(nodelist));
        test.processAllClosenessCentrality();
//        test.processEigenvectorCentrality(nodeSet,relSet);
        test.generateAdjacentMatrix(nodelist.toArray(new Node[nodelist.size()]));
//        test.processPagerankFromMatlab(nodelist.toArray(new Node[nodelist.size()]));
        test.processDegreeCentrality(new HashSet<Node>(nodelist));
        test.db.shutDown();
    }
}
