package com.unit16.z.time;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;


import org.joda.time.LocalDate;

import com.google.common.base.Predicate;
import com.unit16.conf.C;
import com.unit16.conf.Config;
import com.unit16.conf.GroupDepInit;
import com.unit16.conf.Group.Checker;
import com.unit16.conf.GroupDef.DelayedInit;
import com.unit16.conf.GroupDef;
import com.unit16.z.Sequence;

public class DateRange {
	
	public final static GroupDef CONF = GroupDef.create("dates")
		.check(new Checker.I() {		
			@Override
			public boolean validate(Config c) {
				if(c.isset(TO) && c.isset(FROM)) {
					LocalDate f = c.get(FROM);
					LocalDate t = c.get(TO);
					if(t.isBefore(f)) {
						setError(String.format("to '%s' is before from '%s'", t, f));
						return false;
					}												
				}
				return true;
			}		
			})				
		.init(new DelayedInit() {
			@Override public void init(GroupDepInit d) {
				d.multiple(EXCLUDE);
			}
		})
		.build();
	
	
	private final static GroupDef EXCLUDE = GroupDef.create("exclude")
		.init(new DelayedInit() {
			@Override public void init(GroupDepInit d) {
				d.mergeValues(DateRange.CONF);				
			}
		})
		.build();
		
	
	public static final C<LocalDate> FROM = CONF.localDate("from").build();
	public static final C<LocalDate> TO = CONF.localDate("to").optional().build();
	public static final C<Boolean> WEEKDAYONLY = CONF.bool("weekday_only").Default(Boolean.TRUE).build();
	
	final LocalDate from;
	final LocalDate to;
	final List<Predicate<LocalDate>> filters;
	
	public DateRange(LocalDate from_, LocalDate to_, List<Predicate<LocalDate>> filters_) {
		checkArgument(!to_.isBefore(from_));
		from = from_;
		to = to_;
		filters = filters_;
	}

	public Sequence<LocalDate> period() {
		Sequence<LocalDate> ret = DateUtils.period(from, to);
		for(Predicate<LocalDate> l : filters) {
			ret = ret.filter(l);
		}
		return ret;
	}
	
	public static DateRange create(Config c) {
		ArrayList<Predicate<LocalDate>> f = new ArrayList<>();
		if(c.get(WEEKDAYONLY)) {
			f.add(DateUtils.WEEKDAY);
		}
		if(c.isset(EXCLUDE)) {
			for(Config sub : c.subs(EXCLUDE)) {
				final DateRange r = DateRange.create(sub);
				f.add(new Predicate<LocalDate>() {
					@Override
					public boolean apply(LocalDate input) {
						return !r.inRange(input);
					}				
				});
			}
		}
		return new DateRange(c.get(FROM), c.isset(TO) ? c.get(TO) : c.get(FROM), f);
	}

	private boolean inRange(LocalDate input) {
		return !input.isBefore(from) && !input.isAfter(to);
	}
		
}
