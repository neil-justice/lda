import java.io.*;
import java.util.*;
import tester.Tester;

class Preprocessor {
  
  private final WordFrequencyList wordfreqs = new WordFrequencyList();
  private final Set<String> toRemove = new HashSet<String>();
  private final Set<String> doNotRemove = new HashSet<String>();
  private final int minFreq = 64;
  private final int maxFreq = 1000000;
  
  public void process(String dir) {
    wordfreqs.load(dir);
    buildRemovalList();
    System.out.println(toRemove.size() + " / " + wordfreqs.size());
    wordfreqs.removeAll(toRemove);
    wordfreqs.write(dir);
    removeWords(dir);
  }
  
  private void buildRemovalList() {
    
    loadList(LDA.stopwords, toRemove);
    loadList(LDA.gowords, doNotRemove);
    // loadList(LDA.gowords, toRemove);
    
    for (Map.Entry<String, Integer> e: wordfreqs.entrySet()) {
      String word = e.getKey();
      int freq = e.getValue();
      if (freq < minFreq) toRemove.add(word);
      if (freq > maxFreq) toRemove.add(word);
      if (word.length() <= 2) toRemove.add(word);
      // if (word.startsWith("@") && freq < 2000) toRemove.add(word);
    }
    
    toRemove.removeAll(doNotRemove);
  }
  
  private void loadList(String filename, Collection<String> coll) {
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
  
  private void removeWords(String dir){
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(dir + LDA.cleanedFile)));
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir + LDA.processedFile)));
      String line;
      int cnt = 0;
      
      while ((line = reader.readLine()) != null) {
        cnt++;
        String[] splitLine = line.split("\t");
        String tags = "";
        if (splitLine.length != 4) {
          throw new Error("length " + splitLine.length + " at " + splitLine[0]);
        }
        String processed = processLine(splitLine[1]);
        writer.write(splitLine[0] + "\t" + processed + "\t" + splitLine[2] + "\t" + splitLine[3]);
        writer.newLine();
        if (cnt % 100 == 0) writer.flush();
      }
      writer.flush();
      
    } catch (FileNotFoundException e) {
      throw new Error("clean file not found at " + dir);
    } catch (IOException e) {
      throw new Error("IO error");
    }
  }
  
  private String processLine(String text) {
    String[] splitText = text.split(" ");
    List<String> out = new ArrayList<String>();
    
    for (String s: splitText) {
      if (wordfreqs.contains(s)) out.add(s);
    }
    
    return String.join(" ", out);
  }
}