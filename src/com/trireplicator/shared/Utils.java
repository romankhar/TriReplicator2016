/**
 * Tri-Replicator Application
 * 
 * To learn more about the app, visit this blog:
 * http://kharkovski.blogspot.com/2013/01/tri-replicator-free-app-on-google-app.html
 * 
 *  @author Roman Kharkovski, http://kharkovski.blogspot.com
 *  Created: December 19, 2012
 */

package com.trireplicator.shared;

import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

/**
 * Useful stuff for client and server
 * 
 * @author Roman Kharkovski, http://kharkovski.blogspot.com
 */
public class Utils {

	/**
	 * Version number of the software
	 */

	public static final String VERSION = "2.22";
	/**
	 * Name of the software package
	 */
	public static final String APP_NAME = "Tri-Replicator (https://tri-replicator.mybluemix.net)";

	/**
	 * Default competition start and end dates
	 */
	public static final String DATE_PATTERN = "MM/dd/yyyy";
	public static final String START_DATE = "08/15/2016";
	public static final String END_DATE = "03/15/2017";

	/**
	 * Log level for entire application THIS IS NOT USED - see server.xml
	 * <logging> instead
	 */
	// public static Level LOG_LEVEL = Level.ALL;

	/**
	 * This can be used to subtract one day from System.currentTimeMillis()
	 */
	public static long ONE_DAY = 1000 * 60 * 60 * 24;
	public static long ONE_HOUR = 1000 * 60 * 60;

	/**
	 * Tomorrow date
	 * 
	 * @return date of tomorrow in long format
	 */

	public static long tomorrow() {
		return System.currentTimeMillis() + ONE_DAY;
	}

	public static long today() {
		return System.currentTimeMillis();
	}

	/**
	 * Converts meters to yards
	 * 
	 * @param meters
	 * @return yards
	 */
	public static Double meters2Yards(Double meters) {
		if (meters == null)
			return new Double(0.0);

		return meters / 0.9144;
	}

	public static long approximateMeters2Yards(Double meters) {
		return meters2Yards(meters).longValue();
	}

	public static double yards2miles(Double yards) {
		return yards / 1760;
	}

	public static long oneDayAgo() {
		return System.currentTimeMillis() - ONE_DAY;
	}

	public static long twoDaysAgo() {
		return System.currentTimeMillis() - ONE_DAY * 2;
	}

	public static long hourAgo() {
		return System.currentTimeMillis() - ONE_HOUR;
	}

	public static long fefteenDaysAgo() {
		return System.currentTimeMillis() - ONE_DAY * 15;
	}

	public static long oneDayAhead() {
		return System.currentTimeMillis() + ONE_DAY;
	}

	public static long fewDaysAgo(int days) {
		return System.currentTimeMillis() - ONE_DAY * days;
	}

	/**
	 * Used by the security encryption to do Base64 encoding so that encrypted
	 * data can be later serialized and stored in the DB, etc.
	 * 
	 * @param input
	 * @return Base64 encoded representation of the input
	 */
	public static String encodeByteArrayIntoString(byte[] input) {
		return DatatypeConverter.printBase64Binary(input);
	}

	/**
	 * Used by the security encryption to do Base64 de-coding
	 * 
	 * @param input
	 * @return Base64 de-coded representation of the input
	 */
	public static byte[] decodeStringIntoByteArray(String input) {
		return DatatypeConverter.parseBase64Binary(input);
	}

	public static Logger getLogger(String name) {
		Logger newlog = Logger.getLogger(name);
		// ---------------------- THIS IS NOT USED - see server.xml <logging>
		// instead
		// ConsoleHandler handler = new ConsoleHandler();
		// handler.setLevel(LOG_LEVEL);
		// newlog.addHandler(handler);
		return newlog;
	}

}