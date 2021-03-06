/**
 * Copyright 2019, FleetTLC. All rights reserved
 */
package controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Transaction;

import play.mvc.*;
import play.data.*;

import static play.data.Form.*;

import play.Logger;

import com.avaje.ebean.PagedList;

import models.*;
import views.formdata.InputLines;
import modules.WorkerExecutionContext;
import modules.CalcHelper;

import java.util.List;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import play.db.ebean.Transactional;
import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.*;

import play.libs.concurrent.HttpExecution;

/**
 * Manage a database of equipment.
 */
public class EquipmentController extends Controller {

    class CalcRowItem extends CalcHelper<Equipment> {

        public CalcRowItem(WorkerExecutionContext ec) {
            super(ec);
        }

        protected PagedList<Equipment> init(int page, boolean disabled) {
            return init(Equipment.list(page, disabled));
        }

        @Override
        protected String getTag(Equipment item) {
            if (mPass == 0) {
                return item.getTagProjectsLine();
            } else {
                return item.getTagNumEntries();
            }
        }

        @Override
        protected String calc(Equipment equip) {
            if (mPass == 0) {
                return equip.getProjectsLine();
            } else {
                return Integer.toString(equip.getNumEntries());
            }
        }

        @Override
        protected boolean repeat() {
            return (mPass == 0);
        }
    }

    private FormFactory formFactory;
    private WorkerExecutionContext executionContext;
    private CalcRowItem calcRowItem;

    @Inject
    public EquipmentController(FormFactory formFactory,
                               WorkerExecutionContext ec) {
        this.formFactory = formFactory;
        this.executionContext = ec;
        this.calcRowItem = new CalcRowItem(ec);
    }

    /**
     * Display the list of equipments.
     */
    @Security.Authenticated(Secured.class)
    public Result list(int page, boolean disabled) {
        return ok(views.html.equipment_list.render(
                calcRowItem.init(page, disabled),
                Secured.getClient(ctx()),
                disabled));
    }

    public Result LIST() {
        return Results.redirect(routes.EquipmentController.list(0, false));
    }

    public CompletionStage<Result> fillNext() {
        return calcRowItem.calcNext(this);
    }

    /**
     * Display the 'edit form' of an existing equipment name.
     *
     * @param id Id of the equipment to edit
     */
    @Security.Authenticated(Secured.class)
    public Result edit(Long id) {
        Form<Equipment> equipmentForm = formFactory.form(Equipment.class).fill(Equipment.find.byId(id));
        return ok(views.html.equipment_editForm.render(id, equipmentForm));
    }

    /**
     * Handle the 'edit form' submission
     *
     * @param id Id of the equipment to edit
     */
    @Security.Authenticated(Secured.class)
    public Result update(Long id) throws PersistenceException {
        Form<Equipment> equipmentForm = formFactory.form(Equipment.class).bindFromRequest();
        if (equipmentForm.hasErrors()) {
            return badRequest(views.html.equipment_editForm.render(id, equipmentForm));
        }
        Transaction txn = Ebean.beginTransaction();
        try {
            Equipment newEquipmentData = equipmentForm.get();
            if (Equipment.hasEquipmentWithName(newEquipmentData.name, id)) {
                return badRequest("Already have an equipment named: " + newEquipmentData.name);
            }
            Equipment savedEquipment = Equipment.find.byId(id);
            if (savedEquipment != null) {
                savedEquipment.name = newEquipmentData.name;
                savedEquipment.update();
                flash("success", "Equipment " + newEquipmentData.name + " has been updated");
                txn.commit();

                Version.inc(Version.VERSION_EQUIPMENT);
            }
        } finally {
            txn.end();
        }
        return LIST();
    }

    /**
     * Display the 'new equipment form'.
     */
    @Security.Authenticated(Secured.class)
    public Result create() {
        Form<Equipment> equipmentForm = formFactory.form(Equipment.class);
        return ok(views.html.equipment_createForm.render(equipmentForm));
    }

    /**
     * Handle the 'new equipment form' submission
     */
    @Security.Authenticated(Secured.class)
    public Result save() {
        Form<Equipment> equipmentForm = formFactory.form(Equipment.class).bindFromRequest();
        if (equipmentForm.hasErrors()) {
            return badRequest(views.html.equipment_createForm.render(equipmentForm));
        }
        Client client = Secured.getClient(ctx());
        Equipment equip = equipmentForm.get();
        if (client != null && client.id > 0) {
            equip.created_by = Long.valueOf(client.id).intValue();
            equip.created_by_client = true;
        }
        List<Equipment> equipments = Equipment.findByName(equip.name);
        if (equipments.size() > 0) {
            return badRequest("An equipment already exists with name: " + equip.name);
        }
        equip.save();
        flash("success", "Equipment " + equipmentForm.get().name + " has been created");
        return LIST();
    }

    /**
     * Display a form to enter in many equipments at once.
     */
    @Security.Authenticated(Secured.class)
    public Result createMany() {
        Form<InputLines> linesForm = formFactory.form(InputLines.class);
        return ok(views.html.equipments_createForm.render(linesForm));
    }

    /**
     * Create many equipments at once.
     */
    @Transactional
    public Result saveMany() {
        Form<InputLines> linesForm = formFactory.form(InputLines.class).bindFromRequest();
        if (linesForm.hasErrors()) {
            return badRequest(views.html.equipments_createForm.render(linesForm));
        }
        String[] lines = linesForm.get().getLines();
        Project activeProject = null;
        final String PROJECT = "Project:";
        for (String name : lines) {
            name = name.trim();
            if (!name.isEmpty()) {
                if (name.startsWith(PROJECT)) {
                    String fullName = name.substring(PROJECT.length()).trim();
                    Project project = Project.findByName(fullName);
                    if (project != null) {
                        activeProject = project;
                        continue;
                    }
                }
                if (activeProject == null) {
                    return badRequest("First line must indicate valid project");
                }
                List<Equipment> equipments = Equipment.findByName(name);
                Equipment equipment;
                if (equipments.size() == 0) {
                    equipment = new Equipment();
                    equipment.name = name;
                    equipment.save();
                } else {
                    if (equipments.size() > 1) {
                        Logger.error("Too many equipments with name: " + name);
                    }
                    equipment = equipments.get(0);
                }
                if (activeProject != null) {
                    ProjectEquipmentCollection collection = new ProjectEquipmentCollection();
                    collection.project_id = activeProject.id;
                    collection.equipment_id = equipment.id;
                    if (!ProjectEquipmentCollection.has(collection)) {
                        collection.save();
                    }
                }
            }
        }
        Version.inc(Version.VERSION_EQUIPMENT);
        return LIST();
    }

    /**
     * Handle equipment deletion
     */
    @Security.Authenticated(Secured.class)
    public Result delete(Long id) {
        Equipment equipment = Equipment.find.byId(id);
        if (Entry.hasEntryForEquipment(id)) {
            equipment.disabled = true;
            equipment.update();
            Logger.info("Equipment has been disabled: it had entries: " + equipment.name);
        } else {
            equipment.remove();
            Logger.info("Equipment has been deleted: " + equipment.name);
        }
        Version.inc(Version.VERSION_EQUIPMENT);
        return LIST();
    }

    @Security.Authenticated(Secured.class)
    @Transactional
    public Result enable(Long id) {
        Equipment equipment = Equipment.find.byId(id);
        equipment.disabled = false;
        equipment.update();
        Version.inc(Version.VERSION_EQUIPMENT);
        return LIST();
    }

    @Transactional
    public Result addProject(Long id, Long project_id) {
        ProjectEquipmentCollection collection = new ProjectEquipmentCollection();
        collection.project_id = project_id;
        collection.equipment_id = id;
        if (!ProjectEquipmentCollection.has(collection)) {
            collection.save();
            Version.inc(Version.VERSION_EQUIPMENT);
        }
        return edit(id);
    }

    @Transactional
    public Result removeProject(Long id, Long project_id) {
        ProjectEquipmentCollection collection = new ProjectEquipmentCollection();
        collection.project_id = project_id;
        collection.equipment_id = id;
        collection = ProjectEquipmentCollection.get(collection);
        if (collection != null) {
            ProjectEquipmentCollection.find.byId(collection.id).delete();
            Version.inc(Version.VERSION_EQUIPMENT);
        }
        return edit(id);
    }

    public Result queryOldWay() {
        return query(false);
    }

    public Result queryWithRoot() {
        return query(true);
    }

    private Result query(boolean withRoot) {
        JsonNode json = request().body().asJson();
        JsonNode value = json.findValue("tech_id");
        if (value == null) {
            return badRequest("missing field: tech_id");
        }
        int tech_id = value.intValue();
        ObjectNode top = Json.newObject();
        ArrayNode array = top.putArray("equipments");
        List<Equipment> equipments = Equipment.all();
        List<Long> equipmentIds = new ArrayList<Long>();
        for (Equipment item : equipments) {
            ObjectNode node = array.addObject();
            node.put("id", item.id);
            node.put("name", item.name);
            node.put("disabled", item.disabled);
            if (item.created_by != 0) {
                node.put("is_local", true);
            }
            equipmentIds.add(item.id);
        }
        array = top.putArray("project_equipment");
        for (ProjectEquipmentCollection item : ProjectEquipmentCollection.find.all()) {
            if (equipmentIds.contains(item.equipment_id)) {
                Project project = Project.get(item.project_id);
                Equipment equipment = Equipment.get(item.equipment_id);
                if (project != null && !project.disabled && equipment != null && !equipment.disabled && project.isValid(withRoot)) {
                    ObjectNode node = array.addObject();
                    node.put("id", item.id);
                    node.put("project_id", item.project_id);
                    node.put("equipment_id", item.equipment_id);
                }
            }
        }
        return ok(top);
    }

}

