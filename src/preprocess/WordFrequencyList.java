import java.util.*;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.*;

class WordFrequencyList {
  
  private final Map<String, Integer> wordfreqs = new HashMap<String, Integer>();
  
  public void load(String dir) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(dir + LDA.FREQFILE)));
      String line;
      
      while ((line = reader.readLine()) != null) {
        String[] splitLine = line.split(" ");
        if (splitLine.length != 2) {
          throw new Error("length " + splitLine.length + " at " + splitLine[0]);
        }
        wordfreqs.put(splitLine[0], Integer.parseInt(splitLine[1]));
      }
    } catch(NumberFormatException e) {
      throw new Error("invalid word freq file format");
    } catch (FileNotFoundException e) {
      throw new Error("word freq file not found at " + dir);
    } catch (IOException e) {
      throw new Error("IO error");
    }
  }
  
  public void write(String dir) {
    write(dir, LDA.FREQFILE);
  }
  
  public void write(String dir, String filename) {
    List<String> data = new ArrayList<String>();
    Path filepath = Paths.get(dir + filename);
    
    for (Map.Entry<String, Integer> e: wordfreqs.entrySet()) {
      data.add(e.getKey() + " " + e.getValue());
    }
    
    try {
      Files.write(filepath, data, Charset.forName("UTF-8"));
    }
    catch(IOException e) {
      e.printStackTrace();
    }    
  }
  
  public void add(String s) { wordfreqs.merge(s, 1, Integer::sum); }
  
  public int size() { return wordfreqs.size(); }
  
  public boolean contains(String s) { return wordfreqs.containsKey(s); }
  
  public Set<Map.Entry<String, Integer>> entrySet() {
    return wordfreqs.entrySet();
  }
  
  public void removeAll(Collection<String> coll) {
    wordfreqs.keySet().removeAll(coll);
  }
  
}