/**
 * Copyright 2018, FleetTLC. All rights reserved
 */
package models;

import java.util.*;
import java.lang.Integer;

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
public class TruckV6 extends com.avaje.ebean.Model {

    private static final long serialVersionUID = 1L;

    @Id
    public Long id;

    @Constraints.Required
    public int truck_number;

    @Constraints.Required
    public String license_plate;

    @Constraints.Required
    public int upload_id;

    @Constraints.Required
    public int created_by;

    @Constraints.Required
    public boolean created_by_client;

    @Constraints.Required
    public long project_id;

    @Constraints.Required
    public long company_name_id;

    public static Finder<Long, TruckV6> find = new Finder<Long, TruckV6>(TruckV6.class);

    // TODO: Once data has been transfered, this code can be removed
    // and the database can be cleaned up by removing this table.
    @Transactional
    public static void transfer() {
    }

}

