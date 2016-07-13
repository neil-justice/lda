/* An undirected, weighted, unmodifiable graph data structure.  */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import tester.Tester;

class Graph {
  private final SparseIntMatrix matrix;   // adjacency matrix with weight info
  private final TIntArrayList[] adjList;  // adjacency list
  private final TIntArrayList[] comms;    // community members
  private final int[] degrees;            // degree of each node
  private final int order;                // no. of nodes
  private final int size;                 // sum of edge weights
  private final double m2;                // sum of edge weights * 2
  private final LouvainDetector detector = new LouvainDetector(this);
  private final CommunityStructure cs;
  private final int layer; // if > 0, its a coarse-grained community graph
  
  public Graph(GraphBuilder builder) {
    matrix  = builder.matrix();
    adjList = builder.adjList();
    degrees = builder.degrees();
    order   = builder.order();
    size    = builder.size();
    m2      = (double) size * 2d;
    cs      = builder.communityStructure();
    layer   = builder.layer();
    
    initialiseCommunities();
  }
  
  private void initialiseCommunities() {
    for (int i = 0; i < order; i++) {
      communities()[i] = i;
      totDegrees()[i] = degree(i);
      intDegrees()[i] = matrix.get(i, i); // catches self-edges
    }
  }
  
  public void moveToComm(int node, int community) {
    int oldComm = community(node);
    if (oldComm == community) return;
    
    communities()[node] = community;
    totDegrees()[oldComm] -= degree(node);
    totDegrees()[community] += degree(node);
    
    for (int i = 0; i < adjList[node].size(); i++) {
      int neighbour = adjList[node].get(i);
      if (community(neighbour) == community) {
        intDegrees()[community] += (matrix.get(node, neighbour) * 2);
      }
      if (community(neighbour) == oldComm) {
        intDegrees()[oldComm] -= (matrix.get(node, neighbour) * 2);
      }
    }
    
    if (totDegree(oldComm) == 0) decNumComms();
  }
  
  // weight between a community and a node
  public int dnodecomm(int node, int community) {
    int dnodecomm = 0;
    for (int i = 0; i < adjList[node].size(); i++) {
      int neigh = adjList[node].get(i);
      if (community(neigh) == community) dnodecomm += matrix.get(node, neigh);
    }
    return dnodecomm;
  } 
  
  public double modularity() {
    double q  = 0d;
    
    for (int comm = 0; comm < order; comm++) {
      double ctot = (double)totDegree(comm);
      double cint = (double)intDegree(comm);
      if (ctot != 0d && cint != 0d) q += (cint/m2) - (ctot/m2)*(ctot/m2);
    }

    return q;
  }

  public int[] detectCommunities() {
    if (detector.run() > 0) {
      cs.newLayer(order);
    }
    return communities();
  }
  
  public int[] communityNeighbours(int community) {
    int[] neighbours = new int[order];
    
    for (int i = 0; i < )
  }
  
  private int[] totDegrees() { return cs.totDegrees(layer); }
  private int[] intDegrees() { return cs.intDegrees(layer); }
  private int[] communities() { return cs.communities(layer); }

  public int numComms() { return cs.numComms(layer); }
  public void decNumComms() { cs.decNumComms(layer); }
  
  public int community(int node) { return cs.communities(layer)[node]; }
  public int totDegree(int comm) { return cs.totDegrees(layer)[comm]; }
  public int intDegree(int comm) { return cs.intDegrees(layer)[comm]; }

  public double m2() { return m2; }
  public int edge(int n1, int n2) { return matrix.get(n1, n2); }
  public int size() { return size; }
  public int layer() { return layer; }
  public int order() { return order; }
  public int degree(int node) { return degrees[node]; }
  public TIntArrayList neighbours(int node) { return adjList[node]; }

  public static void main(String[] args) {
    Tester t = new Tester();
    Graph g = new GraphBuilder().setSize(7)
                                .addEdge(0,1,12)
                                .addEdge(1,2,14)
                                .addEdge(0,2,5)
                                .addEdge(3,4,10)
                                .addEdge(4,5,10)
                                .addEdge(3,5,10)
                                .addEdge(4,6,11)
                                .addEdge(5,6,17)
                                .build();
    g.moveToComm(1,0);
    g.moveToComm(2,0);
    g.moveToComm(4,3);
    g.moveToComm(5,3);
    
    t.is(g.degree(1),26);
    t.is(g.size(),89);
    t.is(g.degree(0),17);
    t.is(g.edge(0,1), g.edge(1,0));
    t.is(g.dnodecomm(6,3),28);
    t.is(g.totDegree(3), 20 + 31 + 37);
    t.is(g.totDegree(1), 0);
    t.is(g.totDegree(5), 0);
    t.is(g.intDegree(3), 60);
    t.is(g.intDegree(1), 0);
    t.is(g.intDegree(2), 0);
    t.is(g.intDegree(6), 0);
    t.is(g.numComms(),3);
    t.is(g.createLayerMap().get(0), 0);
    t.is(g.createLayerMap().get(3), 1);
    t.is(g.createLayerMap().get(6), 2);
    t.is(g.createLayerMap().size(), 3);
    
    t.results();
    
    g = new GraphBuilder().fromFile("data/gtests/arxiv.txt").build();
    g.detectCommunities();
  }
}