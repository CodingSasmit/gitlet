package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.io.File;

public class Commit implements Serializable {
    /** Used to store exact date and time of commit. */
    private Date date;
    /** Fields for the message and id of commit,
     * as well as id of both parents. */
    private String msg, id, parentID, mergeParentID;
    /** Used to store exact depth of commit within tree. */
    private int depth;
    /** Used to store files in the commit and their respective blobs. */
    private HashMap<String, String> fileToBlobID;

    public Date getDate() {
        return date;
    }

    public String getMsg() {
        return msg;
    }

    public String getId() {
        return id;
    }

    public String getParentID() {
        return parentID;
    }

    public String getMergeParentID() {
        return mergeParentID;
    }

    public int getDepth() {
        return depth;
    }

    public HashMap<String, String> getFileToBlobID() {
        return fileToBlobID;
    }

    public Commit() {
        date = new Date(0);
        msg = "initial commit";
        id = Utils.sha1(Utils.serialize(date), msg);
        parentID = null;
        mergeParentID = null;
        depth = 0;
        fileToBlobID = new HashMap<>();
        updateShortenedCommits(id);
    }

    public Commit(String m, Commit parent) {
        date = new Date();
        this.msg = m;
        id = Utils.sha1(Utils.serialize(date), m);

        parentID = parent.getId();
        mergeParentID = null;
        depth = parent.getDepth() + 1;
        fileToBlobID = new HashMap<>();
        fileToBlobID.putAll(parent.getFileToBlobID());
        updateShortenedCommits(id);

        List<String> x = Utils.plainFilenamesIn(Main.ADDITION);
        if (x != null && !x.isEmpty()) {
            for (String fname : x) {
                File f = Utils.join(Main.ADDITION, fname);
                String hash = Utils.sha1(fname, Utils.readContents(f));

                f.renameTo(Utils.join(Main.BLOBS, hash));
                fileToBlobID.put(fname, hash);
            }
        }

        x = Utils.plainFilenamesIn(Main.REMOVAL);
        if (x != null && !x.isEmpty()) {
            for (String fname : x) {
                Utils.join(Main.REMOVAL, fname).delete();
                fileToBlobID.remove(fname);
            }
        }
    }

    public Commit(String m, Commit parent, Commit secondParent) {
        this(m, parent);
        mergeParentID = secondParent.getId();
    }

    public Commit parent() {
        if (parentID == null) {
            return null;
        }
        return getCommit(parentID);
    }

    public Commit mergeParent() {
        if (mergeParentID == null) {
            return null;
        }
        return getCommit(mergeParentID);
    }

    public static Commit getCommit(String id) {
        return Utils.readObject(
                Utils.join(Main.COMMITS, id), Commit.class);
    }

    public boolean isTracked(String fname) {
        return fileToBlobID.containsKey(fname);
    }

    public static void updateShortenedCommits(String commitId) {
        HashMap<String, String> h = Main.getShortenedCommits();
        h.put(commitId.substring(0, 6), commitId);
        Main.saveShortenedCommits(h);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===\n");
        sb.append("commit " + id + "\n");
        sb.append("Date: " + Main.dateFormat(date) + "\n");
        sb.append(msg + "\n");
        return sb.toString();
    }

    public boolean equals(Commit c) {
        return id.equals(c.id);
    }
}
