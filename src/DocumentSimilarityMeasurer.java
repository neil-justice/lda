import java.util.*;

public class DocumentSimilarityMeasurer {
  private final CommunityStructure structure;
  private final double[][] theta;
  private final double[][] thetaLog;
  private final int topicCount;
  private final int docCount;
  
  public DocumentSimilarityMeasurer(CommunityStructure structure) {
    this.structure = structure;
    theta = structure.theta();
    topicCount = structure.topicCount();
    docCount = structure.docCount();
    thetaLog = new double[topicCount][docCount];
    calculateLogs();
  }

  private void calculateLogs() {
    for (int doc = 0; doc < docCount; doc++) {
      for (int topic = 0; topic < topicCount; topic++) {
        thetaLog[topic][doc] = Math.log(theta[topic][doc]);
      }
    }
    System.out.println("Logs calculated.");
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
        KLDivergence += theta[topic][doc] * (thetaLog[topic][doc] - Math.log(prob[topic]));
      }
    }
    return KLDivergence / Math.log(2);
  }
}