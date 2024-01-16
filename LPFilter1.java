public class LPFilter1 {
    /**
     * Apply a low pass filter to the inputSignal array
     * 
     */
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        double[] outputSignal = new double[inputSignal.length];

        // Applying the moving average low-pass filter
        for (int i = 0; i < inputSignal.length; i++) {
            double sum = 0;
            for (int j = Math.max(0, i - (int) cutoffFreq + 1); j <= i; j++) {
                sum += inputSignal[j];
            }
            outputSignal[i] = sum / Math.min(cutoffFreq, i + 1);
        }
    
        return outputSignal;
    }
}
