package br.com.jpttrindade.calendarview.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import br.com.jpttrindade.calendarview.R;
import br.com.jpttrindade.calendarview.data.Day;
import br.com.jpttrindade.calendarview.data.Month;
import br.com.jpttrindade.calendarview.data.WeekManager;
import br.com.jpttrindade.calendarview.holders.MonthHolder;
import br.com.jpttrindade.calendarview.view.CalendarView;

/**
 * Created by joaotrindade on 06/09/16.
 */
public class CalendarAdapter extends RecyclerView.Adapter<MonthHolder> {

    public enum CalendarEvent {
        Refill, Shipment, Medication
    }


    private List<String> mMonthLabels;
    public ArrayList<Month> mMonths = new ArrayList<Month>();
    private final Context mContext;

    private int startYear; //ano atual (real)
    private int startMonth; // mes atual (real)
    private int today;


    private int earlyMonthLoaded; //mes mais antigo ja carregado
    private int earlyYearLoaded; //ano mais antigo ja carregado

    public int laterMonthLoaded; //mes mais a frente ja carregado
    public int laterYearLoaded; //ano mais a frente ja carregado

    private WeekManager weekManager;
    public Calendar max = Calendar.getInstance();
    public Calendar min = Calendar.getInstance();


    private int PAYLOAD = 3; // o numero de meses que serao carregados antes e depois do mes atual.
    private CalendarView.OnDayClickListener onDayClickListener;
    public HashMap<String, Boolean> mEvents;
    public HashMap<String, LinkedHashSet<String>> mEventsColor;
    private HashMap<String, Boolean> selectedDate;
    private CalendarView.Attributes attrs;
    CalendarView.OnMonthChangedListener onMonthChangedListener;
    private HashMap<String, Boolean> blockedDates;
    public boolean minReachedEnd = false;
    public boolean maxReachedEnd = false;
    Calendar current = Calendar.getInstance();
    boolean isSelectDay = false;


    public CalendarAdapter(Context context, CalendarView.Attributes calendarAttrs, CalendarView.OnMonthChangedListener onMonthChangedListener) {
        mContext = context;
        attrs = calendarAttrs;
        this.onMonthChangedListener = onMonthChangedListener;
        Calendar c = Calendar.getInstance();
        reloadCalendar(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, false);
    }

    public void reloadCalendar(int year, int month, boolean isSelectDay) {
        minReachedEnd = false;
        maxReachedEnd = false;
        this.isSelectDay = isSelectDay;
        mMonthLabels = Arrays.asList(mContext.getResources().getStringArray(R.array.months));
        startYear = year;
        startMonth = month;
        today = current.get(Calendar.DAY_OF_MONTH);

        String[] minarr = attrs.minDate.split("/");
        min.set(Calendar.DAY_OF_MONTH, Integer.parseInt(minarr[0]));
        min.set(Calendar.MONTH, Integer.parseInt(minarr[1]));
        min.set(Calendar.YEAR, Integer.parseInt(minarr[2]));


        String[] maxarr = attrs.maxDate.split("/");
        max.set(Calendar.DAY_OF_MONTH, Integer.parseInt(maxarr[0]));
        max.set(Calendar.MONTH, Integer.parseInt(maxarr[1]));
        max.set(Calendar.YEAR, Integer.parseInt(maxarr[2]));

        if (min.getTimeInMillis() > max.getTimeInMillis()) {
            Toast.makeText(mContext, "end date should be miss matched", Toast.LENGTH_LONG).show();
        }
        /*attrs.minDate
        attrs.maxDate*/

        mMonths.clear();
        notifyDataSetChanged();
        mEvents = new HashMap<>();
        mEventsColor = new HashMap<>();
        selectedDate = new HashMap<>();
        blockedDates = new HashMap<>();

        earlyMonthLoaded = startMonth;
        earlyYearLoaded = startYear;
        laterYearLoaded = startYear;
        laterMonthLoaded = startMonth;


        onMonthChangedListener.onMonthChanged(String.valueOf(mMonthLabels.get(current.get(Calendar.MONTH) - 1)), String.valueOf(startYear));

        mMonths.add(new Month(startMonth, startYear));
        getPreviousMonth();
        getNextMonths();
    }


    @Override
    public int getItemViewType(int position) {
        return mMonths.get(position).weeks.length;
    }

    @Override
    public MonthHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.month_view, parent, false);
        MonthHolder mh = new MonthHolder(v, viewType, attrs, new CalendarView.OnDayClickListener() {
            @Override
            public void onClick(int day, int month, int year, boolean hasEvent) {
                if (onDayClickListener != null) {
                    String key = String.format("%d%d%d", day, month, year);
                    if (!blockedDates.containsKey(key)) {
                        onDayClickListener.onClick(day, month, year, hasEvent(day, month, year));
                        selectedDate.clear();
                        selectedDate.put(key, true);
                        notifyDataSetChanged();
                    }
                }
            }
        });
        mh.generateWeekRows();
        return mh;
    }


    @Override
    public void onBindViewHolder(MonthHolder holder, int position) {
        Month m = mMonths.get(position);
        if (m.weeks.length == 6) {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).setMargins(0, 0, 0, 50);
        }
        setLabel(holder, m);
        setWeeks(holder, m);

      /*  if (position == 0) {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).setMargins(0, 0, 0, 0);
        } else if (position == mMonths.size() - 1) {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).setMargins(0, attrs.monthDividerSize, 0, 150);
        } else if (position==3) {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).setMargins(0, 0, 0, 0);
        }*/
        holder.mYear = m.year;
        holder.mMonth = m.value;


    }

    private void setLabel(MonthHolder holder, Month m) {
        String year = (m.year != startYear ? " de " + m.year : "");
        // onMonthChangedListener.onMonthChanged(mMonthLabels.get(m.value - 1), String.valueOf(m.year));
        holder.label_month.setText(mMonthLabels.get(m.value - 1) + year);
        if (m.value == startMonth && m.year == startYear) {
            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            holder.label_month.setTextSize(TypedValue.COMPLEX_UNIT_PX, (attrs.monthLabelSize + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6, displayMetrics)));
        } else {
            holder.label_month.setTextSize(TypedValue.COMPLEX_UNIT_PX, (attrs.monthLabelSize));
        }
    }


    private void setWeeks(MonthHolder holder, Month m) {
        MonthHolder.WeekDayView[] weekColumns;
        Day[] days;
        View container;
        TextView tv_day;
        View event_circle1;
        View event_circle2;
        View event_circle3;
        for (int i = 0; i < holder.weekRowsCount; i++) {
            weekColumns = holder.weeksColumns.get(i);
            days = m.weeks[i].days;
            String key;
            for (int j = 0; j < 7; j++) {
                event_circle1 = weekColumns[j].v_event_circle;
                event_circle2 = weekColumns[j].event_circle1;
                event_circle3 = weekColumns[j].event_circle2;
                container = weekColumns[j].container;
                tv_day = weekColumns[j].tv_value;
                tv_day.setText("" + days[j].value);

                container.setTag(days[j].value);
                container.setClickable(days[j].value != 0);
                if (hasEvent(days[j].value, m.value, m.year)) {
                    if (hasEventColor(days[j].value, m.value, m.year) != null) {
                        setEventCircleColor(hasEventColor(days[j].value, m.value, m.year), event_circle1, event_circle2, event_circle3);
                    }
                } else {
                    event_circle1.setVisibility(View.INVISIBLE);
                    event_circle2.setVisibility(View.INVISIBLE);
                    event_circle3.setVisibility(View.INVISIBLE);
                }
                //v_circle.setVisibility(hasEvent(days[j].value, m.value, m.year) ? View.VISIBLE : View.INVISIBLE);

                weekColumns[j].every_first.setVisibility(hasSelectedDate(days[j].value, m.value, m.year) ? View.VISIBLE : View.INVISIBLE);


                if (m.year == current.get(Calendar.YEAR) && m.value == current.get(Calendar.MONTH) + 1 && days[j].value == today) {
                    tv_day.setTextColor(Color.WHITE);
                    weekColumns[j].v_today_circle.setVisibility(View.VISIBLE);
                } else {
                    if ((days[j].value == 0)) {
                        tv_day.setTextColor(Color.TRANSPARENT);
                    } else {
                        if (j == 0 || j == 6) {
                            if (attrs.weekendDifferenctColor) {
                                tv_day.setTextColor(attrs.weekEndColor);
                            } else {
                                tv_day.setTextColor(attrs.monthLabelColor);
                            }
                        } else {
                            tv_day.setTextColor(attrs.monthLabelColor);
                        }
                    }

                    weekColumns[j].v_today_circle.setVisibility(View.GONE);
                    if (attrs.markEveryFirstofMonth) {
                        if (!findCurrentMonth(m)) {
                            if (days[j].value == 1) {
                                weekColumns[j].every_first.setVisibility(View.VISIBLE);
                            }
                        } else {
                            weekColumns[j].every_first.setVisibility(View.INVISIBLE);
                        }

                    }
                }

                weekColumns[j].blocked.setVisibility(View.GONE);
               /* if (min.get(Calendar.YEAR) == m.year || max.get(Calendar.YEAR) == m.year) {
                    if (min.get(Calendar.MONTH) == m.value || max.get(Calendar.MONTH) == m.value) {
                        if (min.get(Calendar.DAY_OF_MONTH) < days[j].value || max.get(Calendar.DAY_OF_MONTH) < days[j].value) {
                            weekColumns[j].v_today_circle.setVisibility(View.GONE);
                            weekColumns[j].v_event_circle.setVisibility(View.GONE);
                            weekColumns[j].every_first.setVisibility(View.GONE);
                            weekColumns[j].blocked.setVisibility(View.VISIBLE);
                            String blockeskey = String.format("%d%d%d", days[j].value, m.value, m.year);
                            blockedDates.put(blockeskey, true);
                        }
                    }
                }*/
            }
        }

    }

    private void setEventCircleColor(HashSet<String> eventsWithoutDuplication, View event_circle1, View event_circle2, View event_circle3) {
        try {
            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            event_circle1.setVisibility(View.INVISIBLE);
            event_circle2.setVisibility(View.INVISIBLE);
            event_circle3.setVisibility(View.INVISIBLE);
            ArrayList<String> events = new ArrayList<String>();
            for (String event : eventsWithoutDuplication) {
                events.add(event);
            }
            //Collections.reverse(events);
            if (events != null) {
                switch (events.size()) {
                    case 1:
                        event_circle2.setVisibility(View.VISIBLE);
                        findEventColor(event_circle2, events.get(0));
                        break;
                    case 2:
                        event_circle2.setVisibility(View.VISIBLE);
                        event_circle3.setVisibility(View.VISIBLE);
                        event_circle2.bringToFront();
                        /*ConstraintLayout.LayoutParams eventParam1 = (ConstraintLayout.LayoutParams) event_circle2.getLayoutParams();
                        eventParam1.setMargins(0, 0, Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (Integer) 2, displayMetrics)), 0);
                        event_circle2.setLayoutParams(eventParam1);*/
                        ConstraintLayout.LayoutParams eventParam2 = (ConstraintLayout.LayoutParams) event_circle3.getLayoutParams();
                        eventParam2.setMargins(0, 0, Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) 15, displayMetrics)), 0);
                        event_circle3.setLayoutParams(eventParam2);
                        findEventColor(event_circle2, events.get(0));
                        findEventColor(event_circle3, events.get(1));
                        break;
                    case 3:
                        event_circle1.setVisibility(View.VISIBLE);
                        event_circle2.setVisibility(View.VISIBLE);
                        event_circle3.setVisibility(View.VISIBLE);
                        event_circle1.bringToFront();
                        event_circle2.bringToFront();
                        findEventColor(event_circle1, events.get(0));
                        findEventColor(event_circle2, events.get(1));
                        findEventColor(event_circle3, events.get(2));
                        break;
                    default:

                        break;
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void findEventColor(View event_circle, String event) {
        try {
            if (!TextUtils.isEmpty(event) && event != null && event_circle != null) {
                if (event.toLowerCase().equals(CalendarEvent.Medication.name().toLowerCase())) {
                    ((GradientDrawable) event_circle.getBackground()).setColor(ContextCompat.getColor(mContext, R.color.medication));
                } else if (event.toLowerCase().equals(CalendarEvent.Refill.name().toLowerCase())) {
                    event_circle.setBackground(ContextCompat.getDrawable(mContext, R.drawable.refill_circle));
                    ((GradientDrawable) event_circle.getBackground()).setColor(ContextCompat.getColor(mContext, R.color.refill));
                } else if (event.toLowerCase().equals(CalendarEvent.Shipment.name().toLowerCase())) {
                    event_circle.setBackground(ContextCompat.getDrawable(mContext, R.drawable.shipment_circle));
                    ((GradientDrawable) event_circle.getBackground()).setColor(ContextCompat.getColor(mContext, R.color.shipment));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    private boolean hasEvent(int day, int month, int year) {
        String key = String.format("%d%d%d", day, month, year);

        return mEvents.containsKey(key);
    }

    public LinkedHashSet<String> hasEventColor(int day, int month, int year) {
        try {
            String key = String.format("%d%d%d", day, month, year);
            if (mEventsColor.containsKey(key)) {
                return mEventsColor.get(key);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private boolean hasSelectedDate(int day, int month, int year) {
        String key = String.format("%d%d%d", day, month, year);
        Log.i("key", String.valueOf(selectedDate.containsKey(key)));
        return selectedDate.containsKey(key);
    }

    @Override
    public int getItemCount() {
        return mMonths.size();
    }


    public void getPreviousMonth() {
        if (earlyMonthLoaded <= PAYLOAD) {
            for (int i = earlyMonthLoaded - 1; i > 0; i--) {
                if (minDateComparsion(earlyMonthLoaded, min.get(Calendar.MONTH), earlyYearLoaded, min.get(Calendar.YEAR))) {
                    mMonths.add(0, new Month(i, earlyYearLoaded));
                }

                //notifyItemRangeInserted(0, 1);

            }

            if (minDateComparsion(earlyMonthLoaded, min.get(Calendar.MONTH), earlyYearLoaded, min.get(Calendar.YEAR))) {
                earlyMonthLoaded = 12 - (PAYLOAD - earlyMonthLoaded);
                earlyYearLoaded--;
            } else {
                return;
            }

            for (int i = 12; i >= earlyMonthLoaded; i--) {
                if (minDateComparsion(i, min.get(Calendar.MONTH), earlyYearLoaded, min.get(Calendar.YEAR))) {
                    mMonths.add(0, new Month(i, earlyYearLoaded));
                }
                //notifyItemRangeInserted(0, 1);
            }
        } else {
            for (int i = earlyMonthLoaded - 1; i >= earlyMonthLoaded - PAYLOAD; i--) {
                if (minDateComparsion(i, min.get(Calendar.MONTH), earlyYearLoaded, min.get(Calendar.YEAR))) {
                    mMonths.add(0, new Month(i, earlyYearLoaded));
                }
                //notifyItemRangeInserted(0, 1);

            }
            earlyMonthLoaded -= PAYLOAD;
        }

        notifyItemRangeInserted(0, PAYLOAD);

    }

    public void getNextMonths() {
        int positionStart = mMonths.size() - 1;
        if (laterMonthLoaded > (12 - PAYLOAD)) {
            for (int i = laterMonthLoaded + 1; i <= 12; i++) {
                if (maxDateComparsion(laterMonthLoaded, max.get(Calendar.MONTH), laterYearLoaded, max.get(Calendar.YEAR))) {
                    mMonths.add(new Month(i, laterYearLoaded));
                }
            }
            if (maxDateComparsion(laterMonthLoaded, max.get(Calendar.MONTH), laterYearLoaded, max.get(Calendar.YEAR))) {
                laterMonthLoaded = laterMonthLoaded + PAYLOAD - 12;
                laterYearLoaded++;
            } else {
                return;
            }
            for (int i = 1; i <= laterMonthLoaded; i++) {
                if (maxDateComparsion(i, max.get(Calendar.MONTH), laterYearLoaded, max.get(Calendar.YEAR))) {
                    mMonths.add(new Month(i, laterYearLoaded));
                }
            }
        } else {
            for (int i = laterMonthLoaded + 1; i <= laterMonthLoaded + PAYLOAD; i++) {
                if (maxDateComparsion(i, max.get(Calendar.MONTH), laterYearLoaded, max.get(Calendar.YEAR))) {
                    mMonths.add(new Month(i, laterYearLoaded));
                }
            }
            laterMonthLoaded += PAYLOAD;
        }
        notifyItemRangeInserted(positionStart, PAYLOAD);
    }

    public void setOnDayClickListener(CalendarView.OnDayClickListener onDayClickListener) {
        this.onDayClickListener = onDayClickListener;
    }

    public void addEvent(int day, int month, int year, LinkedHashSet<String> events) {
        //Month m = getMonth(month, year);
        String key = String.format("%d%d%d", day, month, year);
        mEventsColor.put(key, events);
        mEvents.put(key, true);
        notifyDataSetChanged();
    }

    public void deleteEvent(int day, int month, int year) {
        String key = String.format("%d%d%d", day, month, year);
        mEvents.remove(key);
        mEventsColor.remove(key);
        notifyDataSetChanged();
    }

    public boolean maxDateComparsion(int monthLoaded, int maxMonth, int laterYearLoaded, int maxYear) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, monthLoaded);
            calendar.set(Calendar.YEAR, laterYearLoaded);
            Date selectedDate = sdf.parse(sdf.format(calendar.getTime()));
            calendar.set(Calendar.MONTH, maxMonth);
            calendar.set(Calendar.YEAR, maxYear);
            Date maxDate = sdf.parse(sdf.format(calendar.getTime()));
            if (maxDate.compareTo(selectedDate) == 0) {
                maxReachedEnd = true;
                return false;
            } else {
                return true;
            }

/*
            if (laterYearLoaded < maxYear) {
                return true;
            } else {
                if (monthLoaded <= maxMonth) {
                    return true;
                } else {
                    maxReachedEnd = true;
                    return false;
                }
            }*/
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }


    public boolean minDateComparsion(int monthLoaded, int minMonth, int laterYearLoaded, int minYear) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, monthLoaded);
            calendar.set(Calendar.YEAR, laterYearLoaded);
            Date selectedDate = sdf.parse(sdf.format(calendar.getTime()));
            calendar.set(Calendar.MONTH, minMonth);
            calendar.set(Calendar.YEAR, minYear);
            Date minDate = sdf.parse(sdf.format(calendar.getTime()));
            if (minDate.compareTo(selectedDate) == 0) {
                minReachedEnd = true;
                return false;
            } else {
                return true;
            }
            /*if (laterYearLoaded > minYear) {
                return true;
            } else {
                //return false;
                if (monthLoaded >= minMonth) {
                    return true;
                } else {
                    minReachedEnd = true;
                    return false;
                }
            }*/
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private boolean findCurrentMonth(Month month) {
        try {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
            Date currentDate = sdf.parse(sdf.format(calendar.getTime()));
            calendar.set(Calendar.MONTH, month.value - 1);
            calendar.set(Calendar.YEAR, month.year);
            Date selectedDate = sdf.parse(sdf.format(calendar.getTime()));
            if (currentDate.compareTo(selectedDate) == 0) {
                return true;
            } else {
                return false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }


}
