public class ArrayUtils {
  
  public static int lastIndexOf(double[] a, double n) {
    if (a == null) return -1;
    int lastIndex = -1;
    for (int i = 0; i < a.length; i++) {
      if (a[i] == n) lastIndex = i;
    }
    return lastIndex;
  }
}