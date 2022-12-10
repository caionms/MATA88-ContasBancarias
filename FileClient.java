import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileClient {

    public static int SOCKET_PORT;  // Porta
    public static String SERVER = "127.0.0.1";  // localhost
    public static String CLIENT_RG;
    public static String CLIENT_NAME;
    public static int CLOCK = 0;
    /*
     * file size temporary hard coded
     * should bigger than the file to be downloaded
     */
    public final static int FILE_SIZE = 6022386;

    private static Scanner scanner = new Scanner(System.in);

    public static void main (String [] args ) throws IOException {
        System.out.println("Digite o ip do servidor:");
        SERVER = scanner.next();

        System.out.println("Digite a porta desejada:");
        SOCKET_PORT = Integer.parseInt(scanner.next());

        System.out.println("Digite o seu RG:");
        CLIENT_RG = scanner.next();

        System.out.println("Digite o seu nome:");
        CLIENT_NAME = scanner.next();

        //Incrementa pelo evento de buscar dados do usuário
        incrementaRelogio();

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        ServerSocket servsock = null;
        Socket sock = null;

        try {
            sock = new Socket(SERVER, SOCKET_PORT);

            System.out.println("Connecting...");
            
            //Envia o rg e o nome do cliente para apresentar o saldo
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            oos.writeUTF(CLIENT_RG);
            oos.writeUTF(CLIENT_NAME);
            oos.flush();

            //Imprime o saldo do cliente
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
            String texto = ois.readUTF();
            System.out.println(texto);

            int controle = 0;
            System.out.println("\n1-Saque.\n2-Depósito.\n3-Transferência.\n4-Cancelar.");
            controle = Integer.parseInt(scanner.next());

            switch(controle) {
                case 1:
                case 2:
                    fazSaqueOuDeposito(sock, controle);
                    break;
                case 3:
                    fazTransferencia(sock, controle);
                    break;
                case 4:
                    if (sock != null) sock.close();
                    if (scanner != null) scanner.close();
                    break;
            }
        }
        finally {
            if (sock != null) sock.close();
            if (scanner != null) scanner.close();
        }


    }

    private static void fazSaqueOuDeposito(Socket sock, int acao) throws IOException {
        //Guarda os valores no socket
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeInt(acao);
        os.flush();

        //Recebe mensagem solicitando o valor de saque/deposito
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        String texto = ois.readUTF();
        System.out.println(texto);

        //Envia o valor do saque/deposito
        os = new ObjectOutputStream(sock.getOutputStream());
        Double valorDeposito = Double.valueOf(scanner.next());
        os.writeDouble(valorDeposito);
        os.flush();

        //Imprime mensagem de saque/deposito
        ois = new ObjectInputStream(sock.getInputStream());
        texto = ois.readUTF();
        System.out.println(texto);
    }

    private static void fazTransferencia(Socket sock, int acao) throws IOException {
        //Guarda os valores no socket
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeInt(acao);
        os.flush();

        //Recebe mensagem solicitando para quem ira transferir
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        System.out.println(ois.readUTF());
        String rgAlvo = scanner.next();
        System.out.println(ois.readUTF());
        String nomeAlvo = scanner.next();

        //Envia o valor o nome e RG
        os = new ObjectOutputStream(sock.getOutputStream());
        os.writeUTF(rgAlvo);
        os.writeUTF(nomeAlvo);
        os.flush();

        //Recebe a resposta se a conta alvo existe
        //Caso sim, pergunta o valor a ser transferido
        ois = new ObjectInputStream(sock.getInputStream());
        if(!ois.readBoolean()){
            System.out.println(ois.readUTF()); //Mensagem de erro
        }
        else {
            System.out.println(ois.readUTF()); //Pergunta valor a ser transferido
            //Envia o valor a ser transferido
            os = new ObjectOutputStream(sock.getOutputStream());
            Double valorTransferencia = Double.valueOf(scanner.next());
            os.writeDouble(valorTransferencia);
            os.flush();

            //Imprime mensagem de sucesso
            ois = new ObjectInputStream(sock.getInputStream());
            System.out.println( ois.readUTF());
        }
    }

    public static void incrementaRelogio() {
        CLOCK++;
        System.out.println("Relógio lógico do cliente: " + CLOCK);
    }

    public static void atualizaRelogioMensagem(int tempoRecebido) {
        //Max entre CLOCK atual e CLOCK recebido
        if(tempoRecebido >= CLOCK) {
            CLOCK = tempoRecebido;
        }
        incrementaRelogio();
    }
}
