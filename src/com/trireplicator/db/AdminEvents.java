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
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.trireplicator.shared.Utils;

/**
 * This DB table is designed to keep track of admin events with the server - stop times, start times, etc.
 *  
 * @author Roman Kharkovski, http://kharkovski.blogspot.com
 */
@Entity(name = "ADMIN_EVENTS")
public class AdminEvents {

	@Transient
	public static final String tableName = "ADMIN_EVENTS";
	@Transient
	public static final String dateName = "date";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long eventId;

	/**
	 * Arbitrary name of the event
	 */
	@Column(nullable = false, length = 50)
	private String eventName;

	/**
	 * Date the event was generated
	 */
	@Column(nullable = false)
	@Temporal(TemporalType.DATE)
	private Date date;

	/**
	 * Whatever additional info about the event
	 */
	private String comments;

	/**
	 * Used to keep the app version number
	 */
	private String version;

	@Transient
	private static final Logger log = Utils.getLogger(AdminEvents.class.getName());

	public AdminEvents() {
		super();
		// set version to the current app version
		setVersion(Utils.VERSION);
		// the date is now
		setDate(new Date());
	}

	public AdminEvents(String eventName, String comments) {
		super();
		setEventName(eventName);
		setDate(new Date());
		setComments(comments);
		setVersion(Utils.VERSION);
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "AdminEvents [eventId=" + eventId + ", eventName=" + eventName + ", date=" + date + ", comments=" + comments + ", version="
				+ version + "]";
	}
}
