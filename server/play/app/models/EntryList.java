package models;

import java.util.*;

import com.avaje.ebean.*;

import play.db.ebean.*;
import play.Logger;

/**
 * Created because Ebean doesn't work in one annoying aspect:
 * When I try to join the Entry and Company tables so that I can do
 * proper ordering and so on, well I can get them joined. But I can't
 * insert new entries with the company_id field being filled in.
 */
public class EntryList implements Comparator<Entry> {

    private static final int PAGE_SIZE = 100;

    public enum SortBy {
        TECH_ID("tech_id"),
        TIME("entry_time"),
        PROJECT("project_id"),
        TRUCK_NUMBER("truck_number"),
        COMPANY("company"),
        STATE("state"),
        CITY("city"),
        ZIPCODE("zipcode"),
        STREET("street");

        String code;

        SortBy(String code) {
            this.code = code;
        }

        public static SortBy from(String code) {
            for (SortBy item : values()) {
                if (item.code == code) {
                    return item;
                }
            }
            return null;
        }
    }

    public enum Order {
        ASC("asc"),
        DESC("desc");

        String code;

        Order(String code) {
            this.code = code;
        }

        public static Order from(String code) {
            for (Order item : values()) {
                if (item.code == code) {
                    return item;
                }
            }
            return null;
        }
    }

    class Parameters {
        SortBy        sortBy;
        Order         order;
        List<Integer> projectIds;

        Parameters() {
            sortBy = SortBy.TIME;
            order = Order.DESC;
        }

        Parameters(Parameters other) {
            sortBy = other.sortBy;
            order = other.order;
            if (other.projectIds != null) {
                projectIds = new ArrayList<Integer>();
                for (int i = 0; i < other.projectIds.size(); i++) {
                    projectIds.add(other.projectIds.get(i));
                }
            }
        }

        public boolean equals(Parameters other) {
            if (sortBy == other.sortBy && order == other.order) {
                if (projectIds == null && other.projectIds == null) {
                    return true;
                }
                if (projectIds == null || other.projectIds == null) {
                    return false;
                }
                if (projectIds.size() != other.projectIds.size()) {
                    return false;
                }
                for (int i = 0; i < projectIds.size(); i++) {
                    if (projectIds.get(i) != other.projectIds.get(i)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    List<Entry> mComputed;
    List<Entry> mResult;
    HashMap<Long, Company> mMap            = new HashMap<Long, Company>();
    Parameters             mLastParameters = new Parameters();
    Parameters             mNextParameters = new Parameters();
    int                    mPageSize       = PAGE_SIZE;
    int mPage;

    public EntryList() {
    }

    public void setSortBy(String sortBy) {
        mNextParameters.sortBy = SortBy.from(sortBy);
    }

    public void setOrder(String order) {
        mNextParameters.order = Order.from(order);
    }

    public void setPage(int page) {
        mPage = page;
    }

    public void setProjectIdFilter(List<Integer> projects) {
        mNextParameters.projectIds = projects;
    }

    String getOrderBy() {
        return mNextParameters.sortBy.code + " " + mNextParameters.order.code;
    }

    void compute() {
        mComputed = null;
        boolean needsSort = false;
        switch (mNextParameters.sortBy) {
            case TECH_ID:
            case TIME:
            case TRUCK_NUMBER:
            case PROJECT:
                mComputed = Entry.find.where().orderBy(getOrderBy()).findList();
                break;
            case COMPANY:
            case STATE:
            case CITY:
            case STREET:
            case ZIPCODE:
                mComputed = Entry.find.findList();
                needsSort = true;
                break;
            default:
                Logger.error("Invalid sort by code: " + mNextParameters.sortBy.toString());
                return;
        }
        Logger.info("COMPUTED " + mComputed.size());

        if (mNextParameters.projectIds != null && mNextParameters.projectIds.size() > 0) {
            List<Entry> list = new ArrayList<Entry>();
            for (Entry entry : mComputed) {
                if (mNextParameters.projectIds.contains(entry.project_id)) {
                    list.add(entry);
                }
            }
            mComputed = list;
        }
        if (needsSort) {
            mComputed.sort(this);
        }
        mLastParameters = new Parameters(mNextParameters);
    }

    public List<Entry> getList() {
        if (!mNextParameters.equals(mLastParameters)) {
            compute();
        }
        if (mComputed == null) {
            return new ArrayList<Entry>();
        }
        if (mComputed.size() > mPageSize) {
            int fromIndex = mPage * mPageSize;
            int toIndex = fromIndex + mPageSize;
            if (toIndex > mResult.size()) {
                toIndex = mResult.size();
            }
            mResult = mComputed.subList(fromIndex, toIndex);
        } else {
            mResult = mComputed;
        }
        return mResult;
    }

    public int getPageIndex() {
        return mPage;
    }

    public boolean hasPrev() {
        return mPage > 0 && mComputed != null && mComputed.size() > mPageSize;
    }

    public boolean hasNext() {
        return mComputed != null && ((mPage + 1) * mPageSize < mComputed.size());
    }

    public int getTotalRowCount() {
        if (mComputed == null) {
            return 0;
        }
        return mComputed.size();
    }

    public String getDisplayXtoYofZ(String to, String of) {
        StringBuilder sbuf = new StringBuilder();
        int iFrom = mPage * mPageSize + 1;
        int iTo = iFrom + mPageSize - 1;
        if (iTo >= mComputed.size()) {
            iTo = mComputed.size() - 1;
        }
        sbuf.append(iFrom + 1);
        sbuf.append(to);
        sbuf.append(iTo + 1);
        sbuf.append(of);
        sbuf.append(mComputed.size() + 1);
        return sbuf.toString();
    }

    Company getCompany(long id) {
        if (mMap.containsKey(id)) {
            return mMap.get(id);
        }
        Company company = Company.find.byId(id);
        mMap.put(id, company);
        return company;
    }

    public int compare(Entry o1, Entry o2) {
        Company c1 = getCompany(o1.address_id);
        Company c2 = getCompany(o2.address_id);
        int value;
        switch (mNextParameters.sortBy) {
            case COMPANY:
                if (c1.name != null && c2.name != null) {
                    value = c1.name.compareTo(c2.name);
                } else if (c1.name != null) {
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
        if (mNextParameters.order == Order.DESC) {
            value *= -1;
        }
        return value;
    }
}
