package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import xyz.zedler.patrick.grocy.R;

public class DateUtil {

    private static final String TAG = "DateUtil";

    private Context context;

    public DateUtil(Context context) {
        this.context = context;
    }

    public static int getDaysFromNow(String dateString) {
        Date current = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "getDaysAway: ");
        }
        if(date == null) return 0;
        return ((int)(date.getTime() / 86400000) - (int)(current.getTime() / 86400000)) + 1;
    }

    public String getHumanFromDays(int days) {
        if(days == 0) {
            return context.getString(R.string.date_today);
        } else if(days > 0) {
            if(days == 1) {
                return context.getString(R.string.date_tomorrow);
            } else {
                if(days < 30) {
                    return context.getString(
                            R.string.date_from_now,
                            context.getString(R.string.date_days, days)
                    );
                } else {
                    if(days < 60) {
                        return context.getString(
                                R.string.date_from_now,
                                context.getString(R.string.date_month)
                        );
                    } else {
                        if(days < 365) {
                            return context.getString(
                                    R.string.date_from_now,
                                    context.getString(
                                            R.string.date_months,
                                            days / 30
                                    )
                            );
                        } else {
                            if(days < 700) { // how many days do you understand as two years?
                                return context.getString(
                                        R.string.date_from_now,
                                        context.getString(R.string.date_year)
                                );
                            } else {
                                return context.getString(
                                        R.string.date_from_now,
                                        context.getString(
                                                R.string.date_years,
                                                days / 365
                                        )
                                );
                            }
                        }
                    }
                }
            }
        } else {
            if(days == -1) {
                return context.getString(R.string.date_yesterday);
            } else {
                if(days > -30) {
                    return context.getString(
                            R.string.date_ago,
                            context.getString(R.string.date_days, -1 * days)
                    );
                } else {
                    if(days > -60) {
                        return context.getString(
                                R.string.date_ago,
                                context.getString(R.string.date_month)
                        );
                    } else {
                        if(days > -365) {
                            return context.getString(
                                    R.string.date_ago,
                                    context.getString(
                                            R.string.date_months,
                                            days / 30 * -1
                                    )
                            );
                        } else {
                            if(days > -700) {
                                return context.getString(
                                        R.string.date_ago,
                                        context.getString(R.string.date_year)
                                );
                            } else {
                                return context.getString(
                                        R.string.date_ago,
                                        context.getString(
                                                R.string.date_years,
                                                days / 365 * -1
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }
    }
}