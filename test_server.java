import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.zip.GZIPOutputStream;


public class test_server extends Thread {
	
	private ServerSocket serverSocket;
	BufferedReader inFromClient;
	DataOutputStream outToClient;
	boolean running=true;
	Socket server;
	static String encoding;
	static String request_line;
	public test_server (int port) throws IOException
	{
		serverSocket=new ServerSocket(port);
	}
	
	public void run()
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				try
				{
					running=false;
					serverSocket.close();
					if(server!=null)
					{
						server.close();
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}				
		});
		
		while (running)
		{
			try
			{
				System.out.println("Waiting for client on port "+serverSocket.getLocalPort());
				server=serverSocket.accept();
				System.out.println("Connected to client on "+server.getRemoteSocketAddress());
				
				inFromClient=new BufferedReader(new InputStreamReader(server.getInputStream()));
				outToClient=new DataOutputStream(server.getOutputStream());
				
				String request;
				String http_request="";
				String[] request_line=new String[10];
				
				while (!(request=inFromClient.readLine()).equals(""))
				{
					http_request=http_request+request+"\n";
				}
				
				request_line=http_request.split("\n");
				//System.out.println("1st line of request="+request_line[0]);
				String first_line_of_request=request_line[0];
				String[] partition_request = new String [10];
				test_server.request_line=request_line[0];
				if (request_line[4]!=null)
				{
					String encoding_line=request_line[4];
					test_server.encoding=encoding_line;
				
					System.out.println("encoding-line="+encoding_line);
				}
				http_request=first_line_of_request;
				http_request=URLDecoder.decode(first_line_of_request,"UTF-8");
				System.out.println("http_request="+http_request);
				partition_request=http_request.split("\\s+");
				String http_request_type=partition_request[0];
				String http_command=partition_request[1];
				String http_protocol=partition_request[2];
				String command;
				String[] command_array=new String[20];
				command_array=http_command.split("exec/");
				boolean flag=true;
				String[] curl_command=new String[10];
				String[] curl_request=new String[10];
				curl_request=http_request.split("\\s+");
				curl_command=curl_request[1].split("exec/");
				String curl="";
				int length_for_curl=partition_request.length;
				
				if (length_for_curl==4)
				{
					curl=curl_command[1]+" "+curl_request[2];
				}
				
				if (http_request.contains("HTTP/1.1"))
				{
					if (http_request.contains("GET"))
					{
						if(http_request.contains("/exec/"))
						{
							command_array=http_command.split("exec/");
							
							if(command_array.length!=1)
							{
								command=command_array[1];
								//command=URLDecoder.decode(command,"UTF-8");								
							
								if (length_for_curl==4)
								{
									flag=false;
									sendResponse(200,curl);
								}
							
								if(flag==true)
								{
									sendResponse(200,command);
								}
							}
							else
							{
								sendResponse(200,"");
							}
						}
						
						else
						{
							sendResponse(404,"");
						}
					}
					
					else
					{
						sendResponse(404,"");
					}
				}
				
				else 
				{
					sendResponse(404,"");
				}
				
				
				inFromClient.close();
			}
			
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String [] args)
	   {
		  //int port=9999;
	      int port = Integer.parseInt(args[0]);
	      try
	      {
	         Thread t = new test_server(port);
	         t.start();
	      }
	      catch(IOException e)
	      {
	         e.printStackTrace();
	      }
	   }
	
	public void sendResponse(int responseCode, String command_to_execute) throws IOException
	{
		String output_of_shell_command="";
		String status_for_200="HTTP/1.1 200 OK\r\n";
		String status_for_404="HTTP/1.1 404 Not Found\r\n";
		
		if (responseCode==200)
		{
			output_of_shell_command=execute_shell_command(command_to_execute);
			sendResponse_to_browser(output_of_shell_command,status_for_200);
		}
		
		if (responseCode==404)
		{
			output_of_shell_command="";
			sendResponse_to_browser(output_of_shell_command,status_for_404);
		}
		
		
	}
	
	public String execute_shell_command (String command_to_execute) throws IOException
	{
		String output="";
		System.out.println("command to execute="+command_to_execute);
		try
		{
			ProcessBuilder builder = new ProcessBuilder (new String[] {"bash","-c",command_to_execute});
			builder.directory(new File("."));
			final Process process= builder.start();
			InputStream is=process.getInputStream();
			InputStreamReader isr=new InputStreamReader(is);	
			BufferedReader br=new BufferedReader (isr);
			String line;
		
			while ((line=br.readLine())!=null)
			{
				output=output+line+"\n";
			}
			//System.out.println("output="+output);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
			return output;
	}
	
	public void sendResponse_to_browser(String output_to_browser, String statusLine_fromFunction) throws IOException
	{
		String response="";
		if (test_server.encoding.contains("Accept-Encoding:gzip"))
		{
			String gzip_output_to_browser=perform_gzip(output_to_browser);
			String encoding="gzip";
			String statusLine=statusLine_fromFunction;
			String contentType="Content-Type: text\r\n";
			String serverInfo="Server: Java HTTP Server\r\n";
			String contentLength_gzip="Content-Length: "+gzip_output_to_browser.length()+"\r\n";
			String contentEncoding="Content-Encoding: "+encoding+"\r\n";
			String connection="Connection: close\r\n";
			response=statusLine+contentEncoding+contentType+contentLength_gzip+serverInfo+connection+"\r\n"+gzip_output_to_browser;
			System.out.println("output_to_browser="+gzip_output_to_browser);
		}
		else
		{		
			String statusLine=statusLine_fromFunction;
			String contentType="Content-Type: text/plain\r\n";
			String serverInfo="Server: Java HTTP Server\r\n";
			String contentLength="Content-Length: "+output_to_browser.length()+"\r\n";
			String connection="Connection: close\r\n";		
			response=statusLine+contentType+contentLength+serverInfo+connection+"\r\n"+output_to_browser;
			System.out.println("output_to_browser="+output_to_browser);
		}
		
		outToClient.writeBytes(response);
		
	}
	
	public String perform_gzip(String output) throws IOException
	{
		ByteArrayOutputStream bout=null;
		try
		{
			bout=new ByteArrayOutputStream();
			GZIPOutputStream gzip=new GZIPOutputStream(bout);
			gzip.write(output.getBytes());
			gzip.flush();
			gzip.close();
			bout.close();			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			return (new String(bout.toByteArray(),"UTF-8"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
	}
}
