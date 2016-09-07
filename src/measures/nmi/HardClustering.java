import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class HardClustering implements Clustering {
  private final int N; // no. of nodes
  private final double[] dist;
  private final int numComms;
  private final int layer;
  private final TIntArrayList[] members;
  private final int[] commFromIndex;
  private final int[] community;
  private final TIntIntHashMap indexFromComm;
  
  public HardClustering(CommunityStructure structure, int layer) {
    this.layer = layer;
    members = structure.members(layer);
    commFromIndex = structure.commIndex(layer);
    indexFromComm = structure.indexFromComm(layer);
    community = structure.communities(layer);
    N = structure.docCount();
    numComms = structure.numComms(layer);
    
    dist = new double[numComms];
    
    for (int i = 0; i < numComms; i++) {
      int comm = structure.commIndex(layer, i);
      dist[i] = (double) structure.commSize(layer, comm) / N;
    }
  }
  
  @Override
  public double distribution(int index) { return dist[index]; }
  
  @Override
  public double entropy() { return Entropy.entropy(dist); }
  
  @Override
  public int length() { return numComms; }
  
  public int N() { return N; }
  
  public int community(int comm) { 
    return community[comm];
  }
  
  public int indexFromComm(int comm) {
    return indexFromComm.get(comm);
  }
  
  public int commFromIndex(int index) {
    return commFromIndex[index];
  }
  
  public TIntArrayList members(int comm) {
    return members[comm];
  }
}