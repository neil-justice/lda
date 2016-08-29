/* makes one cluster per part, and assigns all nodes to that cluster if the 
 * corresponding part is the highest for that node. */ 
import java.util.*;
import gnu.trove.list.array.TIntArrayList;

public class PurityClusterer implements Clusterer {
  private final Graph g;
  private final int order;
  private final int topicCount;
  private final int partCount;
  private final int[] community;
  private final double[][] inverseTheta;
  private final TIntArrayList[] members;
  private final Graph[] subgraphs; // one for each topic + 1 for high entropy nodes
  private final List<int[]> subComms;
  
  public PurityClusterer(Graph g, double[][] theta) {
    this.g = g;
    this.order = g.order();
    inverseTheta = MatrixTransposer.transpose(theta);
    topicCount = theta.length;
    partCount = theta.length + 1;
    
    community = new int[order];
    members = new TIntArrayList[partCount];
    subgraphs = new Graph[partCount];
    subComms = new ArrayList<int[]>();
    
    for (int part = 0; part < partCount; part++) {
      members[part] = new TIntArrayList();
    }
  }
  
  @Override
  public List<int[]> run() {
    initialiseComms();
    checkMemberLists();
    
    List<int[]> list = new ArrayList<>();
    list.add(community);
    return list;
  }
  
  public void initialiseComms() {
    for (int node = 0; node < order; node++) {
      int bestPart = 0;
      if (Entropy.entropy(inverseTheta[node], topicCount) > 0.9) {
        bestPart = partCount - 1;
      }
      else bestPart = getStrongestTopic(node);
      members[bestPart].add(node);
      community[node] = bestPart;
    }
  }
  
  private int getStrongestTopic(int node) {
    int bestPart = 0;
    double maxTheta = 0d;    
    for (int topic = 0; topic < topicCount; topic++) {
      double theta = inverseTheta[node][topic];
      if (theta > maxTheta) {
        maxTheta = theta;
        bestPart = topic;
      }
    }
    return bestPart;
  }
  
  private void checkMemberLists() {
    int sum = 0;
    for (int part = 0; part < partCount; part++) {
      sum += members[part].size();
    }    
    if (sum != g.order()) throw new Error("Order / member list size mismatch");
  }
  
  // for each part, creates a subgraph of only those nodes where that part is
  // the strongest.
  private void createSubgraphs() {
    for (int part = 0; part < partCount; part++) {
      subgraphs[part] = new GraphBuilder().fromCommunity(g, members[part]).build();
    }
  }
  
  // each subgraph is partitioned using the specified clusterer
  private void partitionSubgraphs() {
    Clusterer clusterer;
    for (int part = 0; part < partCount; part++) {
      Graph sub = subgraphs[part];
      // double[][] subTheta = getSubTheta(sub, part);
      clusterer = new LouvainDetector(sub);
      List<int[]> list = clusterer.run();
      subComms.add(list.get(list.size() - 1));
    }
  }
  
  private double[][] getSubTheta(Graph sub, int part) {
    double[][] subTheta = new double[sub.order()][partCount];
    for (int subnode = 0; subnode < sub.order(); subnode++) {
      int node = members[part].get(subnode);
      for (int topic = 0; topic < partCount - 1; topic++) {
        subTheta[subnode][topic] = inverseTheta[node][topic];
      }
    }
    return subTheta;
  }
  
  // takes the communities from the subgraphs and applies them to the nodes of
  // the main graph
  private void translateCommunities() {
    for (int part = 0; part < partCount; part++) {
      Graph sub = subgraphs[part];
      for (int subnode = 0; subnode < sub.order(); subnode++) {
        int node = members[part].get(subnode);
        community[node] = subComms.get(part)[subnode];
      }
    }
  }
}