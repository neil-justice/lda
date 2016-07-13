/* transporter class for community layer info */

public class CommunityStructure {
  private int layer = 0;                  // current community layer
  private final TIntArrayList numComms;   // total no. of communities per layer
  private final List<int[]> commLayers;   // list of community layers
  private final List<int[]> totDegrees;   // total degree of community in layer
  private final List<int[]> intDegrees;   // int. degree of community in layer
  private final List<Graph> coarseGraphs  // coarse-grained community graphs
  
  public CommunityStructure(int[] communities, int[] commTotalDegree, 
                            int[] commIntDegree, int numComms)
    this.communities     = communities;
    this.commTotalDegree = commTotalDegree;
    this.commIntDegree   = commIntDegree;
    this.numComms        = numComms;
  }
  
  public int[] totDegrees() { return commTotalDegree; }
  public int[] intDegrees() { return commIntDegree; }
  public int[] communities() { return communities; }
  public int numComms { return numComms; }
}