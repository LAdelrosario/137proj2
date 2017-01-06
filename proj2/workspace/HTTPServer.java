
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class  HTTPServer extends Thread {
	
	static final String HTML_START = 
			"<html>" +
			"<title>HTTP Server in Java</title>" +
			"<body>";
			
    static final String HTML_END = 
			"</body>" +
			"</html>";
			
	Socket connectedClient = null;	
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;
	
			
	public HTTPServer(Socket client) {
		connectedClient = client;
	}			
			
	public void run() {
		
	  String currentLine = null, postBoundary = null, contentength = null, filename = null, contentLength = null;
	  PrintWriter fout = null;
		
	  try {
		
		System.out.println( "The Client "+
        connectedClient.getInetAddress() + ":" + connectedClient.getPort() + " is connected");
            
        inFromClient = new BufferedReader(new InputStreamReader (connectedClient.getInputStream()));                  
        outToClient = new DataOutputStream(connectedClient.getOutputStream());
      	currentLine = inFromClient.readLine();
        String headerLine = currentLine;            	
        StringTokenizer tokenizer = new StringTokenizer(headerLine);
		String httpMethod = tokenizer.nextToken();
		String httpQueryString = tokenizer.nextToken();
		System.out.println(currentLine);
		String fileName = "";
      
      	if (httpMethod.equals("POST")){ //POST request
			String keyVal = "";
			String previousLine = "";
		    System.out.println("POST Request"); 
			do {
				currentLine = inFromClient.readLine();		
			    if (currentLine.indexOf("Content-Type: multipart/form-data") != -1) {
				  String boundary = currentLine.split("boundary=")[1];//POST boundary			  			 
				  String parsedResult = "<table>";
				  while (true) {
				  	System.out.println(currentLine);
				  	if (currentLine.equals("--" + boundary + "--")) {
				  		parsedResult = parsedResult + "<td>" + previousLine + "</td></tr>";	//store prevline as value

				  		break;
				  	}
				  	else if(currentLine.equals("--" + boundary)){
				  		//store prevline as value
				  		parsedResult = parsedResult + "<td>" + previousLine + "</td></tr>";

				  	}
				  	else if(currentLine.indexOf("name") != -1){
				  		//keyVal
				  		keyVal = currentLine.split("\"")[1];
				  		parsedResult = parsedResult + "<tr> <td>" +keyVal + "</td>";
				  	}	
				  	previousLine = currentLine;			  		
				  	currentLine = inFromClient.readLine();
			      }
			      
			      System.out.println(parsedResult);
			      parsedResult = parsedResult  +"</table>";
			      sendResponse(200, parsedResult, false);
			      			   
				} //if							  				
			}while (inFromClient.ready()); //End of do-while
	  	}
        if (httpMethod.equals("GET")){    
        	System.out.println("GET Request");
        	String parsedResult = ""; 
			if (httpQueryString.equals("/")) {
				  fileName = "index.html";// The default home page				  			  
			}else {
                  fileName = httpQueryString.substring(1);	
                  if(fileName.indexOf("?") != -1){		//if there is ? found
					parsedResult = parsedResult + "<table>";
					String[] questionParts = fileName.split(Pattern.quote("?"));		//split by the encountered ?
					fileName = questionParts[0];	//get the file name
					String queryString = questionParts[1];	//query part
					String[] subqueryString = queryString.split("&");		//split by the & character
					for(String j: subqueryString){	
						parsedResult = parsedResult + "<tr>";
						String contents[] = j.split("=");
						for(String k: contents){
							parsedResult = parsedResult + "<td>"+ k +"</td>";	//put it inside an html table
						}
						parsedResult = parsedResult + "</tr>";
					}
					parsedResult = parsedResult + "</table>";
                  }	  
				}
				File file = new File(fileName);
  				String responseString = "";
  				String k ="";
				try {
					Scanner scanner = new Scanner(file);
				     while (scanner.hasNextLine()) {
				        responseString += scanner.nextLine();
				     }
				        responseString =responseString + parsedResult;
				        scanner.close();
				        sendResponse(200, responseString , false);
				    }catch (FileNotFoundException e) {
				    	if(e.toString().indexOf("Permission denied") != -1){
				        	responseString = HTML_START + "403 FORBIDDEN" + HTML_END;
				        	sendResponse(403, responseString , false);
				        }
				        else if(e.toString().indexOf("No such file or directory") != -1 ){
				        	responseString = HTML_START + "404 NOT FOUND" + HTML_END;
				        	sendResponse(404, responseString , false);
				        }
				 	}
		} 
	} catch (Exception e) {
			e.printStackTrace();
	}	
}
	
	public void sendResponse (int statusCode, String responseString, boolean isFile) throws Exception {
		String statusLine = null;
		String serverdetails = "Server: Java HTTPServer";
		String contentLengthLine = null;
		String fileName = null;		
		String contentTypeLine = "Content-Type: text/html" + "\r\n";
		FileInputStream fin = null;
		
		if (statusCode == 200)
			statusLine = "HTTP/1.1 200 OK" + "\r\n";
		else
			statusLine = "HTTP/1.1 404 Not Found" + "\r\n";	
			
		if (isFile) {
			fileName = responseString;			
			fin = new FileInputStream(fileName);
			contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";
			if (!fileName.endsWith(".htm") && !fileName.endsWith(".html"))
				contentTypeLine = "Content-Type: \r\n";	
		}						
		else {
			responseString = HTTPServer.HTML_START + responseString + HTTPServer.HTML_END;
			contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";	
		}					 
		outToClient.writeBytes(statusLine);
		outToClient.writeBytes(serverdetails);
		outToClient.writeBytes(contentTypeLine);
		outToClient.writeBytes(contentLengthLine);
		outToClient.writeBytes("Connection: close\r\n");
		outToClient.writeBytes("\r\n");		
		if (isFile) sendFile(fin, outToClient);
		else outToClient.writeBytes(responseString);
		outToClient.close();
	}
	
	public void sendFile (FileInputStream fin, DataOutputStream out) throws Exception {
		byte[] buffer = new byte[1024] ;
		int bytesRead;
		while ((bytesRead = fin.read(buffer)) != -1 ) {
			out.write(buffer, 0, bytesRead);
	    }
	    fin.close();
	}
			
	public static void main (String args[]) throws Exception {
		ServerSocket Server = new ServerSocket (Integer.parseInt(args[0]), 10);         
		System.out.println ("HTTP Server started on port :" + Integer.parseInt(args[0]));
		while(true){	                	   	      	
			Socket connected = Server.accept();
	        (new HTTPServer(connected)).start();
        }      
	}
}