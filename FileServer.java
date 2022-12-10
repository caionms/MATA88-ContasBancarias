import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FileServer {
    public static int SOCKET_PORT;  // Porta

    public static String SERVER_DIRECTORY = ".\\server\\";

    public static void main(String[] args) throws IOException {
        System.out.println("Digite a porta desejada:");
        Scanner scanner = new Scanner(System.in);
        SOCKET_PORT = Integer.parseInt(scanner.next());

        ObjectInputStream ois = null;
        ServerSocket servsock = null;
        Socket sock = null;
        try {
            servsock = new ServerSocket(SOCKET_PORT);

            while (true) {

                System.out.println("Aguardando...");
                try {
                    sock = servsock.accept();
                    System.out.println("Accepted connection : " + sock);

                    //Recebe o rg e o nome do cliente para apresentar o salldo
                    ois = new ObjectInputStream(sock.getInputStream());
                    String rgCliente = ois.readUTF();
                    String nomeCliente = ois.readUTF();
                    String idCliente = rgCliente + "_" + nomeCliente;

                    //Chama o método que envia o dado do saldo do cliente
                    apresentarSaldo(sock, idCliente, rgCliente, nomeCliente);

                    //Recebe qual ação o cliente quer executar
                    ois = new ObjectInputStream(sock.getInputStream());
                    int acao = ois.readInt();

                    switch (acao) {
                        case 1:
                            //Saque
                            fazSaque(sock, rgCliente, nomeCliente, idCliente);
                            break;
                        case 2:
                            //Deposito
                            fazDeposito(sock, rgCliente, nomeCliente, idCliente);
                            break;
                        case 3:
                            //Transferencia
                            fazTransferencia(sock, rgCliente, nomeCliente, idCliente);
                            break;
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (ois != null) ois.close();
                    if (sock != null) sock.close();
                }
            }
        } finally {
            if (servsock != null) servsock.close();
        }
    }

    public static void fazSaque(Socket sock, String rgCliente, String nomeCliente, String idCliente) throws IOException, ParseException {
        //Pergunta ao cliente qual valor a ser depositado
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeUTF("Qual valor gostaria de sacar?");
        os.flush();

        //Recebe a resposta com o valor a ser depositado
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        Double valorSaque = ois.readDouble();

        //Busca o saldo cliente
        JSONObject jsonObject = recuperaArquivoJson();
        Double saldoCliente = (Double) jsonObject.get(idCliente);

        StringBuilder resposta = new StringBuilder();

        if(saldoCliente < valorSaque) {
            resposta.append("O saldo é insuficiente na conta do cliente " + nomeCliente + " de RG " + rgCliente + ".");
        }
        else {
            saldoCliente -= valorSaque;
            atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);
            resposta.append("Foram sacados " + valorSaque + " reais da conta do cliente " + nomeCliente + " de RG " + rgCliente + "!");
            resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente);
        }

        os = new ObjectOutputStream(sock.getOutputStream());
        os.writeUTF(resposta.toString());
        os.flush();
    }

    public static void fazDeposito(Socket sock, String rgCliente, String nomeCliente, String idCliente) throws IOException, ParseException {
        //Pergunta ao cliente qual valor a ser depositado
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeUTF("Qual valor gostaria de depositar?");
        os.flush();

        //Recebe a resposta com o valor a ser depositado
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        Double valorDeposito = ois.readDouble();

        //Busca o saldo cliente
        JSONObject jsonObject = recuperaArquivoJson();
        Double saldoCliente = (Double) jsonObject.get(idCliente);
        saldoCliente += valorDeposito;

        atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);

        StringBuilder resposta = new StringBuilder();
        resposta.append("Foram depositados " + valorDeposito + " reais na conta do cliente " + nomeCliente + " de RG " + rgCliente + "!");
        resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente);
        os = new ObjectOutputStream(sock.getOutputStream());
        os.writeUTF(resposta.toString());
        os.flush();
    }

    public static void fazTransferencia(Socket sock, String rgCliente, String nomeCliente, String idCliente) throws IOException, ParseException {
        StringBuilder resposta = new StringBuilder();

        //Pergunta ao cliente para quem será transferido
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeUTF("Digite o RG do cliente para o qual gostaria de fazer uma trasnferência:");
        os.writeUTF("Digite o nome do cliente para o qual gostaria de fazer uma trasnferência:");
        os.flush();

        //Recebe a resposta com os dados para quem irá transferir
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        String rgAlvo = ois.readUTF();
        String nomeAlvo = ois.readUTF();
        String idAlvo = rgAlvo + "_" + nomeAlvo;

        //Testa se a conta existe do alvo
        JSONObject jsonObject = recuperaArquivoJson();
        Double saldoAlvo = (Double) jsonObject.get(idAlvo);
        if(saldoAlvo == null) { //Caso não encontre o id do cliente
            os = new ObjectOutputStream(sock.getOutputStream());
            os.writeBoolean(false);
            os.writeUTF("A pessoa " + nomeAlvo + " de RG " + rgAlvo + " não possui uma conta em nosso sistema...");
            os.flush();
        }
        else{
            //Pergunta qual valor a ser transferido
            os = new ObjectOutputStream(sock.getOutputStream());
            os.writeBoolean(true);
            os.writeUTF("Conta do cliente " + nomeAlvo + " de RG " + rgAlvo + " encontrada!\nQual valor a ser transferido?");
            os.flush();

            //Recebe o valor a ser transferido
            ois = new ObjectInputStream(sock.getInputStream());
            Double valorTransferencia = ois.readDouble();

            Double saldoCliente = (Double) jsonObject.get(idCliente);
            if(saldoCliente < valorTransferencia) {
                resposta.append("O saldo é insuficiente na conta do cliente " + nomeCliente + " de RG " + rgCliente + ".");
            }
            else {
                //Remove valor da conta original
                saldoCliente -= valorTransferencia;
                atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);
                //Adiciona valor na conta destino
                saldoAlvo += valorTransferencia;
                atualizaSaldoCliente(jsonObject, idAlvo, saldoAlvo);

                resposta.append("Foram transferidos " + valorTransferencia + " reais da conta do cliente " + nomeCliente + " de RG " + rgCliente
                        + " para a conta do cliente " + nomeAlvo + " de RG " + rgAlvo + "!");
                resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente +"\nE o saldo do cliente "
                        + nomeAlvo + " de RG " + rgAlvo + " é: " + saldoAlvo);
            }

            os = new ObjectOutputStream(sock.getOutputStream());
            os.writeUTF(resposta.toString());
            os.flush();
        }
    }

    public static void apresentarSaldo(Socket sock, String idCliente, String rgCliente, String nomeCliente) throws IOException, ParseException {
        StringBuilder resposta = new StringBuilder();
        Double saldoCliente = null;

        //Percorre todas as máquinas e salva os arquivos sem repetir
        File server = new File("./server");
        File[] serverFolders = server.listFiles(); //Arquivo JSON com as contas
        if(serverFolders.length == 0) { //Caso o arquivo JSON não exista
            criaArquivoJson(); //Cria o arquivo JSON
        }
        else { //Caso o arquivo exista
            //Busca o saldo cliente
            JSONObject jsonObject = recuperaArquivoJson();
            saldoCliente = (Double) jsonObject.get(idCliente);

            if(saldoCliente == null) { //Caso não encontre o id do cliente
                saldoCliente = cadastraCliente(jsonObject, idCliente); //Cadastra o cliente
                resposta.append("O cliente " + nomeCliente + " de RG " + rgCliente + " foi cadastrado com sucesso!");
            }
        }

        resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente);
        ObjectOutputStream os = new ObjectOutputStream(sock.getOutputStream());
        os.writeUTF(resposta.toString());
        os.writeDouble(saldoCliente);
        os.flush();
    }

    public static JSONObject recuperaArquivoJson() throws IOException, ParseException {
        //Faz a leitura do arquivo
        JSONParser parser = new JSONParser();
        Reader reader = new FileReader(SERVER_DIRECTORY + "\\clients.json");
        return (JSONObject) parser.parse(reader);
    }

    //Método que cadastra o cliente, preenchendo seus dados no JSON
    public static Double cadastraCliente(JSONObject jsonObject, String idCliente) {
        Double saldoCliente = Double.valueOf(0);
        //Guardar o id do cliente com saldo zerado no JSON
        atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);
        System.out.print(jsonObject.toJSONString());
        return saldoCliente;
    }

    //Método que atualiza o saldo do cliente no arquivo JSON
    public static void atualizaSaldoCliente(JSONObject jsonObject, String idCliente, Double valorSaldo) {
        jsonObject.put(idCliente, valorSaldo);
        try {
            FileWriter file = new FileWriter(SERVER_DIRECTORY + "\\clients.json");
            file.write(jsonObject.toJSONString());
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Método que cria o arquivo JSON caso não esteja criado
    public static void criaArquivoJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            FileWriter file = new FileWriter(SERVER_DIRECTORY + "\\clients.json");
            file.write(jsonObject.toJSONString());
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
