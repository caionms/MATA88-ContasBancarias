import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class FileServer implements Runnable {
    public static int SOCKET_PORT; //Porta

    public static String SERVER_DIRECTORY = ".\\server\\"; //Diretório do servidor

    public static int CLOCK = 0; //Relógio lógico do servidor

    public static int THREAD_COUNT = 0; //Contador de threads (quantidades de clientes conectados)

    public Socket cliente; //Socket para conexão com o cliente

    //Semaforo para permitir apenas 1 thread nas operações de escrita no arquivo JSON
    private Semaphore mutex = new Semaphore(1);

    //Construtor que permite criacao de thread para cada cliente
    public FileServer(Socket cliente) {this.cliente = cliente;}

    public static void main(String[] args) throws IOException {
        System.out.println("Digite a porta desejada:");
        Scanner scanner = new Scanner(System.in);
        SOCKET_PORT = Integer.parseInt(scanner.next());

        ServerSocket servsock = null;
        try {
            //Libera para conexão de cliente
            servsock = new ServerSocket(SOCKET_PORT);

            while (true) {
                System.out.println("Aguardando...");
                incrementaRelogio(); //Incrementa pelo evento de incializar

                Socket sockCliente = servsock.accept(); //Aceita conexão de 1 cliente
                System.out.println("Conexão aceita : " + sockCliente);
                incrementaRelogio(); //Incrementa pelo evento de aceitar conexão do cliente

                //Cria uma thread do servidor para tratar a conexão
                FileServer tratamento = new FileServer(sockCliente);
                Thread t = new Thread(tratamento);
                t.start();

            }
        } finally {
            //Fecha o servidor
            if (servsock != null) servsock.close();
        }
    }

    @Override
    public void run() {
        System.out.println("Thread: " + (++THREAD_COUNT));

        ObjectInputStream ois = null;

        try {
            //Recebe o RG e o nome do cliente para apresentar o saldo
            ois = new ObjectInputStream(this.cliente.getInputStream());
            String rgCliente = ois.readUTF();
            String nomeCliente = ois.readUTF();
            String idCliente = rgCliente + "_" + nomeCliente;

            atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

            //Chama o método que envia o dado com o saldo do cliente
            apresentarSaldo(idCliente, rgCliente, nomeCliente);

            incrementaRelogio(); //Incrementa pelo evento de envio de mensagem

            //Faz leitura de qual operação foi escolhida pelo usuario
            ois = new ObjectInputStream(this.cliente.getInputStream());
            int acao = ois.readInt();

            switch (acao) {
                case 1:
                    //Operação de saque
                    try {
                        //Tenta "travar" o semaforo e realizar a operação
                        //Caso já esteja travado, entra em stand-by
                        mutex.acquire();
                        fazSaque(ois, rgCliente, nomeCliente, idCliente);
                    } catch (InterruptedException e) {
                        System.out.println("Interrompido na conexão: " + cliente);
                        System.out.println("Thread: " + (--THREAD_COUNT));
                    } finally {
                        //Ao concluir, libera o semaforo
                        mutex.release();
                    }
                    break;
                case 2:
                    //Operação de deposito
                    try {
                        //Tenta "travar" o semaforo e realizar a operação
                        //Caso já esteja travado, entra em stand-by
                        mutex.acquire();
                        fazDeposito(ois, rgCliente, nomeCliente, idCliente);
                    } catch (InterruptedException e) {
                        System.out.println("Interrompido na conexão: " + cliente);
                        System.out.println("Thread: " + (--THREAD_COUNT));
                    } finally {
                        //Ao concluir, libera o semaforo
                        mutex.release();
                    }
                    break;
                case 3:
                    //Operação de transferencia
                    try {
                        //Tenta "travar" o semaforo e realizar a operação
                        //Caso já esteja travado, entra em stand-by
                        mutex.acquire();
                        fazTransferencia(ois, rgCliente, nomeCliente, idCliente);
                    } catch (InterruptedException e) {
                        System.out.println("Interrompido na conexão: " + cliente);
                        System.out.println("Thread: " + (--THREAD_COUNT));
                    } finally {
                        //Ao concluir, libera o semaforo
                        mutex.release();
                    }
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

        //Busca o saldo cliente no arquivo JSON
        JSONObject jsonObject = recuperaArquivoJson();
        Double saldoCliente = (Double) jsonObject.get(idCliente);

        StringBuilder resposta = new StringBuilder();
        if (saldoCliente < valorSaque) {
            resposta.append("O saldo é insuficiente na conta do cliente " + nomeCliente + " de RG " + rgCliente + ".");
        } else {
            saldoCliente -= valorSaque;
            atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);
            resposta.append("Foram sacados " + valorSaque + " reais da conta do cliente " + nomeCliente + " de RG " + rgCliente + "!");
            resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente);
        }

        incrementaRelogio(); //Incrementa pelo evento de saque

        incrementaRelogio(); //Incrementa pelo evento de envio de mensagem

        //Envia mensagem com resposta e o relógio atual
        ObjectOutputStream os = new ObjectOutputStream(this.cliente.getOutputStream());
        os.writeUTF(resposta.toString());
        os.writeInt(CLOCK);
        os.flush();
    }

    public void fazDeposito(ObjectInputStream ois, String rgCliente, String nomeCliente, String idCliente) throws IOException, ParseException {
        //Recebe a resposta com o valor a ser depositado
        Double valorDeposito = ois.readDouble();
        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        //Busca o saldo cliente no arquivo JSON e deposita o valor
        JSONObject jsonObject = recuperaArquivoJson();
        Double saldoCliente = (Double) jsonObject.get(idCliente);
        saldoCliente += valorDeposito;

        atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);

        StringBuilder resposta = new StringBuilder();
        resposta.append("Foram depositados " + valorDeposito + " reais na conta do cliente " + nomeCliente + " de RG " + rgCliente + "!");
        resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente);

        incrementaRelogio(); //Incrementa pelo evento de deposito

        incrementaRelogio(); //Incrementa pelo evento de envio de mensagem

        //Envia mensagem com resposta e o relógio atual
        ObjectOutputStream os = new ObjectOutputStream(this.cliente.getOutputStream());
        os.writeUTF(resposta.toString());
        os.writeInt(CLOCK);
        os.flush();
    }

    public void fazTransferencia(ObjectInputStream ois, String rgCliente, String nomeCliente, String idCliente) throws IOException, ParseException {
        StringBuilder resposta = new StringBuilder();

        //Recebe a mensagem do cliente com os dados para quem irá transferir e o valor
        String rgAlvo = ois.readUTF();
        String nomeAlvo = ois.readUTF();
        String idAlvo = rgAlvo + "_" + nomeAlvo;
        Double valorTransferencia = ois.readDouble();

        atualizaRelogioMensagem(ois.readInt()); //Atualiza clock com mensagem recebida

        //Busca o saldo do alvo no arquivo JSON
        JSONObject jsonObject = recuperaArquivoJson();
        Double saldoAlvo = (Double) jsonObject.get(idAlvo);

        if (saldoAlvo == null) { //Caso não encontre o id do alvo no arquivo JSON
            resposta.append("A pessoa " + nomeAlvo + " de RG " + rgAlvo + " não possui uma conta em nosso sistema...");
        } else {
            //Busca o saldo cliente no arquivo JSON
            Double saldoCliente = (Double) jsonObject.get(idCliente);
            if (saldoCliente < valorTransferencia) {
                resposta.append("O saldo é insuficiente na conta do cliente " + nomeCliente + " de RG " + rgCliente + ".");
            } else {
                //Remove valor da conta original
                saldoCliente -= valorTransferencia;
                atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);
                //Adiciona valor na conta destino
                saldoAlvo += valorTransferencia;
                atualizaSaldoCliente(jsonObject, idAlvo, saldoAlvo);

                resposta.append("Foram transferidos " + valorTransferencia + " reais da conta do cliente " + nomeCliente + " de RG " + rgCliente
                        + " para a conta do cliente " + nomeAlvo + " de RG " + rgAlvo + "!");
                resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente + "\nE o saldo do cliente "
                        + nomeAlvo + " de RG " + rgAlvo + " é: " + saldoAlvo);
            }
        }

        incrementaRelogio(); //Incrementa pelo evento de transferencia

        incrementaRelogio(); //Incrementa pelo evento de envio de mensagem

        //Envia mensagem com resposta e o relógio atual
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
        if (serverFolders.length == 0) { //Caso o arquivo JSON não exista
            criaArquivoJson(); //Cria o arquivo JSON
        } else { //Caso o arquivo exista
            //Busca o saldo cliente
            JSONObject jsonObject = recuperaArquivoJson();
            saldoCliente = (Double) jsonObject.get(idCliente);

            if (saldoCliente == null) { //Caso não encontre o id do cliente
                saldoCliente = cadastraCliente(jsonObject, idCliente); //Cadastra o cliente
                resposta.append("O cliente " + nomeCliente + " de RG " + rgCliente + " foi cadastrado com sucesso!");
            }
        }

        resposta.append("\nO saldo do cliente " + nomeCliente + " de RG " + rgCliente + " é: " + saldoCliente);

        incrementaRelogio(); //Incrementa pelo evento de fazer saque

        incrementaRelogio(); //Incrementa pelo evento de enviar mensagem pro cliente

        //Envia mensagem com resposta e o relógio atual
        ObjectOutputStream os = new ObjectOutputStream(this.cliente.getOutputStream());
        os.writeUTF(resposta.toString());
        os.writeInt(CLOCK);
        os.flush();
    }

    public static JSONObject recuperaArquivoJson() throws IOException, ParseException {
        //Faz a leitura do arquivo JSON
        JSONParser parser = new JSONParser();
        Reader reader = new FileReader(SERVER_DIRECTORY + "\\clients.json");
        return (JSONObject) parser.parse(reader);
    }

    //Método que cadastra o cliente, preenchendo os seus dados no JSON
    public static Double cadastraCliente(JSONObject jsonObject, String idCliente) {
        Double saldoCliente = Double.valueOf(0);
        //Guardar o id do cliente com saldo zerado no JSON
        atualizaSaldoCliente(jsonObject, idCliente, saldoCliente);
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
        if (tempoRecebido >= CLOCK) {
            CLOCK = tempoRecebido;
        }
        incrementaRelogio();
    }
}
