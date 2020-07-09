package com.basteez.utils;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Use this class to calculate working days/hours/minutes between two dates
 * @author Tiziano Basile
 */
public class WorkingTimeRangesCalculator{
  private static final int WORK_HOUR_START = 9; //Edit this with the actual start time of your office hours
  private static final int WORK_HOUR_END = 18; //Edit this with the actual end time of your office hours
  private static final boolean HAS_EASTER_MONDAY = true; //Edit this if you don't want to add Easter Monday in holiday list
  private static final int LAST_WORKING_DAY_IN_WEEK = DayOfWeek.SATURDAY.getValue(); //edit this to SUNDAY if you want to include SATURDAY as working day

  private List<LocalDate> holidays = new ArrayList<>();
  private int startYear = 0;
  private int endYear = 0;


  /**
   * Calculates easter day using Gauss' algorithm
   * @param year
   * @return
   */
  private LocalDate findEaster(int year){
    int y = year;
    int a = y % 19;
    int b = y / 100;
    int c = y % 100;
    int d = b / 4;
    int e = b % 4;
    int g = (8 * b + 13) / 25;
    int h = (19* a + b - d - g + 15) % 30;
    int j = c / 4;
    int k = c % 4;
    int m = (a + 11 * h)  / 319;
    int r = (2 * e + 2 * j - k - h + m + 32) % 7;
    int n = (h - m + r + 90) / 25;
    int p = (h - m + r + n + 19) % 32;

    return LocalDate.of(year, n , p);
  }

  /**
   * Calculates working minutes between two dates, excluding holidays and weekends
   * @param startTimestamp
   * @param endTimestamp
   * @return
   */
  public int getWorkingMinutes(final Timestamp startTimestamp, final Timestamp endTimestamp) {
    if (null == startTimestamp || null == endTimestamp) {
      throw new IllegalStateException();
    }
    if (endTimestamp.before(startTimestamp)) {
      return -1;
    }

    // start and end working hours
    LocalTime workStart = LocalTime.of(WORK_HOUR_START, 0);
    LocalTime workEnd = LocalTime.of(WORK_HOUR_END, 0);

    // start and end dates
    LocalDateTime start = startTimestamp.toLocalDateTime();
    LocalDateTime end = endTimestamp.toLocalDateTime();

    //initialize holidays list
    int startYear = start.getYear();
    int endYear = end.getYear();
    if(this.startYear == 0) this.startYear = startYear;
    if(this.endYear == 0) this.endYear = endYear;
    if(holidays.size() == 0 || startYear < this.startYear || endYear > this.endYear  ){
      initializeHolidaysList(startYear, endYear);
    }

    int totalMinutes = 0;
    LocalDateTime startHour = start;
    // if start is before 9 or after 18, adjust it
    if (start.toLocalTime().isBefore(workStart)) { // before 9
      startHour = start.with(workStart); // set time to 9
    } else if (start.toLocalTime().isAfter(workEnd)) { // after 18
      startHour = start.with(workEnd); // set time to 18
    }
    LocalDateTime endHour = end;
    // if end is before 9 or after 18, adjust it
    if (end.toLocalTime().isAfter(workEnd)) { // after 18
      endHour = end.with(workEnd); // set time to 18
    } else if (end.toLocalTime().isBefore(workStart)) { // before 9
      endHour = end.with(workStart); // set time to 9
    }

    while(startHour.isBefore(endHour)) {
      if (startHour.toLocalDate().equals(endHour.toLocalDate())) { // same day
        if(!isWorkingDay(startHour) || isHoliday(startHour)){break;}
        totalMinutes += ChronoUnit.MINUTES.between(startHour, endHour);
        break;
      } else {
        if(isWorkingDay(startHour) && !isHoliday(startHour)){
          LocalDateTime endOfDay = startHour.with(workEnd); // 6PM of the day
          totalMinutes += ChronoUnit.MINUTES.between(startHour, endOfDay);
        }
        startHour = startHour.plusDays(1).with(workStart); // go to next day
      }
    }

    return totalMinutes;
  }

  /**
   * Populates holidays list for each year in range
   * @param startYear
   * @param endYear
   */
  private void initializeHolidaysList(int startYear, int endYear){
    this.startYear = startYear;
    this.endYear = endYear;
    for(int year = startYear; year<= endYear; year++){
      holidays.add(LocalDate.of(year, 1, 1));
      holidays.add(LocalDate.of(year, 1, 6));
      holidays.add(LocalDate.of(year, 4, 25));
      holidays.add(LocalDate.of(year, 5, 1));
      holidays.add(LocalDate.of(year, 6, 2));
      holidays.add(LocalDate.of(year, 8, 15));
      holidays.add(LocalDate.of(year, 11, 1));
      holidays.add(LocalDate.of(year, 12, 8));
      holidays.add(LocalDate.of(year, 12, 25));
      holidays.add(LocalDate.of(year, 12, 26));
      LocalDate easter = findEaster(year);
      holidays.add(easter);
      if(HAS_EASTER_MONDAY){
        holidays.add(easter.plusDays(1));
      }
    }
  }

  /**
   * Calculates working minutes between two dates, excluding holidays and weekends
   * @param startTime
   * @param endTime
   * @return
   */
  public double getWorkingHours(final Timestamp startTime, final Timestamp endTime){
    Double workingHours = getWorkingMinutes(startTime,endTime) / 60.0;
    return workingHours;
  }

  /**
   * Calculates working minutes between two dates, excluding holidays and weekends
   * @param startTime
   * @param endTime
   * @return
   */
  public double getWorkingDays(final Timestamp startTime, final Timestamp endTime){
    Double workingDays = getWorkingMinutes(startTime,endTime) / (60.0 * (WORK_HOUR_END-WORK_HOUR_START));
    return workingDays;
  }

  /**
   * Check if a date is a working day
   * @param time
   * @return
   */
  private boolean isWorkingDay(final LocalDateTime time) {
    return time.getDayOfWeek().getValue() < LAST_WORKING_DAY_IN_WEEK;
  }

  /**
   * Check if date-time is during working hours
   * @param time
   * @return
   */
  private boolean isWorkingHours(final LocalDateTime time) {
    int hour = time.getHour();
    return WORK_HOUR_START <= hour && hour <= WORK_HOUR_END;
  }

  /**
   * Check if date is holiday
   * @param time
   * @return
   */
  private boolean isHoliday(final LocalDateTime time) {
    return holidays.contains(time.toLocalDate());
  }
}