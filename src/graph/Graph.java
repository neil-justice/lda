/* An undirected, weighted, unmodifiable graph data structure. (though nodes
 * can be moved between communities, and this functionality is rolled into this
 * object. ) */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import tester.Tester;

class Graph {
  private final SparseIntMatrix matrix;   // adjacency matrix with weight info
  private final TIntArrayList[] adjList;  // adjacency list
  private final SparseIntMatrix cmatrix;  // weights between communities
  private int numComms;                   // total no. of communities
  private final int[] communities;        // comm of each node
  private final int[] totDegrees;         // total degree of community
  private final int[] intDegrees;         // int. degree of community
  private final int layer; // if > 0, its a coarse-grained community graph
  
  private final int[] degrees;            // degree of each node
  private final int order;                // no. of nodes
  private final int size;                 // sum of edge weights
  private final double m2;                // sum of edge weights * 2
  
  public Graph(GraphBuilder builder) {
    matrix  = builder.matrix();
    cmatrix = new SparseIntMatrix(matrix);
    adjList = builder.adjList();
    degrees = builder.degrees();
    order   = builder.order();
    size    = builder.sizeDbl() / 2;
    m2      = (double) builder.sizeDbl();
    layer   = builder.layer();
    
    communities = new int[order];
    totDegrees = new int[order];
    intDegrees = new int[order];
    numComms = order;

    initialiseCommunities();
  }
  
  private void initialiseCommunities() {
    for (int i = 0; i < order; i++) {
      communities[i] = i;
      totDegrees[i] = degree(i);
      intDegrees[i] = matrix.get(i, i); // catches self-edges
    }
  }
  
  public void moveToComm(int node, int newComm) {
    int oldComm = community(node);
    int oldTotDegree = totDegree(oldComm);
    int oldNewTotDegree = totDegree(newComm);
    if (oldComm == newComm) return;
    
    communities[node] = newComm;
    totDegrees[oldComm] -= degree(node);
    totDegrees[newComm] += degree(node);

    for (int i = 0; i < adjList[node].size(); i++) {
      int neighbour = adjList[node].get(i);
      int weight = weight(node, neighbour);
      if (neighbour != node) {
        cmatrix.add(newComm, community(neighbour), weight);
        cmatrix.add(community(neighbour), newComm, weight);
        cmatrix.add(oldComm, community(neighbour), -weight);
        cmatrix.add(community(neighbour), oldComm, -weight);
        if (community(neighbour) == newComm) {
          intDegrees[newComm] += (weight * 2);
        }                                     
        if (community(neighbour) == oldComm) {                
          intDegrees[oldComm] -= (weight * 2);
        }
      }
    }
    int selfWeight = weight(node, node);
    cmatrix.add(newComm, newComm, selfWeight);
    cmatrix.add(oldComm, oldComm, -selfWeight);
    intDegrees[oldComm] -= selfWeight;
    intDegrees[newComm] += selfWeight;
    
    if (totDegree(oldComm) == 0 && oldTotDegree > 0) numComms--;
    if (totDegree(newComm) > 0 && oldNewTotDegree == 0) numComms++;
    if (totDegree(oldComm) < 0) throw new Error("-ve total degree");
  }
  
  // weight between a community and a node
  public int dnodecomm(int node, int community) {
    int dnodecomm = 0;
    for (int i = 0; i < adjList[node].size(); i++) {
      int neigh = adjList[node].get(i);
      if (community(neigh) == community && node != neigh) dnodecomm += weight(node, neigh);
    }
    return dnodecomm;
  } 
  
  public double modularity() {
    double q  = 0d;
    
    for (int comm = 0; comm < order; comm++) {
      double ctot = (double)totDegree(comm);
      double cint = (double)intDegree(comm);
      q += (cint/m2) - (ctot/m2)*(ctot/m2);
    }
    return q;
  }
  
  // returns the contribution that this comm makes to the total modularity
  public double modularityContribution(int comm) {
    double ctot = (double)totDegree(comm);
    double cint = (double)intDegree(comm);
    return (cint/m2) - (ctot/m2)*(ctot/m2);
  }
  
  public int[] communities() { return Arrays.copyOf(communities, order); }
  public int[] communitiesBacked() { return communities; }

  public int numComms() { return numComms; }
  
  public int community(int node) { return communities[node]; }
  public int totDegree(int comm) { return totDegrees[comm]; }
  public int intDegree(int comm) { return intDegrees[comm]; }

  public double m2() { return m2; }
  public int size() { return size; }
  public int layer() { return layer; }
  public int order() { return order; }
  public int degree(int node) { return degrees[node]; }
  public int weight(int n1, int n2) { return matrix.get(n1, n2); }
  public TIntArrayList neighbours(int node) { return adjList[node]; }
  
  public int communityWeight(int c1, int c2) { return cmatrix.get(c1, c2); }
  public SparseIntMatrix.Iterator commWeightIterator() { return cmatrix.iterator(); }
  
  public static void main(String[] args) {
    Tester t = new Tester();
    Graph g = new GraphBuilder().setSize(7)
                                .addEdge(0,1,12)
                                .addEdge(1,2,14)
                                .addEdge(0,2,5)
                                .addEdge(3,4,10)
                                .addEdge(4,4,10)
                                .addEdge(3,5,10)
                                .addEdge(4,6,11)
                                .addEdge(5,6,17)
                                .build();
    g.moveToComm(1,0);
    g.moveToComm(2,0);
    g.moveToComm(4,3);
    g.moveToComm(5,3);
    
    t.is(g.degree(1),26);
    t.is(g.size(),84);
    t.is(g.degree(0),17);
    t.is(g.weight(0,1), g.weight(1,0));
    t.is(g.dnodecomm(6,3),28);
    t.is(g.totDegree(3), g.degree(3) + g.degree(4) + g.degree(5)); // 10 + 41 + 27
    t.is(g.totDegree(1), 0);
    t.is(g.totDegree(5), 0);
    t.is(g.intDegree(0), 62);
    t.is(g.intDegree(3), 50);
    t.is(g.intDegree(1), 0);
    t.is(g.intDegree(2), 0);
    t.is(g.intDegree(6), 0);
    t.is(g.numComms(),3);
    t.is(g.communityWeight(0, 0), 62);
    t.is(g.communityWeight(3, 0), 0);
    t.is(g.communityWeight(3, 6), 28);
    t.is(g.communityWeight(3, 3), 50);
    t.is(g.communityWeight(6, 6), 0);
    
    t.results();
    
    g = new GraphBuilder().fromFile("data/gtests/arxiv.txt").build();
    LouvainDetector ld = new LouvainDetector(g);
    ld.run();
  }
}