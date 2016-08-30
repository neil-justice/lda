import gnu.trove.list.array.TIntArrayList;

public class HardClustering implements Clustering {
  private final int N; // no. of nodes
  private final double[] dist;
  private final CommunityStructure structure;
  private final int numComms;
  private final int layer;
  
  public HardClustering(CommunityStructure structure, int layer) {
    this.structure = structure;
    this.layer = layer;
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
    return structure.community(layer, comm);
  }
  
  public int indexFromComm(int comm) {
    return structure.indexFromComm(layer, comm);
  }
  
  public int commFromIndex(int index) {
    return structure.commIndex(layer, index);
  }
  
  public TIntArrayList members(int comm) {
    return structure.members(layer, comm);
  }
}