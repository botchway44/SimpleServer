/*
 * File: SimpleServerListener
 * -------------
 * A student has to implement this interface in order for the server to be
 * able to call the callback method that they overload: requestMade
 */

public interface SimpleServerListener {

	String requestMade(Request request);
	
}
