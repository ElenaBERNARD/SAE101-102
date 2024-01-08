import java.io.*;
import java.util.Arrays;

public class DosRead {
    static final int FP = 1000;
    static final int BAUDS = 100;
    static final int[] START_SEQ = {1, 0, 1, 0, 1, 0, 1, 0};
    FileInputStream fileInputStream;
    int sampleRate = 44100;
    int bitsPerSample;
    int dataSize;
    double[] audio;
    int[] outputBits;
    char[] decodedChars;

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
                audio[j] = ((audioData[i + 1] & 0xFF) << 8 | (audioData[i] & 0xFF)) / 32768.0;
            } else if (bytesPerSample == 4) {
                int intBits = ((audioData[i + 3] & 0xFF) << 24) |
                        ((audioData[i + 2] & 0xFF) << 16) |
                        ((audioData[i + 1] & 0xFF) << 8) |
                        ((audioData[i + 1] & 0xFF) << 8) |
                        (audioData[i] & 0xFF);
                audio[j] = Float.intBitsToFloat(intBits);
            }
        }
    }

    public void audioRectifier() {
        for (int i = 0; i < audio.length; i++) {
            audio[i] = Math.abs(audio[i]);
        }
    }

    public void audioLPFilter(int n) {
        double[] filteredAudio = new double[audio.length];

        for (int i = 0; i < audio.length; i++) {
            double sum = 0;
            for (int j = Math.max(0, i - n + 1); j <= i; j++) {
                sum += audio[j];
            }
            filteredAudio[i] = sum / Math.min(n, i + 1);
        }

        audio = filteredAudio;
    }

    public void audioResampleAndThreshold(int period, int threshold) {
    int targetSize = audio.length / period;
    outputBits = new int[targetSize];

    for (int i = 0; i < targetSize; i++) {
        double sum = 0;
        for (int j = 0; j < period; j++) {
            sum += audio[i * period + j];
        }
        double avg = sum / period;

        outputBits[i] = (avg > threshold) ? 1 : 0;
    }

    System.out.println("Resampled Audio: " + Arrays.toString(audio));
    System.out.println("Output Bits: " + Arrays.toString(outputBits));
}

    public void decodeBitsToChar() {
      // Find the start sequence in outputBits
      int startIdx = -1;
      for (int i = 0; i < outputBits.length - START_SEQ.length; i++) {
        if (Arrays.equals(Arrays.copyOfRange(outputBits, i, i + START_SEQ.length), START_SEQ)) {
          startIdx = i;
          break;
        }
      }

      if (startIdx != -1) {
        // Start sequence found, decode the message
        int messageSize = (outputBits.length - startIdx) / 8;
        decodedChars = new char[messageSize];
        int charIndex = 0;

        for (int i = startIdx; i < outputBits.length; i += 8) {
          int charCode = 0;
          for (int j = 0; j < 8; j++) {
            charCode = (charCode << 1) | outputBits[i + j];
          }
          decodedChars[charIndex++] = (char) charCode;
        }
      } else {
        System.out.println("Start sequence not found in outputBits.");
      }
    }

    public static void printIntArray(char[] data) {
        System.out.println(Arrays.toString(data));
    }

    public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        // TODO: Implement or integrate the displaySig method as needed
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DosRead <input_wav_file>");
            return;
        }
        String wavFilePath = args[0];

        DosRead dosRead = new DosRead();
        dosRead.readWavHeader(wavFilePath);

        System.out.println("Fichier audio: " + wavFilePath);
        System.out.println("\tSample Rate: " + dosRead.sampleRate + " Hz");
        System.out.println("\tBits per Sample: " + dosRead.bitsPerSample + " bits");
        System.out.println("\tData Size: " + dosRead.dataSize + " bytes");

        dosRead.readAudioDouble();
        dosRead.audioRectifier();
        dosRead.audioLPFilter(44);
        dosRead.audioResampleAndThreshold(dosRead.sampleRate / BAUDS, 12000);

        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null) {
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }

        displaySig(dosRead.audio, 0, dosRead.audio.length - 1, "line", "Signal audio");

        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
