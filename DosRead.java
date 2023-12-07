
import java.io.*;

public class DosRead {
    static final int FP = 1000;
    static final int BAUDS = 100;
    static final int[] START_SEQ = {1,0,1,0,1,0,1,0};
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
     * @param path the path of the wav file to read
     */
    public void readWavHeader(String path){
        byte[] header = new byte[44]; // The header is 44 bytes long
        try {
            fileInputStream= new FileInputStream(path);
            fileInputStream.read(header);
            /*
              À compléter
            */
            // Extract relevant information from the WAV header
            sampleRate = byteArrayToInt(header, 24, 32);
            bitsPerSample = byteArrayToInt(header, 34, 16);
            dataSize = byteArrayToInt(header, 40, 32);

            // Output information (you can modify or extend this as needed)
            System.out.println("Sample Rate: " + sampleRate + " Hz");
            System.out.println("Bits per Sample: " + bitsPerSample + " bits");
            System.out.println("Data Size: " + dataSize + " bytes");


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to convert a little-endian byte array to an integer
     * @param bytes the byte array to convert
     * @param offset    the offset in the byte array
     * @param fmt   the format of the integer (16 or 32 bits)
     * @return  the integer value
     */
    private static int byteArrayToInt(byte[] bytes, int offset, int fmt) {
        if (fmt == 16)
            return ((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);
        else if (fmt == 32)
            return ((bytes[offset + 3] & 0xFF) << 24) |
                    ((bytes[offset + 2] & 0xFF) << 16) |
                    ((bytes[offset + 1] & 0xFF) << 8) |
                    (bytes[offset] & 0xFF);
        else return (bytes[offset] & 0xFF);
    }

    /**
     * Read the audio data from the wav file
     * and convert it to an array of doubles
     * that becomes the audio attribute
     */
    public void readAudioDouble(){
        byte[] audioData = new byte[dataSize];
        try {
            fileInputStream.read(audioData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
          À compléter
        */
        int bytesPerSample = bitsPerSample / 8;
        audio = new double[dataSize / bytesPerSample];

        for (int i = 0, j = 0; i < dataSize; i += bytesPerSample, j++) {
          if (bytesPerSample == 2) {
            audio[j] = ((audioData[i + 1] & 0xFF) << 8 | (audioData[i] & 0xFF)) / 32768.0;
          } else if (bytesPerSample == 4) {
            int intBits = ((audioData[i + 3] & 0xFF) << 24) |
                ((audioData[i + 2] & 0xFF) << 16) |
                ((audioData[i + 1] & 0xFF) << 8) |
                (audioData[i] & 0xFF);
            audio[j] = Float.intBitsToFloat(intBits);
          }
        }

    }

    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier(){
      /*
        À compléter
      */
    }

    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n)*FECH
     * @param n the number of samples to average
     */
    public void audioLPFilter(int n) {
      /*
        À compléter
      */
    }

    /**
     * Resample the audio array and apply a threshold
     * @param period the number of audio samples by symbol
     * @param threshold the threshold that separates 0 and 1
     */
    public void audioResampleAndThreshold(int period, int threshold){
      /*
        À compléter
      */
    }

    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar(){
      /*
        À compléter
      */
      StringBuilder decodedString = new StringBuilder();
      int startIdx = -1;

      // Find the index where START_SEQ starts in outputBits
      for (int i = 0; i <= outputBits.length - START_SEQ.length; i++) {
        boolean match = true;
        for (int j = 0; j < START_SEQ.length; j++) {
          if (outputBits[i + j] != START_SEQ[j]) {
            match = false;
            break;
          }
        }
        if (match) {
          startIdx = i;
          break;
        }
      }

      if (startIdx != -1) {
        // Iterate over outputBits starting from startIdx
        for (int i = startIdx; i < outputBits.length; i += 8) {
          // Extract 8 bits to form a byte
          int byteValue = 0;
          for (int j = 0; j < 8; j++) {
            byteValue = (byteValue << 1) | outputBits[i + j];
          }

          // Convert the byte to a char and append to the decoded string
          decodedString.append((char) byteValue);
        }

        // Display the decoded string
        System.out.println(decodedString.toString());
      } else {
        System.out.println("Start sequence not found in outputBits.");
      }
    }

    /**
     * Print the elements of an array
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {
      /*
        À compléter
      */
      for (char value : data) {
        System.out.print(value + " ");
      }
      System.out.println();
    }


    /**
     * Display a signal in a window
     * @param sig  the signal to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title){
      /*
        À compléter. Méthode a priori identique à sa version dans DosSend.
      */
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
