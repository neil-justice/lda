import java.util.*;
import gnu.trove.map.hash.*;
import gnu.trove.iterator.*;
import gnu.trove.list.array.TLongArrayList;

public class Translator {
  private final SQLConnector c;
  
  public Translator() {
    c = new SQLConnector();
    c.open();
    
    try {
      c.buildDocumentDictionary(documents);
      c.buildWordDictionary(words);
      } finally {
      c.close();
    }
  }
  
  public String getWord(int i) { return words.get(i); }
  
  public Long getDoc(int i) { return documents.get(i); }
  
  public close() { c.close; }
}