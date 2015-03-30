package m.sina;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.impl.centrality.BetweennessCentrality;
import org.neo4j.graphalgo.impl.centrality.ClosenessCentrality;
import org.neo4j.graphalgo.impl.centrality.CostDivider;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPath;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPathDijkstra;
import org.neo4j.graphalgo.impl.util.DoubleAdder;
import org.neo4j.graphdb.*;

import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

    private Set<Node> getBFSNodes(
            final Node user )
    {
        Set<Node> set = new HashSet<>();
        set.add(user);
        try ( Transaction tx = db.graphDb.beginTx() ) {
            TraversalDescription td = db.graphDb.traversalDescription()
                    .breadthFirst()
                    .relationships(EmbeddedNeo4j.RelTypes.REPOST, Direction.OUTGOING)
                    .evaluator(Evaluators.excludeStartPosition());
            Traverser children = td.traverse(user);
            for (Path path : children) {
                set.add(path.endNode());
            }
        }
        return set;
    }

    private double getClosenessByBFS(Node user){
        double result = 0;
        int numOfChildren = 0;
        try ( Transaction tx = db.graphDb.beginTx() ) {
            TraversalDescription td = db.graphDb.traversalDescription()
                    .breadthFirst()
                    .relationships(EmbeddedNeo4j.RelTypes.REPOST, Direction.OUTGOING)
                    .evaluator(Evaluators.excludeStartPosition());
            Traverser children = td.traverse(user);
            for (Path path : children) {
                numOfChildren++;
                result += 1.0/path.length();
                System.out.println("At depth " + path.length() + " => "
                        + path.endNode()
                        .getProperty( "name" ));
            }
        }
        return result/numOfChildren;
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
            double closeness = getClosenessByBFS(root);
            System.out.println("root is " + db.getNodeName(root) + ",closeness is "+closeness);
        }
    }

    public static void main(String[] args) {
        Centrality test = new Centrality();
        test.db.createDb();
//        Set<Node> nodeSet = test.db.getAllNodes();
//        test.processBetweennessCentrality(nodeSet);
        test.processAllClosenessCentrality();
        test.db.shutDown();
    }
}
