import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.iterator.TIntObjectIterator;

public class DocumentSimilarityMeasurer {
  private final double[][] theta;
  private final double[][] thetaLog;
  private final int topicCount;
  private final int docCount;
  private final double[] UNIFORM_DIST;
  
  public DocumentSimilarityMeasurer(double[][] theta) {
    this.theta = theta;
    topicCount = theta.length;
    docCount = theta[0].length;

    thetaLog = new double[topicCount][docCount];
    UNIFORM_DIST = new double[topicCount];
    
    setUniformDist();
    calculateLogs();    
  }
  
  private void setUniformDist() {
    for (int topic = 0; topic < topicCount; topic++) {
      UNIFORM_DIST[topic] = 1d / topicCount;
    }
  }

  private void calculateLogs() {
    for (int doc = 0; doc < docCount; doc++) {
      for (int topic = 0; topic < topicCount; topic++) {
        thetaLog[topic][doc] = Math.log(theta[topic][doc]);
      }
    }
  }
  
  // avg JS distance between all docs in all comms
  public double[] JSAvgDistance(int layer, int[] communities) {
    double[] js = new double[docCount];
    int[] count = new int[docCount];
    TIntObjectHashMap<TIntArrayList> members = new TIntObjectHashMap<>();
    
    for (int doc = 0; doc < docCount; doc++) {
      TIntArrayList list = members.get(communities[doc]);
      if (list == null) {
        list = new TIntArrayList();
        members.put(communities[doc], list);
      }
      list.add(doc);
    }
    
    System.out.println("Calculating JS averages...");
    for ( TIntObjectIterator<TIntArrayList> it = members.iterator(); it.hasNext(); ) {
      it.advance();
      int comm = it.key();
      TIntArrayList list = it.value();
      for (int i = 0; i < list.size(); i++) {
        for (int j = 0; j < list.size(); j++) {
          int doc = list.get(i);
          int doc2 = list.get(j);
          if (doc != doc2) {
            js[comm] += JSDistance(doc, doc2);
            count[comm]++;
          }
        }
      }
      js[comm] /= count[comm];
    }
  
    return js;
  }  
  
  // Jenson-Shannon distance is a smoothed symmetric version of Kullback-Leiber
  // divergence.
  public double JSDistance(int doc, int doc2) {
    double M[] = new double[topicCount]; //avg of the two distributions
    for (int topic = 0; topic < topicCount; topic++) {
      M[topic] = (theta[topic][doc] + theta[topic][doc2]) / 2;
    }
    return (KLDivergence(doc, M) + KLDivergence(doc2, M)) / 2;
  }
  
  // Kullback-Leiber divergence presents a rating of the similarity of two 
  // probability distributions.  Here one is accessed via doc ID and one is 
  // passed as an array, to use as part of JSDistance above.
  private double KLDivergence(int doc, double[] prob) {
    double KLDivergence = 0d;
    for (int topic = 0; topic < topicCount; topic++) {
      if (theta[topic][doc] != 0 && prob[topic] != 0) {
        KLDivergence += theta[topic][doc] * 
                        (thetaLog[topic][doc] - Math.log(prob[topic]));
      }
    }
    return KLDivergence / Math.log(2);
  }

  // JS distance between a comm and the uniform distribution
  public double JSCommUniformDistance(int comm, SparseDoubleMatrix commThetas) {
    double M[] = new double[topicCount]; //avg of the two distributions
    
    for (int topic = 0; topic < topicCount; topic++) {
      M[topic] = (UNIFORM_DIST[topic] + commThetas.get(topic, comm)) / 2;
    }
    return (KLDivergence(UNIFORM_DIST, M) + KLCommDivergence(comm, M, commThetas)) / 2;
  }
  
  // JS distance between a doc and a comm
  public double JSCommDistance(int comm, int doc, SparseDoubleMatrix commThetas) {
    double M[] = new double[topicCount]; //avg of the two distributions
    
    for (int topic = 0; topic < topicCount; topic++) {
      M[topic] = (theta[topic][doc] + commThetas.get(topic, comm)) / 2;
    }
    return (KLDivergence(doc, M) + KLCommDivergence(comm, M, commThetas)) / 2;
  }
  
  // KL div between a comm and a prob. dist.
  private double KLCommDivergence(int comm, double[] prob, 
                                  SparseDoubleMatrix commThetas) {
    double KLDivergence = 0d;
    for (int topic = 0; topic < topicCount; topic++) {
      if (commThetas.get(topic, comm) != 0 && prob[topic] != 0) {
        KLDivergence += commThetas.get(topic, comm) * 
                        Math.log(commThetas.get(topic, comm) / prob[topic]);
      }
    }
    return KLDivergence / Math.log(2);
  }
  
  public static double JSDistance(final double[] p1, final double[] p2) {
    if (p1.length != p2.length) throw new Error("length mismatch: " + p1.length + " " + p2.length);
    double[] M = new double[p1.length];
    
    for (int i = 0; i < p1.length; ++i) {
        M[i] += (p1[i] + p2[i]) / 2;
    }
    return (KLDivergence(p1, M) + KLDivergence(p2, M)) / 2;
  }

  public static double KLDivergence(final double[] p1, final double[] p2) {
    double KLDivergence = 0d;
    
    for (int i = 0; i < p1.length; ++i) {
      if (p1[i] != 0d || p2[i] != 0d) {
        KLDivergence += p1[i] * Math.log(p1[i] / p2[i]);
      }
    }
    return KLDivergence / Math.log(2);
  }
}