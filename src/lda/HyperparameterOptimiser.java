/* implements algorithm 2.2 / eq. 2.58 from Wallach's phd thesis
 * https://people.cs.umass.edu/~wallach/theses/wallach_phd_thesis.pdf */

public class HyperparameterOptimiser {
  private final int topicCount;
  private final int docCount;
  private final int maxLength;       // length of longest doc. maxdN|d in alg. 2.2
  private final int[] docLengthHist; // dLH[l] == no. of docs of length [l]
  private int[][] docTopicCountHist; // dTCH[k][cnt] = no. of docs with
                                     // [cnt] no. of topic [k] tokens
                                     // these are N|d and Nk|d respectively in the paper
                                 
  public HyperparameterOptimiser(int[] tokensInDoc, int topicCount, int maxLength) {
    this.maxLength = maxLength;
    this.docCount = tokensInDoc.length;
    docLengthHist = new int[maxLength];
    for (int doc = 0; doc < docCount; doc++) {
      int length = tokensInDoc[doc];
      docLengthHist[length]++;
    }
    this.topicCount = topicCount;
  }
  
  // alpha is modified in-place.
	public double optimiseAlpha(double[] alpha,int[][] docTopicCountHist, int iterations) {
    this.docTopicCountHist = docTopicCountHist;
		double alphaSum = 0d;
		
    for (int topic = 0; topic < topicCount; topic++) {
			alphaSum += alpha[topic];
		}

    // highestCountForTopic[topic] = the no. of times that topic appears in the
    // doc in which it appears most.
		int[] highestCountForTopic = new int[topicCount];
		Arrays.fill(highestCountForTopic, -1);

		for (topic = 0; topic < topicCount; topic++) {
			for (length = 0; length < maxLength; length++) {
				if (docTopicCountHist[topic][length] > 0) {
					highestCountForTopic[topic] = length;
				}
			}
		}

		for (int it = 0; it < iterations; it++) {
			double S = 0;
			double D = 0;
			for (length  = 1; length < maxLength; length++) {
				D += 1 / (length - 1 + alphaSum); 
				S += docLengthHist[length] * D;
			}
			S--;
      
			alphaSum = 0;
			for (topic = 0; topic < topicCount; topic++) {
				double oldAlpha = alpha[topic];
				alpha[topic] = 0; // alpha[topic] temporarily plays the role of S[k]         
				D = 0;
				for (count = 1; count <= highestCountForTopic[topic]; i++) {
					D += 1 / (count - 1 + oldAlpha);
					alpha[topic] += docTopicCountHist[topic][count] * D;
				}
				alpha[topic] = oldAlpha * (alpha[topic] + 1.001) / S; 
				alphaSum += alpha[topic];
			}
		}

		if (alphaSum < 0) { throw new RuntimeException("alphaSum: " + alphaSum); }

		return alphaSum;
	}  
}