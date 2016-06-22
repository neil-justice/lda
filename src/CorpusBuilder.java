import java.util.*;
import java.io.*;
import gnu.trove.map.hash.*;

public class CorpusBuilder {
  
  private final List<Long> documents = new ArrayList<Long>();
  private final TObjectIntHashMap<String> words = new TObjectIntHashMap<>();
  private final List<Token> tokens = new ArrayList<Token>();
  
  public CorpusBuilder fromFile(String dir){
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(dir + LDA.processedFile)));
      String line;
      int i = 0;
      
      while ((line = reader.readLine()) != null) {
        String[] splitLine = line.split("\t");
        if (splitLine.length != 4) {
          throw new Error("length " + splitLine.length + " at " + splitLine[0]);
        }
        processLine(Long.parseLong(splitLine[0]), splitLine[1].split(" "));
        i++;
        if (i % 100 == 0) System.out.println(i + " lines read");
      }
    } catch(NumberFormatException e) {
      throw new Error("invalid doc ID");
    } catch (FileNotFoundException e) {
      throw new Error("file " + LDA.processedFile + " not found at " + dir);
    } catch (IOException e) {
      throw new Error("IO error");
    }
    
    Collections.shuffle(tokens);
    return this;
  }
  
  private void processLine(long id, String[] tokens) {
    documents.add(id);
    int doc = documents.size() - 1;
    
    for (String word: tokens) {
      processWord(word, doc);
    }
  }
  
  private void processWord(String word, int doc) {
    int in = 0;
    
    if (!words.containsKey(word)) {
      in = words.size();
      words.put(word, in);
    }
    else in = words.get(word);
    tokens.add(new Token(in, doc));
  }
  
  public List<Token> tokens() { return tokens; }
  public TObjectIntHashMap<String> words() { return words; }
  public List<Long> documents() { return documents; }
  public int wordCount() { return words.size(); }
  public int docCount() { return documents.size(); }
  public int tokenCount() { return tokens.size(); }
  public Corpus build() { return new Corpus(this); }  
}