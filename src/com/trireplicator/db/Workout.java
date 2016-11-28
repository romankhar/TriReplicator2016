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

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.trireplicator.shared.WorkoutSession;

/**
 * Workout is the holder for the purpose of storing the WorkoutSession + userId
 * + unique ID in the database
 *
 * @author Roman Kharkovski, http://kharkovski.blogspot.com
 */
@Entity(name = "WORKOUTS")
public class Workout {
	@Transient
	public static final String tableName = "WORKOUTS";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Embedded
	@OneToOne(cascade = CascadeType.ALL)
	private WorkoutSession workout;

	/**
	 * This is the timestamp when this workout was first recorded in
	 * Tri-Replicator, hence automatic date
	 */
	@Temporal(TemporalType.DATE)
	// @Basic(optional = false)
	// @Column(name = "timestamp", insertable = false, updatable = false)
	// @Temporal(TemporalType.TIMESTAMP)
	private Date timestamp = new Date(System.currentTimeMillis());

	// TODO - this really needs to have the relationship with the Users
	// database, but in DataNucleus JPA it does not
	// work properly - since this works in Liberty JPA really need to use this,
	// instead of storing iserId.
	// @ManyToOne private User user;
	private Long userId;

	public Long getId() {
		return id;
	}

	public Workout() {
		super();
	}

	public Workout(WorkoutSession workout, Long userId) {
		super();
		this.workout = workout;
		this.userId = userId;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public WorkoutSession getWorkout() {
		return workout;
	}

	public void setWorkout(WorkoutSession workout) {
		this.workout = workout;
	}

	public String toString() {
		String sessionAsString = "-empty workout-";
		if (getWorkout() != null)
			sessionAsString = getWorkout().toString();
		return "------ WORKOUT: ID='" + getId() + "' userId='" + userId.toString() + "'" + " recorded time: "
				+ ((getTimestamp() != null) ? getTimestamp().toString() : "null") + " " + sessionAsString;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
