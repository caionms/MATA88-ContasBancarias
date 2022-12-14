# Trabalho avaliativo de MATA88 - Fundamentos de Sistemas Distribuídos
### Caio Nery, Eduardo Góes e Luca Argolo 

## ARQUITETURA CLIENTE-SERVIDOR:

A arquitetura cliente servidor, que é o ponta pé inicial para o desenvolvimento do nosso projeto. 

Há um hospedeiro sempre em funcionamento, denominado servidor, que atende a requisições de muitos outros hospedeiros, denominados clientes. Seguindo os padrões desta arquitetura, não há comunicação direta entre os clientes. O servidor tem um endereço fixo, bem conhecido, o endereço IP. Por causa dessa característica do servidor e pelo fato de ele estar sempre em funcionamento, um cliente sempre pode contatá-lo.

## RODANDO A APLICAÇÃO: 

Para rodar a aplicação, primeiro deve-se executar a classe FileServer e selecionar uma porta de sua preferência. Feito isso, o servidor irá estar pronto para receber conexões e ficará no estado aguardando.

Agora, é possível executar a classe FileClient. Para isso, é preciso passar o endereço IP do servidor (por exemplo, o endereço IPV4 do adaptador Ethernet), a porta utilizada na inicialização do FileServer, o RG do cliente e o nome do mesmo. A conexão será estabelecida com o servidor, e o saldo do cliente será exibido na tela, caso haja uma conta cadastrada no arquivo JSON. Caso contrário, uma conta será criada com os dados fornecidos e com o saldo zerado.
A partir daí, o cliente poderá escolher entre três opções, correspondentes às funcionalidades da aplicação:
1. Saque de saldo: nesta opção, o cliente informa o valor a ser sacado. Se houver saldo suficiente, o valor será descontado da conta.
2. Depósito de saldo: nesta opção, o cliente informa o valor a ser depositado, que será adicionado à conta.
3. Transferência de saldo: nesta opção, o cliente informa o RG e nome da conta destino, além do valor a ser transferido. Se a conta destino existir e o valor a ser transferido for menor ou igual ao saldo do cliente, a transferência será efetuada.
