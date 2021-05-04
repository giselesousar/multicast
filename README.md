# Multicast

### Introdução
Trabalho avaliativo desenvolvido para a disciplina de Sistemas Distribuídos. A aplicação permite a comunicação entre uma máquina cliente e um grupo de servidores. O cliente envia, periodicamente, requisições, via Multicast IP, com expressões aritméticas simples. O servidor disponível, com menor número identificador, responde à requisisão do cliente com o resultado da expressão.

### Como executar 
A aplicação contém dois códigos fontes com extensão *.java*. A máquina que atuará como cliente deverá compilar o código utilizando o comando `javac Client.java`.

As maquinas que irão compor o grupo de servidores devem compilar o código com a linha de comando `javac Server.java`. 

Após compilados, para iniciar a execução dos servidores, basta executar o comando `java Server <identificador>`, onde <identificador> deve ser substituído por um inteiro que será o identificador do servidor em questão. 

Uma vez que as máquinas servidoras estão em execução, deve-se executar o comando `java Client` na máquina cliente.

### Detalhes de implementação

+ Cliente
  * O código fonte é executado em loop na máquina cliente. Dentro desse loop, é instanciada uma mensagem contendo a expressão aritmética a ser enviada ao servidor. A expressão aritmética é gerada de maneira aleatória, contendo sempre 4 operandos inteiros e 3 operadores aritméticos. Os operandos gerados variam entre 1 e 10, enquanto os operadores podem ser de adição, subtração, multiplicação e divisão. 
  * A mensagem contendo a expressão é enviada ao grupo de servidores através de um datagrama via Multicast IP, com o auxílio de um socket na porta designada para comunicação com o grupo. Em seguida, é executado um segundo loop, interno ao primeiro, em que o cliente fica à espera de uma resposta vinda do grupo de servidores. Caso seja recebida a resposta, é impresso no terminal o resultado da expressão aritmética e o endereço IP da máquina servidora que enviou a resposta. Uma vez que a expressão é resolvida, o segundo loop deixa de ser executado e o loop inicial recomeça com uma nova expressão a ser calculada. 

+ Servidor
  * O código fonte executado em cada máquina que compõe o grupo de servidores realiza, primeiramente, a instanciação de uma thread que será responsável por enviar, a cada segundo, mensagens informando seu identificador para as máquinas do grupo. As mensagens são enviadas via Multicast IP com o auxílio de um socket na porta designada para o grupo. Em seguida, a execução entra em loop. Dentro desse loop, é recebida uma mensagem do cliente contendo a expressão matemática a ser resolvida. O cliente se comunica com o grupo de servidores a partir de uma porta específica designada. Após a comunicação do cliente, o socket espera durante 500 milissegundos por mensagens do grupo de servidores. Cada identificador de servidor recebido é armazenado numa estrutura Stack. Após a expiração do tempo de recebimento, é gerada uma exceção e o programa volta ao fluxo de execução principal. 
  * O servidor verifica se deve ou não responder à requisição do cliente, avaliando se seu identificador é o menor dentre os armazenados na lista de identificadores recebidos. Caso seja o menor, o valor da expressão é calculado e o resultado é enviado diretamente ao cliente. Além disso, é exibido no terminal do servidor uma confirmação de que ele está respondendo a requisição. Caso o seu identificador não seja o menor, o servidor não calcula o resultado da expressão e tampouco envia algo para o cliente, ele apenas exibe no terminal que não deve responder à requisição
