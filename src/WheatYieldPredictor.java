import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WheatYieldPredictor {

  private static final String HDR_PATH = "test-u8-output.hdr";
  private static final String RAW_PATH = "test-u8-output";

  public static void main(String[] args) {
    try {
      // 1 ── Load header & raw data
      Map<String, String> meta = parseHdr(HDR_PATH);
      int rows = Integer.parseInt(meta.get("rows"));
      int cols = Integer.parseInt(meta.get("cols"));
      int bands = Integer.parseInt(meta.get("bands"));
      int dtype = Integer.parseInt(meta.getOrDefault("data type", "1"));
      if (bands != 1 || dtype != 1) {
        System.out.println("❌ Expecting 1 band and 8‑bit unsigned (data type = 1).");
        return;
      }
      System.out.printf("✅ Image size : %d rows × %d cols\n\n", rows, cols);

      byte[] raw = Files.readAllBytes(Paths.get(RAW_PATH));
      if (raw.length < rows * cols) {
        System.out.println("❌ Raw too small");
        return;
      }

      float[][] img = new float[rows][cols];
      int idx = 0;
      for (int r = 0; r < rows; r++)
        for (int c = 0; c < cols; c++)
          img[r][c] = raw[idx++] & 0xFF;

      Scanner sc = new Scanner(System.in);

      // 2 ── Ask user for pixel length
      System.out.print("Enter pixel length (in meters): ");
      double pixelLength = Double.parseDouble(sc.nextLine());

      // 3 ── Collect calibration samples
      System.out.print("Enter number of samples: ");
      int N = Integer.parseInt(sc.nextLine());
      double[] mu = new double[N];
      double[] yld = new double[N];
      for (int i = 0; i < N; i++) {
        System.out.printf("\nSample %d\n", i + 1);
        System.out.print("  Yield value (in tonnes per acre): ");
        yld[i] = Double.parseDouble(sc.nextLine());
        System.out.print("  Row index  (0‑" + (rows - 1) + "): ");
        int r = Integer.parseInt(sc.nextLine());
        System.out.print("  Col index  (0‑" + (cols - 1) + "): ");
        int c = Integer.parseInt(sc.nextLine());
        if (r < 0 || r >= rows || c < 0 || c >= cols) {
          System.out.println("  ⚠ out of bounds – repeat");
          i--;
          continue;
        }
        mu[i] = img[r][c];
        System.out.printf("  meu (pixel value) : %.0f\n", mu[i]);
      }

      // 4 ── Choose model
      System.out.print("\nChoose model [linear / exponential / log / sigmoid / polynomial] : ");
      String model = sc.nextLine().trim().toLowerCase();
      double[] coeff;

      switch (model) {
        case "exponential" -> coeff = fitExponential(mu, yld);
        case "log" -> coeff = fitLogarithmic(mu, yld);
        case "sigmoid" -> coeff = fitSigmoid(mu, yld);
        case "polynomial" -> coeff = fitPolynomial(mu, yld);
        default -> {
          model = "linear";
          coeff = fitLinear(mu, yld);
        }
      }

      // 5 ── Apply model to image
      double cultivatedSum = 0.0;
      double totalYield = 0.0;
      for (float[] row : img)
        for (float m : row) {
          if (m == 0)
            continue;
          cultivatedSum += m;
          double yPred = switch (model) {
            case "exponential" -> coeff[0] * Math.exp(coeff[1] * m);
            case "log" -> coeff[0] + coeff[1] * Math.log(m);
            case "sigmoid" -> coeff[0] / (1 + Math.exp(-coeff[1] * (m - coeff[2])));
            case "polynomial" -> coeff[0] * m * m + coeff[1] * m + coeff[2];
            default -> coeff[0] + coeff[1] * m;
          };
          totalYield += (yPred / 4046.86) * (pixelLength * pixelLength);
        }

      double areaHa = (cultivatedSum / 255.0) * (pixelLength * pixelLength) / 10000.0;

      // 6 ── Report
      System.out.printf("\n🔷 Selected Model: %s%n", switch (model) {
        case "exponential" -> "Exponential";
        case "log" -> "Logarithmic";
        case "sigmoid" -> "Sigmoid";
        case "polynomial" -> "Polynomial";
        default -> "Linear";
      });
      System.out.printf("🟢 Total Area       : %.2f ha%n", areaHa);
      System.out.printf("🟢 Total Yield      : %.2f tonnes%n", totalYield);
      System.out.printf("🟢 Yield per hectare: %.2f tonnes/ha%n", totalYield / areaHa);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // --- Model fitting methods ---
  private static double[] fitLinear(double[] x, double[] y) {
    double[] b_a = linearRegression(x, y);
    System.out.printf("%nEquation    : Y = %.4f *meu + %.4f%n", b_a[0], b_a[1]);
    return new double[] { b_a[1], b_a[0] };
  }

  private static double[] fitExponential(double[] x, double[] y) {
    double[] lnY = new double[y.length];
    for (int i = 0; i < y.length; i++)
      lnY[i] = Math.log(y[i]);
    double[] b_lnA = linearRegression(x, lnY);
    double a = Math.exp(b_lnA[1]);
    double b = b_lnA[0];
    System.out.printf("%nEquation    : Y = %.4f * exp( %.4f * meu )%n", a, b);
    return new double[] { a, b };
  }

  private static double[] fitLogarithmic(double[] x, double[] y) {
    double[] lnX = new double[x.length];
    for (int i = 0; i < x.length; i++)
      lnX[i] = Math.log(x[i]);
    double[] b_a = linearRegression(lnX, y);
    System.out.printf("%nEquation    : Y = %.4f + %.4f * ln (meu)%n", b_a[1], b_a[0]);
    return new double[] { b_a[1], b_a[0] };
  }

  private static double[] fitSigmoid(double[] x, double[] y) {
    double a = Arrays.stream(y).max().orElse(1.0);
    double c = Arrays.stream(x).average().orElse(0.0);
    double b = 0.1;
    System.out.printf("%nEquation    : Y = %.4f / (1 + exp( -%.4f ( meu - %.4f ) ))  [approx]%n", a, b, c);
    return new double[] { a, b, c };
  }

  private static double[] fitPolynomial(double[] x, double[] y) {
    int n = x.length;
    double Sx = 0, Sx2 = 0, Sx3 = 0, Sx4 = 0, Sy = 0, Sxy = 0, Sx2y = 0;
    for (int i = 0; i < n; i++) {
      double xi = x[i], yi = y[i], xi2 = xi * xi;
      Sx += xi;
      Sx2 += xi2;
      Sx3 += xi2 * xi;
      Sx4 += xi2 * xi2;
      Sy += yi;
      Sxy += xi * yi;
      Sx2y += xi2 * yi;
    }
    double[][] A = { { Sx4, Sx3, Sx2 }, { Sx3, Sx2, Sx }, { Sx2, Sx, n } };
    double[] B = { Sx2y, Sxy, Sy };
    double[] c = gaussianSolve(A, B);
    System.out.printf("%nEquation    : Y = %.4f *meu^2 + %.4f *meu + %.4f%n", c[0], c[1], c[2]);
    return c;
  }

  // --- Math helpers ---
  private static double[] linearRegression(double[] x, double[] y) {
    int n = x.length;
    double Sx = 0, Sy = 0, Sxy = 0, Sx2 = 0;
    for (int i = 0; i < n; i++) {
      Sx += x[i];
      Sy += y[i];
      Sxy += x[i] * y[i];
      Sx2 += x[i] * x[i];
    }
    double b = (n * Sxy - Sx * Sy) / (n * Sx2 - Sx * Sx);
    double a = (Sy - b * Sx) / n;
    return new double[] { b, a };
  }

  private static double[] gaussianSolve(double[][] A, double[] B) {
    int n = B.length;
    for (int p = 0; p < n; p++) {
      int max = p;
      for (int i = p + 1; i < n; i++)
        if (Math.abs(A[i][p]) > Math.abs(A[max][p]))
          max = i;
      double[] tmp = A[p];
      A[p] = A[max];
      A[max] = tmp;
      double t = B[p];
      B[p] = B[max];
      B[max] = t;
      for (int i = p + 1; i < n; i++) {
        double alpha = A[i][p] / A[p][p];
        B[i] -= alpha * B[p];
        for (int j = p; j < n; j++)
          A[i][j] -= alpha * A[p][j];
      }
    }
    double[] x = new double[n];
    for (int i = n - 1; i >= 0; i--) {
      double sum = B[i];
      for (int j = i + 1; j < n; j++)
        sum -= A[i][j] * x[j];
      x[i] = sum / A[i][i];
    }
    return x;
  }

  private static Map<String, String> parseHdr(String path) throws IOException {
    Map<String, String> m = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      for (String line; (line = br.readLine()) != null;) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith(";"))
          continue;
        String[] kv = line.contains("=") ? line.split("=", 2) : line.split(":", 2);
        if (kv.length == 2)
          m.put(kv[0].trim().toLowerCase(), kv[1].trim());
      }
    }
    return m;
  }
}
