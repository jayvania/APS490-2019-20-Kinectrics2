#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Modified from PyBluez simple example rfcomm-server.py

Listens for a connection, receives requests, and responds

Original Sample Code Author: Albert Huang <albert@csail.mit.edu>
$Id: rfcomm-server.py 518 2007-08-10 07:20:07Z albert $
"""

import bluetooth
import sys
import protocol
import database

#flag for printing extra info
verbose = False
if (len(sys.argv) > 1 and sys.argv[1] == "-v"):
    verbose = True
    print "Using verbose mode"

server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
server_sock.bind(("", bluetooth.PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

bluetooth.advertise_service(server_sock, "SampleServer", service_id=uuid,
                            service_classes=[uuid, bluetooth.SERIAL_PORT_CLASS],
                            profiles=[bluetooth.SERIAL_PORT_PROFILE],
                            # protocols=[bluetooth.OBEX_UUID]
                            )

print("Waiting for connection on RFCOMM channel", port)

client_sock, client_info = server_sock.accept()
print("Accepted connection from", client_info)

try:
    while True:
        #receive the request
        request = protocol.receiveJSON(client_sock)
        if (verbose):
            print "Received message:"
            print request
        response = {} #prepare the response object
        
        #parse the request
        if ("request" in request):
        
                #DUMP
                if (request["request"] == "DUMP"):
                    if (verbose):
                        print "Received DUMP request"
                    #dump the database and send back
                    data = database.dump()
                    response["response"] = "DATA"
                    response["data"] = data
                    if (verbose):
                        print "Sending response:"
                        print response
                
                #QUERY
                elif (request["request"] == "QUERY"):
                
                    #execute the query and send back
                    if (not "year" in request):
                            response["response"] = "ERROR"
                            response["error"] = "QUERY requires year parameter"
                            if (verbose):
                                print "Received malformed QUERY: no year"
                    elif (not "month" in request):
                            response["response"] = "ERROR"
                            response["error"] = "QUERY requires month parameter"
                            if (verbose):
                                print "Received malformed QUERY: no month"
                    elif (not "day" in request):
                            response["response"] = "ERROR"
                            response["error"] = "QUERY requires day parameter"
                            if (verbose):
                                print "Received malformed QUERY: no day"
                    else:
                            year = request["year"]
                            month = request["month"]
                            day = request["day"]
                            print "Received QUERY: " + str(year) + "-" + str(month) + "-" + str(day)
                            #non-integers in year, month, or day field
                            if (type(year) is not int or type(month) is not int or type(day) is not int):
                                    response["response"] = "ERROR"
                                    response["error"] = "year, month, and day parameters must be in integer form"
                            
                            #all good, make the query and send it
                            else:
                                    data = database.query(database.getDate(year, month, day))
                                    response["response"] = "DATA"
                                    response["data"] = data
                                    
                                    if (verbose):
                                        print "Sending response:"
                                        print response
                                        print ""
                
                #END
                elif (request["request"] == "END"):
                    if (verbose):
                        "Received END"
                    #send EXIT and close the connection
                    response["response"] = "EXIT"
                    
                    if (verbose):
                        print "Sending response:"
                        print response
                    
                    protocol.sendJSON(client_sock, response)
                    break;
                
                #invalid request
                else:
                    if (verbose):
                        print "Received invalid request: " + request["request"]
                    #send error message
                    response["response"] = "ERROR"
                    response["error"] = request["request"] + " is not a valid request"
        
        #request not in object
        else:
            if (verbose):
                print "Received message without request"
            #send error message
            response["response"] = "ERROR"
            response["error"] = "could not find request in message"
        
        #send the response over the air	
        protocol.sendJSON(client_sock, response)
except IOError:
    pass

print("Disconnected.")

client_sock.close()
server_sock.close()
print("All done.")
