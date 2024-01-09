import java.io.*;
import java.util.Arrays;

/**
 * Béliard Valentin
 * Bernard Elena
 * C1
 */

public class DosRead {
    static final int FP = 1000;
    static final int BAUDS = 100;
    static final int[] START_SEQ = { 1, 0, 1, 0, 1, 0, 1, 0 };
    FileInputStream fileInputStream;
    int sampleRate = 44100;
    int bitsPerSample;
    int dataSize;
    double[] audio;
    int[] outputBits;
    char[] decodedChars;

    /**
     * Constructor that opens the FIlEInputStream
     * and reads sampleRate, bitsPerSample and dataSize
     * from the header of the wav file
     * 
     * @param path the path of the wav file to read
     */
    public void readWavHeader(String path) {
        byte[] header = new byte[44]; // The header is 44 bytes long
        try {
            fileInputStream = new FileInputStream(path);
            fileInputStream.read(header);

            // Extract information from the header
            sampleRate = byteArrayToInt(header, 24, 32);
            bitsPerSample = byteArrayToInt(header, 34, 16);
            dataSize = byteArrayToInt(header, 40, 32);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to convert a little-endian byte array to an integer
     * 
     * @param bytes  the byte array to convert
     * @param offset the offset in the byte array
     * @param fmt    the format of the integer (16 or 32 bits)
     * @return the integer value
     */
    private static int byteArrayToInt(byte[] bytes, int offset, int fmt) {
        if (fmt == 16)
            return ((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);
        else if (fmt == 32)
            return ((bytes[offset + 3] & 0xFF) << 24) |
                    ((bytes[offset + 2] & 0xFF) << 16) |
                    ((bytes[offset + 1] & 0xFF) << 8) |
                    (bytes[offset] & 0xFF);
        else
            return (bytes[offset] & 0xFF);
    }

    /**
     * Read the audio data from the wav file
     * and convert it to an array of doubles
     * that becomes the audio attribute
     */
    public void readAudioDouble() {
        byte[] audioData = new byte[dataSize];
        try {
            fileInputStream.read(audioData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int bytesPerSample = bitsPerSample / 8;
        audio = new double[dataSize / bytesPerSample];

        for (int i = 0, j = 0; i < dataSize; i += bytesPerSample, j++) {
            if (bytesPerSample == 2) {
                audio[j] = byteArrayToInt(audioData, i, 16);
            } else if (bytesPerSample == 4) {
                int intBits = byteArrayToInt(audioData, i, 32);
                audio[j] = Float.intBitsToFloat(intBits);
            }
        }
    }

    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier() {
        // Moving through the audio array
        for (int i = 0; i < audio.length; i++) {
            // Taking the absolute value of each audio value
            audio[i] = Math.abs(audio[i]);
        }
    }

    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n)*FECH
     * 
     * @param n the number of samples to average
     */
    public void audioLPFilter(int n) {
        // List used to store the filtered audio
        double[] filteredAudio = new double[audio.length];

        for (int i = 0; i < audio.length; i++) {
            // Using an average to filter the audio
            double sum = 0;
            for (int j = Math.max(0, i - n + 1); j <= i; j++) {
                sum += audio[j];
            }
            filteredAudio[i] = sum / Math.min(n, i + 1);
        }
        audio = filteredAudio;
    }

    /**
     * Resample the audio array and apply a threshold
     * 
     * @param period    the number of audio samples by symbol
     * @param threshold the threshold that separates 0 and 1
     */
    public void audioResampleAndThreshold(int period, int threshold) {
        int n = audio.length / period;
        // Initializing outputBits, will contain the binary value of the message
        outputBits = new int[n];

        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < period; j++) {
                sum += audio[i * period + j];
            }
            // Getting the average of values over a period
            double avg = sum / period;

            // If the avererage is over the thresold the bit is set to 1, otherwise set to 0
            outputBits[i] = (avg > threshold) ? 1 : 0;
        }
    }

    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of
     * outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar() {
        // Find the start sequence in outputBits
        int startIdx = -1;
        for (int i = 0; i < outputBits.length - START_SEQ.length; i++) {
            // Moving trough outpuBits to find the start sequence
            if (Arrays.equals(Arrays.copyOfRange(outputBits, i, i + START_SEQ.length), START_SEQ)) {
                startIdx = i;
                break;
            }
        }

        if (startIdx != -1) {
            // Start sequence found, decode the message
            // Size of the message, without the start sequence
            int messageSize = (outputBits.length - startIdx - START_SEQ.length) / 8;
            // Char array to hold the final decoded message
            decodedChars = new char[messageSize];
            int charIndex = 0;

            // Iterating through outputBits, starting after the start sequence, by increment of 8 bits (bytes)
            for (int i = startIdx + START_SEQ.length; i < outputBits.length; i += 8) {
                int charCode = 0;
                // Go through each bit individually
                for (int j = 0; j < 8; j++) {
                    charCode = (charCode << 1) | outputBits[i + j];
                }
                // Converting the charCode to a char
                decodedChars[charIndex++] = (char) charCode;
            }
        } else {
            System.out.println("Start sequence not found in outputBits.");
        }
    }

    /**
     * Print the elements of an array
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {
        System.out.println(Arrays.toString(data));
    }

    /**
     * Display a signal in a window
     * @param sig  the signal to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        int width = 800;
        int height = 400;

        StdDraw.setCanvasSize(width, height);
        StdDraw.setXscale(0, stop - start);
        StdDraw.setYscale(-1, 1);
        StdDraw.clear(StdDraw.WHITE);
        StdDraw.setTitle(title);

        if (mode.equals("line")) {
            for (int i = start; i < stop - 1; i++) {
                StdDraw.line(i - start, sig[i], i + 1 - start, sig[i + 1]);
            }
        } else if (mode.equals("point")) {
            for (int i = start; i < stop; i++) {
                StdDraw.point(i - start, sig[i]);
            }
        }

        StdDraw.show();
    }

    /**
    *  Un exemple de main qui doit pourvoir être exécuté avec les méthodes
    * que vous aurez conçues.
    */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DosRead <input_wav_file>");
            return;
        }
        String wavFilePath = args[0];

        // Open the WAV file and read its header
        DosRead dosRead = new DosRead();
        dosRead.readWavHeader(wavFilePath);

        // Print the audio data properties
        System.out.println("Fichier audio: " + wavFilePath);
        System.out.println("\tSample Rate: " + dosRead.sampleRate + " Hz");
        System.out.println("\tBits per Sample: " + dosRead.bitsPerSample + " bits");
        System.out.println("\tData Size: " + dosRead.dataSize + " bytes");

        // Read the audio data
        dosRead.readAudioDouble();
        // reverse the negative values
        dosRead.audioRectifier();
        // apply a low pass filter
        dosRead.audioLPFilter(44);
        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate/BAUDS, 12000 );

        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null){
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }

        displaySig(dosRead.audio, 0, dosRead.audio.length-1, "line", "Signal audio");

        // Close the file input stream
        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
