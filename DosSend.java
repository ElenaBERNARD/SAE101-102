import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Scanner;

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
            /* À compléter */
            // File size (usually done after creation)
            writeLittleEndian((int) nbBytes, 4, outStream);
            // File Type Header, WAVE
            outStream.write(new byte[] { 'W', 'A', 'V', 'E' });

            // Fromat block marker
            outStream.write(new byte[] { 'f', 'm', 't', ' ' });
            // Number of chunk in block
            writeLittleEndian(16, 4, outStream);

            // Type of format
            writeLittleEndian(1, 2, outStream);
            // Number of channels
            writeLittleEndian(CHANNELS, 2, outStream);
            // Frequency
            writeLittleEndian(FECH, 4, outStream);
            // Bytes per second
            writeLittleEndian(BAUDS, 4, outStream);
            // Bytes per block
            writeLittleEndian(1, 2, outStream);
            // Bits per sample
            writeLittleEndian(8, 2, outStream);

            // Data section marker
            outStream.write(new byte[] { 'd', 'a', 't', 'a' });
            // Size of data section
            writeLittleEndian((int) nbBytes - 44, 4, outStream);
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
            for (double data : dataMod) {
                double normalizedData = data / MAX_AMP;

                byte byteData = (byte) ((normalizedData + 1.0) * 127.5);

                // Write the normalized byte sample in little endian format
                writeLittleEndian(byteData, 1, outStream);
            }
        } catch (Exception e) {
            System.out.println("Erreur d'écriture");
        }
    }

    /**
     * Read the text data to encode and store them into dataChar
     * 
     * @return the number of characters read
     */
    public int readTextData() {
        // cree un scanner
        Scanner scan = new Scanner(System.in);

        // lire une chaine de caractere
        String inputString = scan.nextLine();

        // convertir en tableau de charactere
        this.dataChar = inputString.toCharArray();
        return dataChar.length;
    }

    /**
     * convert a char array to a bit array
     * 
     * @param chars
     * @return byte array containing only 0 & 1
     */
    public byte[] charToBits(char[] chars) {
        /* À compléter */
        int n = chars.length;
        byte[] bytes = new byte[n * 8];
        for (int i = 0; i < n; i++) {
            char[] binaryChar = Integer.toBinaryString((byte) chars[i]).toCharArray();
            int nbBits = binaryChar.length;
            for (int j = 0; j < nbBits; j++) {
                if (binaryChar[nbBits - j - 1] == '1')
                    bytes[(i + 1) * 8 - j - 1] = 1;
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
        /* À compléter */
        int index = 0;
        int n = FECH / BAUDS;
        int nbrEchantillons = (bits.length + START_SEQ.length) * n;
        dataMod = new double[nbrEchantillons];
        for (int i = 0; i < START_SEQ.length; i++) {
            if (START_SEQ[i] == 1) {
                for (int j = 0; j < n; j++) {
                    index = i*n + (j + 1);
                    dataMod[index] = MAX_AMP * Math.sin(2*Math.PI*FP*index/FECH);
                }
                System.out.println("1, " + index + " t=" + (double)i/BAUDS);
            }
            else
                System.out.println("0");
        }
        int offset = START_SEQ.length;
        for (int i = offset; i < bits.length+offset-1; i++) {
            if (bits[i-offset] == 1) {
                for (int j = 0; j < n; j++) {
                    index = i*n + (j + 1);
                    dataMod[index] = MAX_AMP * Math.sin(2*Math.PI*FP*index/FECH);
                }
                System.out.println("1, " + index + " t=" + (double)i/BAUDS);
            }
            else
                System.out.println("0");
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
        /*
         * À compléter
         */
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
    public static void displaySig(ArrayList<double[]> listOfSigs, int start, int stop, String mode, String title) {
        /*
         * À compléter
         */
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

        // exemple d'affichage du signal modulé dans une fenêtre graphique
        displaySig(dosSend.dataMod, 1000, 3000, "line", "Signal modulé");
    }
}
