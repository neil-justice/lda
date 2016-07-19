import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TableManager {
  private final Connection c;
  private final String dropInfo  = "DROP TABLE IF EXISTS Info;";
  private final String dropPhi   = "DROP TABLE IF EXISTS Phi;";
  private final String dropTheta = "DROP TABLE IF EXISTS Theta;";
  private final String dropToken = "DROP TABLE IF EXISTS Token;";
  private final String dropDoc   = "DROP TABLE IF EXISTS Doc;";
  private final String dropWord  = "DROP TABLE IF EXISTS Word;";
  
  private final String word  = "CREATE TABLE Word (" + 
                               "id INTEGER PRIMARY KEY, " +
                               "word VARCHAR(20) NOT NULL);";
  private final String doc   = "CREATE TABLE Doc (" +
                               "id INTEGER PRIMARY KEY, " +
                               "doc INTEGER NOT NULL);";
  private final String token = "CREATE TABLE Token (" +
                               "id INTEGER PRIMARY KEY, " +
                               "word INTEGER NOT NULL REFERENCES Word(id), " +
                               "doc INTEGER NOT NULL REFERENCES Doc(id));";
  private final String theta = "CREATE TABLE Theta ( " +
                               "id INTEGER PRIMARY KEY, " +
                               "topic INTEGER NOT NULL, " +
                               "doc INTEGER NOT NULL, " +
                               "val REAL NOT NULL); ";
  private final String phi   = "CREATE TABLE Phi ( " +
                               "id INTEGER PRIMARY KEY, " +
                               "word INTEGER NOT NULL, " +
                               "topic INTEGER NOT NULL, " +
                               "val REAL NOT NULL); ";
  private final String info  = "CREATE TABLE Info (" +
                               "id INTEGER PRIMARY KEY, " +
                               "topics INTEGER NOT NULL, " +
                               "cycles INTEGER NOT NULL);";
  private final String iinit = "INSERT INTO Info VALUES( 0, 0, 0)";
  
  public TableManager(Connection c) { this.c = c; }
  
  public void createTables() {
    if (c == null) { throw new IllegalStateException(); }

    try (Statement s = c.createStatement()) {
      s.addBatch(word);
      s.addBatch(doc);
      s.addBatch(token);
      s.addBatch(theta);
      s.addBatch(phi);
      s.addBatch(info);
      s.addBatch(iinit);
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void dropTables() {
    if (c == null) { throw new IllegalStateException(); }

    try (Statement s = c.createStatement()) {
      s.addBatch(dropInfo);
      s.addBatch(dropPhi);
      s.addBatch(dropTheta);
      s.addBatch(dropToken);
      s.addBatch(dropDoc);
      s.addBatch(dropWord);
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }    
  }
  
  public void dropTokenTable() { dropTable(dropToken); }
  public void dropThetaTable() { dropTable(dropTheta); }
  public void dropPhiTable() { dropTable(dropPhi); }
  
  public void createTokenTable() { createTable(token); }
  public void createThetaTable() { createTable(theta); }
  public void createPhiTable() { createTable(phi); }
    
  private void dropTable(String cmd) { run(cmd); }
  
  private void createTable(String cmd) { run(cmd); }
  
  private void run(String cmd) {
    if (c == null) { throw new IllegalStateException(); }

    try (PreparedStatement s = c.prepareStatement(cmd)) {
      s.executeUpdate();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
}