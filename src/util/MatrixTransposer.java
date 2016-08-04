public class MatrixTransposer {
  
  // converts a matrix[ymax][xmax] to a matrix[xmax][ymax]
  public static double[][] transpose(double[][] matrix) {
    int ymax = matrix.length;
    int xmax = matrix[0].length;
    double[][] out = new double[xmax][ymax];
    
    for (int y = 0; y < ymax; y++) {
      for (int x = 0; x < xmax; x++) {
        out[x][y] = matrix[y][x];
      }
    }
    return out;
  }
  
  // converts a matrix[ymax][xmax] to a matrix[xmax][ymax]
  public static long[][] transpose(long[][] matrix) {
    int ymax = matrix.length;
    int xmax = matrix[0].length;
    long[][] out = new long[xmax][ymax];
    
    for (int y = 0; y < ymax; y++) {
      for (int x = 0; x < xmax; x++) {
        out[x][y] = matrix[y][x];
      }
    }
    return out;
  }  
  
  // converts a matrix[ymax][xmax] to a matrix[xmax][ymax]
  public static int[][] transpose(int[][] matrix) {
    int ymax = matrix.length;
    int xmax = matrix[0].length;
    int[][] out = new int[xmax][ymax];
    
    for (int y = 0; y < ymax; y++) {
      for (int x = 0; x < xmax; x++) {
        out[x][y] = matrix[y][x];
      }
    }
    return out;
  }    
}