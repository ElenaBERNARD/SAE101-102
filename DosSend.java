import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Scanner;

/**
 * Béliard Valentin
 * Bernard Elena
 * C1
 */

public class DosSend {
    final int FECH = 44100; // fréquence d'échantillonnage
    final int FP = 1000; // fréquence de la porteuses
    final int BAUDS = 100; // débit en symboles par seconde
    final int FMT = 16; // format des données
    final int MAX_AMP = (1 << (FMT - 1)) - 1; // amplitude max en entier
    final int CHANNELS = 1; // nombre de voies audio (1 = mono)
    final int[] START_SEQ = { 1, 0, 1, 0, 1, 0, 1, 0 }; // séquence de synchro au début
    final Scanner input = new Scanner(System.in); // pour lire le fichier texte

    long taille; // nombre d'octets de données à transmettre
    double duree; // durée de l'audio
    double[] dataMod; // données modulées
    char[] dataChar; // données en char
    FileOutputStream outStream; // flux de sortie pour le fichier .wav

    /**
     * Constructor
     * 
     * @param path the path of the wav file to create
     */
    public DosSend(String path) {
        File file = new File(path);
        try {
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Erreur de création du fichier");
        }
    }

    /**
     * Write a raw 4-byte integer in little endian
     * 
     * @param octets     the integer to write
     * @param destStream the stream to write in
     */
    public void writeLittleEndian(int octets, int taille, FileOutputStream destStream) {
        char poidsFaible;
        while (taille > 0) {
            poidsFaible = (char) (octets & 0xFF);
            try {
                destStream.write(poidsFaible);
            } catch (Exception e) {
                System.out.println("Erreur d'écriture");
            }
            octets = octets >> 8;
            taille--;
        }
    }

    /**
     * Create and write the header of a wav file
     *
     */
    public void writeWavHeader() {
        taille = (long) (FECH * duree);
        long nbBytes = taille * CHANNELS * FMT / 8;

        try {
            outStream.write(new byte[] { 'R', 'I', 'F', 'F' });
            // File size
            writeLittleEndian((int) (36 + nbBytes), 4, outStream);
            // Writing "WAVE" and "fmt "
            outStream.write(new byte[] { 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ' });
            // Size of fmt chunk
            writeLittleEndian(16, 4, outStream);
            // Audio format (1 for PCM) 
            writeLittleEndian(1, 2, outStream);
            // Number of channels (here 1 is mono)
            writeLittleEndian(CHANNELS, 2, outStream);
            // Sample rate
            writeLittleEndian(FECH, 4, outStream);
            // Byte rate
            writeLittleEndian(FECH * CHANNELS * FMT / 8, 4, outStream);
            // Block align
            writeLittleEndian(CHANNELS * FMT / 8, 2, outStream);
            // Bits per sample
            writeLittleEndian(FMT, 2, outStream);
            // Writing "data"
            outStream.write(new byte[] { 'd', 'a', 't', 'a' });
            // Size of data chunk
            writeLittleEndian((int) nbBytes, 4, outStream);

        } catch (Exception e) {
            System.out.printf(e.toString());
        }
    }

    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits
     * signed)
     */
    public void writeNormalizeWavData() {
        try {
            double scale = Math.pow(2, FMT - 1) - 1;

            for (double data : dataMod) {
                // Set the data between [-1.0, 1.0]
                double normalizedData = data / scale;

                // Forces the normalizedData between -1 and 1
                normalizedData = Math.max(-1.0, Math.min(1.0, normalizedData));

                // Convert the normalized sample to a byte
                int intData = (int) (normalizedData * scale);

                // Write the data in little-endian
                writeLittleEndian(intData, 2, outStream);
            }
        } catch (
        Exception e) {
            System.out.println("Erreur d'écriture");
        }
    }

    /**
     * Read the text data to encode and store them into dataChar
     * 
     * @return the number of characters read
     */
    public int readTextData() {
        // Get the first line of the text file, then converts it to an array of char
        this.dataChar = input.nextLine().toCharArray();
        return dataChar.length;
    }

    /**
     * convert a char array to a bit array
     * 
     * @param chars
     * @return byte array containing only 0 & 1
     */
    public byte[] charToBits(char[] chars) {
        int n = chars.length;
        // Create an array 8 times bigger to hold all bits
        byte[] bytes = new byte[n * 8];

        for (int i = 0; i < n; i++) {
            // Using toBinaryString to get the binary value
            String binaryString = String.format("%8s", Integer.toBinaryString(chars[i])).replace(' ', '0');
            for (int j = 0; j < 8; j++) {
                // Moving threw binarytring to get each bits 
                bytes[i * 8 + j] = (byte) (binaryString.charAt(j) - '0');
            }
        }
        return bytes;
    }

    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     * 
     * @param bits the data to modulate
     */
    public void modulateData(byte[] bits) {
        // Number of sample per symbol
        int n = FECH / BAUDS;
        // Total number of sample
        int nbrEchantillons = (bits.length + START_SEQ.length) * n;
        dataMod = new double[nbrEchantillons];

        // Current index, used to move throw dataMod
        int index = 0;

        // Modulate START_SEQ
        for (int i = 0; i < START_SEQ.length; i++) {
            if (START_SEQ[i] == 1) {
                for (int j = 0; j < n; j++) {
                    index = i * n + j;
                    dataMod[index] = MAX_AMP * Math.sin(2 * Math.PI * FP * index / FECH);
                }
            }
        }

        // Offset to not overwite START_SEQ
        int offset = START_SEQ.length;

        // Modulate bits
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == 1) {
                for (int j = 0; j < n; j++) {
                    index = (i + offset) * n + j;
                    dataMod[index] = MAX_AMP * Math.sin(2 * Math.PI * FP * index / FECH);
                }
            }
        }
    }

    /**
     * Display a signal in a window
     * 
     * @param sig   the signal to display
     * @param start the first sample to display
     * @param stop  the last sample to display
     * @param mode  "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        int width = 1200;
        int height = 600;

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
     * Display signals in a window
     * 
     * @param listOfSigs a list of the signals to display
     * @param start      the first sample to display
     * @param stop       the last sample to display
     * @param mode       "line" or "point"
     * @param title      the title of the window
     */
    public static void displaySig(List<double[]> listOfSigs, int start, int stop, String mode, String title) {
        int width = 1200;
        int height = 600;

        StdDraw.setCanvasSize(width, height);
        StdDraw.setXscale(0, stop - start);
        StdDraw.setYscale(-1, 1);
        StdDraw.clear(StdDraw.WHITE);
        StdDraw.setTitle(title);

        int numSignals = listOfSigs.size();
        double yOffset = 2.0 / numSignals;

        for (int k = 0; k < numSignals; k++) {
            double[] sig = listOfSigs.get(k);
            double yShift = k * yOffset;

            if (mode.equals("line")) {
                for (int i = start; i < stop - 1; i++) {
                    StdDraw.line((double) i - start, sig[i] + yShift, i + 1.0 - start, sig[i + 1] + yShift);
                }
            } else if (mode.equals("point")) {
                for (int i = start; i < stop; i++) {
                    StdDraw.point((double) i - start, sig[i] + yShift);
                }
            }
        }

        StdDraw.show();
    }

    public static void main(String[] args) {
        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");
        // lit le texte à envoyer depuis l'entrée standard
        // et calcule la durée de l'audio correspondant
        dosSend.duree = (double) (dosSend.readTextData() + dosSend.START_SEQ.length / 8) * 8.0 / dosSend.BAUDS;

        // génère le signal modulé après avoir converti les données en bits
        dosSend.modulateData(dosSend.charToBits(dosSend.dataChar));
        // écrit l'entête du fichier wav
        dosSend.writeWavHeader();
        // écrit les données audio dans le fichier wav
        dosSend.writeNormalizeWavData();

        // affiche les caractéristiques du signal dans la console
        System.out.println("Message : " + String.valueOf(dosSend.dataChar));
        System.out.println("\tNombre de symboles : " + dosSend.dataChar.length);
        System.out.println("\tNombre d'échantillons : " + dosSend.dataMod.length);
        System.out.println("\tDurée : " + dosSend.duree + " s");
        System.out.println();

    }
}