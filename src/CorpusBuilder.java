import java.util.*;
import java.io.*;

public class CorpusBuilder {
  
  private final List<Long> documents = new ArrayList<Long>();
  private final List<String> words = new ArrayList<String>();
  private final List<Token> tokens = new ArrayList<Token>();
  
  public CorpusBuilder fromFile(String dir){
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(dir + LDA.processedFile)));
      String line;
      
      while ((line = reader.readLine()) != null) {
        String[] splitLine = line.split("\t");
        if (splitLine.length != 4) {
          throw new Error("length " + splitLine.length + " at " + splitLine[0]);
        }
        processLine(Long.parseLong(splitLine[0]), splitLine[1].split(" "));
        
      }
    } catch(NumberFormatException e) {
      throw new Error("invalid word freq file format");
    } catch (FileNotFoundException e) {
      throw new Error("clean file not found at " + dir);
    } catch (IOException e) {
      throw new Error("IO error");
    }
    
    Collections.shuffle(tokens);
    return this;
  }
  
  private void processLine(long id, String[] tokens) {
    documents.add(id);
    int doc = documents.indexOf(id);
    if (doc != documents.size()) throw new Error("unexpected size");
    
    for (String word: tokens) {
      processWord(word, doc);
    }
  }
  
  private void processWord(String word, int doc) {
    int i = words.indexOf(word);
    if (i == -1) words.add(word);
    tokens.add(new Token(i, doc));
  }
  
  public List<Token> tokens() { return tokens; }
  public List<String> words() { return words; }
  public List<Long> documents() { return documents; }
  public int wordCount() { return words.size(); }
  public int docCount() { return documents.size(); }
  public int tokenCount() { return tokens.size(); }
  
  public Corpus build() {
    return new corpus(this);
  }  
}