import java.util.*;
import gnu.trove.list.array.TIntArrayList;

public class CommunityStructure {
  private final double[][] theta;
  private final double[][] inverseTheta;
  private final int topicCount;
  private final int docCount;
  private final int layers;
  private final int[] bestTopicInDoc;
  
  private final RandomCommunityAssigner rndAssigner;
  private final DocumentSimilarityMeasurer simRanker;
  
  private final List<int[]> communityLayers;
  private final List<int[]> rndCommLayers;
  private final List<int[]> commSizesLayers = new ArrayList<int[]>();
  private final List<SparseDoubleMatrix> commThetaLayers = new ArrayList<>();
  private final int[] numComms;
  
  // JS distance between doc and its comm:
  private final List<double[]> docCommCloseness = new ArrayList<double[]>();
  private final List<int[]> bestTopicInCommLayers = new ArrayList<int[]>();
  private final List<int[]> commScoreLayers = new ArrayList<int[]>();
  private final List<double[]> JSDivLayers = new ArrayList<double[]>();
  private final List<double[]> JSImpLayers = new ArrayList<double[]>();
  
  public CommunityStructure(List<int[]> communityLayers, double[][] theta) {
    this.communityLayers = communityLayers;
    this.theta = theta;
    
    topicCount = theta.length;
    docCount   = communityLayers.get(0).length;
    layers     = communityLayers.size();
    numComms   = new int[layers];
    
    inverseTheta   = new double[docCount][topicCount];
    rndAssigner    = new RandomCommunityAssigner(communityLayers);
    rndCommLayers  = rndAssigner.run();
    bestTopicInDoc = new int[docCount];
    
    for (int i = 0; i < layers; i++) {
      initialiseLayers(i);
    }
    
    simRanker = new DocumentSimilarityMeasurer(theta, inverseTheta);
    
    for (int i = 0; i < layers; i++) {
      initialiseDists(i);
    }
    
    predict();
  }
  
  public void predict() {
    for (int i = 0; i < layers; i++) {
      LayerPredictor lp = new LayerPredictor(i);
      lp.run();
    }    
    System.out.println();
  }
  
  private void initialiseDists(int layer) {
    double[] dc = new double[docCount];
    docCommCloseness.add(dc);
    
    for (int doc = 0; doc < docCount; doc++) {
      int comm = communities(layer)[doc];
      dc[doc] = simRanker.JSDivergence(comm, doc, commThetas(layer));
    }
  }
  
  private void initialiseLayers(int layer) {
    int[] communities = communities(layer);
    int[] commSizes = new int[docCount];
    SparseDoubleMatrix commThetas = new SparseDoubleMatrix(topicCount, docCount);
    commSizesLayers.add(commSizes);
    commThetaLayers.add(commThetas);
    int commCnt = 0;
    
    for (int doc = 0; doc < docCount; doc++) {
      int comm = communities[doc];
      commSizes[comm]++;
      double max = 0d;      
      for (int topic = 0; topic < topicCount; topic++) {
        commThetas.add(topic, comm, theta[topic][doc]);
        inverseTheta[doc][topic] = theta[topic][doc];
        if (theta[topic][doc] > max) {
          max = theta[topic][doc];
          bestTopicInDoc[doc] = topic;
        }        
      }
    }
    
    for (int comm = 0; comm < docCount; comm++) {
      if (commSizes[comm] != 0) {
        for (int topic = 0; topic < topicCount; topic++) {
          commThetas.div(topic, comm, commSizes[comm]);
        }
        commCnt++;
      }
    }
    numComms[layer] = commCnt;
  }
  
  class LayerPredictor {
    private final int[] communities;     // communities[doc] == comm of that doc
    private final int[] rcommunities;    // randomly genned
    
    private final SparseDoubleMatrix commThetas; //aggregated theta vals
    private final int[] bestTopicInComm; // most commonly ocurring topic
    private final int[] commScore;       // number of correct predictions
    private final int[] commSizes;       // size of each community
    private final int layer;
    private int correct = 0; // no. of correct predictions
    private double avgJSImprovement;
    private double avgJS;
    private double avgEntropy;
    
    // community member lists:
    private final TIntArrayList[] members = new TIntArrayList[docCount];
    private final TIntArrayList[] rndMembers = new TIntArrayList[docCount];
    
    private final double[] JSDiv = new double[docCount];
    // improvement in JSDiv over randomly shuffled communities:
    private final double[] JSDivImprovement = new double[docCount];
    
    public LayerPredictor(int layer) {
      this.layer = layer;
      communities  = communityLayers.get(layer);
      rcommunities = rndCommLayers.get(layer);
      commSizes    = commSizesLayers.get(layer);
      commThetas   = commThetaLayers.get(layer);
      commScore       = new int[docCount];
      bestTopicInComm = new int[docCount];
      
      for (int doc = 0; doc < docCount; doc++) {
        members[doc] = new TIntArrayList();
        rndMembers[doc] = new TIntArrayList();
      }
      for (int doc = 0; doc < docCount; doc++) {
        int comm = communities[doc];
        int rcomm = rcommunities[doc];
        members[comm].add(doc);
        rndMembers[rcomm].add(doc);
      }
    }
    
    public void run() {
      calculateJSDists();
      getBestCommTopics();
      getBestFit();
      System.out.printf("L%d: %d/%d = %.01f%% JS: %.03f JSi: %.03f E: %.03f%n", 
                        layer, correct, docCount, 
                        (correct / (double) docCount) * 100, avgJS, 
                        avgJSImprovement, avgEntropy);
      // printScores();
      
      bestTopicInCommLayers.add(bestTopicInComm);
      commScoreLayers.add(commScore);
      JSDivLayers.add(JSDiv);
      JSImpLayers.add(JSDivImprovement);
    }
    
    private void printScores() {
      for (int comm = 0; comm < docCount; comm++) {
        if (commSizes[comm] != 0 && layer == 1) {
          System.out.println("comm: " + comm +
                             " best: " + bestTopicInComm[comm] + " " +
                             commScore[comm] + "/" + commSizes[comm] +
                             " JS " + JSDiv[comm] + 
                             " JSimp " + JSDivImprovement[comm] + 
                             " Entr " + entropy(layer, comm));
        }
      }
    }
    
    private void calculateJSDists() {
      for (int comm = 0; comm < docCount; comm++) {
        if (members[comm].size() > 0) {
          JSDiv[comm] = JSDiv(members[comm]);
          JSDivImprovement[comm] = JSDiv(rndMembers[comm]) - JSDiv[comm];
          avgJS += JSDiv[comm];
          avgJSImprovement += JSDivImprovement[comm];
          avgEntropy += entropy(layer, comm);
        }
      }
      avgJS /= numComms(layer);
      avgJSImprovement /= numComms(layer);
      avgEntropy /= numComms(layer);
    }
    
    private void getBestCommTopics() {
      for (int comm = 0; comm < docCount; comm++) {
        double max = 0d;
        for (int topic = 0; topic < topicCount; topic++) {
          if (commThetas.get(topic, comm) > max) {
            max = commThetas.get(topic, comm);
            bestTopicInComm[comm] = topic;
          }
        }
      }    
    }
    
    // finds the closest community to each doc based on KL-divergence of the theta
    // values of the doc and the community(aggregate of docs)
    private void getBestFit() {
      int checked = 0;
      for (int doc = 0; doc < docCount; doc++) {
        int comm = communities[doc];
        if (bestTopicInDoc[doc] == bestTopicInComm[comm]) {
          correct++;
          commScore[comm]++;
        }
        checked++;
        // System.out.println(correct + "/" + checked + " predicted correctly.");
      }
    }
  }
  
  public double[][] theta() { return theta; }
  public double[][] inverseTheta() { return inverseTheta;}
  
  public int topicCount() { return topicCount; }
  public int docCount() { return docCount; }
  public int layers() { return layers; }
  public int numComms(int layer) { return numComms[layer]; }
  
  public List<SparseDoubleMatrix> commThetaLayers() { return commThetaLayers; }
  public SparseDoubleMatrix commThetas(int layer) { return commThetaLayers.get(layer); }
  
  public List<int[]> communityLayers() { return communityLayers; }
  public int[] communities(int layer) { return communityLayers.get(layer); }
  public int community(int layer, int doc) { return communityLayers.get(layer)[doc]; }
  
  public List<int[]> commSizesLayers() { return commSizesLayers; }
  public int[] commSizes(int layer) { return commSizesLayers.get(layer); }
  public int commSize(int layer, int comm) { return commSizesLayers.get(layer)[comm]; }
  
  public double[] docCommCloseness(int layer) { return docCommCloseness.get(layer); }
  public double docCommCloseness(int layer, int comm) { return docCommCloseness.get(layer)[comm]; }
  
  public double JSDiv(TIntArrayList docs) {
    return simRanker.JSDivergence(docs);
  }

  public double JSDiv(int layer, int comm) { 
    return JSDivLayers.get(layer)[comm];
  }
  
  public double JSImp(int layer, int comm) {
    return JSImpLayers.get(layer)[comm];
  }
  
  public double entropy(int layer, int comm) { 
    return simRanker.entropy(comm, commThetaLayers.get(layer));
  }
  
  public int[] bestTopicInDoc() { return bestTopicInDoc; }
  public int bestTopicInDoc(int doc) { return bestTopicInDoc[doc]; }
  
  public int[] bestTopicInComm(int layer) { return bestTopicInCommLayers.get(layer); }
  public int bestTopicInComm(int layer, int comm) { return bestTopicInCommLayers.get(layer)[comm]; }  
  
  public int[] commScores(int layer) { return commScoreLayers.get(layer); }
  public int commScore(int layer, int comm) { return commScoreLayers.get(layer)[comm]; }  
}