

public class LouvainDetector {
  
  private final Graph g;
  private final double precision = 0.000001;
  
  public LouvainDetector(Graph g) {
    this.g = g;
  }
  
  public void run() {
    long s1 = System.nanoTime();
    reassignCommunities();
    long e1 = System.nanoTime();
    double time = (e1 - s1) / 1000000000d;
    System.out.println("seconds taken: " + time );
  }
  
  private void reassignCommunities() {
    double mod = g.modularity();
    double oldMod;
    int moves;
    boolean hasChanged;
    
    do {
      hasChanged = true;
      oldMod = mod;
      moves = maximiseLocalModularity();
      mod = g.modularity();
      if (mod - oldMod <= precision) hasChanged = false;
      if (moves == 0) hasChanged = false;
      System.out.printf("Mod: %5f  Delta: %5f  Comms: %d Moves:  %d%n", 
                        mod ,(mod - oldMod), g.communitiesCount(), moves);
    } while (hasChanged);
  } 
  
  private int maximiseLocalModularity() {
    int moves = 0;
    for (int node = 0; node < g.order(); node++) {
      if (makeBestMove(node)) moves++;
    }
    return moves;
  }
  
  private boolean makeBestMove(int node) {
    double max = 0d;
    int best = -1;
    
    for (int i = 0; i < g.neighbours(node).size(); i++) {
      int community = g.community(g.neighbours(node).get(i));
      double inc = deltaModularity(node, community);
      if (inc > max) {
        max = inc;
        best = community;
      }
    }
    
    if (best > 0 && best != g.community(node)) {
      g.moveToComm(node, best);
      return true;
    }
    else return false;
  }

  // change in modularity if node is moved to community
  private double deltaModularity(int node, int community) {
    double dnodecomm = (double) g.dnodecomm(node, community);
    double ctot      = (double) g.totDegree(community);
    double wdeg      = (double) g.degree(node);

    return dnodecomm - ((ctot * wdeg) / g.m2());
  }
}