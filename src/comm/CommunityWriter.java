import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.IOException;
import java.util.*;
import java.time.Instant;

public class CommunityWriter {
  private final CommunityStructure structure;
  private final String dir;
  private final int topicCount;
  private final int docCount;
  private final int layers;
  private final long ID;
  private final String del = ",";
  private final String ext = ".csv";
  
  public CommunityWriter(CommunityStructure structure, String dir) {
    this.structure = structure;
    this.dir = dir;
    this.topicCount = structure.topicCount();
    this.docCount = structure.docCount();
    this.layers = structure.layers();
    this.ID = Instant.now().getEpochSecond();
  }
  
  public void write() {
    for (int layer = 0; layer < layers; layer++) {
      write(layer);
    }
  }
  
  public void write(int layer) {
    if (layer >= layers) throw new Error("layer doesn't exist");
    
    Path filepath = Paths.get(dir + "L" + layer + "-cinfo-" + ID + ext);
    List<String> data = prepareData(layer);
    
    try {
      Files.write(filepath, data, Charset.forName("UTF-8"));
    }
    catch(IOException e) {
      e.printStackTrace();
    }
  }
  
  private List<String> prepareData(int layer) {
    List<String> data = new ArrayList<String>();
    data.add(prepareHeader());
    
    for (int comm = 0; comm < docCount; comm++) {
      if (structure.commSize(layer, comm) > 0) {
        String line = prepareLine(layer, comm);
        data.add(line);
      }
    }
    
    return data;
  }
  
  private String prepareHeader() {
    StringBuilder builder = new StringBuilder();
    builder.append("ID");
    builder.append(del);
    builder.append("size");
    builder.append(del);
    builder.append("score");
    builder.append(del);
    builder.append("JSD");
    builder.append(del);
    builder.append("JSi");
    builder.append(del);
    builder.append("E");    
    builder.append(del);
    builder.append("avfoll");    
    builder.append(del);
    builder.append("avfriends");
    builder.append(del);
    builder.append("avwords");
    for (int topic = 0; topic < topicCount; topic++) {
      builder.append(del);
      builder.append(topic);
    }
    return builder.toString();
  }
  
  private String prepareLine(int layer, int comm) {
    StringBuilder builder = new StringBuilder();
    
    builder.append(comm);
    builder.append(del);
    builder.append(structure.commSize(layer, comm));
    builder.append(del);
    builder.append(structure.commScore(layer, comm));
    builder.append(del);
    builder.append(structure.JSDiv(layer, comm));
    builder.append(del);
    builder.append(structure.JSImp(layer, comm));
    builder.append(del);
    builder.append(structure.entropy(layer, comm));
    builder.append(del);
    builder.append(structure.followers(layer, comm));
    builder.append(del);
    builder.append(structure.friends(layer, comm));
    builder.append(del);
    builder.append(structure.wordCount(layer, comm));
    
    for (int topic = 0; topic < topicCount; topic++) {
      builder.append(del);
      builder.append(structure.commTheta(layer, topic, comm));
    }
    
    return builder.toString();
  }
  
}