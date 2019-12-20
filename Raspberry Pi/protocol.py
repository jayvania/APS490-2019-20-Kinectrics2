import json
import bluetooth

#used to add a newline to signify the end of a message
def addNewline(data):
    if (len(data) == 0):
        return data

    if (data[-1] != '\n'):
            data = data + '\n'
    return data

#used to remove the newline signifying the end of a message
def stripNewline(data):
    
    while (data[-1] == '\n'):
            data = data[0:-1]
    return data

#receive a JSON object over the socket
def receiveJSON(sock):
    data = ""
    while True:
        received = sock.recv(1024)
        data += received
        if (not received or (len(received) > 0 and received[-1]) == '\n'):
            break
    
    #print "Received: " + data
    data = stripNewline(data)
    
    return json.loads(data)

#send a JSON object over the socket
def sendJSON(sock, data):
    stringified = json.dumps(data)
    stringified = addNewline(stringified)
    
    #print "Sending: " + stringified
    sock.send(stringified)
    return
	
