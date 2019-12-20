#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Modified from PyBluez simple example rfcomm-client.py

Connects to a server, sends requests, and receives responses.

Original Sample Code Author: Albert Huang <albert@csail.mit.edu>
$Id: rfcomm-client.py 424 2006-08-24 03:35:54Z albert $
"""

import sys
import bluetooth
import protocol
import database

#verbose flag for extra info
verbose = False

def getIntegerFromUser(parameter):
	while True:
		try:
			value = int(input("Please enter the " + parameter + ": "))
		except:
			print("Please enter an integer.")
		else:
			return value	

def prettyDate(date):
    return str(date[0]) + "-" + str(date[1]) + "-" + str(date[2]) + " " + str(date[3]) + ":" + str(date[4]) + ":" + str(date[5])

def handleResponse(sock):
	response = protocol.receiveJSON(sock)
        if ("response" in response):
            res_type = response["response"]
            print "Received " + res_type + " response"
            if (res_type == "DATA"):
                for row in response["data"]:
                    print prettyDate(row[0]) + " - " + str(row[1])
            elif (res_type == "ERROR"):
                print response["error"]
            elif (res_type == "EXIT" and verbose):
                print "Received acknowledgement, exiting"
                sys.exit()
        print "--- End of response ---"
	

# Python 2 compatibility
try:
    input = raw_input
except NameError:
    pass  # Python 3

addr = None

if (len(sys.argv) > 1 and sys.argv[1] == "-v"):
    verbose = True
    print"Using verbose mode"

if len(sys.argv) < 2 or verbose:
    print("No device specified. Searching all nearby bluetooth devices for "
          "the SampleServer service...")
else:
    addr = sys.argv[1]
    print("Searching for SampleServer on {}...".format(addr))

# search for the SampleServer service
uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"
service_matches = bluetooth.find_service(uuid=uuid, address=addr)

if len(service_matches) == 0:
    print("Couldn't find the SampleServer service.")
    sys.exit(0)

first_match = service_matches[0]
port = first_match["port"]
name = first_match["name"]
host = first_match["host"]

print("Connecting to \"{}\" on {}".format(name, host))

# Create the client socket
sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
sock.connect((host, port))

print("Connected. Select a command:\n  DUMP\n  QUERY\n  END\n  HELP")
while True:
    command = input(">").strip().lower()
	
    #DUMP
    if (command == "dump"):
            request = {"request": "DUMP"}
            protocol.sendJSON(sock, request)
            
            handleResponse(sock)		
    
    elif (command == "query"):
            year = getIntegerFromUser("year")
            month = getIntegerFromUser("month")
            day = getIntegerFromUser("day")
            
            request = {	"request": "QUERY",
                                    "year": year,
                                    "month": month,
                                    "day": day
                                    }
            protocol.sendJSON(sock, request)
            
            handleResponse(sock)	
    
    elif (command == "end"):
            request = {"request": "END"}
            protocol.sendJSON(sock, request)
            
            handleResponse(sock)	
    
    elif (command == "help"):
            print("DUMP - causes the server to send over all data that it has, since the beginning of time.")
            print("  Warning! This may be a huge amount of data.")
            print("QUERY - causes the server to send over all data more recent than (inclusive) a specified year, month, and date")
            print("END - close the connection to the server")
            print("HELP - display this help section")
    
    else:
            print("Invalid command. Valid commands:\n\tDUMP\n\tQUERY\n\tEND\n\tHELP")	
    

sock.close()
