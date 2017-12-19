package models;

import java.util.*;

import com.avaje.ebean.*;

import play.db.ebean.*;
import play.Logger;

import play.twirl.api.Html;

/**
 * Created because Ebean doesn't work in one annoying aspect:
 * When I try to join the Entry and Company tables so that I can do
 * proper ordering and so on, well I can get them joined. But I can't
 * insert new entries with the company_id field being filled in.
 */
public class EntryList extends BaseList<Entry> implements Comparator<Entry> {

    protected String mSearch;

    public EntryList() {
        super();
    }

    @Override
    protected List<Entry> getOrderedList() {
        return applySearch(Entry.find.where().orderBy(getOrderBy()).findList());
    }

    @Override
    protected List<Entry> getRawList() {
        return applySearch(Entry.find.findList());
    }

    @Override
    protected void sort(List<Entry> list) {
        list.sort(this);
    }

    @Override
    protected long getProjectId(Entry entry) {
        return entry.project_id;
    }

    @Override
    protected String getCompanyName(Entry entry) {
        return entry.getCompany();
    }

    public void setSearch(String search) {
        if (search != null) {
            mSearch = search.trim();
        } else {
            mSearch = null;
        }
    }

    protected List<Entry> applySearch(List<Entry> list) {
        if (mSearch == null || mSearch.isEmpty()) {
            return list;
        }
        List<Entry> result = new ArrayList<Entry>();
        for (Entry entry : list) {
            if (entry.match(mSearch)) {
                result.add(entry);
            }
        }
        return result;
    }

    public Html highlightSearch(String element) {
        if (element != null && mSearch != null && !mSearch.isEmpty()) {
            if (element.contains(mSearch)) {
                int pos = element.indexOf(mSearch);
                if (pos >= 0) {
                    StringBuilder sbuf = new StringBuilder();
                    sbuf.append(element.substring(0, pos));
                    sbuf.append("<mark>");
                    sbuf.append(element.substring(pos, mSearch.length() + pos));
                    sbuf.append("</mark>");
                    sbuf.append(element.substring(mSearch.length() + pos));
                    return Html.apply(sbuf.toString());
                }
            }
        }
        return Html.apply(element);
    }

    public int compare(Entry o1, Entry o2) {
        int value;
        if (mNextParameters.sortBy == SortBy.TRUCK_NUMBER) {
            value = o1.truckCompareTo(o2);
        } else if (mNextParameters.sortBy == SortBy.PROJECT) {
            value = o1.getProjectLine().compareTo(o2.getProjectLine());
        } else {
            Company c1 = Company.get(o1.company_id);
            Company c2 = Company.get(o2.company_id);
            switch (mNextParameters.sortBy) {
                case COMPANY:
                    String c1name = c1.getName();
                    String c2name = c2.getName();
                    if (c1name != null && c2name != null) {
                        value = c1name.compareTo(c2name);
                    } else if (c1name != null) {
                        value = -1;
                    } else {
                        value = 1;
                    }
                    break;
                case STATE:
                    if (c1.state != null && c2.state != null) {
                        value = c1.state.compareTo(c2.state);
                    } else if (c1.state != null) {
                        value = -1;
                    } else {
                        value = 1;
                    }
                    break;
                case CITY:
                    if (c1.city != null && c2.city != null) {
                        value = c1.city.compareTo(c2.city);
                    } else if (c1.city != null) {
                        value = -1;
                    } else {
                        value = 1;
                    }
                    break;
                case STREET:
                    if (c1.street != null && c2.street != null) {
                        value = c1.street.compareTo(c2.street);
                    } else if (c1.street != null) {
                        value = -1;
                    } else {
                        value = 1;
                    }
                    break;
                case ZIPCODE:
                    if (c1.zipcode != null && c2.zipcode != null) {
                        value = c1.zipcode.compareTo(c2.zipcode);
                    } else if (c1.zipcode != null) {
                        value = -1;
                    } else {
                        value = 1;
                    }
                    break;
                default:
                    value = 0;
                    break;
            }
        }
        if (mNextParameters.order == Order.DESC) {
            value *= -1;
        }
        return value;
    }
}
