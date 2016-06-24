import java.util.*;
import gnu.trove.map.hash.*;
import gnu.trove.iterator.*;
import gnu.trove.list.array.TLongArrayList;

public class Translator {
  private final SQLConnector c;
  
  public Translator() {
    c = new SQLConnector();
    c.open();

  }
  
  public TIntObjectHashMap<String> getWords(TIntArrayList wordIDs) {
    return null;
  }
  
  public TIntLongHashMap getDocs(TIntArrayList docIDs) {
    return null;
  }
  
  public close() { c.close; }
}