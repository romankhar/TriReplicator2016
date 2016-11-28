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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * This is the main holding entity for the workout. It gets embedded to be
 * stored in the DB, can be used to transfer workouts between server and
 * clients, etc.
 * 
 * @author Roman Kharkovski
 */
@Embeddable
public class WorkoutSession implements Serializable, com.google.gwt.user.client.rpc.IsSerializable {

	private static final long serialVersionUID = 6167913802767169351L;

	public enum WorkoutType {
		Swim, Bike, Run, Elliptical, CrossCountrySki, Other
	};

	private WorkoutType workoutType;
	private String workoutName;
	@Temporal(TemporalType.DATE)
	private Date workoutDate;
	private long workoutDistanceYards;

	public WorkoutSession() {
		super();
	}

	public WorkoutSession(WorkoutType type, String name, Date date, long distanceYards) {
		setWorkoutDate(date);
		setWorkoutDistanceYards(distanceYards);
		setWorkoutName(name);
		setWorkoutType(type);
	}

	public WorkoutType getWorkoutType() {
		return workoutType;
	}

	public void setWorkoutType(WorkoutType workoutType) {
		this.workoutType = workoutType;
	}

	public boolean isSubjectToReplication() {
		if (((this.getWorkoutType() == WorkoutType.Swim) || (this.getWorkoutType() == WorkoutType.Bike)
				|| (this.getWorkoutType() == WorkoutType.Run) || (this.getWorkoutType() == WorkoutType.Elliptical)
				|| (this.getWorkoutType() == WorkoutType.CrossCountrySki)) && this.isValidWorkout())

			// The workout is ok to be replicated into the USAT site and stored
			// in the local database
			return true;
		else
			// The workout is not of interest and wont be considered for storage
			// or replication
			return false;
	}

	public String getWorkoutName() {
		return workoutName;
	}

	public void setWorkoutName(String workoutName) {
		this.workoutName = workoutName;
	}

	public Date getWorkoutDate() {
		return workoutDate;
	}

	public void setWorkoutDate(Date workoutDate) {
		this.workoutDate = workoutDate;
	}

	public long getWorkoutDistanceYards() {
		return workoutDistanceYards;
	}

	public void setWorkoutDistanceYards(long workoutDistanceYards) {
		this.workoutDistanceYards = workoutDistanceYards;
	}

	public String toString() {
		return "WorkoutSession: name='" + getWorkoutName() + "', type='" + getWorkoutType() + "', date='"
				+ getWorkoutDate() + "', distance (yards) ='" + getWorkoutDistanceYards() + "'";
	}

	public double getWorkoutDistanceMiles() {
		return Utils.yards2miles((double) getWorkoutDistanceYards());
	}

	/**
	 * This method validates if the workout is reasonably valid and returns TRUE
	 * if it is It checks for distances, dates, etc.
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public boolean isValidWorkout() {
		// It must be this year or last year
		if ((getWorkoutDate().getYear() != new Date().getYear())
				&& (getWorkoutDate().getYear() != new Date().getYear() - 1))
			return false;

		// It can not be in the future date
		if (getWorkoutDate().after(new Date()))
			return false;

		// Check for other conditions
		switch (getWorkoutType()) {
		case Swim:
			if (getWorkoutDistanceYards() > 50000)
				return false;
			if (getWorkoutDistanceYards() < 10)
				return false;
			break;

		case Bike:
			if (getWorkoutDistanceMiles() > 400.00)
				return false;
			if (getWorkoutDistanceMiles() < 0.01)
				return false;
			break;

		case Run:
			if (getWorkoutDistanceMiles() > 200.00)
				return false;
			if (getWorkoutDistanceMiles() < 0.01)
				return false;
			break;

		case Elliptical:
			if (getWorkoutDistanceMiles() > 100.00)
				return false;
			if (getWorkoutDistanceMiles() < 0.01)
				return false;
			break;

		case CrossCountrySki:
			if (getWorkoutDistanceMiles() > 200.00)
				return false;
			if (getWorkoutDistanceMiles() < 0.01)
				return false;
			break;

		default:
			break;
		}

		// Seems like after all checks and balances the workout is valid
		return true;
	}
}