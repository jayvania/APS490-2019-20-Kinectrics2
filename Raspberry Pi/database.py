import sqlite3
import datetime

verbose = False

#convert a datetime object to JSON-serializable form
def dateJSON(date):
   date_array = []
   date_array.append(date.year)
   date_array.append(date.month)
   date_array.append(date.day)
   date_array.append(date.hour)
   date_array.append(date.minute)
   date_array.append(date.second)
   return date_array

def initTable():

    conn = sqlite3.connect('sensor.db', detect_types = sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES)
    c = conn.cursor()
    
    #Create a table
    c.execute('''CREATE TABLE sensor_values (time timestamp, value real)''')
    
    #save (commit) changes
    conn.commit()
    
    conn.close()

    print("Successfully initialized the table.")

    
def add(value):

    conn = sqlite3.connect('sensor.db', detect_types = sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES)
    c = conn.cursor()
    
    #print("Connected to SQLite")
    
    insert_statement = """INSERT INTO 'sensor_values' ('time', 'value') VALUES (?, ?);"""
    
    now = datetime.datetime.now()
    data_tuple = (now, value)
    
    c.execute(insert_statement, data_tuple)
    
    conn.commit()

    if (verbose):
        print("Added value", value, "at", now)
    
    conn.close()

        
def dump():
    conn = sqlite3.connect('sensor.db', detect_types = sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES)
    c = conn.cursor()
    
    #print("Connected to SQLite")
    
    select_query = """SELECT * from sensor_values;"""
    
    c.execute(select_query)
    
    records = []
    for row in c:
        date = dateJSON(row[0])
        value = row[1]
        records.append( (date, value) )
        if (verbose):
            print (date, value)
    
    c.close()
    return records

def query(low):
    conn = sqlite3.connect('sensor.db', detect_types = sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES)
    c = conn.cursor()
    
    #print("Connected to SQLite")
    
    select_query = """SELECT * from sensor_values WHERE time >= ?;"""
    
    c.execute(select_query, (low,))
    
    records = []
    for row in c:
        date = dateJSON(row[0])
        value = row[1]
        records.append( (date, value) )
        if (verbose):
            print(date, value)
    
    c.close()
    return records

#a function to make getting the date easier
#will assume all values are the same as the current date, up until the first supplied value
#e.g. if day=13 is supplied, will return the 13th of the current month and year.
#calling without parameters will return the current day.
def getDate(year=0, month=0, day=0, hour=0, minute=0):
    now = datetime.datetime.now()
    if (year is not 0):
        if (month == 0): #default to December 31st
            month = 12
        if (day == 0):
            day = 31
        return datetime.datetime(year, month, day, hour, minute)
    elif (month is not 0):
        year = now.year

        #set the day if necessary
        if (day == 0):
            if (month in [1, 3, 5, 7, 8, 10, 12]):
                day = 31
            elif (month == 2): #January, potentially a leap year
                if (year % 4 == 0):
                    day = 29
                else:
                    day = 28
            else:
                day = 30

        return datetime.datetime(year, month, day, hour, minute)
    elif (day is not 0):
        year = now.year
        month = now.month
        return datetime.datetime(year, month, day, hour, minute)
    elif (hour is not 0):
        year = now.year
        month = now.month
        day = now.day
        return datetime.datetime(year, month, day, hour, minute)
    elif (minute is not 0):
        year = now.year
        month = now.month
        day = now.day
        hour = now.hour
        return datetime.datetime(year, month, day, hour, minute)
    else:
        return now

if __name__ == "__main__":
    initTable()
