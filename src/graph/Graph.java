/* An undirected, weighted, unmodifiable graph data structure.  */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import tester.Tester;

class Graph {
  private final int[][] matrix;          // adjacency matrix with weight info
  private final TIntArrayList[] adjList; // adjacency list
  private final int[] degrees;           // degree of each node
  private final int[] communities;       // community of each node
  private final int[] commTotalDegree;   // total degree of community
  private final int[] commIntDegree;     // internal degree of community
  private final int order;               // no. of nodes
  private final int size;                // sum of edge weights
  private final double m2;               // sum of edge weights * 2
  private int communitiesCount = 0;      // total no. of communities
  
  public Graph(GraphBuilder builder) {
    matrix      = builder.matrix();
    adjList     = builder.adjList();
    degrees     = builder.degrees();
    order       = builder.order();
    size        = builder.size();
    m2          = (double) size * 2d;
    
    communities     = new int[order];
    commTotalDegree = new int[order];
    commIntDegree   = new int[order];
    initialiseCommunities();
  }
  
  private void initialiseCommunities() {
    for (int i = 0; i < order; i++) {
      communities[i] = i;
      commTotalDegree[i] = degree(i);
      commIntDegree[i] = matrix[i][i]; // catches self-edges
      communitiesCount = order;
    }
  }
  
  public void moveTo(int node, int community) {
    int oldComm = communities[node];
    if (oldComm == community) return;
    
    communities[node] = community;
    commTotalDegree[oldComm] -= degree(node);
    commTotalDegree[community] += degree(node);
    
    for (int i = 0; i < adjList[node].size(); i++) {
      int neighbour = adjList[node].get(i);
      if (communities[neighbour] == community) {
        commIntDegree[community] += (matrix[node][neighbour] * 2);
      }
      if (communities[neighbour] == oldComm) {
        commIntDegree[oldComm] -= (matrix[node][neighbour] * 2);
      }
    }
    
    if (totDegree(oldComm) == 0) communitiesCount--;
  }
  
  // weight between a community and a node
  public int dnodecomm(int node, int community) {
    int dnodecomm = 0;
    for (int i = 0; i < adjList[node].size(); i++) {
      int neigh = adjList[node].get(i);
      if (communities[neigh] == community) dnodecomm += matrix[node][neigh];
    }
    return dnodecomm;
  }
  
  public void reassignCommunities() {
    double mod = modularity();
    double precision = 0.000001;
    double oldMod;
    int moves;
    boolean hasChanged;
    
    do {
      hasChanged = true;
      oldMod = mod;
      moves = maximiseLocalModularity();
      mod = modularity();
      if (mod - oldMod <= precision) hasChanged = false;
      if (moves == 0) hasChanged = false;
      System.out.printf("Mod: %5f  Delta: %5f  Comms: %d Moves:  %d%n", 
                        mod ,(mod - oldMod), communitiesCount(), moves);
    } while (hasChanged);
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
  
  private int maximiseLocalModularity() {
    int moves = 0;
    for (int node = 0; node < order; node++) {
      if (makeBestMove(node)) moves++;
    }
    return moves;
  }
  
  private boolean makeBestMove(int node) {
    double max = 0d;
    int oldComm = communities[node];
    int best = -1;
    
    for (int i = 0; i < adjList[node].size(); i++) {
      int community = communities[adjList[node].get(i)];
      double inc = deltaModularity(node, community);
      if (inc > max) {
        max = inc;
        best = community;
      }
    }
    
    if (best > 0 && best != oldComm) {
      moveTo(node, best);
      return true;
    }
    else return false;
  }

  // change in modularity if node is moved to community
  private double deltaModularity(int node, int community) {
    double dnodecomm = (double) dnodecomm(node, community);
    double ctot      = (double) totDegree(community);
    double wdeg      = (double) degree(node);

    return dnodecomm - ((ctot * wdeg) / m2);
  }
  
  public int communitiesCount() { return communitiesCount; }
   
  public int totDegree(int community) { return commTotalDegree[community]; }
  
  public int intDegree(int community) { return commIntDegree[community]; }
  
  public int size() { return size; }
  
  public int degree(int node) { return degrees[node]; }
  
  public int edge(int n1, int n2) { return matrix[n1][n2]; }

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
    g.moveTo(1,0);
    g.moveTo(2,0);
    g.moveTo(4,3);
    g.moveTo(5,3);
    
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
    t.is(g.communitiesCount(),3);
    
    t.results();
    
    g = new GraphBuilder().fromFile("data/gtests/arxiv.txt").build();
    g.reassignCommunities();
  }
}