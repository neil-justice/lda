import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.IOException;
import java.util.*;
import java.time.Instant;

public class CommThetaWriter {
  private final String dir;
  private final SparseDoubleMatrix commTheta;
  private final long ID;
  private final String del = ",";
  private final String ext = ".csv";
  
  public CommThetaWriter(SparseDoubleMatrix commTheta, String dir) {
    this.commTheta = commTheta;
    this.dir = dir;
    ID = Instant.now().getEpochSecond();
  }
  
  public void write() {
    
    Path filepath = Paths.get(dir + "commTheta-" + ID + ext);
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
    
    commTheta.compress();
    for ( SparseDoubleMatrix.Iterator it = commTheta.iterator(); it.hasNext(); ) {
      StringBuilder builder = new StringBuilder();
      it.advance();
      int topic = it.x();
      builder.append(topic);
      builder.append(del);
      int comm = it.y();
      builder.append(comm);
      builder.append(del);
      double val = it.value();
      builder.append(val);
      data.add(builder.toString());
    }
    
    return data;
  }
  
  private String prepareHeader() {
    StringBuilder builder = new StringBuilder();
    builder.append("topic");
    builder.append(del);
    builder.append("comm");
    builder.append(del);
    builder.append("theta");
    return builder.toString();
  }
}