package models;

import java.util.*;

import javax.persistence.*;

import play.db.ebean.*;
import play.data.validation.*;
import play.db.ebean.Transactional;
import play.data.format.*;

import play.Logger;

/**
 * User entity managed by Ebean
 */
@Entity
public class Truck extends com.avaje.ebean.Model {

    private static final long serialVersionUID = 1L;

    @Id
    public Long id;

    @Constraints.Required
    public int truck_number;

    @Constraints.Required
    public String license_plate;

    public static Finder<Long, Truck> find = new Finder<Long, Truck>(Truck.class);

    public static List<Truck> list() {
        return find.all();
    }

    public static List<Truck> findBy(int truck_number, String license_plate) {
        List<Truck> list;
        if (truck_number != 0) {
            list = find.where().eq("truck_number", truck_number).findList();
        } else if (license_plate != null) {
            list = find.where().eq("license_plate", license_plate).findList();
        } else {
            list = null;
        }
        return list;
    }

    public static Truck findFirst(int truck_number, String license_plate) {
        List<Truck> trucks = findBy(truck_number, license_plate);
        if (trucks == null || trucks.size() == 0) {
            return null;
        }
        if (trucks.size() > 1) {
            Logger.error("Found too many trucks with " + truck_number + ", " + license_plate);
        }
        return trucks.get(0);
    }

    public static Truck add(int truck_number, String license_plate) {
        List<Truck> list = findBy(truck_number, license_plate);
        Truck truck = null;
        if (list.size() > 1) {
            Logger.error("Found too many trucks with " + truck_number + ", " + license_plate);
            for (Truck t : list) {
                Logger.error(t.toString());
            }
            truck = list.get(0);
        } else if (list.size() == 1) {
            truck = list.get(0);
        }
        if (truck == null) {
            truck = new Truck();
            truck.truck_number = truck_number;
            truck.license_plate = license_plate;
            truck.save();
        } else {
            if (truck_number != 0 && truck_number != truck.truck_number) {
                truck.truck_number = truck_number;
            }
            if (license_plate != null && !license_plate.equals(truck.license_plate)) {
                truck.license_plate = license_plate;
            }
            truck.update();
        }
        return truck;
    }

    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(id);
        sbuf.append(":");
        if (truck_number != 0) {
            sbuf.append(truck_number);
            sbuf.append("#");
        }
        if (license_plate != null) {
            sbuf.append(license_plate);
        }
        return sbuf.toString();
    }
}

