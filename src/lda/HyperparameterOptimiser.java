/* implements algorithm 2.2 / eq. 2.58 from Wallach's phd thesis
 * https://people.cs.umass.edu/~wallach/theses/wallach_phd_thesis.pdf */
import java.util.*;

public class HyperparameterOptimiser {
  private final int topicCount;
  private final int docCount;
  private final int maxLength;       // length of longest doc. maxdN|d in alg. 2.2
  private final int[] docLengthHist; // dLH[l] == no. of docs of length [l]
  private int[][] docTopicCountHist; // dTCH[k][cnt] = no. of docs with
                                     // [cnt] no. of topic [k] tokens
                                     // these are C(n) and Ck(n) respectively in the paper
                                     // where n is length or count
  private final double s = 1d;       // gamma hyperprior
  private final double c = 1.001;    // gamma hyperprior - see see sect. 2.5 in paper
                                 
  public HyperparameterOptimiser(int[] tokensInDoc, int topicCount, int maxLength) {
    this.docCount = tokensInDoc.length;
    this.topicCount = topicCount;
    this.maxLength = maxLength;
    docLengthHist = new int[maxLength];
    
    for (int doc = 0; doc < docCount; doc++) {
      int length = tokensInDoc[doc];
      docLengthHist[length]++;
    }
  }
  
	public double optimiseAlpha(double[] alpha, int[][] docTopicCountHist) {
    return optimiseAlpha(alpha, docTopicCountHist, 10);
  }
  
  // alpha is modified in-place.
	public double optimiseAlpha(double[] alpha, int[][] docTopicCountHist, 
                              int iterations) {
    this.docTopicCountHist = docTopicCountHist;
		double alphaSum = 0d;
		
    for (int topic = 0; topic < topicCount; topic++) {
			alphaSum += alpha[topic];
		}

		int[] highestCountForTopic = setHighestCountForTopic();

		for (int it = 0; it < iterations; it++) {
			double S = 0;
			double D = 0;
			for (int length  = 1; length < maxLength; length++) {
				D += 1 / (length - 1 + alphaSum); 
				S += docLengthHist[length] * D;
			}
			S -= 1 / s; // s is a component of the hyperprior
      
			alphaSum = 0;
      double Sk;
			for (int topic = 0; topic < topicCount; topic++) {
        Sk = 0d;
				D = 0;
				for (int count = 1; count <= highestCountForTopic[topic]; count++) {
					D += 1 / (count - 1 + alpha[topic]);
					Sk += docTopicCountHist[topic][count] * D;
				}
				alpha[topic] *= (Sk + c) / S; // c is a component of the hyperprior
				alphaSum += alpha[topic];
			}
		}

		if (alphaSum < 0) { throw new RuntimeException("alphaSum: " + alphaSum); }

		return alphaSum;
	}
  
  // highestCountForTopic[topic] = the no. of times that topic appears in the
  // doc in which it appears most.  
  private int[] setHighestCountForTopic() {
		int[] highestCountForTopic = new int[topicCount];
		Arrays.fill(highestCountForTopic, -1);

		for (int topic = 0; topic < topicCount; topic++) {
			for (int length = 0; length < maxLength; length++) {
				if (docTopicCountHist[topic][length] > 0) {
					highestCountForTopic[topic] = length;
				}
			}
		}
    return highestCountForTopic;
  }
}