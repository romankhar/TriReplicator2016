/**
 * Tri-Replicator Application
 * 
 * To learn more about the app, visit this blog:
 * http://kharkovski.blogspot.com/2013/01/tri-replicator-free-app-on-google-app.html
 * 
 *  @author Roman Kharkovski, http://kharkovski.blogspot.com
 *  Created: December 19, 2012
 */

package com.trireplicator.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.trireplicator.server.ConfigMgr;
import com.trireplicator.shared.MoreThanOneUserFound;
import com.trireplicator.shared.UserNotFound;
import com.trireplicator.shared.Utils;

/**
 * This is utility class that provides all of the operations for working with
 * the database No other class in the project works with the database directly.
 * Only via this class
 * 
 * @author Roman Kharkovski, http://kharkovski.blogspot.com
 */

public class DatabaseAccess {
	// public class DatabaseAccess extends HttpServlet {

	private static final Logger log = Utils.getLogger(DatabaseAccess.class.getName());

	/**
	 * The JNDI name for the persistence context is the one defined in web.xml
	 */
	private static final String JNDI_NAME = "java:comp/env/jpa/TriReplicatorEntityManager";

	private static Context ctx = null;
	private EntityManager em;
	UserTransaction transaction;

	/**
	 * This means that every call to the function by default will start and stop
	 * transaction. However if someone manually started transaction it will not
	 * be automatically committed and one must call transactionCommit to finish
	 * the work.
	 */
	private boolean automaticTransaction = true;

	/**
	 * This indicates if the server manages transactions or not (including the
	 * need to close EntityManager in the end)
	 */
	private static final boolean containerManagedTransaction = true;

	public DatabaseAccess() throws DatabaseException {
		super();
		log.fine("---> DatabaseAccess() constructor");
		try {
			if (ctx == null) {
				ctx = new InitialContext();
			}
		} catch (NamingException e) {
			log.severe("---- Could not initialize the database: " + e.getMessage());
			throw new DatabaseException(e.getMessage());
		}
		// emf = Persistence.createEntityManagerFactory("TriReplicator");

		log.fine("<--- DatabaseAccess() constructor completed ctx = " + ctx);
	}

	public List<User> listUsers() throws DatabaseException {
		log.fine("---> listUsers()");
		transactionBegin();
		Query q = em.createQuery("select u from " + User.tableName + " u");
		@SuppressWarnings("unchecked")
		List<User> users = q.getResultList();
		transactionCommit();
		if (users == null)
			// In case there are no users, we will return empty result, as
			// opposed to NULL
			users = new ArrayList<User>();
		log.fine("<--- listUsers()");
		return users;
	}

	public void removeUser(long id) throws DatabaseException {
		log.info("---> removeUser() userId=" + id);
		transactionBegin();
		try {
			User user = em.find(User.class, id);
			em.remove(user);
		} finally {
			transactionCommit();
		}
		log.fine("<--- removeUser()");
	}

	/**
	 * This will actually remove all users with given names, even if passwords
	 * are different, but it will check for password match for at least one of
	 * those users Return value is how many users deleted
	 * 
	 * @throws NamingException
	 */
	public int removeUser(String nameTP, String passwordTP, String nameUSAT, String passwordUSAT)
			throws DatabaseException {
		log.info("--- removeUser(): nameTP=" + nameTP + " nameUSAT=" + nameUSAT);
		transactionBegin();
		Query q = em.createQuery("delete from " + User.tableName
				+ " u where (u.nameUSAT = :nameUSAT) and (u.nameTP = :nameTP) and (u.encryptedPasswordUSAT = :passwordUSAT) and (u.encryptedPasswordTP = :passwordTP)");
		q.setParameter("nameUSAT", nameUSAT);
		q.setParameter("nameTP", nameTP);
		q.setParameter("passwordUSAT", User.plain2encrypted(passwordUSAT));
		q.setParameter("passwordTP", User.plain2encrypted(passwordTP));

		int i = q.executeUpdate();
		log.info("IMPORTANT (!!!) Deleted '" + i + "' users from the database");
		transactionCommit();
		return i;
	}

	public int removeUser(String nameTP, String nameUSAT) throws DatabaseException {
		log.info("--- removeUser(by names): nameTP=" + nameTP + " nameUSAT=" + nameUSAT);
		transactionBegin();
		Query q = em.createQuery(
				"delete from " + User.tableName + " u where (u.nameUSAT = :nameUSAT) and (u.nameTP = :nameTP)");
		q.setParameter("nameUSAT", nameUSAT);
		q.setParameter("nameTP", nameTP);

		int i = q.executeUpdate();
		log.info("Deleted '" + i + "' users from the database");
		transactionCommit();
		return i;
	}

	/**
	 * Adds new user to the system
	 * 
	 * @param user
	 * @return @TRUE if user was added and @FALSE otherwise
	 * @throws NamingException
	 */
	public boolean addUser(User user) throws DatabaseException {
		synchronized (this) {
			log.info("Add user: " + user.toString());
			// First we need to check if the user already exists
			if (!checkExistingUser(user.getNameUSAT(), user.getPlainPasswordUSAT(), user.getNameTP(),
					user.getPlainPasswordTP())) {
				// Now we can add new user
				transactionBegin();
				em.persist(user);
				transactionCommit();
				return true;
			} else {
				// The user with given user names and passwords is already in
				// the system, so we do nothing
				log.finest("The user with given attributes is already registered in the system");
			}
		}
		return false;
	}

	public List<User> getUsersByUSATName(String name) throws DatabaseException {
		log.fine("---> getUsersByUSATName()");
		transactionBegin();
		Query q = em.createQuery("select u from " + User.tableName + " u where u.nameUSAT = :name");
		q.setParameter("name", name);
		@SuppressWarnings("unchecked")
		List<User> users = q.getResultList();
		transactionCommit();
		return users;
	}

	public boolean checkExistingUser(String nameUSAT, String passwordUSAT, String nameTP, String passwordTP)
			throws DatabaseException {
		log.fine("---> checkExistingUser()");
		Long userId = null;
		try {
			// We do not really need to know the user ID, but need to know if
			// the user was found
			userId = findUser(nameTP, passwordTP, nameUSAT, passwordUSAT, false);
		} catch (UserNotFound e1) {
			return false;
		} catch (MoreThanOneUserFound e) {
			// This should never happen
			log.log(Level.WARNING,
					"More than 1 user found in the database: nameUSAT='" + nameUSAT + "' nameTP='" + nameTP + "'");
			return true;
		}

		if (userId != null) {
			// Now we know that the login is successful, so we shall update user
			// access timestamp
			updateUserAccessTime(userId);
			return true;
		} else {
			return false;
		}
	}

	// private User findUserById(Long userId) throws MoreThanOneUserFound,
	// UserNotFound, DatabaseException {
	private User findUserById(Long userId) throws DatabaseException {
		log.fine("---> findUserById()");
		transactionBegin();
		User user = em.find(User.class, userId);
		transactionCommit();
		log.fine("<--- findUserById()");
		return user;

		// Query q = em.createQuery("select u from " + User.tableName + " u
		// where (u.userId = :userId)");
		// q.setParameter("userId", userId);
		// @SuppressWarnings("unchecked")
		// List<User> users = q.getResultList();
		// transactionCommit();
		//
		// // Check if we did not find any users
		// if ((users == null) || (users.size() == 0)) {
		// String error = "User with given ID is not found in the system:
		// userId='" + userId;
		// log.finest(error);
		// throw new UserNotFound(error);
		// }
		//
		// // Check if we found multiple users
		// if (users.size() > 1) {
		// String error = "More than 1 user with given names is found in the
		// system: userId='" + userId;
		// log.log(Level.WARNING, error);
		// throw new MoreThanOneUserFound(error);
		// }
		//
		// // Return the found user
		// return users.get(0);
	}

	public Long findUser(String nameTP, String passwordTP, String nameUSAT, String passwordUSAT, boolean ignoreMultiple)
			throws UserNotFound, MoreThanOneUserFound, DatabaseException {
		log.fine("---> findUser()");
		transactionBegin();
		Query q = em.createQuery("select u from " + User.tableName
				+ " u where (u.nameUSAT = :nameUSAT) and (u.nameTP = :nameTP) and (u.encryptedPasswordUSAT = :passwordUSAT) and (u.encryptedPasswordTP = :passwordTP)");
		q.setParameter("nameUSAT", nameUSAT);
		q.setParameter("nameTP", nameTP);
		q.setParameter("passwordUSAT", User.plain2encrypted(passwordUSAT));
		q.setParameter("passwordTP", User.plain2encrypted(passwordTP));
		@SuppressWarnings("unchecked")
		List<User> users = q.getResultList();
		transactionCommit();

		// Check if we did not find any users
		if ((users == null) || (users.size() == 0)) {
			String error = "User with given names is not found in the system: nameUSAT='" + nameUSAT + "' nameTP='"
					+ nameTP + "'";
			log.finest(error);
			throw new UserNotFound(error);
		}

		// Check if we found multiple users
		if ((!ignoreMultiple) && (users.size() > 1)) {
			String error = "More than 1 user with given names is found in the system: nameUSAT='" + nameUSAT
					+ "' nameTP='" + nameTP + "'";
			log.log(Level.WARNING, error);
			throw new MoreThanOneUserFound(error);
		}

		// Return the ID of the first found user
		return users.get(0).getUserId();
	}

	/**
	 * Finds workout in the database
	 * 
	 * @param workout
	 * @return WorkoutId if the workout is found and ZERO (0) if it is not found
	 * @throws DatabaseException
	 */
	public Long findWorkout(Workout workout) throws DatabaseException {
		log.fine("---> findWorkout() - start: " + workout.toString());
		transactionBegin();
		Query q = em.createQuery("select w from " + Workout.tableName + " w where (w.userId = :userId) and "
				+ "(w.workout.workoutType = :workoutType) and "
				// + "(w.workout.workoutName = :workoutName) and "
				+ "(w.workout.workoutDate = :workoutDate) and "
				+ "(w.workout.workoutDistanceYards = :workoutDistanceYards)");
		q.setParameter("userId", workout.getUserId());
		q.setParameter("workoutType", workout.getWorkout().getWorkoutType());
		// q.setParameter("workoutName", workout.getWorkout().getWorkoutName());
		q.setParameter("workoutDate", workout.getWorkout().getWorkoutDate());
		q.setParameter("workoutDistanceYards", workout.getWorkout().getWorkoutDistanceYards());
		@SuppressWarnings("unchecked")
		List<Workout> workouts = q.getResultList();
		transactionCommit();

		if ((workouts == null) || (workouts.size() == 0)) {
			String msg = "Workout with given parameters is not found in the system: " + workout.toString();
			log.finest(msg);
			return new Long(0);
		}

		return workouts.get(0).getId();
	}

	/**
	 * Read all workouts from the database
	 * 
	 * @return complete list of all workouts ever recorded in the system
	 * @throws DatabaseException
	 */
	public List<Workout> listWorkouts() throws DatabaseException {
		log.fine("---> listWorkouts()");
		transactionBegin();
		// Read the existing entries
		Query q = em.createQuery("select u from " + Workout.tableName + " u");
		@SuppressWarnings("unchecked")
		List<Workout> workouts = q.getResultList();
		transactionCommit();
		return workouts;
	}

	public List<Workout> findWorkoutsForUser(Long id) throws DatabaseException {
		log.fine("---> getWorkoutsForUser: userId=" + id.toString());
		transactionBegin();
		Query q = em.createQuery("select w from " + Workout.tableName + " w where w.userId = :userId");
		q.setParameter("userId", id);
		@SuppressWarnings("unchecked")
		List<Workout> workouts = q.getResultList();
		transactionCommit();
		return workouts;
	}

	public void addWorkout(Workout workout) throws DatabaseException {
		synchronized (this) {
			log.fine("--->Add workout");
			transactionBegin();
			em.persist(workout);
			transactionCommit();
		}
	}

	public void addAdminEvent(AdminEvents event) throws DatabaseException {
		log.fine("---> Add admin event");
		transactionBegin();
		em.persist(event);
		transactionCommit();
	}

	public List<AdminEvents> listAdminEvents() throws DatabaseException {
		log.fine("---> listAdminEvents()");
		transactionBegin();
		// Read the existing entries
		Query q = em.createQuery("select u from " + AdminEvents.tableName + " u order by u." + AdminEvents.dateName);
		@SuppressWarnings("unchecked")
		List<AdminEvents> events = q.getResultList();
		transactionCommit();
		if (events == null)
			events = new ArrayList<AdminEvents>();
		log.fine("<--- listAdminEvents()");
		return events;
	}

	/**
	 * Adds new config element to the system - current design assumes that there
	 * is only one config in the system - one row in the DB
	 * 
	 * @param config
	 * @return @TRUE if user was added and @FALSE otherwise
	 * @throws NamingException
	 */
	public void updateAppConfig(AppConfig config) throws DatabaseException {
		log.fine("---> AppConfig: " + config.toString());
		transactionBegin();

		// Delete old configuration
		Query q = em.createQuery("delete from " + AppConfig.tableName + " w");
		q.executeUpdate();

		// Now create new configuration record
		em.persist(config);
		transactionCommit();
		log.fine("<--- AppConfig()");
	}

	/**
	 * Loads app configuration from the database
	 * 
	 * @return
	 * @throws DatabaseException
	 */
	public AppConfig loadConfig() throws DatabaseException {
		log.fine("---> loadConfig()");
		transactionBegin();
		Query q = em.createQuery("select u from " + AppConfig.tableName + " u");
		@SuppressWarnings("unchecked")
		List<AppConfig> configs = q.getResultList();
		transactionCommit();

		// Check if we did not find any users
		if ((configs == null) || (configs.size() == 0)) {
			String error = "No existing configuration was found in the database - this may be normal if this is the first start of the app on the new system";
			log.severe(error);
			return ConfigMgr.generateDefaultConfiguration();
		}

		// Check if we found multiple configs
		if (configs.size() > 1) {
			String error = "More than 1 configuration was found in the system  this should never happen!";
			log.severe(error);
			return ConfigMgr.generateDefaultConfiguration();
		}

		log.fine("<--- loadConfig()");
		return configs.get(0);
	}

	public int deleteAllWorkouts() throws DatabaseException {
		log.fine("---> deleteAllWorkouts() !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		transactionBegin();
		Query q = em.createQuery("delete from " + Workout.tableName + " w");
		int i = q.executeUpdate();
		transactionCommit();
		log.info("IMPORTANT (!!!): Deleted '" + i + "' rows from WORKOUTS");
		return i;
	}

	public int deleteAllUsers() throws DatabaseException {
		log.fine("---> deleteAllUsers() !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		transactionBegin();
		Query q = em.createQuery("delete from " + User.tableName + " u");
		int i = q.executeUpdate();
		transactionCommit();
		log.info("IMPORTANT (!!!) Deleted '" + i + "' rows from USERS");
		return i;
	}

	public int deleteAllAdminEvents() throws DatabaseException {
		log.fine("---> deleteAllAdminEvents() !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		transactionBegin();
		Query q = em.createQuery("delete from " + AdminEvents.tableName + " a");
		int i = q.executeUpdate();
		transactionCommit();
		log.info("IMPORTANT (!!!) Deleted '" + i + "' rows from AdminEvents");
		return i;
	}

	/**
	 * This can be called if a series of calls needs to be done in sequence
	 * without committing transaction in the middle
	 * 
	 * @throws NamingException
	 */
	// public void longTransactionBegin() throws DatabaseException {
	// automaticTransaction = true;
	// transactionBegin();
	// automaticTransaction = false;
	// }

	/**
	 * This needs to be called before each entityManager use
	 * 
	 * @throws NamingException
	 * @throws DatabaseException
	 */
	public void transactionBegin() throws DatabaseException {
		log.finest("> transactionBegin()...");
		try {
			log.finest("transactionBegin:ctx = " + ctx);
			em = (EntityManager) ctx.lookup(JNDI_NAME);
			log.finest("transactionBegin:EntityManager = " + em);
		} catch (NamingException e) {
			log.severe("Error while looking up EntityManager from Initial Context: " + e.getMessage());
			throw new DatabaseException(e.getMessage());
		}

		if (automaticTransaction) {
			try {
				transaction = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
				transaction.begin();
			} catch (NamingException | NotSupportedException | SystemException e) {
				log.severe("Error while starting transaction: " + e.getMessage());
				throw new DatabaseException(e.getMessage());
			}
		}
		log.finest("< ... transactionBegin()");
	}

	/**
	 * This needs to be called after long running transaction that was started
	 * by {@link #longTransactionBegin()}
	 * 
	 * @throws DatabaseException
	 */
	// public void longTransactionCommit() throws DatabaseException {
	// automaticTransaction = true;
	// transactionCommit();
	// }

	/**
	 * This needs to be called after each entity manager use
	 * 
	 * @throws DatabaseException
	 */
	public void transactionCommit() throws DatabaseException {
		log.finest("> transactionCommit()...");
		if (automaticTransaction) {
			try {
				transaction.commit();
			} catch (IllegalStateException | SecurityException | HeuristicMixedException | HeuristicRollbackException
					| RollbackException | SystemException e) {
				log.severe("Error while commiting transaction: " + e.getMessage());
				throw new DatabaseException(e.getMessage());
			}
			// In container managed transaction need not close entity manager
			if (!containerManagedTransaction) {
				log.finest("Closing entity manager");
				em.close();
			}
		}
		log.finest("< ... transactionCommit()");
	}

	public void updateUserAccessTime(Long userId) {
		log.fine("---> updateUserAccessTime()");
		try {
			User user = findUserById(userId);
			transactionBegin();
			user.setLastVisitDate(new Date(System.currentTimeMillis()));
			user.setTotalLoginCount(user.getTotalLoginCount() + 1);
			em.merge(user);
			transactionCommit();
		} catch (DatabaseException e) {
			log.severe("Error while updating last access time for the user ID: " + userId + " Exception thrown: "
					+ e.getMessage());
		}
		log.fine("<--- updateUserAccessTime()");
	}

	public int getNumOfUsers() throws DatabaseException {
		log.fine("---> getNumOfUsers()");
		transactionBegin();
		Query q = em.createQuery("select count(u) from " + User.tableName + " u");
		int count = ((Long) q.getSingleResult()).intValue();
		transactionCommit();
		log.fine("<--- getNumOfUsers():" + count);
		return count;
	}

	public int getNumOfWorkouts() throws DatabaseException {
		log.fine("---> getNumOfWorkouts()");
		transactionBegin();
		Query q = em.createQuery("select count(u) from " + Workout.tableName + " u");
		int count = ((Long) q.getSingleResult()).intValue();
		transactionCommit();
		log.fine("<--- getNumOfWorkouts():" + count);
		return count;
	}

	public int getNumOfWorkoutsPastHour() throws DatabaseException {
		log.fine("---> getNumOfWorkoutsPastHour()");
		transactionBegin();
		Query q = em.createQuery("select count(u) from " + Workout.tableName + " u where u.timestamp >= :hourAgo");
		q.setParameter("hourAgo", new Date(Utils.hourAgo()));
		int count = ((Long) q.getSingleResult()).intValue();
		transactionCommit();
		log.fine("<--- getNumOfWorkoutsPastHour():" + count);
		return count;
	}

}
