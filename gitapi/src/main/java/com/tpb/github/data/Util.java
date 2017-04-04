package com.tpb.github.data;

import android.support.annotation.Nullable;
import android.util.Base64;

import com.tpb.github.data.models.DataModel;
import com.tpb.github.data.models.MergedModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by theo on 02/04/17.
 */

public class Util {

    //http://stackoverflow.com/a/10621553/4191572
    private static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * Transform ISO 8601 string to Calendar.
     */
    public static Calendar toCalendar(final String iso8601string)
            throws ParseException {
        final Calendar calendar = GregorianCalendar.getInstance();
        String s = iso8601string.replace("Z", "+00:00");
        try {
            s = s.substring(0, 22) + s.substring(23);  // to get rid of the ":"
        } catch(IndexOutOfBoundsException e) {
            throw new ParseException("Invalid length", iso8601string.length());
        }
        final Date date = ISO8601.parse(s);
        calendar.setTime(date);
        return calendar;
    }

    /**
     * Converts a UNIX time value in seconds to an ISO8061 string
     *
     * @param t The time since 1970 in seconds
     * @return Time formatted as yyyy-MM-dd'T'HH:mm:ssZ
     */
    public static String toISO8061FromSeconds(long t) {
        return ISO8601.format(new Date(t * 1000));
    }

    /**
     * Converts a UNIX time value in milliseconds to an ISO8061 string
     *
     * @param t The time since 1970 in milliseconds
     * @return Time formatted as yyyy-MM-dd'T'HH:mm:ssZ
     */
    public static String toISO8061FromMilliseconds(long t) {
        return ISO8601.format(new Date(t));
    }

    public static long getUTCTimeInMillis() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    }

    public static String shortenSha(@Nullable String sha) {
        return (sha == null || DataModel.JSON_NULL.equals(sha)) ? null : sha.substring(0, 7);
    }


    /**
     * @param base64 A base64 encoded String
     * @return The decoded value with Base64.DEFAULT
     */
    public static String base64Decode(String base64) {
        return new String(Base64.decode(base64, Base64.DEFAULT));
    }

    public static ArrayList<DataModel> mergeModels(List<? extends DataModel> models, Comparator<DataModel> comparator) {
        final ArrayList<DataModel> merged = new ArrayList<>();
        ArrayList<DataModel> toMerge = new ArrayList<>();
        DataModel last = null;
        for(int i = 0; i < models.size(); i++) {
            //If we have two of the same event, happening at the same time
            if(comparator.compare(models.get(i), last) == 0) {
                /*If multiple events (labels or assignees) were added as the first event,
                * then we need to stop the first item being duplicated
                 */
                if(merged.size() > 1) merged.remove(merged.size()-1);
                if(merged.size() == 1 && merged.get(0).equals(last)) merged.remove(0);
                toMerge.add(models.get(i - 1)); //Add the previous event
                int j = i;
                //Loop until we find an event which shouldn't be merged
                while(j < models.size() && comparator.compare(models.get(j), last) == 0) {
                    toMerge.add(models.get(j++));
                }
                i = j - 1; //Jump to the end of the merged positions
                merged.add(new MergedModel<>(toMerge));
                toMerge = new ArrayList<>(); //Reset the list of merged events
            } else {
                merged.add(models.get(i));
            }
            last = models.get(i); //Set the last event
        }
        return merged;

    }

}
