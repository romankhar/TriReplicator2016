/**
 * Tri-Replicator Application 
 * 
 * To learn more about the app, visit this blog:
 * http://kharkovski.blogspot.com/2013/01/tri-replicator-free-app-on-google-app.html
 * 
 *  @author Roman Kharkovski, http://kharkovski.blogspot.com
 *  Created: December 19, 2012
 */

package com.trireplicator.secrets;

/**
 * This class should NOT be in the public version control system as it has all
 * kinds of private user account names, passwords, keys, etc. This is designed
 * to prevent secret information getting into the public domain.
 */
public class Constants {

	/**
	 * USAT account data for testing
	 */
	public static String USER_USAT = "1234567890";
	public static String PASSWORD_USAT = "usatpassword";

	/**
	 * Trainingpeaks.com account data for testing
	 */
	public static String USER_TRAININGPEAKS = "tpuser";
	public static String PASSWORD_TRAININGPEAKS = "tppassword";

	/**
	 * Pass phrase for encryption of user data
	 */
	public static String PASSPHRASE = "";

	/**
	 * Key for encryption of user data
	 */
	public static String SECRET_KEY = "1238762429872340";

	public static String CIPHER_INIT_STRING = "DES/ECB/PKCS5Padding";
	public static String KEY_ALGORYTHM = "DES";
	public static int KEY_SIZE = 56;
}