/* Jenson-Shannon distance is a smoothed symmetric version of Kullback-Leiber
 * divergence.  Kullback-Leiber divergence presents a rating of the similarity of two 
 * probability distributions.  JSD can be taken between many distributions. */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.iterator.TIntObjectIterator;

public class JSD {
  private final double[][] inverseTheta;
  private final double[][] thetaLog;
  private final int topicCount;
  private final int docCount;
  
  public JSD(double[][] inverseTheta) {
    this.inverseTheta = inverseTheta;
    topicCount = inverseTheta[0].length;
    docCount = inverseTheta.length;

    thetaLog = new double[docCount][topicCount];

    calculateLogs();    
  }
  
  private void calculateLogs() {
    for (int doc = 0; doc < docCount; doc++) {
      for (int topic = 0; topic < topicCount; topic++) {
        thetaLog[doc][topic] = Math.log(inverseTheta[doc][topic]);
      }
    }
  }

  // measures the divergence between multiple probability distributions
  // the upper bound of this is not 1, but log2(size).
  public double JSDivergence(TIntArrayList docs) {
    int size = docs.size();
    double weight = 1d / size;
    double[] sum = new double[topicCount];
    double entropy = 0d;
    
    for (int i = 0; i < size; i++) {
      int doc = docs.get(i);
      for (int topic = 0; topic < topicCount; topic++) {
        sum[topic] += inverseTheta[doc][topic] * weight;
      }
      entropy += Entropy.entropy(inverseTheta[doc]) * weight;
    }
    return (Entropy.entropy(sum) - entropy);
  }
  

  public double JSDivergence(int doc, int doc2) {
    double M[] = new double[topicCount]; //avg of the two distributions
    for (int topic = 0; topic < topicCount; topic++) {
      M[topic] = (inverseTheta[doc][topic] + inverseTheta[doc2][topic]) / 2;
    }
    return (KLDivergence(doc, M) + KLDivergence(doc2, M)) / 2;
  }
  
  // Here one is accessed via doc ID and one is 
  // passed as an array, to use as part of JSDistance above.  This enables
  // the log table to be used to speed up calculations
  private double KLDivergence(int doc, double[] prob) {
    double KLDivergence = 0d;
    for (int topic = 0; topic < topicCount; topic++) {
      if (inverseTheta[doc][topic] != 0 && prob[topic] != 0) {
        KLDivergence += inverseTheta[doc][topic] * 
                        (thetaLog[doc][topic] - Math.log(prob[topic]));
      }
    }
    return KLDivergence / Math.log(2);
  }
  
  // JS div between a doc and a comm
  public double JSDivergence(int comm, int doc, SparseDoubleMatrix commThetas) {
    return JSDivergence(comm, inverseTheta[doc], commThetas);
  }

  // JS div between a comm and a distribution
  public static double JSDivergence(int comm, double[] prob, 
                                    SparseDoubleMatrix commThetas) {
    double M[] = new double[prob.length]; //avg of the two distributions
    
    for (int topic = 0; topic < prob.length; topic++) {
      M[topic] = (prob[topic] + commThetas.get(topic, comm)) / 2;
    }
    return (KLDivergence(prob, M) + KLCommDivergence(comm, M, commThetas)) / 2;
  }
  
  // KL div between a comm and a prob. dist.
  private static double KLCommDivergence(int comm, double[] prob, 
                                  SparseDoubleMatrix commThetas) {
    double KLDivergence = 0d;
    for (int topic = 0; topic < prob.length; topic++) {
      if (commThetas.get(topic, comm) != 0 && prob[topic] != 0) {
        KLDivergence += commThetas.get(topic, comm) * 
                        Math.log(commThetas.get(topic, comm) / prob[topic]);
      }
    }
    return KLDivergence / Math.log(2);
  }
  
  public static double JSDivergence(final double[] p1, final double[] p2) {
    if (p1.length != p2.length) throw new Error("length mismatch: " + p1.length + " " + p2.length);
    double[] M = new double[p1.length];
    
    for (int i = 0; i < p1.length; ++i) {
        M[i] += (p1[i] + p2[i]) / 2;
    }
    return (KLDivergence(p1, M) + KLDivergence(p2, M)) / 2;
  }
  
  // Kullback-Leiber divergence presents a rating of the similarity of two 
  // probability distributions.
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