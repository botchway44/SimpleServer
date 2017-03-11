/*
 * File: Request
 * -------------
 * This encapsulates a HTTP get request (which has a command and
 * parameters).
 */
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

public class Request {

	/* This is the string right after the HOST */
	private String command;
	
	/* Get requests map parameter-names to their values */
	private Map<String, String> params;
	
	/**
	 * Static Method: Decode
	 * --------------
	 * A public (static) method that takes in a String that has been
	 * UTF-8 encoded and decodes it. There is *no* error checking to
	 * make sure that the string had been encoded... if you call this
	 * on an unencoded string, functionality is undefined (and not good).
	 */
	public static String decode(String str) {
		try {
			return URLDecoder.decode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Static Method: Encode
	 * --------------
	 * A public (static) method that takes in a String and UTF-8 encodes it. 
	 * There is *no* error checking to make sure that the string has not 
	 * already been encoded. See the method above (Decode) for reversing this
	 * process.
	 */
	public static String encode(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Constructor: Request
	 * --------------------
	 * Makes a request, initially by setting its command. The parameters are saved
	 * as a TreeMap so that order or parameters is consistent.
	 */
	public Request(String cmd){
		if(!isValid(cmd)) throw new RuntimeException("The command " + cmd + " is not valid.");
		command = cmd;
		params = new TreeMap<String, String>();
	}

	/*** Some Getters and Setters ***/
	
	public String getCommand() {
		return command;
	}
	
	public String getParam(String key) {
		return decode(params.get(key));
	}
	
	public void addParam(String key, String value) {
		params.put(key, encode(value));
	}
	
	public void addRaw(String key, String value) {
		params.put(key, value);
	}
	
	public String getRaw(String key) {
		return params.get(key);
	}
	
	/**
	 * Method: toString
	 * --------------------
	 * Makes a request, initially by setting its command. The parameters are saved
	 * as a TreeMap so that order or parameters is consistent.
	 */
	@Overload
	public String toString() {
		String str = command + " (";
		boolean isFirst = true;
		for(String p : params.keySet()) {
			if(!isFirst) str += ", ";
			str += p + "=" + getParam(p); 
			isFirst = false;
		}
		str += ")";
		return str;
	}

	public String toGetRequest() {
		String getRequest = command;
		if(!params.isEmpty()) {
			boolean isFirst = true;
			for(String key : params.keySet()) {
				if(isFirst) {
					getRequest += "?" + key + "=" + params.get(key);
					isFirst = false;
				} else {
					getRequest += "&" + key + "=" + params.get(key);
				}
			}
		}
		return getRequest;
	}
	
	private boolean isValid(String cmd) {
		if(cmd.contains("?")) return false;
		if(cmd.contains(" ")) return false;
		return true;
	}
	
	
}