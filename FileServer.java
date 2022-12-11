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

public class FileServer implements Runnable{
    public static int SOCKET_PORT;  // Porta

    public static String SERVER_DIRECTORY = ".\\server\\";

    public static int CLOCK = 0;

    public static int THREAD_COUNT = 0;

    public Socket cliente;

    public FileServer(Socket cliente){
        this.cliente = cliente;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Digite a porta desejada:");
        Scanner scanner = new Scanner(System.in);
        SOCKET_PORT = Integer.parseInt(scanner.next());

        ServerSocket servsock = null;
        try {
            servsock = new ServerSocket(SOCKET_PORT);

            while (true) {

                System.out.println("Aguardando...");
                incrementaRelogio(); //Incrementa pelo evento de incializar

                Socket sockCliente = servsock.accept();
                System.out.println("Conexão aceita : " + sockCliente);
                incrementaRelogio(); //Incrementa pelo evento de aceitar conexão do cliente

                // Cria uma thread do servidor para tratar a conexão
                FileServer tratamento = new FileServer(sockCliente);
                Thread t = new Thread(tratamento);
                // Inicia a thread para o cliente conectado
                t.start();

            }
        } finally {
            if (servsock != null) servsock.close();
        }
    }

    @Override
    public void run() {
        System.out.println("Thread: " + (++THREAD_COUNT));

        ObjectInputStream ois = null;

        //Recebe o rg e o nome do cliente para apresentar o salldo
        try {
            ois = new ObjectInputStream(this.cliente.getInputStream());

        String rgCliente = ois.readUTF();
        String nomeCliente = ois.readUTF();
        String idCliente = rgCliente + "_" + nomeCliente;

        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        //Chama o método que envia o dado do saldo do cliente
        apresentarSaldo(idCliente, rgCliente, nomeCliente);

        incrementaRelogio();
        ois = new ObjectInputStream(this.cliente.getInputStream());
        int acao = ois.readInt();

        switch (acao) {
            case 1:
                //Saque
                fazSaque(ois, rgCliente, nomeCliente, idCliente);
                break;
            case 2:
                //Deposito
                fazDeposito(ois, rgCliente, nomeCliente, idCliente);
                break;
            case 3:
                //Transferencia
                fazTransferencia(ois, rgCliente, nomeCliente, idCliente);
                break;
            case 4:
                if (ois != null) ois.close();
                System.out.println("Thread: " + (--THREAD_COUNT));
                break;
        }
        } catch (IOException | ParseException e) {
            System.out.println(e.getMessage());
            System.out.println("Thread: " + (--THREAD_COUNT));

        }
    }

    public void fazSaque(ObjectInputStream ois, String rgCliente, String nomeCliente, String idCliente) throws IOException, ParseException {
        //Recebe a resposta com o valor a ser depositado
        Double valorSaque = ois.readDouble();
        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

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

        incrementaRelogio(); //Incrementa pelo evento de saque

        incrementaRelogio(); //Incrementa pelo evento de envio de mensagem

        ObjectOutputStream os = new ObjectOutputStream(this.cliente.getOutputStream());
        os.writeUTF(resposta.toString());
        os.writeInt(CLOCK);
        os.flush();
    }

    public void fazDeposito(ObjectInputStream ois, String rgCliente, String nomeCliente, String idCliente) throws IOException, ParseException {
        //Recebe a resposta com o valor a ser depositado
        Double valorDeposito = ois.readDouble();
        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        //Busca o saldo cliente
        JSONObject jsonObject = recuperaArquivoJson();
        Double saldoCliente = (Double) jsonObject.get(idCliente);
        saldoCliente += valorDeposito;

        atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);

        StringBuilder resposta = new StringBuilder();
        resposta.append("Foram depositados " + valorDeposito + " reais na conta do cliente " + nomeCliente + " de RG " + rgCliente + "!");
        resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente);

        incrementaRelogio(); //Incrementa pelo evento de deposito

        incrementaRelogio(); //Incrementa pelo evento de envio de mensagem

        ObjectOutputStream os = new ObjectOutputStream(this.cliente.getOutputStream());
        os.writeUTF(resposta.toString());
        os.writeInt(CLOCK);
        os.flush();
    }

    public void fazTransferencia(ObjectInputStream ois, String rgCliente, String nomeCliente, String idCliente) throws IOException, ParseException {
        StringBuilder resposta = new StringBuilder();

        //Recebe a resposta com os dados para quem irá transferir
        String rgAlvo = ois.readUTF();
        String nomeAlvo = ois.readUTF();
        String idAlvo = rgAlvo + "_" + nomeAlvo;
        Double valorTransferencia = ois.readDouble();

        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        //Testa se a conta existe do alvo
        JSONObject jsonObject = recuperaArquivoJson();
        Double saldoAlvo = (Double) jsonObject.get(idAlvo);

        if(saldoAlvo == null) { //Caso não encontre o id do cliente
            resposta.append("A pessoa " + nomeAlvo + " de RG " + rgAlvo + " não possui uma conta em nosso sistema...");
        }
        else{
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
        }

        incrementaRelogio(); //Incrementa pelo evento de transferencia

        incrementaRelogio(); //Incrementa pelo evento de envio de mensagem

        ObjectOutputStream os = new ObjectOutputStream(this.cliente.getOutputStream());
        os.writeUTF(resposta.toString());
        os.writeInt(CLOCK);
        os.flush();
    }

    public void apresentarSaldo(String idCliente, String rgCliente, String nomeCliente) throws IOException, ParseException {
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

        incrementaRelogio(); //Incrementa pelo evento de fazer saque

        incrementaRelogio(); //Incrementa pelo evento de enviar mensagem pro cliente

        ObjectOutputStream os = new ObjectOutputStream(this.cliente.getOutputStream());
        os.writeUTF(resposta.toString());
        os.writeInt(CLOCK);
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

    public static void incrementaRelogio() {
        CLOCK++;
        System.out.println("Relógio lógico do servidor: " + CLOCK);
    }

    public static void atualizaRelogioMensagem(int tempoRecebido) {
        //Max entre CLOCK atual e CLOCK recebido
        if(tempoRecebido >= CLOCK) {
            CLOCK = tempoRecebido;
        }
        incrementaRelogio();
    }
}
