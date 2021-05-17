/**
 * Copyright 2020, FleetTLC. All rights reserved
 */
package controllers;

import play.mvc.*;
import play.data.*;
import play.data.validation.ValidationError;

import models.*;
import modules.WorkerExecutionContext;
import views.formdata.EntryFormData;
import views.formdata.InputSearch;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.HashMap;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import java.util.concurrent.*;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import modules.AmazonHelper;
import modules.AmazonHelper.OnDownloadComplete;
import modules.Globals;
import modules.EntryListWriter;
import modules.TimeHelper;
import modules.StringHelper;

import java.io.File;

import play.db.ebean.Transactional;
import play.Logger;
import play.libs.concurrent.HttpExecution;

/**
 * Manage a database of equipment.
 */
public class EntryController extends Controller {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'zzz";
    private static final String EXPORT_FILENAME = "/tmp/export.csv";

    private AmazonHelper mAmazonHelper;
    private EntryPagedList mLastEntryList;
    private FormFactory mFormFactory;
    private SimpleDateFormat mDateFormat;
    private Globals mGlobals;
    private WorkerExecutionContext mExecutionContext;
    private EntryListWriter mExportWriter;
    private boolean mExporting;
    private boolean mAborted;
    private long mDownloadPicturesForEntryId;
    private long mDownloadPicturesLastAttemptEntryId;
    private ArrayList<Long> mDownloaded = new ArrayList<>();
    private ArrayList<CompletableFuture<Result>> mDownloadStatusRequests = new ArrayList<>();

    @Inject
    public EntryController(AmazonHelper amazonHelper,
                           FormFactory formFactory,
                           WorkerExecutionContext executionContext,
                           Globals globals) {
        mAmazonHelper = amazonHelper;
        mFormFactory = formFactory;
        mDateFormat = new SimpleDateFormat(DATE_FORMAT);
        mGlobals = globals;
        mExecutionContext = executionContext;
    }

    @Security.Authenticated(Secured.class)
    public Result list2(int page, int pageSize, String sortBy, String order, String searchTerm, String searchField) {
        return list(page, pageSize, sortBy, order, searchTerm, searchField);
    }

    @Security.Authenticated(Secured.class)
    public Result list(int page, int pageSize, String sortBy, String order, String searchTerm, String searchField) {
        EntryPagedList list = new EntryPagedList();

        String decodedSearchTerm = StringHelper.decode(searchTerm);

        Logger.info("list(" + page + ", " + pageSize + ", " + sortBy + ", " + order + ", " + decodedSearchTerm + ", " + searchField + ")");

        Form<InputSearch> searchForm = mFormFactory.form(InputSearch.class);

        list.setSearch(decodedSearchTerm, searchField);
        list.setPage(page);
        list.setPageSize(pageSize);
        list.setSortBy(sortBy);
        list.setOrder(order);
        list.computeFilters(Secured.getClient(ctx()));
        list.compute();

        mLastEntryList = list;

        InputSearch isearch = new InputSearch(decodedSearchTerm, searchField);
        searchForm.fill(isearch);

        return ok(views.html.entry_list.render(list, searchForm, Secured.getClient(ctx())));
    }

    public Result list() {
        return list(0, 100, "date", "desc", "null", "null");
    }

    @Security.Authenticated(Secured.class)
    public Result search(int page, int pageSize, String sortBy, String order) {
        Form<InputSearch> searchForm = mFormFactory.form(InputSearch.class).bindFromRequest();
        InputSearch isearch = searchForm.get();
        String searchTerm = isearch.searchTerm;
        String searchField = isearch.searchField;

        Logger.info("search(" + page + ", " + pageSize + ", " + sortBy + ", " + order + ", " + searchTerm + ", " + searchField + ")");

        EntryPagedList list = new EntryPagedList();
        list.setSearch(searchTerm, searchField);
        list.setPage(page);
        list.setPageSize(pageSize);
        list.setSortBy(sortBy);
        list.setOrder(order);
        list.computeFilters(Secured.getClient(ctx()));
        list.clearCache();
        list.compute();

        mLastEntryList = list;

        searchForm.fill(isearch);

        return ok(views.html.entry_list.render(list, searchForm, Secured.getClient(ctx())));
    }

    @Security.Authenticated(Secured.class)
    public Result searchClear() {
        EntryPagedList list = new EntryPagedList();
        list.computeFilters(Secured.getClient(ctx()));
        list.compute();
        Form<InputSearch> searchForm = mFormFactory.form(InputSearch.class);

        mLastEntryList = list;

        return ok(views.html.entry_list.render(list, searchForm, Secured.getClient(ctx())));
    }

    @Security.Authenticated(Secured.class)
    public Result showByTruck(long truck_id) {
        mGlobals.setClearSearch(false);
        EntryPagedList list = new EntryPagedList();
        list.setByTruckId(truck_id);
        list.computeFilters(Secured.getClient(ctx()));
        list.compute();

        mLastEntryList = list;

        Form<InputSearch> searchForm = mFormFactory.form(InputSearch.class);
        return ok(views.html.entry_list.render(list, searchForm, Secured.getClient(ctx())));
    }

    public CompletionStage<Result> computeTotalNumRows() {
        Executor myEc = HttpExecution.fromThread((Executor) mExecutionContext);
        return CompletableFuture.completedFuture(mLastEntryList.computeTotalNumRows()).thenApplyAsync(result -> {
            StringBuilder sbuf = new StringBuilder();
            sbuf.append(mLastEntryList.getPrevClass());
            sbuf.append("|");
            sbuf.append(mLastEntryList.getDisplayingXtoYofZ());
            sbuf.append("|");
            sbuf.append(mLastEntryList.getNextClass());
            sbuf.append("|");
            if (result == 0) {
                sbuf.append("No entries");
            } else if (result == 1) {
                sbuf.append("One entry");
            } else {
                sbuf.append(result);
                sbuf.append(" entries found");
            }
            return ok(sbuf.toString());
        }, myEc);
    }

    @Security.Authenticated(Secured.class)
    public Result export(String searchTerm, String searchField) {
        if (mExporting) {
            mAborted = true;
            mExporting = false;
            Logger.info("export() ABORT INITIATED");
            return ok("R");
        }
        mExporting = true;
        mAborted = false;
        Client client = Secured.getClient(ctx());

        String decodedSearchTerm = StringHelper.decode(searchTerm);

        Logger.info("export(" + decodedSearchTerm + ", " + searchField + ") START");

        EntryPagedList entryList = new EntryPagedList();
        entryList.computeFilters(client);
        entryList.setSearch(decodedSearchTerm, searchField);
        entryList.computeFilters(Secured.getClient(ctx()));
        entryList.compute();
        entryList.computeTotalNumRows();
        mExportWriter = new EntryListWriter(entryList);

        int count;
        try {
            mExportWriter.open();
            count = mExportWriter.writeNext();
        } catch (IOException ex) {
            mExporting = false;
            return badRequest2(ex.getMessage());
        }
        Logger.info("export(): " + entryList.getTotalRowCount() + ", " + entryList.getDisplayingXtoYofZ());
        return ok("#" + Integer.toString(count) + "...");
    }

    public Result exportNext() {
        try {
            if (mAborted) {
                mExportWriter.abort();
                mAborted = false;
                mExporting = false;
                Logger.info("exportNext(): ABORT");
                return ok("R");
            }
            if (mExportWriter.computeNext()) {
                int count = mExportWriter.writeNext();
                Logger.info("exportNext() " + count);
                return ok("#" + Integer.toString(count) + "...");
            } else {
                Logger.info("exportNext() DONE!");
                mExportWriter.finish(EXPORT_FILENAME);
                mExporting = false;
                return ok("E" + mExportWriter.getFile().getName());
            }
        } catch (IOException ex) {
            mExporting = false;
            mAborted = false;
            return badRequest2(ex.getMessage());
        }
    }

    public Result exportDownload() {
        return ok(mExportWriter.getFile());
    }

    /**
     * Load pictures in background
     */
    public Result reloadImages() {
        mDownloaded.clear();
        return Results.redirect(routes.EntryController.pictures(mDownloadPicturesLastAttemptEntryId));
    }

    public CompletionStage<Result> checkLoadingPictures() {
        CompletableFuture<Result> completableFuture = new CompletableFuture<>();
        if (mDownloadPicturesForEntryId > 0) {
            downloadPicturesFor(mDownloadPicturesForEntryId);
            mDownloadPicturesForEntryId = 0;
            mDownloadStatusRequests.add(completableFuture);
        } else {
            completableFuture.complete(ok("0"));
        }
        return completableFuture;
    }

    private void downloadPicturesFor(long entry_id) {
        Entry entry = Entry.find.byId(entry_id);
        if (entry == null) {
            Logger.error("Could not find entry ID " + entry_id);
        } else {
            loadPictures(entry);
        }
    }

    /**
     * Display the picture for an entry.
     */
    public Result pictures(Long entry_id) {
        Entry entry = Entry.find.byId(entry_id);
        if (entry == null) {
            return badRequest2("Could not find entry ID " + entry_id);
        }
        mDownloadPicturesLastAttemptEntryId = entry_id;
        if (!mDownloaded.contains(entry_id)) {
            Logger.info("pictures(" + entry_id + "): permitted size=" + mDownloaded.size());
            mDownloaded.add(entry_id);
            mDownloadPicturesForEntryId = entry_id;
        } else {
            Logger.info("pictures(" + entry_id + "): already done");
        }
        return ok(views.html.entry_list_picture.render(entry.getPictures()));
    }

    private void loadPictures(Entry entry) {
        entry.loadPictures(request().host(), mAmazonHelper, () -> loadPicturesDone());
    }

    private void loadPicturesDone() {
        if (mDownloadStatusRequests.size() > 0) {
            for (CompletableFuture<Result> completableFuture : mDownloadStatusRequests) {
                completableFuture.complete(ok("1"));
            }
            mDownloadStatusRequests.clear();
        }
    }

    public Result getImage(String picture) {
        File localFile = mAmazonHelper.getLocalFile(picture);
        if (localFile.exists()) {
            return ok(localFile);
        } else {
            return ok(picture);
        }
    }

    /**
     * Display details for the entry including delete button.
     * Note: intentionally NOT secure
     */
    public Result view(Long entry_id) {
        Entry entry = Entry.find.byId(entry_id);
        if (entry == null) {
            return badRequest2("Could not find entry ID " + entry_id);
        }
        loadPictures(entry);
        return ok(views.html.entry_view.render(entry, Secured.getClient(ctx())));
    }

    /**
     * Display the notes for an entry.
     */
    public Result notes(Long entry_id) {
        Entry entry = Entry.find.byId(entry_id);
        if (entry == null) {
            return badRequest2("Could not find entry ID " + entry_id);
        }
        return ok(views.html.entry_list_note.render(entry.getNotes(getClientId())));
    }

    private long getClientId() {
        Client client = Secured.getClient(ctx());
        return client == null ? 0 : client.id;
    }

    @Security.Authenticated(Secured.class)
    public Result delete(Long entry_id) {
        String host = request().host();
        Entry entry = Entry.find.byId(entry_id);
        if (entry != null) {
            entry.remove(mAmazonHelper.deleteAction().host(host).listener((deleted, errors) -> {
                Logger.info("Entry has been deleted: " + entry_id);
            }));
        }
        return list();
    }

    @Security.Authenticated(Secured.class)
    public Result edit(Long entry_id) {
        Entry entry = Entry.find.byId(entry_id);
        if (entry == null) {
            return badRequest2("Could not find entry ID " + entry_id);
        }
        String home = "/entry/" + entry_id + "/view";
        Form<EntryFormData> entryForm = mFormFactory.form(EntryFormData.class).fill(new EntryFormData(entry));
        DynamicForm noteValues = mFormFactory.form().fill(entry.getNoteValues(getClientId()));
        return ok(views.html.entry_editForm.render(entry.id, entryForm, noteValues, home, Secured.getClient(ctx())));
    }

    @Transactional
    @Security.Authenticated(Secured.class)
    public Result update(Long id) throws PersistenceException {
        Client client = Secured.getClient(ctx());
        Form<EntryFormData> entryForm = mFormFactory.form(EntryFormData.class).bindFromRequest();
        if (entryForm.hasErrors()) {
            StringBuffer sbuf = new StringBuffer();
            for (ValidationError error : entryForm.allErrors()) {
                sbuf.append(error.message());
                sbuf.append("\n");
            }
            return badRequest(sbuf.toString());
        }
        Entry entry = Entry.find.byId(id);
        if (entry == null) {
            return badRequest("Could not find entry with ID " + id);
        }
        try {
            EntryFormData data = entryForm.get();
            Technician tech = Technician.findMatch(data.name);
            if (tech != null) {
                entry.tech_id = tech.id.intValue();
            }
            SimpleDateFormat format = new SimpleDateFormat(TimeHelper.DATE_TIME_FORMAT);
            entry.entry_time = format.parse(data.date);
            Project project = Project.findByName(data.rootProject, data.subProject);
            if (project != null) {
                entry.project_id = project.id;
            }
            Company company = Company.findByNameAndAddress(data.companyName, data.address);
            if (company != null) {
                entry.company_id = company.id;
            } else {
                Logger.error("Could not find company with name='" + data.companyName + "', address='" + data.address + "'");
            }
            Truck truck = Truck.getTruckByID(data.truck);
            if (truck != null) {
                entry.truck_id = truck.id;
            } else {
                Truck previous = entry.getTruck();
                if (previous != null) {
                    String[] items = data.truck.split(":");
                    if (items.length > 0) {
                        truck = new Truck();
                        truck.company_name_id = previous.company_name_id;
                        truck.project_id = previous.project_id;
                        truck.upload_id = previous.upload_id;
                        truck.created_by = client.id.intValue();
                        truck.created_by_client = true;
                        truck.truck_number = items[0];
                        if (items.length > 1) {
                            truck.license_plate = items[1];
                        }
                        truck.save();
                        Logger.info("New truck: " + truck.toString());
                        entry.truck_id = truck.id;
                    }
                }
            }
            Entry.Status status = Entry.findStatus(data.status);
            if (status != null) {
                entry.status = status;
            }
            EntryEquipmentCollection.replace(entry.equipment_collection_id, Equipment.getChecked(entryForm));

            // Don't know how this happened, but somehow very occassionally note_collection_id became 0.
            if (entry.note_collection_id == 0) {
                entry.note_collection_id = Version.inc(Version.NEXT_NOTE_COLLECTION_ID);
            }
            EntryNoteCollection.replace(entry.note_collection_id, Note.getChecked(entryForm));
            entry.applyToNotes(client.id, entryForm);
            entry.update();
        } catch (ParseException ex) {
            Logger.error(ex.getMessage());
            return badRequest(ex.getMessage());
        }
        return ok(views.html.entry_view.render(entry, Secured.getClient(ctx())));
    }

    @Security.Authenticated(Secured.class)
    synchronized
    public CompletionStage<Result> deleteEntries(String idsLine) {
        String[] idsArray = idsLine.split(",");
        List<String> idsList = new ArrayList<String>(Arrays.asList(idsArray));
        String commaSeparated;
        Logger.info("Delete request " + idsLine);
        if (idsList.size() > 0) {
            String idsText = idsList.get(0);
            try {
                long id = Long.parseLong(idsText);
                deleteNext(id);
                idsList.remove(0);
                commaSeparated = String.join(",", idsList);
            } catch (NumberFormatException ex) {
                Logger.error(ex.getMessage());
                commaSeparated = "";
            }
        } else {
            commaSeparated = "";
        }
        CompletableFuture<Result> completableFuture = new CompletableFuture<>();
        completableFuture.complete(ok(commaSeparated));
        return completableFuture;
    }

    private boolean deleteNext(Long id) {
        try {
            String host = request().host();
            Logger.debug("Deleting ENTRY ID " + id);
            Entry entry = Entry.find.byId(id);
            if (entry != null) {
                entry.remove(mAmazonHelper.deleteAction().host(host).listener((deleted, errors) -> {
                    Logger.warn("Remote entry has been deleted: " + id);
                }));
            } else {
                Logger.error("Called deleteNext() with bad ID " + id);
                return false;
            }
        } catch (NumberFormatException ex) {
            Logger.error(ex.getMessage());
            return false;
        }
        return true;
    }

    @Transactional
    @BodyParser.Of(BodyParser.Json.class)
    public Result enter() {
        Entry entry = new Entry();
        ArrayList<String> missing = new ArrayList<String>();
        JsonNode json = request().body().asJson();
        Logger.debug("GOT: " + json.toString());
        boolean retServerId = false;
        JsonNode value;
        value = json.findValue("tech_id");
        if (value == null) {
            missing.add("tech_id");
        } else {
            entry.tech_id = value.intValue();
        }
        int secondary_tech_id;
        value = json.findValue("secondary_tech_id");
        if (value != null) {
            secondary_tech_id = value.intValue();
        } else {
            secondary_tech_id = 0;
        }
        value = json.findValue("date_string");
        if (value != null) {
            String date_value = value.textValue();
            try {
                entry.entry_time = mDateFormat.parse(date_value);
                entry.time_zone = StringHelper.pickOutTimeZone(date_value, 'Z');
            } catch (Exception ex) {
                Logger.error("While parsing " + date_value + ":" + ex.getMessage());
            }
        } else {
            value = json.findValue("date");
            if (value == null) {
                missing.add("date");
            } else {
                entry.entry_time = new Date(value.longValue());
            }
        }
        value = json.findValue("server_id");
        if (value != null) {
            entry.id = value.longValue();
            retServerId = true;
            Entry existing;
            if (entry.id > 0) {
                existing = Entry.find.byId(entry.id);
                if (existing == null) {
                    existing = Entry.findByDate(entry.tech_id, entry.entry_time);
                    if (existing != null) {
                        Logger.info("Could not find entry with ID " + entry.id + ", so located based on time=" + entry.entry_time);
                    }
                } else {
                    existing.entry_time = entry.entry_time;
                    existing.tech_id = entry.tech_id;
                }
            } else {
                existing = Entry.findByDate(entry.tech_id, entry.entry_time);
            }
            if (existing == null) {
                entry.id = 0L;
            } else {
                entry = existing;
            }
        }
        // This is OLD SCHOOL:
        int truck_id;
        String truck_number;
        String license_plate;
        value = json.findValue("truck_id");
        if (value == null) {
            truck_id = 0;
        } else {
            truck_id = value.intValue();
        }
        value = json.findValue("truck_number_string");
        if (value == null) {
            value = json.findValue("truck_number");
            if (value != null) {
                truck_number = Integer.toString(value.intValue());
            } else {
                truck_number = null;
            }
        } else {
            truck_number = value.textValue();
        }
        value = json.findValue("license_plate");
        if (value != null) {
            license_plate = value.textValue();
        } else {
            license_plate = null;
        }
        value = json.findValue("project_id");
        if (value == null) {
            // This is OLD school:
            value = json.findValue("project_name");
            if (value == null) {
                missing.add("project_id");
                missing.add("project_name");
            } else {
                String projectName = value.textValue();
                Project project = Project.findByName(projectName);
                if (project != null) {
                    entry.project_id = project.id;
                } else {
                    missing.add("project named '" + projectName + "'");
                }
            }
        } else {
            entry.project_id = value.longValue();
        }
        if (truck_number == null && license_plate == null && truck_id == 0) {
            // In flow style this is okay. So just ignore this.
        } else {
            // Old pre-flow school:

            // Note: I don't call Version.inc(Version.VERSION_TRUCK) intentionally.
            // The reason is that other techs don't need to know about a local techs truck updates.
            Truck truck = Truck.add(entry.project_id, entry.company_id, truck_id, truck_number, license_plate, entry.tech_id);
            entry.truck_id = truck.id;
        }
        value = json.findValue("status");
        if (value != null) {
            entry.status = Entry.Status.from(value.textValue());
        }
        value = json.findValue("address_id");
        if (value == null) {
            // This is old school:
            value = json.findValue("address");
            if (value == null) {
                missing.add("address_id");
            } else {
                String address = value.textValue().trim();
                if (address.length() > 0) {
                    try {
                        Company company = Company.parse(address);
                        Company existing = Company.has(company);
                        if (existing != null) {
                            company = existing;
                        } else {
                            company.created_by = entry.tech_id;
                            company.save();
                            Version.inc(Version.VERSION_COMPANY);
                        }
                        entry.company_id = company.id;
                        CompanyName.save(company.name);
                    } catch (Exception ex) {
                        return badRequest2("address: " + ex.getMessage());
                    }
                } else {
                    return badRequest2("address");
                }
            }
        } else {
            entry.company_id = value.longValue();
            Company company = Company.get(entry.company_id);
            if (company == null) {
                Technician.AddReloadCode(entry.tech_id, 'c');
                return badRequest2("address: no such company with ID " + entry.company_id);
            }
        }
        value = json.findValue("equipment");
        if (value != null) {
            if (value.getNodeType() != JsonNodeType.ARRAY) {
                Logger.error("Expected array for element 'equipment'");
            } else {
                int collection_id;
                if (entry.equipment_collection_id > 0) {
                    collection_id = (int) entry.equipment_collection_id;
                    EntryEquipmentCollection.deleteByCollectionId(entry.equipment_collection_id);
                } else {
                    collection_id = Version.inc(Version.NEXT_EQUIPMENT_COLLECTION_ID);
                }
                boolean newEquipmentCreated = false;
                Iterator<JsonNode> iterator = value.elements();
                while (iterator.hasNext()) {
                    JsonNode ele = iterator.next();
                    EntryEquipmentCollection collection = new EntryEquipmentCollection();
                    collection.collection_id = (long) collection_id;
                    JsonNode subValue = ele.findValue("equipment_id");
                    if (subValue == null) {
                        // This is old school:
                        subValue = ele.findValue("equipment_name");
                        if (subValue == null) {
                            missing.add("equipment_id");
                            missing.add("equipment_name");
                        } else {
                            String name = subValue.textValue();
                            List<Equipment> equipments = Equipment.findByName(name);
                            Equipment equipment;
                            if (equipments.size() == 0) {
                                equipment = new Equipment();
                                equipment.name = name;
                                equipment.created_by = entry.tech_id;
                                equipment.created_by_client = false;
                                equipment.save();
                                Logger.info("Created new equipment: " + equipment.toString());
                                newEquipmentCreated = true;

                                ProjectEquipmentCollection.addNew(entry.project_id, equipment);
                                Logger.info("Registered for project:" + entry.project_id);
                            } else {
                                if (equipments.size() > 1) {
                                    Logger.error("Too many equipments found with name: " + name);
                                }
                                equipment = equipments.get(0);
                            }
                            collection.equipment_id = equipment.id;
                        }
                    } else {
                        collection.equipment_id = subValue.longValue();
                    }
                    collection.save();
                }
                entry.equipment_collection_id = collection_id;
                if (newEquipmentCreated) {
                    Version.inc(Version.VERSION_EQUIPMENT);
                }
            }
        }
        HashMap<Long, Long> pictureIdMap = new HashMap<Long, Long>(); // app-side pictureId, PictureCollection.id
        value = json.findValue("picture");
        if (value != null) {
            if (value.getNodeType() != JsonNodeType.ARRAY) {
                Logger.error("Expected array for element 'picture'");
            } else {
                int collection_id;
                if (entry.picture_collection_id > 0) {
                    collection_id = (int) entry.picture_collection_id;
                    PictureCollection.deleteByCollectionId(entry.picture_collection_id, null);
                } else {
                    collection_id = Version.inc(Version.NEXT_PICTURE_COLLECTION_ID);
                }
                Iterator<JsonNode> iterator = value.elements();
                while (iterator.hasNext()) {
                    JsonNode ele = iterator.next();
                    PictureCollection collection = new PictureCollection();
                    collection.collection_id = (long) collection_id;
                    // This reference is old school. Will not be seen in flow style:
                    JsonNode subValue = ele.findValue("note");
                    if (subValue != null) {
                        collection.note = subValue.textValue();
                    }
                    // The new way:
                    JsonNode flowElementIdValue = ele.findValue("flow_element_id");
                    if (flowElementIdValue != null) {
                        long flowElementId = flowElementIdValue.longValue();
                        collection.flow_element_id = flowElementId;
                    } else {
                        JsonNode flowStageValue = ele.findValue("flow_stage");
                        if (flowStageValue != null) {
                            String flowStage = flowStageValue.textValue();
                            if (flowStage != null) {
                                if (flowStage.equals("truck_number")) { // See DCPing for the "truck_number" flow reference
                                    collection.flow_element_id = PictureCollection.FLOW_TRUCK_NUMBER_ID;
                                } else if (flowStage.equals("truck_damage")) { // See DCPing for the track_damage flow reference
                                    collection.flow_element_id = PictureCollection.FLOW_TRUCK_DAMAGE_ID;
                                }
                            }
                        }
                    }
                    subValue = ele.findValue("filename");
                    if (subValue == null) {
                        missing.add("filename");
                    } else {
                        collection.picture = subValue.textValue();
                        collection.save();
                    }
                    JsonNode idValue = ele.findValue("id");
                    if (idValue != null) {
                        long appId = idValue.longValue();
                        pictureIdMap.put(appId, collection.id);
                    }
                }
                entry.picture_collection_id = collection_id;
            }
        }
        value = json.findValue("notes");
        if (value != null) {
            if (value.getNodeType() != JsonNodeType.ARRAY) {
                Logger.error("Expected array for element 'notes'");
            } else {
                int collection_id;
                if (entry.note_collection_id > 0) {
                    collection_id = (int) entry.note_collection_id;
                    EntryNoteCollection.deleteByCollectionId(entry.note_collection_id);
                } else {
                    collection_id = Version.inc(Version.NEXT_NOTE_COLLECTION_ID);
                }
                Iterator<JsonNode> iterator = value.elements();
                while (iterator.hasNext()) {
                    JsonNode ele = iterator.next();
                    EntryNoteCollection collection = new EntryNoteCollection();
                    collection.collection_id = (long) collection_id;
                    JsonNode subValue = ele.findValue("id");
                    if (subValue == null) {
                        // This is old school:
                        subValue = ele.findValue("name");
                        if (subValue == null) {
                            missing.add("note:id, note:name");
                        } else {
                            String name = subValue.textValue();
                            List<Note> notes = Note.findByName(name);
                            if (notes == null || notes.size() == 0) {
                                // A tech can get into a situation where they are effectively
                                // creating notes, even though the APP doesn't explicitly allow it,
                                // if the server created a note, they got it, and then the server
                                // later deletes it before the tech's app can update.
                                Note note = new Note();
                                note.name = name;
                                note.type = Note.Type.TEXT;
                                note.created_by = entry.tech_id;
                                note.created_by_client = false;
                                note.save();
                                collection.note_id = note.id;
                                // Note: don't inc version number because other techs don't really need to know.
                                Logger.info("Created new note: " + note.toString());
                            } else if (notes.size() > 1) {
                                Logger.error("Too many notes with name: " + name);
                                missing.add("note: '" + name + "'");
                            } else {
                                collection.note_id = notes.get(0).id;
                            }
                        }
                    } else {
                        collection.note_id = subValue.longValue();
                    }
                    subValue = ele.findValue("value");
                    if (value == null) {
                        missing.add("note:value");
                    } else {
                        collection.note_value = subValue.textValue();
                    }
                    collection.save();
                }
                entry.note_collection_id = collection_id;
            }
        }
        if (missing.size() > 0) {
            return missingRequest(missing);
        }
        if (entry.id != null && entry.id > 0) {
            entry.update();
            Logger.debug("Updated entry " + entry.id);
        } else {
            entry.save();
            Logger.debug("Created new entry " + entry.id);
        }
        if (secondary_tech_id > 0) {
            SecondaryTechnician.save(entry.id, secondary_tech_id);
            Logger.debug("Assigned secondary technician " + secondary_tech_id + " to " + entry.id);
        }
        long ret_id;
        if (retServerId) {
            ret_id = entry.id;
        } else {
            ret_id = 0;
        }
        return ok(Long.toString(ret_id));
    }

    Result missingRequest(ArrayList<String> missing) {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("Missing fields:");
        boolean comma = false;
        for (String field : missing) {
            if (comma) {
                sbuf.append(", ");
            }
            sbuf.append(field);
            comma = true;
        }
        sbuf.append("\n");
        return badRequest2(sbuf.toString());
    }

    Result badRequest2(String field) {
        Logger.error("ERROR: " + field);
        return badRequest(field);
    }


//    public CompletionStage<Result> exportBackground() {
//        if (mExporting) {
//            return ok("...");
//        }
//        mExporting = true;
//        Client client = Secured.getClient(ctx());
//        Executor myEc = HttpExecution.fromThread((Executor) mExecutionContext);
//        return CompletableFuture.completedFuture(export(client)).thenApplyAsync(result -> {
//            mExporting = false;
//            if (result.startsWith("ERROR:")) {
//                return badRequest2(result);
//            }
//            return ok(result);
//        }, myEc);
//    }

//    @Security.Authenticated(Secured.class)
//    public CompletionStage<Result> exportBackground() {
//        Logger.info("export() BACKGROUND BEGIN");
//        Client client = Secured.getClient(ctx());
//        Executor myEc = HttpExecution.fromThread((Executor) mExecutionContext);
//        return CompletableFuture.supplyAsync(() -> export(client), myEc);
//        return CompletionStage<Result>.completedFuture(export(client)).thenApplyAsync(result -> {
//            return result;
//        }, myEc);
//    }


//    @Security.Authenticated(Secured.class)
//    public Result pageSize(String size) {
//        return pageSize2(size, "");
//    }
//
//    @Security.Authenticated(Secured.class)
//    public Result pageSize2(String size, String searchTerm, String searchField) {
//        EntryPagedList list = new EntryPagedList();
//        list.computeFilters(Secured.getClient(ctx()));
//        list.setSearch(searchTerm, searchField);
//        try {
//            int pageSize = Integer.parseInt(size);
//            list.setPageSize(pageSize);
//        } catch (NumberFormatException ex) {
//            Logger.error(ex.getMessage());
//        }
//        list.setSortBy(mLastEntryList.getSortBy());
//        list.setOrder(mLastEntryList.getOrder());
//        list.computeFilters(Secured.getClient(ctx()));
//        list.compute();
//
//        mLastEntryList = list;
//
//        Form<InputSearch> searchForm = mFormFactory.form(InputSearch.class);
//        InputSearch isearch = new InputSearch(search);
//        searchForm.fill(isearch);
//
//        return ok(views.html.entry_list.render(list, searchForm, Secured.getClient(ctx())));
//    }
}

