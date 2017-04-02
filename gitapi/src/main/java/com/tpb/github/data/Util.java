package com.tpb.github.data;

import android.support.annotation.Nullable;
import android.util.Base64;

import com.tpb.github.data.models.DataModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

}
