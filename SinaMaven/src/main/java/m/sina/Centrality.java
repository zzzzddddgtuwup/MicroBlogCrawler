package m.sina;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.*;

import java.util.Iterator;

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

    public static void main(String[] args) {

    }
}
