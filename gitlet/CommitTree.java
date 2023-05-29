package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.io.File;

public class CommitTree implements Serializable {
    /** Used to store branch name and the commit it points to. */
    private HashMap<String, String> branchToCommit;
    /** Used to store the current active branch. */
    private String activeBranch;

    public HashMap<String, String> getBranchToCommit() {
        return branchToCommit;
    }

    public String getActiveBranch() {
        return activeBranch;
    }

    public void setActiveBranch(String ab) {
        this.activeBranch = ab;
    }

    public CommitTree() {
        activeBranch = "master";
        Commit c = new Commit();
        branchToCommit = new HashMap<>();
        branchToCommit.put(activeBranch, c.getId());

        File x = Utils.join(Main.COMMITS, c.getId());
        Utils.writeObject(x, c);
    }

    public void addCommit(String msg) {
        Commit parent = Commit.getCommit(branchToCommit.get(activeBranch));
        Commit c = new Commit(msg, parent);
        branchToCommit.put(activeBranch, c.getId());

        File f = Utils.join(Main.COMMITS, c.getId());
        Utils.writeObject(f, c);
    }

    public void addCommit(String msg, Commit secondParent) {
        Commit parent = Commit.getCommit(branchToCommit.get(activeBranch));
        Commit c = new Commit(msg, parent, secondParent);
        branchToCommit.put(activeBranch, c.getId());

        File f = Utils.join(Main.COMMITS, c.getId());
        Utils.writeObject(f, c);
    }

    public Commit headCommit() {
        return Commit.getCommit(branchToCommit.get(activeBranch));
    }

    public void newBranch(String b) {
        branchToCommit.put(b, branchToCommit.get(activeBranch));
    }
}
