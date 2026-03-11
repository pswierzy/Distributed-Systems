import socket;

serverIP = "127.0.0.1"
serverPort = 9011
msg_bytes = (300).to_bytes(4, byteorder='big')


print('PYTHON UDP CLIENT')
client = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
client.sendto(msg_bytes, (serverIP, serverPort))


serverPort = 9012
serverSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
serverSocket.bind(('', serverPort))
buff = []
buff, address = serverSocket.recvfrom(1024)
buff = int.from_bytes(buff, byteorder='big')
print("python udp server received msg: ", buff)