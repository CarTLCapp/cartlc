package models;

import java.util.*;
import javax.persistence.*;

import com.avaje.ebean.Model;
import play.data.format.*;
import play.data.validation.*;

import com.avaje.ebean.*;
import play.Logger;

/**
 * Note entity managed by Ebean
 */
@Entity 
public class Note extends Model implements Comparable<Note> {

    public static class MalformedFieldException extends Exception {
        public MalformedFieldException(String message) {
            super(message);
        }
    }

    private static final long serialVersionUID = 1L;

	@Id
    public Long id;

    @Constraints.Required
    public String name;

    @Constraints.Required
    public short type;

    @Constraints.Required
    public boolean disabled;

    /**
     * Generic query helper for entity Computer with id Long
     */
    public static Finder<Long,Note> find = new Finder<Long,Note>(Note.class);

    public static List<Note> list() { return list("name", "asc"); }

    public static List<Note> list(String sortBy, String order) {
        return
                find.where()
                        .orderBy(sortBy + " " + order)
                        .findList();
    }

    public static Note findByName(String name) {
        List<Note> items = find.where()
                .eq("name", name)
                .findList();
        if (items.size() == 1) {
            return items.get(0);
        } else if (items.size() > 1) {
            Logger.error("Too many notes named: " + name);
        }
        return null;
    }

    public List<Project> getProjects() {
        return ProjectNoteCollection.findProjects(id);
    }

    public String getProjectsLine() {
        List<Project> items = getProjects();
        Collections.sort(items);
        StringBuilder sbuf = new StringBuilder();
        for (Project project : items) {
            if (sbuf.length() > 0) {
                sbuf.append(", ");
            }
            sbuf.append(project.name);
        }
        return sbuf.toString();
    }

    public static boolean hasProject(long note_id, long project_id) {
        Note note = find.byId(note_id);
        if (note != null) {
            return note.hasProject(project_id);
        }
        return false;
    }

    public boolean hasProject(long project_id) {
        for (Project project : getProjects()) {
            if (project.id == project_id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Note item) {
        return name.compareTo(item.name);
    }
}
