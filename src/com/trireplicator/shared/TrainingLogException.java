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

public class TrainingLogException extends Exception {

	private static final long serialVersionUID = -5344433421735470539L;

	public TrainingLogException() {
		super();
	}

	public TrainingLogException(String message, Throwable cause) {
		super(message, cause);
	}

	public TrainingLogException(String message) {
		super(message);
	}

	public TrainingLogException(Throwable cause) {
		super(cause);
	}
}
