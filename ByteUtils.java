import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ByteUtils {
    private static ByteBuffer bufferLong = ByteBuffer.allocate(Long.BYTES);
    private static ByteBuffer bufferInteger = ByteBuffer.allocate(Integer.BYTES);
    private static ByteBuffer bufferChar = ByteBuffer.allocate(Character.BYTES);

    public static byte[] longToBytes(long x) {
        bufferLong.putLong(0, x);
        return bufferLong.array();
    }

    public static long bytesToLong(byte[] bytes) {
        bufferLong.put(bytes, 0, bytes.length);
        bufferLong.flip();//need flip
        return bufferLong.getLong();
    }

    public static byte[] intToBytes(int x) {
        bufferInteger.putInt(0, x);
        return bufferInteger.array();
    }

    public static int bytesToInteger(byte[] bytes) {
        bufferInteger.put(bytes, 0, bytes.length);
        bufferInteger.flip();//need flip
        return bufferInteger.getInt();
    }

    public static byte[] stringToBytes (String valor) {
        return valor.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] bytes) {
        return new String(bytes);
    }

    public static byte[] charToBytes(char valor){
        bufferChar.putChar(valor);
        return bufferChar.array();
    }

    public static char bytesToChar(byte[] bytes) {
        bufferChar.put(bytes, 0, bytes.length);
        bufferChar.flip();
        return bufferChar.getChar();
    }

    public static byte[] combineTwoByteArrays(byte[] ba1, byte[] ba2){
        /* Combine */
        byte[] allByteArray = new byte[ba1.length + ba2.length];

        ByteBuffer buff = ByteBuffer.wrap(allByteArray);
        buff.put(ba1);
        System.out.println(ba1.toString());
        buff.put(ba2);
        System.out.println(ba2.toString());
        System.out.println(buff.array().toString());
        return buff.array();
    }

    public static byte[] combineNByteArrays(List<byte[]> listaArrays){
        int tamanhoTotal = 0;
        for (byte[] array: listaArrays) {
            tamanhoTotal += array.length;
        }

        byte[] allByteArray = new byte[tamanhoTotal];
        ByteBuffer buff = ByteBuffer.wrap(allByteArray);

        for (byte[] array: listaArrays) {
            buff.put(array);
        }

        return buff.array();
    }

    public static byte[] fileToByteArray(File myFile) throws IOException {
        byte [] conteudoArquivoByteArray  = new byte [(int)myFile.length()];
        FileInputStream fis = new FileInputStream(myFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(conteudoArquivoByteArray,0,conteudoArquivoByteArray.length);
        return conteudoArquivoByteArray;
    }
}