/* calculates the mutual information of 2 partition sets, for formula see
 * http://math.stackexchange.com/questions/438078/mutual-information-for-clustering */
import gnu.trove.list.array.TIntArrayList;

public class MutualInformation {
  private CommunityStructure s1;
  private CommunityStructure s2;
  private int layer1;
  private int layer2;
  
  private final int N; // no. of nodes
  
  public MutualInformation(int N) {
    this.N = N;
  }
  
  public double compare(CommunityStructure s1, int layer1, 
                        CommunityStructure s2, int layer2) {
    long s = System.nanoTime();
    double MI = run(s1, layer1, s2, layer2);
    long e = System.nanoTime();
    double time = (e - s) / 1000000000d;
    System.out.println("seconds taken: " + time );
    double NMI = normalise(MI);
    System.out.println("NMI: " + NMI);
    return NMI;
  }
  
  private double normalise(double MI) {
    double[] dist1 = new double[N];
    double[] dist2 = new double[N];
    
    for (int comm = 0; comm < N; comm++) {
      dist1[comm] = s1.commSize(layer1, comm) / (double) N;
      dist2[comm] = s2.commSize(layer2, comm) / (double) N;
    }
    
    double e1 = DocumentSimilarityMeasurer.entropy(dist1);
    double e2 = DocumentSimilarityMeasurer.entropy(dist2);
    return MI / ((e1 + e2) * 0.5);
  }
  
  private double run(CommunityStructure s1, int layer1, 
                     CommunityStructure s2, int layer2) {
    this.s1 = s1;
    this.s2 = s2;
    this.layer1 = layer1;
    this.layer2 = layer2;
    double MI = 0;
    
    for (int i = 0; i < s1.numComms(layer1); i++) {
      for (int j = 0; j < s2.numComms(layer2); j++) {
        int comm1 = s1.commIndex(layer1, i);
        int comm2 = s2.commIndex(layer2, j);
        double intersect = intersection(comm1, comm2);
        long size1 = (long) s1.commSize(layer1, comm1);
        long size2 = (long) s2.commSize(layer2, comm2);
        double d = (N * intersect) / (size1 * size2);
        MI += (intersect / N) * log(d);
      }
    }
    MI /= Math.log(2); // convert to log2
    return MI;
  }
  
  // in information theory, 0 log(0) == 0.
  // this happens because often comm1 and comm2 have no nodes in common
  private double log(double val) {
    if (val == 0) return 0;
    else return Math.log(val);
  }
  
  // returns the no. of nodes which are in both communities
  private double intersection(int comm1, int comm2) {
    TIntArrayList duplicate = new TIntArrayList(s1.members(layer1, comm1));
    duplicate.retainAll(s2.members(layer2, comm2));

    return (double) duplicate.size();
  }
}