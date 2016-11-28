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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.trireplicator.db.AdminEvents;
import com.trireplicator.db.AppConfig;
import com.trireplicator.db.DatabaseAccess;
import com.trireplicator.db.DatabaseException;
import com.trireplicator.db.User;
import com.trireplicator.db.Workout;
import com.trireplicator.shared.MoreThanOneUserFound;
import com.trireplicator.shared.SynchronizerService;
import com.trireplicator.shared.SystemStatusDTO;
import com.trireplicator.shared.TrainingLogException;
import com.trireplicator.shared.UserNotFound;
import com.trireplicator.shared.Utils;
import com.trireplicator.shared.WorkoutSession;
import com.trireplicator.trainingpeaks.TrainingPeaksClient;
import com.trireplicator.usat.USATclient;

/**
 * This class implements main server logic of the entire application It gets
 * called via remote interfaces by GWT clients, etc.
 * 
 * @author Roman Kharkovski, http://kharkovski.blogspot.com
 */
@SuppressWarnings("serial")
public class SynchronizerServiceImpl extends RemoteServiceServlet implements SynchronizerService {

	private static final Logger log = Utils.getLogger(SynchronizerServiceImpl.class.getName());

	@Override
	public int replicateWorkoutsForUser(Long userId, String nameTP, String passwordTP, String nameUSAT,
			String passwordUSAT, Date startDate, Date endDate) throws TrainingLogException {

		List<WorkoutSession> workoutsAdded = replicateWorkoutsForUserWithList(userId, nameTP, passwordTP, nameUSAT,
				passwordUSAT, startDate, endDate);
		if (workoutsAdded == null) {
			return 0;
		} else {
			return workoutsAdded.size();
		}
	}

	public List<WorkoutSession> replicateWorkoutsForUserWithList(Long userId, String nameTP, String passwordTP,
			String nameUSAT, String passwordUSAT, Date startDate, Date endDate) throws TrainingLogException {
		List<WorkoutSession> workoutsAdded = null;
		/*
		 * String info = "nameTP='" + nameTP + "' nameUSAT='" + nameUSAT +
		 * "' startDate=" + startDate.toString() + "' endDate='" +
		 * endDate.toString() + "'"; try { new
		 * DatabaseAccess().addAdminEvent(new
		 * AdminEvents("replicateWorkoutsForUser", info)); } catch
		 * (DatabaseException e) { log.
		 * severe("---- Could not add admin event to the database for auditing: "
		 * + info); log.severe(e.toString()); }
		 */

		log.finest("----------------------- First we need to get workouts from Trainingpeaks");
		List<WorkoutSession> workoutsFromTP = null;

		TrainingPeaksClient tpClient = new TrainingPeaksClient();
		try {
			tpClient.setupTrainingLog(nameTP, passwordTP);
			workoutsFromTP = tpClient.getWorkoutsBetweenDates(startDate, endDate);
			log.finest("Workouts obtained from TP server: " + Debug.workoutsToString(workoutsFromTP));
		} catch (TrainingLogException e) {
			e.printStackTrace();
			String error = "Could not get the list of workouts from a Trainingpeaks.com server for user: " + nameTP;
			log.warning(error);
			return null;
		}

		log.finest(
				"----------------------- Remove workouts that have already been replicated by looking at replication history");
		List<WorkoutSession> filteredWorkoutsFromTP = filterOutAlreadyReplicatedWorkouts(userId, nameTP, passwordTP,
				nameUSAT, passwordUSAT, workoutsFromTP);

		log.finest("----------------------- Now add those workouts to the USAT site");
		USATclient usatClient = new USATclient();
		try {
			usatClient.setupTrainingLog(nameUSAT, passwordUSAT);
			workoutsAdded = usatClient.addWorkouts(filteredWorkoutsFromTP);
		} catch (Exception e) {
			e.printStackTrace();
			String error = "Could not add workouts to USAT server";
			log.info(error);
			return null;
		}
		log.finest("The following workouts were added to the USAT site: ");
		log.finest(Debug.workoutsToString(workoutsAdded));

		log.finest("----------------------- Add workouts to the database for future reference");
		saveWorkouts(userId, nameTP, passwordTP, nameUSAT, passwordUSAT, workoutsAdded);
		return workoutsAdded;
	}

	@Override
	public int replicateWorkoutsForAllUsers() throws TrainingLogException {
		log.fine("---> replicateWorkoutsForAllUsers()");
		int count = 0;
		long methodStartTime = System.currentTimeMillis();

		// First we need to check if the deadline for replication has passed as
		// the challenge only runs till end of
		// winter. However we shall continue to run, but no replication will be
		// done
		ConfigMgr configMgr = new ConfigMgr();
		if (configMgr.isTodayPastDeadline()) {
			log.severe(
					"(WARNING) replicateWorkoutsForAllUsers()-> It is now past the deadline for the USAT NCC challenge. Competition runs between "
							+ configMgr.getConfig().getCompetitionStartDate() + " and "
							+ configMgr.getConfig().getCompetitionEndDate() + " timestamp="
							+ new Date(System.currentTimeMillis()));
			return 0;
		}

		// The date of the erliest workout depends on the date of the
		// competition start and also how many days back the
		// entry is allowed
		// Plus none of the workouts before the user registration date can be
		// replicated
		Date earliestDateOfWorkout = replicationStartDate();

		DatabaseAccess database;
		try {
			database = new DatabaseAccess();
		} catch (DatabaseException e) {
			throw new TrainingLogException(e.getMessage());
		}

		// Get the list of all users from the DB
		Iterator<User> userIterator;
		try {
			userIterator = database.listUsers().iterator();
		} catch (DatabaseException e) {
			throw new TrainingLogException(e.getMessage());
		}

		// Now for every user in the database we will get his or her workouts
		while (userIterator.hasNext()) {
			User user = (User) userIterator.next();
			// This will cause replication from the date of the registration
			// until today
			// Already replicated workouts will be filtered out in the process,
			// so no worries
			log.finest("---------- Version:" + Utils.VERSION + ": replicateWorkoutsFor all users - found user: id='"
					+ user.getUserId() + "' nameTP='" + user.getNameTP() + "' nameUSAT='" + user.getNameUSAT()+"'");

			// We are not allowed to automatically replicate anything that is
			// earlier than the user registration date
			// New version replicates only as NCC allows it to do
			List<WorkoutSession> workouts = replicateWorkoutsForUserWithList(user.getUserId(), user.getNameTP(),
					user.getPlainPasswordTP(), user.getNameUSAT(), user.getPlainPasswordUSAT(),
					earliestDateOfWorkout.after(user.getRegistrationDate()) ? earliestDateOfWorkout
							: user.getRegistrationDate(),
					new Date());

			if ((workouts != null) && (workouts.size() > 0)) {
				log.finest("Replication for user '" + user.getNameTP() + "' completed successfully with "
						+ workouts.size() + " workouts replicated");
				count++;
			} else {
				log.finest("Replication for user '" + user.getNameTP()
						+ "' DID NOT complete successfully. Perhaps there were no new workouts to be added");
			}
		}

		// Since all is done, return the number of users for whom the workouts
		// were replicated
		long replicationTimeSecs = new Double((System.currentTimeMillis() - methodStartTime) / 1000).longValue();
		String adminInfo = "replicateWorkoutsForAllUsers(): Finished replicating all user workouts for " + count
				+ " users. It took " + replicationTimeSecs + " seconds to complete";
		log.info(adminInfo);
		try {
			new DatabaseAccess().addAdminEvent(new AdminEvents("Full replication completed", adminInfo));
		} catch (DatabaseException e) {
			log.severe("Can not write into the audit database after replication was done: " + e.getMessage());
		}
		
		log.fine("<--- replicateWorkoutsForAllUsers()");
		return count;
	}

	/**
	 * This method calculates the earliest date for replication of workouts -
	 * either the start of competition period or about a week ago from today
	 * 
	 * @return Date
	 */
	private Date replicationStartDate() {
		ConfigMgr configMgr = new ConfigMgr();
		// This date is several days back when NCC site allows the replication
		// to happen
		Date fewDaysAgo = new Date(Utils.fewDaysAgo(configMgr.getConfig().getMaxDaysBack()));
		Date competitionStart = configMgr.getConfig().toDate(configMgr.getConfig().getCompetitionStartDate());
		if (competitionStart == null) {
			// If competitionStart date was invalid - we shall return the date
			// of few days ago
			return fewDaysAgo;
		}
		return (fewDaysAgo.after(competitionStart)) ? fewDaysAgo : competitionStart;
	}

	/**
	 * This method looks into the local database of workouts that have been
	 * already replicated in the past and removes them from the output It also
	 * removes workouts that wont need to be copied into USAT site (such as all
	 * non-bike, non-swim, non-run
	 * 
	 * @param userId
	 *            - if it is != 0, then we will use it to lookup DB, otherwise
	 *            will use names and passwords below and incur additional
	 *            database lookup access
	 * @param nameTP
	 * @param passwordTP
	 * @param nameUSAT
	 * @param passwordUSAT
	 * @param workouts
	 * @return
	 * @throws TrainingLogException
	 */
	private List<WorkoutSession> filterOutAlreadyReplicatedWorkouts(Long userId, String nameTP, String passwordTP,
			String nameUSAT, String passwordUSAT, List<WorkoutSession> workouts) throws TrainingLogException {
		log.finest("---filterOutAlreadyReplicatedWorkouts()");
		List<WorkoutSession> filteredWorkouts = new ArrayList<WorkoutSession>();
		if ((workouts == null) || (workouts.size() == 0)) {
			// There is nothing to do if there are no workouts
			return filteredWorkouts;
		}

		// First we need to remove workouts that are not swim, bike or run
		// Also remove all invalid workouts that are not subject to replication
		List<WorkoutSession> properWorkouts = new ArrayList<WorkoutSession>();
		Iterator<WorkoutSession> iterator = workouts.iterator();
		while (iterator.hasNext()) {
			WorkoutSession session = (WorkoutSession) iterator.next();
			if (!session.isSubjectToReplication()) {
				// Do nothing as this workout wont be needed for replication
				// into the USAT site anyway
				log.finest("This kind of workout does NOT need to be replicated: '" + session.toString() + ".'");
			} else {
				log.finest("This kind of workout DOES need to be replicated: '" + session.toString() + ".'");
				properWorkouts.add(session);
			}
		}
		// If there is nothing to check against the database, lets just return
		// empty list
		if (properWorkouts.size() == 0) {
			log.finest("No workouts of proper type found in the input list, nothing to replicate then");
			return filteredWorkouts;
		}

		DatabaseAccess database = getDatabase();

		// In case the userId was passed down to this method, we dont need to
		// lookup the database for it
		if ((userId == null) || (userId == 0)) {
			try {
				userId = database.findUser(nameTP, passwordTP, nameUSAT, passwordUSAT, true);
			} catch (MoreThanOneUserFound | UserNotFound e) {
				e.printStackTrace();
				log.severe(
						"Workouts were found in the TrainingPeaks, but user is not in the database. This should never happen");
				return filteredWorkouts;
			} catch (DatabaseException e) {
				e.printStackTrace();
				log.severe("Database error while finding users:" + e.getMessage());
				throw new TrainingLogException(e.getMessage());
			}
		}

		// Now search our local database to remove all workouts from the list
		// passed into this function that are in the
		// DB, which means they have already been replicated earlier
		Iterator<WorkoutSession> iterator1 = properWorkouts.iterator();
		while (iterator1.hasNext()) {
			WorkoutSession session = (WorkoutSession) iterator1.next();
			Long workoutId;
			try {
				workoutId = database.findWorkout(new Workout(session, userId));
			} catch (DatabaseException e) {
				e.printStackTrace();
				log.severe("Database error while finding workouts:" + e.getMessage());
				throw new TrainingLogException(e.getMessage());
			}
			if (workoutId.longValue() != 0) {
				// Do nothing as this workout has already been replicated
				// earlier
				log.finest("Workout was already replicated: " + session.toString());
			} else {
				// The workout is not recorded in the local DB, so lets
				// replicate it
				log.finest("Workout was not yet replicated: " + session.toString());
				filteredWorkouts.add(session);
			}
		}

		return filteredWorkouts;
	}

	private void saveWorkouts(Long userId, String nameTP, String passwordTP, String nameUSAT, String passwordUSAT,
			List<WorkoutSession> workoutsAdded) throws TrainingLogException {
		log.finest("--- saveWorkouts()");
		if ((workoutsAdded == null) || (workoutsAdded.size() == 0)) {
			// There is nothing to do if there are no workouts
			log.finest("no workouts need to be added");
			return;
		}
		DatabaseAccess database = getDatabase();

		// If userId was passed to this method, we do not need to read it from
		// the database
		if ((userId == null) || (userId == 0)) {
			try {
				userId = database.findUser(nameTP, passwordTP, nameUSAT, passwordUSAT, true);
			} catch (MoreThanOneUserFound | UserNotFound e) {
				e.printStackTrace();
				log.severe(
						"Workouts were found and replicated, but could not be saved in the database. This should never happen");
				return;
			} catch (DatabaseException e) {
				e.printStackTrace();
				log.severe("Database error while finding users:" + e.getMessage());
				throw new TrainingLogException(e.getMessage());
			}
		}
		Iterator<WorkoutSession> iterator = workoutsAdded.iterator();
		while (iterator.hasNext()) {
			try {
				database.addWorkout(new Workout((WorkoutSession) iterator.next(), userId));
			} catch (DatabaseException e) {
				e.printStackTrace();
				log.severe("Database error while adding workout:" + e.getMessage());
				throw new TrainingLogException(e.getMessage());
			}
		}
	}

	@Override
	public boolean addUser(String nameTP, String passwordTP, String nameUSAT, String passwordUSAT)
			throws TrainingLogException {
		User user = new User();

		// Before we add new user, lets check if we can actually login into USAT site
		if (!checkUSATLogin(nameUSAT, passwordUSAT)) {
			log.finest(
					"Unable to login user '" + nameUSAT + "' into the USAT site and therefore can not add new user.");
			return false;
		}

		// Before we add new user, lets check if we can actually login into TP site
		if (!checkTPLogin(nameTP, passwordTP)) {
			log.finest("Unable to login user '" + nameTP
					+ "' into the Trainingpeaks site and therefore can not add new user.");
			return false;
		}

		DatabaseAccess database = getDatabase();

		// By default new users are created active
		user.setActive(true);
		user.setLastVisitDate(new Date());
		user.setRegistrationDate(new Date());
		user.setNameTP(nameTP);
		user.setPlainPasswordTP(passwordTP);
		user.setNameUSAT(nameUSAT);
		user.setPlainPasswordUSAT(passwordUSAT);

		try {
			return database.addUser(user);
		} catch (DatabaseException e) {
			throw new TrainingLogException(e);
		}
	}

	@Override
	public boolean checkExistingUser(String nameTP, String passwordTP, String nameUSAT, String passwordUSAT)
			throws TrainingLogException {

		DatabaseAccess database = getDatabase();
		try {
			database.addAdminEvent(
					new AdminEvents("Check Existing User", "nameTP='" + nameTP + "' nameUSAT='" + nameUSAT + "'"));
			return database.checkExistingUser(escapeHtml(nameUSAT), escapeHtml(passwordUSAT), escapeHtml(nameTP),
					escapeHtml(passwordTP));
		} catch (DatabaseException e) {
			e.printStackTrace();
			log.severe("Database error while adding audit log or checking existing user:" + e.getMessage());
			throw new TrainingLogException(e.getMessage());
		}
	}

	/**
	 * Checks if the given user is able to login into USAT site
	 * 
	 * @param nameUSAT
	 * @param passwordUSAT
	 * @param nameTP
	 * @param passwordTP
	 * @return true - login successful, false - unsuccessful
	 */
	public boolean checkUSATLogin(String nameUSAT, String passwordUSAT) {
		TrainingAPI client = new USATclient();
		try {
			client.setupTrainingLog(nameUSAT, passwordUSAT);
			return client.checkLogin();
		} catch (TrainingLogException e) {
			return false;
		}
	}

	/**
	 * Checks if the given user is able to login into TP site
	 * 
	 * @param nameUSAT
	 * @param passwordUSAT
	 * @param nameTP
	 * @param passwordTP
	 * @return true - login successful, false - unsuccessful
	 */
	public boolean checkTPLogin(String nameTP, String passwordTP) {
		TrainingAPI client = new TrainingPeaksClient();
		try {
			client.setupTrainingLog(nameTP, passwordTP);
			return client.checkLogin();
		} catch (TrainingLogException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int removeUserWithCount(String nameTP, String passwordTP, String nameUSAT, String passwordUSAT)
			throws TrainingLogException {
		log.info("--- removeUser()");
		DatabaseAccess database = getDatabase();
		int i;
		try {
			i = database.removeUser(nameTP, passwordTP, nameUSAT, passwordUSAT);
		} catch (DatabaseException e) {
			throw new TrainingLogException(e);
		}
		return i;
	}

	@Override
	public void setUserActive(String nameTP, String passwordTP, String nameUSAT, String passwordUSAT)
			throws TrainingLogException {
		// TODO Auto-generated method stub
	}

	@Override
	public void setUserInactive(String nameTP, String passwordTP, String nameUSAT, String passwordUSAT)
			throws TrainingLogException {
		// TODO Auto-generated method stub
	}

	/**
	 * Escape an html string. Escaping data received from the client helps to
	 * prevent cross-site script vulnerabilities.
	 * 
	 * @param html
	 *            the html string to escape
	 * @return the escaped string
	 */
	private String escapeHtml(String html) {
		if (html == null) {
			return null;
		}
		return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

	private DatabaseAccess getDatabase() throws TrainingLogException {
		DatabaseAccess database;
		try {
			database = new DatabaseAccess();
		} catch (DatabaseException e) {
			throw new TrainingLogException(e);
		}
		return database;
	}

	@Override
	public String findWorkoutsForUser(Long userId, String nameTP, String passwordTP, String nameUSAT,
			String passwordUSAT) throws TrainingLogException {
		log.fine("---> findWorkoutsForUser(): id=" + userId + " nameTP=" + nameTP + " nameUSAT=" + nameUSAT);
		List<Workout> workouts = new ArrayList<Workout>();

		DatabaseAccess database = getDatabase();

		// In case the userId was passed down to this method, we dont need to
		// lookup the database for it
		if ((userId == null) || (userId == 0)) {
			try {
				userId = database.findUser(nameTP, passwordTP, nameUSAT, passwordUSAT, false);
			} catch (MoreThanOneUserFound e) {
				e.printStackTrace();
				log.severe(
						"findWorkoutsForUser() - ERROR - Multiple users with these names were found in the system. This should never happen");
				throw new TrainingLogException(e.getMessage());
			} catch (UserNotFound e) {
				e.printStackTrace();
				log.severe("findWorkoutsForUser() - ERROR - No user is found in the system.");
				throw new TrainingLogException(e.getMessage());
			} catch (DatabaseException e) {
				e.printStackTrace();
				log.severe("findWorkoutsForUser() - Database error while finding users:" + e.getMessage());
				throw new TrainingLogException(e.getMessage());
			}
		}

		// Now we can go get the list of workouts from the database
		try {
			workouts = database.findWorkoutsForUser(userId);
		} catch (DatabaseException e) {
			e.printStackTrace();
			log.severe("findWorkoutsForUser() - Database error while finding workouts:" + e.getMessage());
			throw new TrainingLogException(e.getMessage());
		}

		// Now copy list of Workouts into list of WorkoutSessions
		List<WorkoutSession> sessions = new ArrayList<WorkoutSession>();

		StringBuilder builder = new StringBuilder();
		Iterator<Workout> iterator = workouts.iterator();
		while (iterator.hasNext()) {
			Workout workout = iterator.next();
			sessions.add(workout.getWorkout());
			// TODO - I know I really should be returning a list of workouts,
			// not a string - client should worry about
			// the formatting, but I got tired of GWT error:
			// com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException:
			// The response could not be deserialized
			// when using the List<WorkoutSession> return type
			builder.append(workout.getWorkout().toString() + "<br>");
			// log.info(workout.getWorkout().toString());
		}

		log.fine("<--- findWorkoutsForUser()");
		return builder.toString();
	}

	@Override
	public SystemStatusDTO getSystemStatus() throws TrainingLogException {
		log.fine("---> getSystemStatus()");
		SystemStatusDTO result = new SystemStatusDTO();
		DatabaseAccess database = getDatabase();

		try {
			AppConfig currentConfig = database.loadConfig();
			result.setNumWorkoutsPast(currentConfig.getTotalWorkoutsPriorYears());
			result.setCompetitionStartDate(currentConfig.getCompetitionStartDate());
			result.setCompetitionEndDate(currentConfig.getCompetitionEndDate());
			result.setNumWorkoutsThisYear(database.getNumOfWorkouts());
			result.setNumAthletes(database.getNumOfUsers());
			result.setNumWorkoutsPastHour(database.getNumOfWorkoutsPastHour());
		} catch (DatabaseException e) {
			e.printStackTrace();
			log.severe("getSystemStatus() - Database error while finding app status: " + e.getMessage());
			throw new TrainingLogException(e.getMessage());
		}

		log.fine("<--- getSystemStatus(): " + result);
		return result;
	}
}