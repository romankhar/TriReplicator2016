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

/**
 * Indicates that the user is not found in the system (in the repository of existing users)
 * 
 * @author Roman Kharkovski, http://kharkovski.blogspot.com
 * 
 */
public class UserNotFound extends Exception {

	private static final long serialVersionUID = -3340468946801978432L;

	public UserNotFound() {
		super();
	}

	public UserNotFound(String message, Throwable cause) {
		super(message, cause);
	}

	public UserNotFound(String message) {
		super(message);
	}

	public UserNotFound(Throwable cause) {
		super(cause);
	}
}
