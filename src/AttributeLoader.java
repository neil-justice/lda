/* loads files containing node attribute info such as follower count. */
import java.io.*;
import java.util.*;

public class AttributeLoader {
  
  private final Translator translator;
  private final String dir;
  private final int order;
  
  public AttributeLoader(SQLConnector c, String dir, int order) {
    translator = new Translator(c);
    this.dir = dir;
    this.order = order;
  }
  
  private int[] read(String filename) {
    try {
      File file = new File(filename);
      if (!file.exists()) return new int[order];
      else return readAll(file, ",");
    } catch(NumberFormatException e) {
      throw new Error("invalid file format");
    } catch (FileNotFoundException e) {
      throw new Error("file not found");
    } catch (IOException e) {
      throw new Error("IO error");
    }
  }

  private int[] readAll(File file, String delimiter) 
  throws NumberFormatException, FileNotFoundException, IOException {
    
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    int[] attributes = new int[order];
    
    while ((line = reader.readLine()) != null) {
      String[] splitLine = line.split(delimiter);
      long raw = Long.parseLong(splitLine[0]);
      int attr = Integer.parseInt(splitLine[1]);
      int node = translator.getDocIndex(raw);
      attributes[node] = attr;
    }
    reader.close();
    return attributes;
  }
  
  public int[] friends() {
    return read(dir + CTUT.ATTR_FRIENDS);
  }
  
  public int[] followers() {
    return read(dir + CTUT.ATTR_FOLLOWERS);
  }
  
  public int[] wordCount() {
    return read(dir + CTUT.ATTR_WORDCOUNT);
  }
}