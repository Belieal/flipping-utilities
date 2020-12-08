package com.flippingutilities.ui.uiutilities;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeFormatters {
    /**
     * Formats a duration into HH:MM:SS
     *
     * @param duration
     * @return Formatted (HH:MM:SS) string
     */
    public static String formatDuration(Duration duration)
    {
        long seconds = duration.toMillis() / 1000;
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }


    /**
     * Formats an instant into a duration between the parameter and now.
     *
     * @return Formatted (HH:MM:SS) string
     */
    public static String formatDuration(Instant fromInstant)
    {
        return formatDuration(Duration.between(fromInstant, Instant.now()));
    }

    /**
     * Formats the duration between one instant to an end instant.
     *
     * @param startInstant Start of duration
     * @param endInstant   End of duration
     * @return Formatted (HH:MM:SS) string
     */
    public static String formatDuration(Instant startInstant, Instant endInstant)
    {
        return formatDuration(Duration.between(startInstant, endInstant));
    }

    /**
     * This method formats a string time from an instant from the time, as specified by the parameter, until now.
     * It truncates the time representation to the greatest unit. For example, 65 seconds will become a minute
     * (not a minute and 5 seconds), 70 minutes will become an hour (not an hour and 10 minutes), etc. This is to
     * save space on the panels.
     *
     * @param fromInstant The start of the duration
     * @return A formatted string.
     */
    public static String formatDurationTruncated(Instant fromInstant)
    {
        if (fromInstant != null)
        {
            long toInstant = Instant.now().getEpochSecond();

            //Time since trade was done.
            long timeAgo = (toInstant - fromInstant.getEpochSecond());

            String result = timeAgo + (timeAgo == 1 ? " second" : " seconds");
            if (timeAgo >= 60)
            {
                //Seconds to minutes.
                long timeAgoMinutes = timeAgo / 60;
                result = timeAgoMinutes + (timeAgoMinutes == 1 ? " minute" : " minutes");

                if (timeAgoMinutes >= 60)
                {
                    //Minutes to hours
                    int timeAgoHours = (int) (timeAgoMinutes / 60);
                    result = timeAgoHours + (timeAgoHours == 1 ? " hour" : " hours");
                    if (timeAgoHours > 24)
                    {
                        int timeAgoDays = timeAgoHours / 24;
                        result = timeAgoDays + (timeAgoDays == 1 ? " day" : " days");
                    }
                }
            }
            return result;
        }
        else
        {
            return "";
        }
    }

    /**
     * This method formats a time instant into a string of hours and minutes.
     *
     * @param time             The time that needs to be formatted
     * @param twelveHourFormat Determines if the returned string should be formatted with 12-hour or 24-hour format
     * @param includeDate      Include the day and month in the format string.
     * @return A formatted string of hours and minutes dependent on user config (hh:mm a  or  HH:mm)
     */
    public static String formatTime(Instant time, boolean twelveHourFormat, boolean includeDate)
    {
        DateTimeFormatter timeFormatter;
        String pattern = "";

        if (includeDate)
        {
            //Example:	04 Sep
            pattern = "dd MMM ";
        }

        if (twelveHourFormat)
        {
            //Example:	2:53 PM
            pattern += "hh:mm a";
        }
        else
        {
            //Example:	14:53
            pattern += "HH:mm";
        }
        timeFormatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
        return timeFormatter.format(time);
    }
}
