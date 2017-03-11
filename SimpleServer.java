import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import acm.util.RandomGenerator;

/**
 * Class: Bottle
 * -------------
 * This is the interesting class. The one that actually hosts the server and
 * the functionality that I want recreated in a CS106 library.
 */
public class SimpleServer {

	// Port 8000 is a good call for security reasons.
	private int port = 8000;

	// This is the student application that can respond to GET requests
	private SimpleServerListener webApp;
	
	/**
	 * Method: Constructor
	 * -------------------
	 * All this method does is create an object that could be used to handle
	 * HTTP requests (but at the time of construction does nothing).
	 */
	public SimpleServer(SimpleServerListener webApp, int port) {
		this.webApp = webApp;
		this.port = port;
	}

	public SimpleServer(SimpleServerListener webApp) {
		this.webApp = webApp;
	}
	
	/**
	 * Method: Start
	 * -------------
	 * This method starts a server on the given port. It is hard coded to handle
	 * img requests and resource requests specially by just reading the files and
	 * returning them. All other requests should be handled by the users code.
	 */
	public void start() {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext("/img", new ServerFileHandler());
			server.createContext("/images", new ServerFileHandler());
			server.createContext("/favicon.ico", new FaveIconHandler());
			server.createContext("/resources", new ServerFileHandler());
			server.createContext("/saveImage", new ServerImageReceiver());
			server.createContext("/", new GetRequestHandler());
			server.setExecutor(null); // creates a default executor
			server.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/* **********************************************************************************
	 * Private helper methods 
	 ************************************************************************************/
	
	/**
	 * Method: Make Request
	 * -------------
	 * This is a factory method which takes in a GET request string and turns it into a 
	 * "Request" object (the main abstraction that we use in CS106A for sending requests).
	 * Uses the standard GET protocol <cmd>?param1=val1&param2=val2 etc
	 */
	private Request constructRequest(String getRequestString) {
		String[] requestParts = getRequestString.split("\\?");
		String command = requestParts[0];
		Request request = new Request(command);
		if(requestParts.length == 2) {
			String paramStr = requestParts[1];
			String[] paramParts = paramStr.split("&");
			for(String paramPart : paramParts) {
				String key = paramPart.split("=")[0];
				String value = "";
				if(paramPart.split("=").length == 2) {
					value = paramPart.split("=")[1];
				} 
				request.addRaw(key, value);
			}
		} 
		return request;
	}

	/**
	 * Method: Get URI String
	 * -----------------------
	 * Gets the part of the http GET request after localhost:8000.
	 */
	private static String getUriString(HttpExchange exchange) {
		URI uri = exchange.getRequestURI();
		String uriStr = uri.toString();
		uriStr = uriStr.substring(1);
		return uriStr;
	}
	
	/**
	 * Method: Make Standard Response
	 * -----------------------
	 * All the responses in the HTTP server allow remote origin, are text, and return response
	 * 200 (success). If the server wants to return an error it returns a string that starts
	 * with "Error".
	 */
	private void makeStandardExchange(HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().set("Content-Type", "text/plain");
	}

	/**
	 * Handlers: FaveIconHandler
	 * -----------------------
	 * If there is a faveicon.ico in the images directory, we return that given
	 * an faveicon request.
	 */
	class FaveIconHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			try {
				File file = new File("images/faveicon.ico");
				makeStandardExchange(exchange);
				exchange.sendResponseHeaders(200, file.length());
				OutputStream outputStream=exchange.getResponseBody();
				Files.copy(file.toPath(), outputStream);
				outputStream.close();
			} catch(IOException e) {
				e.printStackTrace();
			} 
		}
	}

	/**
	 * Handlers: ServerImageReceiver
	 * -----------------------
	 * The SimpleClient / SimpleServer have a protocol for sending images. This
	 * method handles an image sent over by a SimpleClient. It saves it into the 
	 * images dir (which allows for easy access by the acm library GImage). I
	 * wrote a version of this handler that I preferred, where it generated a random
	 * filename for the image, saved the image with that file name and then returned
	 * the filename to the client. 
	 * 
	 * IN THE FUTURE: get the image, and make a filename using a hash of its pixels
	 * values :-)
	 */
	class ServerImageReceiver implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			try{
				String uriStr = getUriString(exchange);
				Request r = constructRequest(uriStr);
				String fileStr = r.getParam("file");
				byte [] bytes = Base64.getUrlDecoder().decode(fileStr);
				String fileName = r.getParam("fileName");
				Path path = Paths.get("images", fileName);
				FileOutputStream fos = new FileOutputStream(path.toString());
				fos.write(bytes);
				fos.close();

				String response = "success";
				makeStandardExchange(exchange);
				exchange.sendResponseHeaders(200, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} catch(IOException e) {
				e.printStackTrace();
			} catch(RuntimeException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		private String choseFileName() {
			RandomGenerator rg = new RandomGenerator();
			String alphabet = "";
			for(int i = 0; i < 26; i++) {
				alphabet += (char)('a' + i);
				alphabet += (char)('A' + i);
			}
			for(int i = 0; i < 10; i++) {
				alphabet += i;
			}
			String fileName = "";
			while(true) {
				for(int i = 0; i < 20; i++) {
					int index = rg.nextInt(alphabet.length());
					char ch = alphabet.charAt(index);
					fileName += ch;
				}
				fileName += ".jpg";
				Path path = Paths.get("images", fileName);
				File tmpFile = path.toFile();
				boolean exists = tmpFile.exists();
				if(!exists) {
					return fileName;
				}
			}
		}
	}

	/**
	 * Class: BottleFileHandler
	 * -----------------------
	 * For some files (eg images and resources) you want the server to 
	 * simply return the file. This handler simply returns the file.
	 */
	class ServerFileHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			try{
				String uriStr = getUriString(exchange);
				System.out.println(uriStr);
				File file = new File(uriStr);
				exchange.sendResponseHeaders(200, file.length());
				exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
				OutputStream outputStream=exchange.getResponseBody();
				Files.copy(file.toPath(), outputStream);
				outputStream.close();
			} catch(IOException e) {
				e.printStackTrace();
			} catch(RuntimeException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Class: BottleHandler
	 * -----------------------
	 * This class passes on an HTTP request to the "webApp" which the
	 * user writes.
	 */
	class GetRequestHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
	
			try{
				t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				t.getResponseHeaders().set("Content-Type", "text/plain");
				String uriStr = getUriString(t);
				
				// turn the uri into a request
				Request request = constructRequest(uriStr);
				
				if(request == null) {
					throw new IOException("Malformed request " + uriStr);
				}
				
				// call the students method
				String response = webApp.requestMade(request);
				
				// pass the response back to the caller
				if(response == null) {
					throw new RuntimeException("Server request returned null.");
				}
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				t.getResponseHeaders().set("Content-Type", "text/plain");
				os.write(response.getBytes());
				os.close();
			} catch(IOException e) {
				e.printStackTrace();
			} catch(RuntimeException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

	}

}


