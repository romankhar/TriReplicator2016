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

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.trireplicator.db.AdminEvents;
import com.trireplicator.db.DatabaseAccess;
import com.trireplicator.db.DatabaseException;
import com.trireplicator.db.User;
import com.trireplicator.db.Workout;
import com.trireplicator.shared.SynchronizerService;
import com.trireplicator.shared.TrainingLogException;
import com.trireplicator.shared.Utils;
import com.trireplicator.shared.WorkoutSession;
import com.trireplicator.shared.WorkoutSession.WorkoutType;

/**
 * Admin servlet is not accessible to anyone, but administrator of the system.
 * Provides browser based UI to do admin tasks (delete users, etc.)
 * 
 * @author Roman Kharkovski, http://kharkovski.blogspot.com
 */
@SuppressWarnings("serial")
public class AdminServlet extends HttpServlet {
	private static final Logger log = Utils.getLogger(AdminServlet.class.getName());

	// /**
	// * Background task to delete data with delay offset
	// */
	// @Resource(lookup = "concurrent/DeletionExecutor")
	// private ManagedScheduledExecutorService deletionExecutor;
	//
	/**
	 * All data deletion commands will be delayed by this much
	 */
	private static int deletionDelayMin = 10;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		String action = null;
		String defaultAction = "view_stats";
		if (req != null) {
			action = (String) req.getParameter("action");
		}
		if (action == null)
			action = defaultAction;
		;
		log.info("ACTION=" + action);
		printHeader(resp);

		try {
			switch (action) {
			case "end2end":
				end2end(resp);
				break;
			case "basicTest":
				doBasicTest(resp);
				break;
			case "Update":
				updateConfig(req, resp);
				break;
			case "view_memory_config":
				viewInMemoryConfig(resp);
				break;
			case "view_and_reload_config_from_db":
				viewAndReloadInDatabaseConfig(resp);
				break;
			case "replicateAllWorkouts":
				replicateAllWorkouts(resp);
				break;
			case "view_stats":
				viewStats(resp);
				break;
			case "view_detailed_stats":
				viewDetailedStats(resp);
				break;
			case "delete_one_user":
				deleteOneUser(req, resp);
				break;
			case "delete_users":
				deleteAllUsers(resp);
				break;
			case "delete_workouts":
				deleteAllWorkouts(resp);
				break;
			case "delete_admin_events":
				deleteAllAdminEvents(resp);
				break;
			case "updateUserAccessTime":
				updateUserAccessTime(resp);
				break;
			default:
				String msg = "Servlet ACTION='" + action + "' - is not yet implemented.";
				resp.getWriter().println(msg);
				log.info(msg);
				break;
			}
		} catch (Exception e) {
			resp.getWriter().println("Program error occured: " + e.getMessage());
			log.severe(e.getMessage());
		}
	}

	private void updateUserAccessTime(HttpServletResponse resp) throws DatabaseException, IOException {
		DatabaseAccess database = new DatabaseAccess();
		Long userId = new Long(7);
		database.updateUserAccessTime(userId);
		resp.getWriter().println("Access time has been updated for userId=" + userId);
	}

	private void deleteOneUser(HttpServletRequest req, HttpServletResponse resp) throws DatabaseException, IOException {
		DatabaseAccess database = new DatabaseAccess();
		String nameTP = req.getParameter("nameTP");
		String nameUSAT = req.getParameter("nameUSAT");
		int i = database.removeUser(nameTP, nameUSAT);

		resp.getWriter()
				.println(i + " users have been removed successfully: nameTP=" + nameTP + " nameUSAT=" + nameUSAT);
	}

	/**
	 * Show current configuration as it is kept in memory
	 * 
	 * @param resp
	 * @throws IOException
	 */
	private void viewInMemoryConfig(HttpServletResponse resp) throws IOException {
		ConfigMgr configMgr = new ConfigMgr();
		resp.getWriter().println(configMgr.getConfig().toString());
	}

	/**
	 * Load the config from the database and show it
	 * 
	 * @param resp
	 * @throws IOException
	 */
	private void viewAndReloadInDatabaseConfig(HttpServletResponse resp) throws IOException {
		ConfigMgr configMgr = new ConfigMgr();
		configMgr.load();
		resp.getWriter().println(configMgr.getConfig().toString());
	}

	private void updateConfig(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		ConfigMgr configMgr = new ConfigMgr();
		configMgr.getConfig().setEnvironmentName(req.getParameter("environmentName"));
		configMgr.getConfig().setCompetitionStartDate(req.getParameter("start_date"));
		configMgr.getConfig().setCompetitionEndDate(req.getParameter("end_date"));
		configMgr.getConfig().setDatePattern(req.getParameter("datePattern"));
		configMgr.getConfig().setConfigurationReloadMinutes(Integer.parseInt(req.getParameter("reloadInterval")));
		configMgr.getConfig()
				.setReplicationFrequencyMinutes(Integer.parseInt(req.getParameter("replicationFrequency")));
		configMgr.getConfig().setMaxDaysBack(Integer.parseInt(req.getParameter("days_back_for_entering")));
		configMgr.getConfig().setTotalWorkoutsPriorYears(Integer.parseInt(req.getParameter("totalWorkoutsPriorYears")));
		configMgr.save();

		resp.getWriter().println("Application configuration has been updated. Here is your new configuration:");
		viewAndReloadInDatabaseConfig(resp);
	}

	private void viewStats(HttpServletResponse resp) throws DatabaseException, IOException {
		DatabaseAccess database = new DatabaseAccess();
		resp.getWriter().println("Total number of registered users: " + database.getNumOfUsers() + "</br>");
		resp.getWriter().println("Total number of replicated workouts: " + database.getNumOfWorkouts());
	}

	private void viewDetailedStats(HttpServletResponse resp) throws DatabaseException, IOException {
		DatabaseAccess database = new DatabaseAccess();

		// Print all users
		List<User> allUsers = database.listUsers();
		if (allUsers != null) {
			resp.getWriter().println(
					"<h2>There are a total of <b>" + allUsers.size() + "</b> users in the database: </h2><br>");
			Iterator<User> iterator = allUsers.iterator();
			while (iterator.hasNext()) {
				User user = iterator.next();
				resp.getWriter().println(user.toString() + "<br>");
			}
		}

		// Print all workouts
		List<Workout> allWorkouts = database.listWorkouts();
		if (allWorkouts != null) {
			resp.getWriter().println(
					"<h2>There are a total of <b>" + allWorkouts.size() + "</b> workouts in the database: </h2><br>");
			Iterator<Workout> iterator = allWorkouts.iterator();
			while (iterator.hasNext()) {
				Workout workout = iterator.next();
				resp.getWriter().println(workout.toString() + "<br>");
			}
		}

		// Print admin events
		List<AdminEvents> allEvents = database.listAdminEvents();
		if (allEvents != null) {
			resp.getWriter().println("<br><h2>There are a total of <b>" + allEvents.size()
					+ "</b> administrative events in the database: </h2><br>");
			Iterator<AdminEvents> iterator = allEvents.iterator();
			while (iterator.hasNext()) {
				AdminEvents event = iterator.next();
				resp.getWriter().println(event.toString() + "<br>");
			}
		}
	}

	/**
	 * This will check if user can login into USAT and try the replication
	 * 
	 * @param resp
	 */
	private void end2end(HttpServletResponse resp) {
		log.finest("Starting 'end2end'...");
		log.warning("-----------------");
		SynchronizerService server = new SynchronizerServiceImpl();

		@SuppressWarnings("unused")
		Date startDate = new Date();
		@SuppressWarnings("unused")
		Date endDate = new Date();

		String nameUSAT = com.trireplicator.secrets.Constants.USER_USAT;
		String passwordUSAT = com.trireplicator.secrets.Constants.PASSWORD_USAT;
		String nameTP = com.trireplicator.secrets.Constants.USER_TRAININGPEAKS;
		String passwordTP = com.trireplicator.secrets.Constants.PASSWORD_TRAININGPEAKS;

		// Delete existing user
		try {
			if (server.removeUserWithCount(nameTP, passwordTP, nameUSAT, passwordUSAT) > 0) {
				log.info("--- user(s) has been deleted");
			} else {
				log.info("--- user has not been deleted, perhaps it did not exist in the system");
			}
		} catch (TrainingLogException e1) {
			e1.printStackTrace();
			log.info("Error adding user");
		}

		// Add new active user
		try {
			// Test for existing user in the local database
			boolean loginResult = server.checkExistingUser(nameTP, passwordTP, nameUSAT, passwordUSAT);
			log.finest("--- login result = " + loginResult);

			// Now add new user and check if he can remotely login into USAT and
			// tp sites
			if (server.addUser(nameTP, passwordTP, nameUSAT, passwordUSAT)) {
				log.info("--- new user has been added");
			} else {
				log.info("--- new user has not been added");
			}
		} catch (TrainingLogException e) {
			e.printStackTrace();
			log.info("Error adding user");
		}

		// Try replication for one user
		try {
			server.replicateWorkoutsForUser(new Long(0), nameTP, passwordTP, nameUSAT, passwordUSAT, startDate,
					endDate);
			log.finest("Finished replicating workouts for one user");
		} catch (TrainingLogException e) {
			e.printStackTrace();
			log.finest("Error replicating workouts for one user");
		}

		// Try replicating workouts for all users
		// try {
		// server.replicateWorkoutsForAllUsers();
		// log.info("Finished replicating workouts for all users");
		// } catch (TrainingLogException e) {
		// log.finest( "Error replicating workouts for all users");
		// }

	}

	private void doBasicTest(HttpServletResponse resp) throws IOException, DatabaseException {
		log.finest("================================ basic TEST");
		Workout workout;
		User user;

		user = new User();
		String name = "Peter" + Math.random();
		user.setNameUSAT("USAT-" + name);
		user.setPlainPasswordUSAT("USAT-password");
		user.setNameTP("TP-" + name);
		user.setPlainPasswordTP("TP-password" + System.currentTimeMillis());
		user.setRegistrationDate(new Date());
		user.setLastVisitDate(new Date());
		user.setActive(true);

		DatabaseAccess database = new DatabaseAccess();
		List<User> allUsers = null;
		database.addUser(user);
		allUsers = database.listUsers();

		if (allUsers != null) {
			resp.getWriter().println("Hello, JPA. We have " + allUsers.size() + " number of entries:<br>");
			Iterator<User> iterator = allUsers.iterator();
			while (iterator.hasNext()) {
				User userTT = iterator.next();
				resp.getWriter().println(userTT.toString() + "<br>");

				workout = new Workout(new WorkoutSession(WorkoutType.Swim, "My swim for  user " + userTT.getUserId(),
						new Date(), 1000), userTT.getUserId());
				database.addWorkout(workout);

				workout = new Workout(new WorkoutSession(WorkoutType.Bike, "My bike for user " + userTT.getUserId(),
						new Date(), 10000), userTT.getUserId());
				database.addWorkout(workout);

				// Now print all workouts for this user
				List<Workout> workoutsList = database.findWorkoutsForUser(userTT.getUserId());
				Iterator<Workout> workoutIterator = workoutsList.iterator();
				while (workoutIterator.hasNext()) {
					Workout userWorkout = (Workout) workoutIterator.next();
					resp.getWriter().println(userWorkout.toString() + "<br>");
				}
			}
		} else {
			resp.getWriter().println("Should not happen");
		}
	}

	private void replicateAllWorkouts(HttpServletResponse resp) throws IOException, TrainingLogException {
		resp.getWriter().println(new SynchronizerServiceImpl().replicateWorkoutsForAllUsers());
	}

	private void scheduleDeletion(Runnable task, HttpServletResponse resp) throws IOException {

		try {
			ManagedScheduledExecutorService deletionExecutor;
			deletionExecutor = (ManagedScheduledExecutorService) new InitialContext()
					.lookup("java:comp/DefaultManagedScheduledExecutorService");
			if (deletionExecutor != null) {
				log.fine("---> Preparing to schedule deletion");
				deletionExecutor.schedule(task, deletionDelayMin, TimeUnit.MINUTES);
				log.severe("Scheduled deletion in " + deletionDelayMin + " minutes !!!!!!!!!!!!!!!!!");
				resp.getWriter().println("<span style='font-size:20pt; color:red'>Your data will be deleted in "
						+ deletionDelayMin
						+ "minutes. If this is production system you still have time to login into server and shut it down!!!!!!!!!</span>");
			} else
				log.severe("!!!!! Unable to schedule regular replication because executor = null");
		} catch (NamingException e) {
			log.severe("Error looking up executor service: " + e.getMessage());
		}

	}

	private void deleteAllUsers(HttpServletResponse resp) throws IOException, DatabaseException {
		Runnable task = new Runnable() {
			public void run() {
				try {
					new DatabaseAccess().deleteAllUsers();
				} catch (DatabaseException e) {
					log.severe(e.getMessage());
				}
			}
		};
		scheduleDeletion(task, resp);
	}

	private void deleteAllWorkouts(HttpServletResponse resp) throws IOException, DatabaseException {
		Runnable task = new Runnable() {
			public void run() {
				try {
					new DatabaseAccess().deleteAllWorkouts();
				} catch (DatabaseException e) {
					log.severe(e.getMessage());
				}
			}
		};
		scheduleDeletion(task, resp);
	}

	private void deleteAllAdminEvents(HttpServletResponse resp) throws IOException, DatabaseException {
		Runnable task = new Runnable() {
			public void run() {
				try {
					new DatabaseAccess().deleteAllAdminEvents();
				} catch (DatabaseException e) {
					log.severe(e.getMessage());
				}
			}
		};
		scheduleDeletion(task, resp);
	}

	private void printHeader(HttpServletResponse resp) {
		try {
			resp.getWriter().println("<h1>Application Name: " + Utils.APP_NAME + "</h1><p>Version: " + Utils.VERSION
					+ "</br>" + "Current date: " + new Date().toString() + "</p>");
		} catch (IOException e) {
		}
	}
}