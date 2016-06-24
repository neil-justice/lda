import java.io.*;
import java.util.*;

public class ListLoader {
    
  public static void load(String filename, Collection<String> coll) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
      String line;
      
      while ((line = reader.readLine()) != null) {
        coll.add(line.toLowerCase());
      }
    } catch(Exception e) {
      throw new Error("file not found");
    }
  }
}