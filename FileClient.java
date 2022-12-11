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

            //Incrementa pelo evento de conectar
            incrementaRelogio();

            //Incrementa pelo evento de enviar dados pro servidor
            incrementaRelogio();

            //Envia o rg e o nome do cliente para apresentar o saldo
            //Também envia o clock
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            oos.writeUTF(CLIENT_RG);
            oos.writeUTF(CLIENT_NAME);
            oos.writeInt(CLOCK);
            oos.flush();

            //Imprime o saldo do cliente
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
            System.out.println(ois.readUTF());
            atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

            int controle = 0;
            //Saque: 1_RG_NOME_VALOR
            //Deposito: 2_RG_NOME_VALOR
            //Transferencia: 3_RG_NOME_RGDESTINATARIO_NOMEDESTINATARIO_VALOR
            //Saida: 4
            System.out.println("Escolha a operação:\n1-Saque.\n2-Depósito.\n3-Transferência.\n4-Cancelar.");
            controle = Integer.parseInt(scanner.next());

            switch(controle) {
                case 1:
                case 2:
                    System.out.println("Digite o valor:");
                    Double valorSaqueOuDeposito = Double.valueOf(scanner.next());
                    fazSaqueOuDeposito(sock, controle, valorSaqueOuDeposito);
                    break;
                case 3:
                    System.out.println("Digite o RG da conta destino:");
                    String rgAlvo = scanner.next();
                    System.out.println("Digite o nome da conta destino:");
                    String nomeAlvo = scanner.next();
                    System.out.println("Digite o valor:");
                    Double valorTransferencia = Double.valueOf(scanner.next());
                    fazTransferencia(sock, controle, rgAlvo, nomeAlvo, valorTransferencia);
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

    //Entrada: 1/2_xxx (saque/deposito)_valor
    private static void fazSaqueOuDeposito(Socket sock, int acao, Double valor) throws IOException {
        //Incrementa pelo evento de enviar dados pro servidor
        incrementaRelogio();

        //Envia os dados da solicitação para o servidor
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeInt(acao);
        os.writeDouble(valor);
        os.writeInt(CLOCK);
        os.flush();

        //Imprime mensagem de saque/deposito
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        System.out.println(ois.readUTF());
        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        //Incrementa pelo evento fazer saque ou deposito
        incrementaRelogio();
    }

    private static void fazTransferencia(Socket sock, int acao, String rgAlvo, String nomeAlvo, Double valorTransferencia) throws IOException {
        //Incrementa pelo evento de enviar dados pro servidor
        incrementaRelogio();

        //Guarda os valores no socket
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeInt(acao);
        os.writeUTF(rgAlvo);
        os.writeUTF(nomeAlvo);
        os.writeDouble(valorTransferencia);
        os.writeInt(CLOCK);
        os.flush();

        //Recebe a resposta se a conta alvo existe
        //Caso sim, Imprime mensagem de sucesso
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        System.out.println(ois.readUTF());

        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        //Incrementa pelo evento fazer transferencia
        incrementaRelogio();
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
