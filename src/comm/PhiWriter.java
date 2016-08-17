import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.IOException;
import java.util.*;
import java.time.Instant;

public class PhiWriter {
  private final String dir;
  private final double[][] phi;
  private final int topicCount;
  private final int wordCount;
  private final long ID;
  private final Translator translator;
  private final String del = ",";
  private final String ext = ".csv";
  
  public PhiWriter(double[][] phi, Translator translator, String dir) {
    this.phi = phi;
    this.dir = dir;
    this.translator = translator;
    wordCount = phi.length;
    topicCount = phi[0].length;
    ID = Instant.now().getEpochSecond();
  }
  
  public void write() {
    
    Path filepath = Paths.get(dir + "phi-" + ID + ext);
    List<String> data = prepareData();
    
    try {
      Files.write(filepath, data, Charset.forName("UTF-8"));
    }
    catch(IOException e) {
      e.printStackTrace();
    }
  }
  
  private List<String> prepareData() {
    List<String> data = new ArrayList<String>();
    data.add(prepareHeader());
    
    for (int word = 0; word < wordCount; word++) {
      data.add(prepareLine(word));
    }
    
    return data;
  }
  
  private String prepareHeader() {
    StringBuilder builder = new StringBuilder();
    builder.append("word");
    for (int topic = 0; topic < topicCount; topic++) {
      builder.append(del);
      builder.append("phi (" + topic + ")");
    }
    return builder.toString();
  }
  
  private String prepareLine(int word) {
    StringBuilder builder = new StringBuilder();
    String wordString = translator.getWord(word);
    
    builder.append(wordString);
    
    for (int topic = 0; topic < topicCount; topic++) {
      builder.append(del);
      builder.append(phi[word][topic]);
    }
    
    return builder.toString();
  }
  
}