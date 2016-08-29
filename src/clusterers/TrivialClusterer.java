/* 'clusters' nodes such that each community contains only 1 node. */ 
import java.util.*;

public class TrivialClusterer implements Clusterer {
  private final int order;
  private final int[] community;
  
  public TrivialClusterer(int order, double[][] theta) {
    this.order = order;
    community = new int[order];
  }
  
  @Override
  public List<int[]> run() { 
    List<int[]> list = new ArrayList<>();
    list.add(community);
    
    for (int node = 0; node < order; node++) {
      community[node] = node;
    }
    return list;
  }
}