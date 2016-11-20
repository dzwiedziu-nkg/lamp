void setup()
{
  Serial.begin(115200); // for communication with ESP

  pinMode(13, OUTPUT);   // LOW - on, HIGH - off
  digitalWrite(13, LOW); // switch on when start initialization

  // Initialize ESP
  sendData("AT+RST\r\n", 2000);             // reset
  sendData("AT+CIPMUX=1\r\n", 1000);        // multi connection mode
  sendData("AT+CIPSERVER=1,80\r\n", 1000);  // run server on 80 port

  digitalWrite(13, HIGH); // switch off when initialization done
}

void loop()
{
  if (Serial.available()) // have a data from ESP
  {
    if (Serial.find("+IPD,")) // have a data from client
    {
      delay(1000); // waiting for receive whole input transmission

      // get connection ID
      int connectionId = Serial.read() - 48; // parse ASCII digit

      Serial.find("cmd="); // move cursor to command phrase "cmd=" 

      int cmd = (Serial.read() - 48); // parse command ID

      switch(cmd) {
        case 0: // switch off command
          digitalWrite(13, HIGH);
          break;

        case 1: // switch on command
          digitalWrite(13, LOW);
          break;
      }

      int state = digitalRead(13); // get current state, LOW - on, HIGH - off

      // make HTTP response
      String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\nContent-Length: 3\r\n\r\n";
      response += state;
      response += "\r\n";

      // make ESP command for send data to client
      String cipSend = "AT+CIPSEND=";
      cipSend += connectionId;
      cipSend += ",";
      cipSend +=response.length(); // we must known HTTP response length before sent ESP command
      cipSend +="\r\n";

      // send ESP command for data transmission to client
      sendData(cipSend,1000);

      // send data (HTTP response)
      sendData(response, 1000);
      
      // close connection with client
      String closeCommand = "AT+CIPCLOSE=";
      closeCommand += connectionId;
      closeCommand += "\r\n";

      sendData(closeCommand, 1000);
    }
  }
}

/*
  Send command to ESP
  Params:
  command - command with all parameters
  timeout - waiting for response
*/
String sendData(String command, const int timeout)
{
  String response = "";

  Serial.print(command); // wysłanie polecenia do ESP01

  long int time = millis();

  while ( (time + timeout) > millis()) // when timeout was not reached
  {
    while (Serial.available()) // and when data is available
    {
      char c = Serial.read(); // then read next char from ESP
      response += c;
    }
  }

  return response;
}

