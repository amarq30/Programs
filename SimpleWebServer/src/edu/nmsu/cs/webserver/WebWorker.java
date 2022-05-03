package edu.nmsu.cs.webserver;

import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.*;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/
import java.io.*;

public class WebWorker implements Runnable
{

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{	
			String httpResponse = "501 Not Implemented";
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			File fileName = readHTTPRequest(is);
			if(fileName.exists()){
				httpResponse = "200 OK";
			}
			else{
				httpResponse = "404 Not Found";
			}
			writeHTTPHeader(os, "text/html", httpResponse);
			writeContent(os,fileName,"text/html",httpResponse);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private File readHTTPRequest(InputStream is)
	{
		String line;
		String readLine[] = new String[100];
		File wantedFile = null;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				readLine = line.split(" ");
				System.err.println("Request line: (" + line + ")");

				if (line.length() == 0)
					break;

				if(readLine[0].equals("GET")){
					if(readLine[1].equals("/")){
						wantedFile = new File("index.html");
					}
					else{
						wantedFile = new File(readLine[1].substring(1));
					}

				}//end if 
				System.out.println(" This is to debug " + wantedFile);
				
				return wantedFile;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
			
		}

		return wantedFile;
	}




	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, String httpReq) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		os.write(("HTTP/1.1 " + httpReq + "\n").getBytes());
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, File wFile, String cType, String httpRes) throws Exception
	{
		String line;
		String cTypeA[] = cType.split("/");
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		if(httpRes.equals("404 Not Found")){
			//This will return a 404 error for file not found
			os.write(httpRes.getBytes());
			return;
		}//end 404 check

		if(cTypeA[0].equals("text")){
			BufferedReader br = new BufferedReader(new FileReader(wFile));
			while((line = br.readLine()) != null){

				os.write(
					line.replace("<cs371server>", "Andres' Server").replace("<cs371date>", d.toString()).getBytes()
				);

			}
			br.close();
		}//end text file check
	}

} // end class
