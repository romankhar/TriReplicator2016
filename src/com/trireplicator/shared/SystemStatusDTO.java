package com.trireplicator.shared;

import java.io.Serializable;

/**
 * Data Transfer Object class to pass system status information from server to
 * client
 * 
 * @author roman
 */
public class SystemStatusDTO implements Serializable {

	private static final long serialVersionUID = -9004955341825251113L;

	public SystemStatusDTO() {
		super();
	}

	private int numAthletes;
	private int numWorkoutsThisYear;
	private int numWorkoutsPast;
	private int numWorkoutsPastHour;
	private String competitionStartDate;
	private String competitionEndDate;

	public int getNumAthletes() {
		return numAthletes;
	}

	public void setNumAthletes(int numAthletes) {
		this.numAthletes = numAthletes;
	}

	public int getNumWorkoutsThisYear() {
		return numWorkoutsThisYear;
	}

	public void setNumWorkoutsThisYear(int numWorkoutsThisYear) {
		this.numWorkoutsThisYear = numWorkoutsThisYear;
	}

	public int getNumWorkoutsPast() {
		return numWorkoutsPast;
	}

	public void setNumWorkoutsPast(int numWorkoutsPast) {
		this.numWorkoutsPast = numWorkoutsPast;
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

	public int getNumWorkoutsPastHour() {
		return numWorkoutsPastHour;
	}

	public void setNumWorkoutsPastHour(int numWorkoutsPastHour) {
		this.numWorkoutsPastHour = numWorkoutsPastHour;
	}

}
