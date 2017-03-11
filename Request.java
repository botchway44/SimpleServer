
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

public class Request {

	private String command;
	private Map<String, String> params;
	
	public static String decode(String str) {
		try {
			return URLDecoder.decode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String encode(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Request(String cmd){
		if(!isValid(cmd)) throw new RuntimeException("The command " + cmd + " is not valid.");
		command = cmd;
		params = new TreeMap<String, String>();
	}

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