public class LPFilter2 {
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        /**
         * Apply a low pass filter to the inputSignal array
         * 
         */
        double[] outputSignal = new double[inputSignal.length];
        double alpha = cutoffFreq / sampleFreq * 10;
        
        outputSignal[0] = inputSignal[0];

        // Applying the weighted average low-pass filter
        for (int i = 1; i < inputSignal.length; i++) {
            outputSignal[i] = alpha * inputSignal[i] + (1 - alpha) * outputSignal[i - 1];
        }

        return outputSignal;
    }
}
