/* 
 * File: SimpleClient.java
 * -----------------------
 * This class provides static methods to make it easier to write a "client" program.
 * It exposes three public methods:
 * 	makeRequest
 *  saveImage
 *  getImage
 * Which all send data to a server computer.
 * Written by Chris Piech (piech@cs.stanford.edu)
 */

import acm.graphics.*;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;

import javax.imageio.ImageIO;

public class SimpleClient {

	/* If a server response starts with this magical string, I throw an exception */
	private static final String ERROR_KEY = "Error";
	
	/**
	 * Method: Make Request
	 * ------------------
	 * This method nicely packages the few unsightly details in making an HTTP get request.
	 * Before it hands the server's response back to the user, it checks if the server's
	 * response starts with the ERROR_KEY which is how CS106A students have a server return
	 * an error.
	 */
	public static String makeRequest(String host, Request request) throws IOException {
		host = sanitizeHost(host);
		try{
			String fromServer = makeGetRequest(host, request);
			
			// By protocol, if the server response starts with Error then I throw an
			// IO Exception (see the FacePamphlet handout)
			if(fromServer.startsWith(ERROR_KEY)) {
				 String msg = sanitizeErrorMsg(fromServer);
				throw new IOException(msg.trim());
			}
			
			// Else, just return the result
			return fromServer;
		} catch (ConnectException e) {
			throw new ConnectException("Unable to connect to the server. Did you start it?");
		}
	}
	

	/**
	 * Method: Load Image
	 * ------------------
	 * This method leverages Image's ability to load directly from a URL. It seems like
	 * this should not be necessary, since GImage can also load from host. But we have
	 * found out that GImage doesn't seem to be able to load from localhost with a port,
	 * whereas Java image can.
	 */
	public static GImage getImage(String host, String fileName) {
		try {
			host = sanitizeHost(host);
			URL url = new URL(host + "images/" +fileName);
			Image image = ImageIO.read(url);
			return new GImage(image);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Method: Save Image
	 * ------------------
	 * This method saves an image using a get request! to a server. I rethought this
	 * method after starter code was released to the students. I think that the right
	 * model would be to have the client send a GImage (scaled down somehow) and then
	 * have the server chose a fileName and return that back to the user. Otherwise you
	 * run the risk of overwritting someone elses image -- bad times.
	 * Also in the future I would look into doing this without a get request :-).
	 */
	public static String saveImage(String host, String fileName, int width, int height) {
		host = sanitizeHost(host);
		try{
			byte[] bytes = getImageByteArray(fileName, width, height);
			String contents = Base64.getUrlEncoder().encodeToString(bytes);
					
			Request r = new Request("saveImage");
			r.addParam("file", "fileName");
			r.addParam("file", contents);
			
			return makeGetRequest(host, r);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	

	/**
	 * Make Get Request
	 * -------------
	 * This is the most important helper method. It takes in a host string 
	 * (eg http://localhost:8000/ or http://cba9ae21.ngrok.io/) and a Request
	 * object. It makes the request as a GET request and returns the result.
	 */
	private static String makeGetRequest(String host, Request r) throws IOException{
		URL destination = new URL(host + r.toGetRequest());
		HttpURLConnection conn = (HttpURLConnection) destination.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = null;
		StringBuilder result = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		String fromServer = result.toString();
		rd.close();
		return fromServer;
	}

	/**
	 * Get Image Byte Array
	 * -------------
	 * Takes in an image (defined by a fileName) resizes it, and pushes it through jpeg compression. 
	 * The method then returns the resulting byte array which is ready to be send to the server.
	 */
	private static byte[] getImageByteArray(String fileName, int width, int height) throws IOException {
		GImage toSend = new GImage(fileName);
		Image original = toSend.getImage();
		BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = newImage.createGraphics();
		g.drawImage(original, 0, 0, width, height, null);
		g.dispose();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(newImage, "jpg", byteArrayOutputStream);
		byte[] bytes = byteArrayOutputStream.toByteArray();
		return bytes;
	}

	/**
	 * Sanitize Error Msg
	 * -------------
	 * This method takes in a server "error message" (eg one which starts with Error)
	 * and extracts the message after the ERROR_KEY. Student's could format the Error
	 * string in many ways, and I wanted to be robust to the different approaches. To
	 * do so I make sure to remove all punctuation between the ERROR_KEY and the student
	 * message (so they could send "Error: <msg>" or "Error; <msg>" or "Error <msg>")
	 */
	private static String sanitizeErrorMsg(String fromServer) {
		String msg = fromServer.substring(ERROR_KEY.length());
		while(startsWithPunctuation(msg)) {
			msg = msg.substring(1);
		}
		return msg.trim();
	}

	/**
	 * Starts with punctuation
	 * -------------
	 * We define punctuation to be anything that is not a character or a digit :-).
	 */
	private static boolean startsWithPunctuation(String msg) {
		if(msg.isEmpty()) return false;
		return !Character.isLetterOrDigit(msg.charAt(0));
	}
	
	/**
	 * Sanitize Host
	 * -------------
	 * Many of the public methods generate get requests by appending the host name to the
	 * get request string. This method standardizes the host name so that it ends with a 
	 * '/'.
	 */
	private static String sanitizeHost(String host) {
		if(!host.endsWith("/")) {
			host = host + "/";
		}
		return host;
	}

}
