import java.util.*;
import gnu.trove.map.hash.*;

public class Translator {
  
  private final List<Long> documents;
  private final List<String> words = new ArrayList<String>();

  public Translator(TObjectIntHashMap<String> wordmap, List<Long> documents) {
    System.out.println("building translator...");
    // for (int i = 0; i < wordmap.size(); i++) {
    //   words.add(wordmap.get(i));
    // }
    this.documents = documents;
    System.out.println("translator complete");
  }
  
  public String getWord(int i) { return words.get(i); }
  
  public Long getDoc(int i) { return documents.get(i); }
}