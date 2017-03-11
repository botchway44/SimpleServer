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
	private int port = 8080;

	// This is the student application that can respond to calls
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

	private Request makeRequest(String uri) {
		String[] requestParts = uri.split("\\?");
		String command = requestParts[0];
		Request request = new Request(command);
		if(requestParts.length == 2) {
			String paramStr = requestParts[1];
			String[] paramParts = paramStr.split("&");
			for(String paramPart : paramParts) {
				if(paramPart.split("=").length != 2) {
					return null;
				} else {
					String key = paramPart.split("=")[0];
					String value = paramPart.split("=")[1];
					request.addRaw(key, value);
				}
			}
		} 
		return request;
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
			server.createContext("/img", new BottleFileHandler());
			server.createContext("/images", new BottleFileHandler());
			server.createContext("/favicon.ico", new FaveIconHandler());
			server.createContext("/resources", new BottleFileHandler());
			server.createContext("/saveImage", new BottleImgReceiver());
			server.createContext("/", new BottleHandler());
			server.setExecutor(null); // creates a default executor
			server.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	//=--------------- Private -------------=//


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

	class FaveIconHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			try {
				File file = new File("images/faveicon.ico");
				exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, file.length());
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

	class BottleImgReceiver implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			try{
				String uriStr = getUriString(exchange);
				Request r = makeRequest(uriStr);
				String fileStr = r.getParam("file");
				byte [] bytes = Base64.getUrlDecoder().decode(fileStr);
				String fileName = r.getParam("fileName");
				
				Path path = Paths.get("images", fileName);
				FileOutputStream fos = new FileOutputStream(path.toString());
				fos.write(bytes);
				fos.close();

				String response = "success";
				exchange.sendResponseHeaders(200, response.length());
				exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
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
	class BottleFileHandler implements HttpHandler {
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
	class BottleHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
	
			try{
				t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
				t.getResponseHeaders().set("Content-Type", "text/plain");
				String uriStr = getUriString(t);
				
				// turn the uri into a request
				Request request = makeRequest(uriStr);
				
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


