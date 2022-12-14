import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class FileClient {

    public static int SOCKET_PORT;  //Porta
    public static String SERVER = "127.0.0.1";  //Localhost
    public static String CLIENT_RG; //RG do cliente
    public static String CLIENT_NAME; //Nome do Cliente
    public static int CLOCK = 0; //Relógio lógico do cliente

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

        incrementaRelogio(); //Incrementa pelo evento de buscar dados do usuário

        Socket sock = null;

        try {
            //Tenta conectar com o servidor
            sock = new Socket(SERVER, SOCKET_PORT);

            System.out.println("Connecting...");

            incrementaRelogio(); //Incrementa pelo evento de conectar

            incrementaRelogio(); //Incrementa pelo evento de enviar dados pro servidor

            //Envia o RG e o nome do cliente para apresentar o saldo
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

            //Escolha de operação
            int controle = 0;
            System.out.println("Escolha a operação:\n1-Saque.\n2-Depósito.\n3-Transferência.\n4-Cancelar.");
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
                    //Finaliza a execução do cliente
                    ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
                    os.writeInt(controle);
                    os.flush();
                    if (sock != null) sock.close();
                    if (scanner != null) scanner.close();
                    break;
            }
        }
        finally {
            //Finaliza a execução do cliente
            if (sock != null) sock.close();
            if (scanner != null) scanner.close();
        }
    }

    private static void fazSaqueOuDeposito(Socket sock, int acao) throws IOException {
        System.out.println("Digite o valor:");
        Double valorSaqueOuDeposito = Double.valueOf(scanner.next());

        incrementaRelogio(); //Incrementa pelo evento de enviar dados pro servidor

        //Envia os dados da solicitação para o servidor
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeInt(acao);
        os.writeDouble(valorSaqueOuDeposito);
        os.writeInt(CLOCK);
        os.flush();

        //Imprime mensagem de saque/deposito
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        System.out.println(ois.readUTF());

        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        incrementaRelogio(); //Incrementa pelo evento fazer saque ou deposito
    }

    private static void fazTransferencia(Socket sock, int acao) throws IOException {
        System.out.println("Digite o RG da conta destino:");
        String rgAlvo = scanner.next();
        System.out.println("Digite o nome da conta destino:");
        String nomeAlvo = scanner.next();
        System.out.println("Digite o valor:");
        Double valorTransferencia = Double.valueOf(scanner.next());

        incrementaRelogio(); //Incrementa pelo evento de enviar dados pro servidor

        //Envia os dados da solicitação para o servidor
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeInt(acao);
        os.writeUTF(rgAlvo);
        os.writeUTF(nomeAlvo);
        os.writeDouble(valorTransferencia);
        os.writeInt(CLOCK);
        os.flush();

        //Recebe a resposta se a conta alvo existe e se foi um sucesso ou não a transferência
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        System.out.println(ois.readUTF());

        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        incrementaRelogio(); //Incrementa pelo evento fazer transferencia
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
