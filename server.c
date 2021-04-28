#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <time.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>

#define PORT_CLIENT 8888
#define PORT_SERVER_GROUP 4444
#define IP "224.2.2.3"
#define NUM_SERVERS 4

int servers_alive[NUM_SERVERS];

struct args_thread
{
    int *server_id;
    int sock;
    struct sockaddr_in addr;
};

void handle_server_sock(void *server_id);
void *server_send(void *arguments);
void *server_receive(void *arguments);

int main(int argc, int *argv[])
{
    struct sockaddr_in addr; // basic structures for all syscalls and functions that deal with internet addresses
    int addrlen, sock, cnt;
    struct ip_mreq mreq; // The ip_mreq structure provides multicast group information for IPv4 addresses.
    char message[50];
    pthread_t thread;
    int ts = 0;

    /********* set up socket *********/
    sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock < 0)
    {
        perror("socket");
        exit(1);
    }

    // The memset function places nbyte null bytes in the string s.
    // This function is used to set all the socket structures with null values.
    memset((char *)&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET; // Internet Protocol v4 addresses
    // addr.sin_addr.s_addr = htonl(INADDR_ANY);
    addr.sin_port = htons(PORT_CLIENT);
    addrlen = sizeof(addr);

    addr.sin_addr.s_addr = inet_addr(IP);
    /***********************************/

    // creating thread to handle communication between servers
    handle_server_sock(argv[1]);
    printf("1\n");

    // The bind() function shall assign a local socket address address to
    // a socket identified by descriptor socket that has no local socket address assigned
    if (bind(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
    {
        perror("bind");
        exit(1);
    }

    // The ip_mreq structure provides multicast group information for IPv4 addresses.
    mreq.imr_multiaddr.s_addr = inet_addr(IP);

    // The setsockopt() function shall set the option specified by the option_name argument, at the protocol level
    // specified by the level argument, to the value pointed to by the option_value argument for the socket
    // associated with the file descriptor specified by the socket argument.
    if (setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq)) < 0)
    {
        perror("setsockopt mreq");
        exit(1);
    }
    while (1)
    {
        cnt = recvfrom(sock, message, sizeof(message), 0, (struct sockaddr *)&addr, &addrlen);

        /* handle recvfrom return value */
        // Upon successful completion, recvfrom() shall return the length of the message in bytes. If no messages are available
        // to be received and the peer has performed an orderly shutdown, recvfrom() shall return 0.
        // Otherwise, the function shall return -1 and set errno to indicate the error.
        if (cnt < 0)
        {
            perror("recvfrom");
            exit(1);
        }
        else if (cnt == 0)
        {
            break;
        }

        int flag = 0;
        // handle servers_alive

        if (flag)
        {
            printf("Não devo responder \n");
        }
        else
        {
            // The function inet_ntoa() converts a network address in a struct in_addr to a dots-and-numbers format string.
            // The "n" in "ntoa" stands for network, and the "a" stands for ASCII for historical reasons
            // (so it's "Network To ASCII"--the "toa" suffix has an analogous friend in the C library called atoi()
            // which converts an ASCII string to an integer.)
            printf("%s: message = \"%s\"\n", inet_ntoa(addr.sin_addr), message);
        }
    }
}

void handle_server_sock(void *server_id)
{
    int id = atoi((char *)server_id);
    struct sockaddr_in addr;
    int addrlen, sock;
    struct ip_mreq mreq;

    // thread
    pthread_t thread_send, thread_receive;
    int t_send, t_receive;

    sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock < 0)
    {
        perror("socket");
        exit(1);
    }
    bzero((char *)&addr, sizeof(addr));
    addr.sin_family = AF_INET;
    // addr.sin_addr.s_addr = htonl(INADDR_ANY); // necessário?
    addr.sin_port = htons(PORT_SERVER_GROUP);
    addrlen = sizeof(addr);

    addr.sin_addr.s_addr = inet_addr(IP);

    if (bind(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
    {
        perror("bind");
        exit(1);
    }
    mreq.imr_multiaddr.s_addr = inet_addr(IP);
    mreq.imr_interface.s_addr = htonl(INADDR_ANY);
    if (setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP,
                   &mreq, sizeof(mreq)) < 0)
    {
        perror("setsockopt mreq");
        exit(1);
    }

    struct args_thread *args;
    // (args)->addr = addr;
    // (args)->server_id = server_id;
    // (args)->sock = sock;

    // t_send = pthread_create(&thread_send, NULL, server_send, (void *)args);
    // t_receive = pthread_create(&thread_receive, NULL, server_receive, (void *)args);
}

void *server_send(void *arguments)
{
    struct args_thread *args = arguments;
    int cnt;
    int addrlen = sizeof(args->addr);

    while (1)
    {
        cnt = sendto(args->sock, args->server_id, sizeof(args->server_id), 0,
                     (struct sockaddr *)&args->addr, addrlen);
        if (cnt < 0)
        {
            perror("sendto");
            exit(1);
        }

        // handle success

        sleep(2);
    }
}

void *server_receive(void *arguments)
{
    struct args_thread *args = arguments;
    int cnt;
    int addrlen = sizeof(args->addr);
    char message[50];

    while (1)
    {
        cnt = recvfrom(args->sock, message, sizeof(message), 0,
                       (struct sockaddr *)&args->addr, &addrlen);
        if (cnt < 0)
        {
            perror("recvfrom");
            exit(1);
        }
        else if (cnt == 0)
        {
            break;
        }
        printf("%s: resposta do servidor \"%s\"\n", inet_ntoa(args->addr.sin_addr), message);
    }
}