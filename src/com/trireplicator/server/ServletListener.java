/**
 * Tri-Replicator Application
 * 
 * To learn more about the app, visit this blog:
 * http://kharkovski.blogspot.com/2013/01/tri-replicator-free-app-on-google-app.html
 * 
 *  @author Roman Kharkovski, http://kharkovski.blogspot.com
 *  Created: December 19, 2012
 */

package com.trireplicator.server;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.trireplicator.db.AdminEvents;
import com.trireplicator.db.DatabaseAccess;
import com.trireplicator.db.DatabaseException;
import com.trireplicator.shared.SecurityTools;
import com.trireplicator.shared.Utils;

/**
 * Initialize the application when the app is first (re) started
 * 
 * @author Roman Kharkovski
 */
public class ServletListener implements ServletContextListener {

	private static final Logger log = Utils.getLogger(ServletListener.class.getName());

	/**
	 * This will be invoked as part of a warmup request, or the first user
	 * request if no warmup request was invoked.
	 */
	public void contextInitialized(ServletContextEvent event) {
		log.info("---> ServletListener.contextInitialized() - started...");

		try {
			DatabaseAccess database = new DatabaseAccess();
			database.addAdminEvent(new AdminEvents("Server start", "Starting server..."));
		} catch (DatabaseException e) {
			log.severe("Error while initializing database access: " + e.getMessage());
			throw new RuntimeException("Error while initializing database access", e);
		} catch (Exception e) {
			log.severe("Error while initializing application: " + e.getMessage());
			throw new RuntimeException("Error while initializing database access", e);
		}

		try {
			// Initialize encryption library and provider
			SecurityTools.setup();
		} catch (Exception e) {
			log.severe("Error while initializing security for the application: " + e.getMessage());
			throw new RuntimeException("Error while initializing application security", e);
		}

		// Load proper config from whatever permanent storage - if there was no
		// storage defined, then default
		// configuration will be created
		ConfigMgr config = new ConfigMgr();
		config.load();
		// Now save the configuration into the database - this is useful if
		// there is no database setup when we run for
		// the first time ever
		config.save();

		// First we need to check if the deadline for replication has passed as
		// the challenge only runs till end of
		// winter. However we shall continue to run, but no replication will be
		// done
		if (config.isTodayPastDeadline()) {
			log.severe("It is now past the deadline for the USAT NCC challenge. Competition runs between "
					+ config.getConfig().getCompetitionStartDate() + " and "
					+ config.getConfig().getCompetitionEndDate()
					+ ". No replication will be done and the application will be available as read-only.");
		}

		log.info("<--- ServletListener.contextInitialized() - completed OK");
	}

	public void contextDestroyed(ServletContextEvent event) {
		log.info("---> contextDestroyed() - shutting down the server!");
		try {
			DatabaseAccess database = new DatabaseAccess();
			database.addAdminEvent(new AdminEvents("Server shutdown", "Shutting down server..."));
		} catch (DatabaseException e) {
			log.severe("Error while writing shutdown message into the server audit DB: " + e.getMessage());
		} catch (Exception e) {
			log.severe("Error while writing shutdown message into the server audit DB: " + e.getMessage());
		}
	}
}