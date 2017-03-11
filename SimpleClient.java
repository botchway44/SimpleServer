/* 
 * File: FacePamphlet.java
 * -----------------------
 * When it is finished, this program will implement a basic social network
 * management system.
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

	private static final String ERROR_KEY = "Error";

	public static GImage getImage(String host, String fileName) {
		try {
			if(!host.endsWith("/")) {
				host = host + "/";
			}
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
		if(!host.endsWith("/")) {
			host = host + "/";
		}
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
	
	public static String makeRequest(String host, Request request) throws IOException {
		if(!host.endsWith("/")) {
			host = host + "/";
		}
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



	

	private static String sanitizeErrorMsg(String fromServer) {
		String msg = fromServer.substring(ERROR_KEY.length());
		while(startsWithPunctuation(msg)) {
			msg = msg.substring(1);
		}
		return msg.trim();
	}

	private static boolean startsWithPunctuation(String msg) {
		if(msg.isEmpty()) return false;
		return !Character.isLetterOrDigit(msg.charAt(0));
	}

}
