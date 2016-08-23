/* makes one cluster per topic, and assigns all nodes to that cluster if the 
 * corresponding topic is the highest for that node. */ 
import java.util.*;
import gnu.trove.list.array.TIntArrayList;

public class PurityClusterer implements Clusterer {
  private final Graph g;
  private final int order;
  private final int topicCount;
  private final int comms;
  private final int[] community;
  private final double[][] inverseTheta;
  private final TIntArrayList[] members;
  private final Graph[] subgraphs; // one for each topic
  private final List<int[]> subComms; // one for each topic
  
  public PurityClusterer(Graph g, double[][] theta) {
    this.g = g;
    this.order = g.order();
    
    inverseTheta = MatrixTransposer.transpose(theta);
    topicCount = theta.length;
    comms = topicCount * topicCount;
    
    community = new int[order];
    members = new TIntArrayList[comms];
    subgraphs = new Graph[comms];
    subComms = new ArrayList<int[]>();
    
    for (int topic = 0; topic < comms; topic++) {
      members[topic] = new TIntArrayList();
    }
  }
  
  @Override
  public List<int[]> run() {
    initialiseComms();
    checkMemberLists();
    createSubgraphs();
    partitionSubgraphs();
    translateCommunities();
    
    List<int[]> list = new ArrayList<>();
    list.add(community);
    return list;
  }
  
  public void initialiseComms() {
    for (int node = 0; node < order; node++) {
      int bestTopic = -1;
      int secondBestTopic = -1;
      double maxTheta = 0d;
      double secondTheta = 0d;
      for (int topic = 0; topic < topicCount; topic++) {
        double theta = inverseTheta[node][topic];
        if (theta > maxTheta) {
          maxTheta = theta;
          bestTopic = topic;
        }
      }
      for (int topic = 0; topic < topicCount; topic++) {
        double theta = inverseTheta[node][topic];
        if (theta > secondTheta && theta < maxTheta) {
          secondTheta = theta;
          secondBestTopic = topic;
        }
      }
      // x + xmax * y
      members[bestTopic + topicCount * secondBestTopic].add(node);
      // community[node] = bestTopic;
    }
  }
  
  private void checkMemberLists() {
    int sum = 0;
    for (int topic = 0; topic < comms; topic++) {
      sum += members[topic].size();
    }    
    if (sum != g.order()) throw new Error("Order / member list size mismatch");
  }
  
  // for each topic, creates a subgraph of only those nodes where that topic is
  // the strongest.
  private void createSubgraphs() {
    for (int topic = 0; topic < comms; topic++) {
      subgraphs[topic] = new GraphBuilder().fromCommunity(g, members[topic]).build();
    }
  }
  
  // each subgraph is partitioned using the specified clusterer
  private void partitionSubgraphs() {
    Clusterer clusterer;
    for (int topic = 0; topic < comms; topic++) {
      Graph sub = subgraphs[topic];
      // double[][] subTheta = getSubTheta(sub, topic);
      clusterer = new LouvainDetector(sub);
      List<int[]> list = clusterer.run();
      subComms.add(list.get(list.size() - 1));
    }
  }
  
  private double[][] getSubTheta(Graph sub, int part) {
    double[][] subTheta = new double[sub.order()][topicCount];
    for (int subnode = 0; subnode < sub.order(); subnode++) {
      int node = members[part].get(subnode);
      for (int topic = 0; topic < topicCount; topic++) {
        subTheta[subnode][topic] = inverseTheta[node][topic];
      }
    }
    return subTheta;
  }
  
  // takes the communities from the subgraphs and applies them to the nodes of
  // the main graph
  private void translateCommunities() {
    for (int topic = 0; topic < comms; topic++) {
      Graph sub = subgraphs[topic];
      for (int subnode = 0; subnode < sub.order(); subnode++) {
        int node = members[topic].get(subnode);
        community[node] = subComms.get(topic)[subnode];
      }
    }
  }
}