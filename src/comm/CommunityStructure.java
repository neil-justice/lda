import java.util.*;
import gnu.trove.list.array.TIntArrayList;

public class CommunityStructure {
  private final double[][] theta;
  private final double[][] inverseTheta; // transposed version of theta
  private final int topicCount;
  private final int docCount;
  private final int layers;
  private final int[] bestTopicInDoc; // topic w/ highest theta per doc
  
  private final RandomCommunityAssigner rndAssigner;
  private final DocumentSimilarityMeasurer simRanker;  
  private final NodeAttributes attributes;
  
  private final List<int[]> commWordCount = new ArrayList<int[]>(); // average
  private final List<int[]> commFollowers = new ArrayList<int[]>(); // average
  private final List<int[]> commFriends   = new ArrayList<int[]>(); // average
  private final List<int[]> communityLayers;
  // shuffled community assignments
  private final List<int[]> rndCommLayers;
  // size of each comm
  private final List<int[]> commSizesLayers = new ArrayList<int[]>();
  // commIndexes.get(layer)[i] = ID of ith community.
  private final List<int[]> commIndexes     = new ArrayList<int[]>();
  // JS distance between doc and its comm:
  private final List<int[]> commScoreLayers       = new ArrayList<int[]>();
  private final List<int[]> bestTopicInCommLayers = new ArrayList<int[]>();
  private final List<double[]> docCommCloseness = new ArrayList<double[]>();
  private final List<double[]> JSDivLayers      = new ArrayList<double[]>();
  private final List<double[]> JSImpLayers      = new ArrayList<double[]>();
  private final List<double[]> entropyLayers    = new ArrayList<double[]>();
  // these lists keep track of the members of each comm:
  private final List<TIntArrayList[]> memberLayers       = new ArrayList<>();
  private final List<SparseDoubleMatrix> commThetaLayers = new ArrayList<>();  
  private final int[] numComms;
  
  public CommunityStructure(List<int[]> communityLayers, double[][] theta,
                            NodeAttributes attributes) {
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
    
    this.attributes = attributes;
    
    for (int i = 0; i < layers; i++) {
      initialiseLayers(i);
    }
    
    simRanker = new DocumentSimilarityMeasurer(theta, inverseTheta);
    
    for (int i = 0; i < layers; i++) {
      initialiseDists(i);
    }
    
    predict();
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
    int[] wordCount = new int[docCount];
    int[] friends   = new int[docCount];
    int[] followers = new int[docCount];
    int[] indexes   = new int[docCount];
    
    SparseDoubleMatrix commThetas = new SparseDoubleMatrix(topicCount, docCount);
    
    for (int doc = 0; doc < docCount; doc++) {
      int comm = communities[doc];
      commSizes[comm]++;
      wordCount[comm] += attributes.wordCount(doc);
      followers[comm] += attributes.followers(doc);
      friends[comm]   += attributes.friends(doc);
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
    
    int commCnt = 0;
    for (int comm = 0; comm < docCount; comm++) {
      if (commSizes[comm] != 0) {
        for (int topic = 0; topic < topicCount; topic++) {
          // div by total wordcount for that comm to get weighted avr:
          commThetas.div(topic, comm, commSizes[comm]); 
        }
        wordCount[comm] /= commSizes[comm]; // get averages
        followers[comm] /= commSizes[comm]; // get averages
        friends[comm]   /= commSizes[comm]; // get averages
        indexes[commCnt] = comm;
        commCnt++;
      }
    }
    numComms[layer] = commCnt;
    commSizesLayers.add(commSizes);
    commThetaLayers.add(commThetas);
    commWordCount.add(wordCount);
    commFollowers.add(followers);
    commFriends.add(friends);
    commIndexes.add(indexes);
  }
  
  public void predict() {
    System.out.println("(averages)");
    System.out.println("L  corr           % corr JS    JSi   E     size");
    for (int i = 0; i < layers; i++) {
      LayerPredictor lp = new LayerPredictor(i);
      lp.run();
    }    
    System.out.println();
  }
  
  class LayerPredictor {
    private final int[] communities;     // communities[doc] == comm of that doc
    private final int[] rcommunities;    // randomly genned
    
    // community member list:
    private final TIntArrayList[] members = new TIntArrayList[docCount];
    private final TIntArrayList[] rndMembers = new TIntArrayList[docCount];
    
    private final double[] JSDiv = new double[docCount];
    // improvement in JSDiv over randomly shuffled communities:
    private final double[] JSDivImprovement = new double[docCount];
    private final double[] entropy = new double[docCount];
    
    private final SparseDoubleMatrix commThetas; //aggregated theta vals
    private final int[] bestTopicInComm; // most commonly ocurring topic
    private final int[] commScore;       // number of correct predictions
    private final int[] commSizes;       // size of each community
    private final int layer;
    
    private int correct = 0; // no. of correct predictions
    private double avgJSImprovement;
    private double avgJS;
    private double avgEntropy;
    private double avgSize;
    
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
      System.out.printf("%d %6d/%6d = %.01f%%  %.03f %.03f %.03f %5f%n", 
                        layer, correct, docCount, 
                        (correct / (double) docCount) * 100, avgJS, 
                        avgJSImprovement, avgEntropy, avgSize);
      // printScores();
      
      bestTopicInCommLayers.add(bestTopicInComm);
      commScoreLayers.add(commScore);
      JSDivLayers.add(JSDiv);
      JSImpLayers.add(JSDivImprovement);
      entropyLayers.add(entropy);
      memberLayers.add(members);
    }
    
    private void printScores() {
      for (int comm = 0; comm < docCount; comm++) {
        if (commSizes[comm] != 0 && layer == 1) {
          System.out.println("comm: " + comm +
                             " best: " + bestTopicInComm[comm] + " " +
                             commScore[comm] + "/" + commSizes[comm] +
                             " %corr " + ((correct / (double) docCount) * 100) +
                             " JS " + JSDiv[comm] + 
                             " JSimp " + JSDivImprovement[comm] + 
                             " Entr " + entropy[comm]);
        }
      }
    }
    
    private void calculateJSDists() {
      for (int comm = 0; comm < docCount; comm++) {
        if (members[comm].size() > 0) {
          JSDiv[comm] = JSDiv(members[comm]);
          JSDivImprovement[comm] = JSDiv(rndMembers[comm]) - JSDiv[comm];
          entropy[comm] = calcEntropy(layer, comm);
          avgJS += JSDiv[comm];
          avgJSImprovement += JSDivImprovement[comm];
          avgEntropy += entropy[comm] * commSizes[comm];
          avgSize += commSizes[comm];
        }
      }
      avgJS /= numComms(layer);
      avgJSImprovement /= numComms(layer);
      avgEntropy /= docCount;
      avgSize /= numComms(layer);
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
  
  public double theta(int topic, int doc) { return theta[topic][doc]; }
  public double[][] theta() { return theta; }
  public double[][] inverseTheta() { return inverseTheta;}
  
  public int topicCount() { return topicCount; }
  public int docCount() { return docCount; }
  public int layers() { return layers; }
  public int numComms(int layer) { return numComms[layer]; }
  
  public SparseDoubleMatrix commThetas(int layer) { return commThetaLayers.get(layer); }
  public double commTheta(int layer, int topic, int comm) { return commThetaLayers.get(layer).get(topic, comm); }
  
  public List<int[]> communityLayers() { return communityLayers; }
  public int[] communities(int layer) { return communityLayers.get(layer); }
  public int community(int layer, int doc) { return communityLayers.get(layer)[doc]; }
  
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
  
  public double[] JSDiv(int layer) { 
    return JSDivLayers.get(layer);
  }
  
  public double JSDiv(int layer, int doc, int comm) {
    return simRanker.JSDivergence(comm, doc, commThetaLayers.get(layer)); 
  }
  
  public double JSImp(int layer, int comm) {
    return JSImpLayers.get(layer)[comm];
  }
  
  public double entropy(int layer, int comm) { 
    return entropyLayers.get(layer)[comm];
  }
  
  public double[] entropy(int layer) { return entropyLayers.get(layer); }
  
  public double calcEntropy(int layer, int comm) { 
    return simRanker.entropy(comm, commThetaLayers.get(layer), topicCount);
  }
  
  public double docEntropy(int doc) {
    return simRanker.entropy(inverseTheta[doc], topicCount);
  }
  
  public int[] bestTopicInDoc() { return bestTopicInDoc; }
  public int bestTopicInDoc(int doc) { return bestTopicInDoc[doc]; }
  
  public int[] bestTopicInComm(int layer) { return bestTopicInCommLayers.get(layer); }
  public int bestTopicInComm(int layer, int comm) { return bestTopicInCommLayers.get(layer)[comm]; }  
  
  public int[] commScores(int layer) { return commScoreLayers.get(layer); }
  public int commScore(int layer, int comm) { return commScoreLayers.get(layer)[comm]; }
  
  public int[] wordCount(int layer) { return commWordCount.get(layer); }
  public int[] followers(int layer) { return commFollowers.get(layer); }
  public int[] friends(int layer) { return commFriends.get(layer); }
  
  public int commIndex(int layer, int index) { return commIndexes.get(layer)[index]; }
  
  public int wordCount(int layer, int comm) { return commWordCount.get(layer)[comm]; }
  public int followers(int layer, int comm) { return commFollowers.get(layer)[comm]; }
  public int friends(int layer, int comm) { return commFriends.get(layer)[comm]; }
  public NodeAttributes nodeAttributes() { return attributes; }
  
  public TIntArrayList members(int layer, int comm) {
    return memberLayers.get(layer)[comm]; 
  }
}