public class TopicCoocurrenceMonitor {
  private final CommunityStructure structure;
  private final double threshold  = 0.18; // Value for coocurrence to count.
  private final double entropyMax = 3.5; // Communities with entropy over this
                                         // are ignored.
  private final double minSize = 7; // Comms smaller than this are ignored.
  private final double[][] coocurrence;
  private final int topicCount;
  private final int docCount;
  private int commCount;
  
  public TopicCoocurrenceMonitor(CommunityStructure structure) {
    this.structure = structure;
    topicCount = structure.topicCount();
    docCount = structure.docCount();
    coocurrence = new double[topicCount][topicCount];
  }
  
  public void run(int layer) {
    generateCoocurrenceMatrix(layer);
    print();
  }
  
  private void generateCoocurrenceMatrix(int layer) {
    for (int comm = 0; comm < docCount; comm++) {
      if (structure.commSize(layer, comm) >= minSize
      &&  structure.entropy(layer, comm) < entropyMax) {
        checkComm(layer, comm);
        commCount++;
      }
    }
    
    // for (int t1 = 0; t1 < topicCount; t1++) {
    //   for (int t2 = 0; t2 < topicCount; t2++) {
    //     coocurrence[t1][t2] /= commCount;
    //   }
    // }    
  }
  
  private void checkComm(int layer, int comm) {
    for (int t1 = 0; t1 < topicCount; t1++) {
      for (int t2 = 0; t2 < topicCount; t2++) {
        double val1 = structure.commTheta(layer, t1, comm);
        double val2 = structure.commTheta(layer, t2, comm);
        if (val1 > threshold && val2 > threshold) {
          coocurrence[t1][t2] += (val1 + val2) / 2d;
        }
      }
    }
  }
  
  public void print() {
    for (int t1 = 0; t1 < topicCount; t1++) {
      for (int t2 = 0; t2 < topicCount; t2++) {
        System.out.printf("%d %d %.02f%n", t1, t2, coocurrence[t1][t2]);
      }
    }
  }
}