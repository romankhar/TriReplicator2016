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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.trireplicator.shared.Utils;

/**
 * This class is used to store all users in the local database
 * 
 * @author Roman Kharkovski, http://kharkovski.blogspot.com
 */
@Entity(name = "APP_CONFIG")
public class AppConfig {
	@Transient
	public static final String tableName = "APP_CONFIG";

	/**
	 * When was the last time that the configuration was loaded - by default
	 * this is way back so that when we start first it is read from the database
	 */
	@Transient
	private static Date lastLoadTime = new Date(System.currentTimeMillis());

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	/**
	 * Keep version of the app here
	 */
	private String appVersion;

	/**
	 * When configuration was last updated
	 */
	@Temporal(TemporalType.DATE)
	private Date lastUpdateDate;

	/**
	 * Format of the dates in start and end dates for competition
	 */
	private String datePattern;

	/**
	 * The day when competition starts and new workouts can be added to the
	 * system
	 */
	private String competitionStartDate;

	/**
	 * The date when the competition is over and no more new workouts can be
	 * added
	 */
	private String competitionEndDate;
	// private static GregorianCalendar lastDayOfChallengeUpdates = new
	// GregorianCalendar(2015, 02, 15);

	/**
	 * How often to run global replication for all users
	 */
	private int replicationFrequencyMinutes;

	/**
	 * Designation of the environment - PROD, TEST, DEV, etc.
	 */
	private String environmentName;

	/**
	 * How often to re-load configuration from the permanent storage (i.e. every
	 * so often)
	 */
	// TODO - this needs to be 60 minutes
	private int configurationReloadMinutes;

	/**
	 * NCC only allows to add workouts as of 7 days ago, but no later than that.
	 * Hence we add this constraint here.
	 */
	private int maxDaysBack;

	/**
	 * Total number of workouts replicated in prior years - this will be shown
	 * on the home page for fun stats
	 */
	private int totalWorkoutsPriorYears;

	@Transient
	private static final java.util.logging.Logger log = Utils.getLogger(AppConfig.class.getName());

	public AppConfig() {
		super();
	}

	@Override
	public String toString() {
		return "AppConfig [id=" + id + ", environmentName=" + environmentName + " appVersion=" + appVersion
				+ ", lastUpdateDate=" + lastUpdateDate + ", datePattern=" + datePattern + ", competitionStartDate="
				+ competitionStartDate + ", competitionEndDate=" + competitionEndDate + ", replicationFrequencyMinutes="
				+ replicationFrequencyMinutes + ", configurationReloadMinutes=" + configurationReloadMinutes
				+ ", maxDaysBack=" + maxDaysBack + ", totalWorkoutsPriorYears=" + totalWorkoutsPriorYears + "']";
	}

	public Date toDate(String dateStr) {
		try {
			return new SimpleDateFormat(getDatePattern()).parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
			log.severe("Error while conveting date string to date object: " + dateStr);
			return null;
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public Date getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}

	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

	public String getCompetitionStartDate() {
		return competitionStartDate;
	}

	public void setCompetitionStartDate(String competitionStartDate) {
		this.competitionStartDate = competitionStartDate;
	}

	public String getCompetitionEndDate() {
		return competitionEndDate;
	}

	public void setCompetitionEndDate(String competitionEndDate) {
		this.competitionEndDate = competitionEndDate;
	}

	public int getReplicationFrequencyMinutes() {
		return replicationFrequencyMinutes;
	}

	public void setReplicationFrequencyMinutes(int replicationFrequencyMinutes) {
		this.replicationFrequencyMinutes = replicationFrequencyMinutes;
	}

	public int getConfigurationReloadMinutes() {
		return configurationReloadMinutes;
	}

	public void setConfigurationReloadMinutes(int configurationReloadMinutes) {
		this.configurationReloadMinutes = configurationReloadMinutes;
	}

	public int getMaxDaysBack() {
		return maxDaysBack;
	}

	public void setMaxDaysBack(int maxDaysBack) {
		this.maxDaysBack = maxDaysBack;
	}

	public Date getLastLoadTime() {
		return lastLoadTime;
	}

	public void setLastLoadTime(Date loadTime) {
		lastLoadTime = loadTime;
	}

	public int getTotalWorkoutsPriorYears() {
		return totalWorkoutsPriorYears;
	}

	public void setTotalWorkoutsPriorYears(int totalWorkoutsPriorYears) {
		this.totalWorkoutsPriorYears = totalWorkoutsPriorYears;
	}

	public String getEnvironmentName() {
		return environmentName;
	}

	public void setEnvironmentName(String environmentName) {
		this.environmentName = environmentName;
	}
}