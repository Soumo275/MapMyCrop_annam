import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CropYieldGUI extends JFrame implements ActionListener {
    private JButton uploadHdrBtn, uploadBsqBtn, addSampleBtn, predictBtn, saveBtn;
    private JTextField pixelSizeField, numSamplesField;
    private JComboBox<String> modelCombo;
    private JPanel samplesPanel;
    private java.util.List<SampleRow> sampleRows;
    private File hdrFile, bsqFile;
    private float[][] imageData;
    private int rows, cols;
    private String lastPredictionResults;

    public CropYieldGUI() {
        initializeGUI();
        sampleRows = new ArrayList<>();
    }

    private void initializeGUI() {
        setTitle("Crop-Acreage-&-Yield-Estimation-Software");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for file uploads
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Upload buttons
        gbc.gridx = 0;
        gbc.gridy = 0;
        uploadHdrBtn = new JButton("Upload .hdr file");
        uploadHdrBtn.addActionListener(this);
        topPanel.add(uploadHdrBtn, gbc);

        gbc.gridy = 1;
        uploadBsqBtn = new JButton("Upload .bsq file");
        uploadBsqBtn.addActionListener(this);
        topPanel.add(uploadBsqBtn, gbc);

        // Pixel size input
        gbc.gridy = 2;
        topPanel.add(new JLabel("Pixel Size (meters):"), gbc);
        gbc.gridx = 1;
        pixelSizeField = new JTextField("8", 10);
        topPanel.add(pixelSizeField, gbc);

        // Number of samples input
        gbc.gridx = 0;
        gbc.gridy = 3;
        topPanel.add(new JLabel("Number of Samples:"), gbc);
        gbc.gridx = 1;
        numSamplesField = new JTextField("2", 10);
        topPanel.add(numSamplesField, gbc);

        // Add sample button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        addSampleBtn = new JButton("Add Sample Inputs");
        addSampleBtn.addActionListener(this);
        topPanel.add(addSampleBtn, gbc);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center panel for samples
        samplesPanel = new JPanel();
        samplesPanel.setLayout(new BoxLayout(samplesPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(samplesPanel);
        scrollPane.setPreferredSize(new Dimension(700, 200));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel for model selection and prediction
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        bottomPanel.add(new JLabel("Model:"), gbc);
        gbc.gridx = 1;
        modelCombo = new JComboBox<>(new String[] { "linear", "exponential", "log", "sigmoid", "polynomial" });
        bottomPanel.add(modelCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        predictBtn = new JButton("Predict Yield");
        predictBtn.addActionListener(this);
        bottomPanel.add(predictBtn, gbc);

        gbc.gridy = 2;
        saveBtn = new JButton("Save Results");
        saveBtn.addActionListener(this);
        saveBtn.setEnabled(false); // Initially disabled until prediction is made
        bottomPanel.add(saveBtn, gbc);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == uploadHdrBtn) {
            uploadHdrFile();
        } else if (e.getSource() == uploadBsqBtn) {
            uploadBsqFile();
        } else if (e.getSource() == addSampleBtn) {
            addSampleInputs();
        } else if (e.getSource() == predictBtn) {
            predictYield();
        } else if (e.getSource() == saveBtn) {
            saveResults();
        }
    }

    private void uploadHdrFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("HDR files", "hdr"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            hdrFile = chooser.getSelectedFile();
            uploadHdrBtn.setText("HDR: " + hdrFile.getName());
            loadImageDimensions();
        }
    }

    private void uploadBsqFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            bsqFile = chooser.getSelectedFile();
            uploadBsqBtn.setText("BSQ: " + bsqFile.getName());
            loadImageData();
        }
    }

    private void loadImageDimensions() {
        if (hdrFile == null)
            return;

        try {
            Map<String, String> meta = parseHdr(hdrFile.getAbsolutePath());
            rows = Integer.parseInt(meta.get("rows"));
            cols = Integer.parseInt(meta.get("cols"));
            int bands = Integer.parseInt(meta.get("bands"));
            int dtype = Integer.parseInt(meta.getOrDefault("data type", "1"));

            if (bands != 1 || dtype != 1) {
                JOptionPane.showMessageDialog(this, "Expecting 1 band and 8-bit unsigned (data type = 1).",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(this, "Image loaded: " + rows + " x " + cols + " pixels");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading HDR file: " + ex.getMessage());
        }
    }

    private void loadImageData() {
        if (bsqFile == null || rows == 0 || cols == 0)
            return;

        try {
            byte[] raw = Files.readAllBytes(bsqFile.toPath());
            if (raw.length < rows * cols) {
                JOptionPane.showMessageDialog(this, "Raw file too small");
                return;
            }

            imageData = new float[rows][cols];
            int idx = 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    imageData[r][c] = raw[idx++] & 0xFF;
                }
            }

            JOptionPane.showMessageDialog(this, "Image data loaded successfully!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading BSQ file: " + ex.getMessage());
        }
    }

    private void addSampleInputs() {
        try {
            int numSamples = Integer.parseInt(numSamplesField.getText());

            // Clear existing samples
            samplesPanel.removeAll();
            sampleRows.clear();

            // Add header
            JPanel headerPanel = new JPanel(new GridLayout(1, 5));
            headerPanel.add(new JLabel("Sample"));
            headerPanel.add(new JLabel("Yield"));
            headerPanel.add(new JLabel("Row"));
            headerPanel.add(new JLabel("Col"));
            headerPanel.add(new JLabel("Pixel Value"));
            samplesPanel.add(headerPanel);

            // Add sample rows
            for (int i = 0; i < numSamples; i++) {
                SampleRow row = new SampleRow(i + 1);
                sampleRows.add(row);
                samplesPanel.add(row.getPanel());
            }

            samplesPanel.revalidate();
            samplesPanel.repaint();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number of samples");
        }
    }

    private void predictYield() {
        if (imageData == null) {
            JOptionPane.showMessageDialog(this, "Please load both HDR and BSQ files first");
            return;
        }

        try {
            double pixelLength = Double.parseDouble(pixelSizeField.getText());
            String model = (String) modelCombo.getSelectedItem();

            // Collect sample data
            double[] mu = new double[sampleRows.size()];
            double[] yld = new double[sampleRows.size()];

            for (int i = 0; i < sampleRows.size(); i++) {
                SampleRow row = sampleRows.get(i);
                yld[i] = row.getYield();
                int r = row.getRow();
                int c = row.getCol();

                if (r < 0 || r >= rows || c < 0 || c >= cols) {
                    JOptionPane.showMessageDialog(this, "Sample " + (i + 1) + " coordinates out of bounds");
                    return;
                }

                mu[i] = imageData[r][c];
                row.setPixelValue(mu[i]);
            }

            // Fit model
            double[] coeff = fitModel(model, mu, yld);

            // Apply model to image
            double cultivatedSum = 0.0;
            double totalYield = 0.0;

            for (float[] rowData : imageData) {
                for (float m : rowData) {
                    if (m == 0)
                        continue;
                    cultivatedSum += m;

                    double yPred = applyModel(model, coeff, m);
                    totalYield += (yPred / 4046.86) * (pixelLength * pixelLength);
                }
            }

            double areaHa = (cultivatedSum / 255.0) * (pixelLength * pixelLength) / 10000.0;

            // Create detailed results string
            StringBuilder detailedResults = new StringBuilder();
            detailedResults.append("=== CROP YIELD PREDICTION RESULTS ===\n\n");
            detailedResults.append("Date: ").append(new java.util.Date().toString()).append("\n\n");
            detailedResults.append("Input Files:\n");
            detailedResults.append("HDR File: ").append(hdrFile != null ? hdrFile.getName() : "N/A").append("\n");
            detailedResults.append("BSQ File: ").append(bsqFile != null ? bsqFile.getName() : "N/A").append("\n\n");
            detailedResults.append("Parameters:\n");
            detailedResults.append("Pixel Size: ").append(pixelLength).append(" meters\n");
            detailedResults.append("Image Dimensions: ").append(rows).append(" x ").append(cols).append(" pixels\n");
            detailedResults.append("Selected Model: ").append(model).append("\n\n");
            detailedResults.append("Sample Data:\n");
            for (int i = 0; i < sampleRows.size(); i++) {
                SampleRow row = sampleRows.get(i);
                detailedResults.append("Sample ").append(i + 1).append(": ");
                detailedResults.append("Yield=").append(row.getYield());
                detailedResults.append(", Row=").append(row.getRow());
                detailedResults.append(", Col=").append(row.getCol());
                detailedResults.append(", Pixel Value=").append((int) mu[i]).append("\n");
            }
            detailedResults.append("\nModel Coefficients:\n");
            for (int i = 0; i < coeff.length; i++) {
                detailedResults.append("Coefficient ").append(i + 1).append(": ")
                        .append(String.format("%.6f", coeff[i])).append("\n");
            }
            detailedResults.append("\nResults:\n");
            detailedResults.append("Total Area: ").append(String.format("%.8f", areaHa)).append(" ha\n");
            detailedResults.append("Total Yield: ").append(String.format("%.8f", totalYield)).append(" tonnes\n");
            detailedResults.append("Yield per hectare: ").append(String.format("%.8f", totalYield / areaHa))
                    .append(" tonnes/ha\n");

            // Store results for saving
            lastPredictionResults = detailedResults.toString();
            saveBtn.setEnabled(true);

            // Display results
            String results = String.format(
                    "Selected Model: %s\n" +
                            "Total Area: %.8f ha\n" +
                            "Total Yield: %.8f tonnes\n" +
                            "Yield per hectare: %.8f tonnes/ha",
                    model, areaHa, totalYield, totalYield / areaHa);

            JOptionPane.showMessageDialog(this, results, "Prediction Results", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error during prediction: " + ex.getMessage());
        }
    }

    private void saveResults() {
        if (lastPredictionResults == null) {
            JOptionPane.showMessageDialog(this, "No prediction results to save. Please run prediction first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files", "txt"));
        chooser.setSelectedFile(new File("crop_yield_results.txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                // Ensure .txt extension
                if (!file.getName().toLowerCase().endsWith(".txt")) {
                    file = new File(file.getAbsolutePath() + ".txt");
                }

                Files.write(file.toPath(), lastPredictionResults.getBytes(), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE);
                JOptionPane.showMessageDialog(this, "Results saved successfully to: " + file.getAbsolutePath(),
                        "Save Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving results: " + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private double[] fitModel(String model, double[] x, double[] y) {
        switch (model) {
            case "exponential":
                return fitExponential(x, y);
            case "log":
                return fitLogarithmic(x, y);
            case "sigmoid":
                return fitSigmoid(x, y);
            case "polynomial":
                return fitPolynomial(x, y);
            default:
                return fitLinear(x, y);
        }
    }

    private double applyModel(String model, double[] coeff, double m) {
        switch (model) {
            case "exponential":
                return coeff[0] * Math.exp(coeff[1] * m);
            case "log":
                return coeff[0] + coeff[1] * Math.log(m);
            case "sigmoid":
                return coeff[0] / (1 + Math.exp(-coeff[1] * (m - coeff[2])));
            case "polynomial":
                return coeff[0] * m * m + coeff[1] * m + coeff[2];
            default:
                return coeff[0] + coeff[1] * m;
        }
    }

    // Model fitting methods (copied from WheatYieldPredictor)
    private double[] fitLinear(double[] x, double[] y) {
        double[] b_a = linearRegression(x, y);
        return new double[] { b_a[1], b_a[0] };
    }

    private double[] fitExponential(double[] x, double[] y) {
        double[] lnY = new double[y.length];
        for (int i = 0; i < y.length; i++)
            lnY[i] = Math.log(y[i]);
        double[] b_lnA = linearRegression(x, lnY);
        double a = Math.exp(b_lnA[1]);
        double b = b_lnA[0];
        return new double[] { a, b };
    }

    private double[] fitLogarithmic(double[] x, double[] y) {
        double[] lnX = new double[x.length];
        for (int i = 0; i < x.length; i++)
            lnX[i] = Math.log(x[i]);
        double[] b_a = linearRegression(lnX, y);
        return new double[] { b_a[1], b_a[0] };
    }

    private double[] fitSigmoid(double[] x, double[] y) {
        double a = Arrays.stream(y).max().orElse(1.0);
        double c = Arrays.stream(x).average().orElse(0.0);
        double b = 0.1;
        return new double[] { a, b, c };
    }

    private double[] fitPolynomial(double[] x, double[] y) {
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
        return gaussianSolve(A, B);
    }

    private double[] linearRegression(double[] x, double[] y) {
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

    private double[] gaussianSolve(double[][] A, double[] B) {
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

    private Map<String, String> parseHdr(String path) throws IOException {
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

    // Inner class for sample row
    private class SampleRow {
        private JPanel panel;
        private JTextField yieldField, rowField, colField, pixelValueField;

        public SampleRow(int sampleNumber) {
            panel = new JPanel(new GridLayout(1, 5));
            panel.add(new JLabel("Sample " + sampleNumber + " Yield:"));

            yieldField = new JTextField(sampleNumber == 1 ? "3" : "4");
            panel.add(yieldField);

            rowField = new JTextField(sampleNumber == 1 ? "333" : "443");
            panel.add(rowField);

            colField = new JTextField(sampleNumber == 1 ? "333" : "444");
            panel.add(colField);

            pixelValueField = new JTextField();
            pixelValueField.setEditable(false);
            panel.add(pixelValueField);
        }

        public JPanel getPanel() {
            return panel;
        }

        public double getYield() {
            return Double.parseDouble(yieldField.getText());
        }

        public int getRow() {
            return Integer.parseInt(rowField.getText());
        }

        public int getCol() {
            return Integer.parseInt(colField.getText());
        }

        public void setPixelValue(double value) {
            pixelValueField.setText(String.valueOf((int) value));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CropYieldGUI app = new CropYieldGUI();
            app.setVisible(true);
        });
    }
}
