/* class for static corpus utilities */
import java.util.*;

public class LDAUtils {
  private static final int TOP = 20;
  
  // finds the geometric mean of a matrix
  public static double[] geometricMean(double[][] matrix) {
    int height = matrix.length;
    int width = matrix[0].length;
    
    double[] geometricMean = new double[height];
    
    for (int i = 0; i < height; i++) {
      double sumlog = 0d;
      for (int j = 0; j < width; j++) {
        sumlog += Math.log(matrix[i][j]);
      }
      geometricMean[i] = Math.exp(sumlog/width);
    }
    
    return geometricMean;
  }
  
  // as laid out in Blei and Lafferty, 2009.  sorts words in topics by
  // phi * log (phi / geometric mean prob in all topics)
  // and defines topics by their top 10 words.
  public static void termScore(double[][] phi, Translator translator) {
    int wordCount = phi.length;
    int topicCount = phi[0].length;
    double[] geometricMean = geometricMean(phi);
    
    Integer[][] output = new Integer[topicCount][TOP];
    double[][] temp = new double[topicCount][wordCount]; //note the inverse dimensions
    
    for (int topic = 0; topic < topicCount; topic++) {
      for (int word = 0; word < wordCount; word++) {
        temp[topic][word] = phi[word][topic] 
                          * Math.log(phi[word][topic] 
                          / geometricMean[word]);
      }
      IndexComparator comp = new IndexComparator(temp[topic]);
      Integer[] indexes = comp.indexArray();
      Arrays.sort(indexes, comp.reversed());
      output[topic] = Arrays.copyOf(indexes, TOP);
    }

    System.out.println("");
    for (int topic = 0; topic < topicCount; topic++) {
      System.out.printf(" %d : ", topic);
      for (int i = 0; i < TOP; i++) {
        System.out.print(translator.getWord(output[topic][i]) + " ");
      }
      System.out.println("");
    }
  }
  
  public static void mostCommon(double[][] phi, Translator translator) {
    int wordCount = phi.length;
    int topicCount = phi[0].length;
    
    Integer[][] output = new Integer[topicCount][TOP];
    double[][] temp = new double[topicCount][wordCount]; //note the inverse dimensions
    
    for (int topic = 0; topic < topicCount; topic++) {
      for (int word = 0; word < wordCount; word++) {
        temp[topic][word] = phi[word][topic];;
      }
      IndexComparator comp = new IndexComparator(temp[topic]);
      Integer[] indexes = comp.indexArray();
      Arrays.sort(indexes, comp.reversed());
      output[topic] = Arrays.copyOf(indexes, TOP);
    }    
    
    System.out.println("");
    for (int topic = 0; topic < topicCount; topic++) {
      System.out.printf(" %d : ", topic);
      for (int i = 0; i < TOP; i++) {
        System.out.print(translator.getWord(output[topic][i]) + " ");
      }
      System.out.println("");
    }
  }  
}