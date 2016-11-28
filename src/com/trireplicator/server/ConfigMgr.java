package com.trireplicator.server;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.trireplicator.db.AppConfig;
import com.trireplicator.db.DatabaseAccess;
import com.trireplicator.db.DatabaseException;
import com.trireplicator.shared.TrainingLogException;
import com.trireplicator.shared.Utils;

/**
 * This class holds configuration for the server side logic
 * 
 * @author Roman Kharkovski
 */
public class ConfigMgr {

	public static final Logger log = Utils.getLogger(ConfigMgr.class.getName());

	/**
	 * Keep app configuration in this field
	 */
	private static AppConfig currentConfig = new AppConfig();

	/**
	 * Instance of the servlet to run scheduled replication. I put it into
	 * Servlet because otherwise JEE injection fails
	 */
	// private static ServletListener cron = new ServletListener();

	/**
	 * Get a copy of local configuration
	 * 
	 * @return
	 */
	public AppConfig getConfig() {
		return currentConfig;
	}

	public void setConfig(AppConfig currentConfig) {
		ConfigMgr.currentConfig = currentConfig;
	}

	/**
	 * Saves configuration information into some permanent storage
	 */
	public void save() {
		log.finest("---> Config.save()");

		// Mark the time when we are saving this config into the DB
		currentConfig.setLastUpdateDate(new Date(System.currentTimeMillis()));
		currentConfig.setAppVersion(Utils.VERSION);

		try {
			DatabaseAccess database;
			database = new DatabaseAccess();
			database.updateAppConfig(currentConfig);
			log.info("<--- Config.save()");
		} catch (DatabaseException e) {
			log.severe(e.getMessage());
		}
	}

	/**
	 * Loads configuration information from some permanent storage. Additionally
	 * schedule new replication interval
	 */
	public void load() {
		log.fine("---> Config.load()");
		try {
			DatabaseAccess database;
			database = new DatabaseAccess();
			currentConfig = database.loadConfig();
			log.finest("Config.load() - loaded configuration from DB");
		} catch (DatabaseException e) {
			log.severe(e.getMessage());
		}
		currentConfig.setLastLoadTime(new Date(System.currentTimeMillis()));

		// Start background thread of replication per config interval
		scheduleRegularReplication();
		log.info("<--- Config.load()");
	}

	private void scheduleRegularReplication() {
		Runnable replicationTask = new Runnable() {
			public void run() {
				try {
					log.info("Starting regular replication...");
					// At a regular interval we will run replication for all
					// users
					new SynchronizerServiceImpl().replicateWorkoutsForAllUsers();
					log.info("Scheduled replication is complete");
				} catch (TrainingLogException e) {
					log.severe(e.getMessage());
				}
			}
		};

		ManagedScheduledExecutorService executor;
		try {
			executor = (ManagedScheduledExecutorService) new InitialContext()
					.lookup("java:comp/DefaultManagedScheduledExecutorService");
			if (executor != null) {
				log.fine("Preparing to schedule regular replication");
				// First we need to stop all previous scheduled replications
				shutdownAndAwaitTermination(executor);
				// Then we can start a new schedule
				executor.scheduleAtFixedRate(replicationTask, 1, getConfig().getReplicationFrequencyMinutes(),
						TimeUnit.MINUTES); 
				log.info("Scheduled regular replication with the interval of "
						+ getConfig().getReplicationFrequencyMinutes() + " minutes");
			} else
				log.severe("!!!!! Unable to schedule regular replication because executor = null");
		} catch (NamingException e) {
			log.severe("Error looking up executor service: "+e.getMessage());
			log.severe("!!!!!!!!!!! Fix the problem or perform MANUAL replication every once in a while !!!!!!!!!");
		}

	}

	/**
	 * The following method shuts down an ExecutorService in two phases, first
	 * by calling shutdown to reject incoming tasks, and then calling
	 * shutdownNow, if necessary, to cancel any lingering tasks Taken from:
	 * http://docs.oracle.com/javase/7/docs/api/?java/util/concurrent/ExecutorService.html
	 * 
	 * @param pool
	 */
	void shutdownAndAwaitTermination(ManagedScheduledExecutorService pool) {
		try {
			// TODO - for some reason this never works properly and always
			// returns
			// java.lang.UnsupportedOperationException: shutdown
			// this results in many parallel executors firing up while there
			// should only be one
			pool.shutdown(); // Disable new tasks from being submitted
		} catch (IllegalStateException e) {
			log.finest("------- Appears there has not been anything scheduled yet - no shutdown was needed: "
					+ e.getMessage());
			// return;
		}
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(5, TimeUnit.SECONDS))
					log.finest("--------------Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.fine("------ Exception caught while shutting down service executor - " + e.getMessage() + " details="
					+ e.toString());
		}
	}

	/**
	 * This check if we are past end of the USAT challenge
	 * 
	 * @return True if we are past the deadline, False if we are good to go
	 */
	public boolean isTodayPastDeadline() {
		Date today = new Date();
		log.finest("Today's date = " + today.toString() + ". Last day to replicate workouts = "
				+ currentConfig.getCompetitionEndDate());
		if (today.after(currentConfig.toDate(currentConfig.getCompetitionEndDate()))) {
			// if (today.after(lastDayOfChallengeUpdates.getTime())) {
			// We are not yet past the deadline for updates to USAT site
			log.finest("We are now past the deadline, should not proceed with normal business.");
			return true;
		}
		// We are now past the last day of allowed updates to the USAT site -
		// can not do any replication after this date
		log.finest("We are not past the deadline, so can proceed as normal");
		return false;
	}

	public static AppConfig generateDefaultConfiguration() {
		AppConfig newConfig = new AppConfig();
		newConfig.setEnvironmentName("DEVELOPMENT");
		newConfig.setDatePattern(Utils.DATE_PATTERN);
		newConfig.setCompetitionStartDate(Utils.START_DATE);
		newConfig.setCompetitionEndDate(Utils.END_DATE);
		newConfig.setReplicationFrequencyMinutes(60);
		newConfig.setConfigurationReloadMinutes(600);
		newConfig.setMaxDaysBack(14);
		newConfig.setAppVersion(Utils.VERSION);
		newConfig.setTotalWorkoutsPriorYears(13249);
		return newConfig;
	}
}