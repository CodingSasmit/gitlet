package gitlet;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Sasmit Agarwal
 */
public class Main {

    /** Used to store file directories. */
    static final File CWD = new File(".");
    /** Used to store file directories. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** Used to store file directories. */
    static final File COMMITS = Utils.join(GITLET_FOLDER, "commits");
    /** Used to store file directories. */
    static final File BLOBS = Utils.join(GITLET_FOLDER, "blobs");
    /** Used to store file directories. */
    static final File COMMIT_TREE = Utils.join(GITLET_FOLDER, "commitTree.x");
    /** Used to store file directories. */
    static final File SHORTENED_COMMITS =
            Utils.join(GITLET_FOLDER, "commits.x");

    /** Used to store file directories. */
    static final File STAGE = Utils.join(GITLET_FOLDER, "stage");
    /** Used to store file directories. */
    static final File ADDITION = Utils.join(STAGE, "addition");
    /** Used to store file directories. */
    static final File REMOVAL = Utils.join(STAGE, "removal");

    /** Used to store format for date conversions. */
    static final String FORMAT = "%ta %tb %td %tT %tY %tz";

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            CommitTree tree = getTree();
            System.out.println(tree.getBranchToCommit());
            return;
        }

        switch (args[0]) {
        case "init":
            init();
            break;
        case "add":
            add(args[1]);
            break;
        case "commit":
            commit(args[1]);
            break;
        case "rm":
            rm(args[1]);
            break;
        case "log":
            log();
            break;
        case "global-log":
            globalLog();
            break;
        case "find":
            find(args[1]);
            break;
        case "status":
            status();
            break;
        case "checkout":
            if (args[1].equals("--")) {
                checkoutf(getTree().headCommit(), args[2]);
            } else if (args.length == 2) {
                checkoutb(args[1]);
            } else {
                checkoutf(args[1], args[3]);
            }
            break;
        case "branch":
            branch(args[1]);
            break;
        case "rm-branch":
            rmBranch(args[1]);
            break;
        case "reset":
            reset(args[1]);
            break;
        case "merge":
            merge(args[1]);
        default:
            break;
        }
    }

    public static void init() {
        if (GITLET_FOLDER.exists()) {
            error("A Gitlet version-control system already"
                    + " exists in the current directory.");
        }
        GITLET_FOLDER.mkdir();
        COMMITS.mkdirs();
        BLOBS.mkdirs();
        ADDITION.mkdirs();
        REMOVAL.mkdirs();

        HashMap<String, String> h = new HashMap<>();
        saveShortenedCommits(h);

        CommitTree tree = new CommitTree();
        saveTree(tree);
    }

    public static void add(String fname) {
        if (Utils.join(REMOVAL, fname).exists()) {
            Utils.join(REMOVAL, fname).delete();
            return;
        }

        File file = Utils.join(ADDITION, fname);
        File origfile = Utils.join(CWD, fname);
        if (!origfile.exists()) {
            error("File does not exist.");
        }
        Object contents = Utils.readContents(origfile);
        String hash = Utils.sha1(fname, contents);

        CommitTree tree = getTree();
        Commit c = tree.headCommit();
        if (c.getFileToBlobID().containsKey(fname)
                && c.getFileToBlobID().get(fname).equals(hash)) {
            if (file.exists()) {
                Utils.restrictedDelete(file);
            }
            return;
        }
        Utils.writeContents(file, contents);
    }

    public static void commit(String msg) {
        if (msg == null || msg.equals("")) {
            error("Please enter a commit message.");
        }

        if (!changesStaged()) {
            error("No changes added to the commit.");
        }

        CommitTree tree = getTree();
        tree.addCommit(msg);
        saveTree(tree);
    }

    public static boolean changesStaged() {
        List<String> x = Utils.plainFilenamesIn(ADDITION);
        List<String> y = Utils.plainFilenamesIn(REMOVAL);
        return (x != null && !x.isEmpty()) || (y != null && !y.isEmpty());
    }

    public static void rm(String fname) {
        if (Utils.join(ADDITION, fname).exists()) {
            Utils.join(ADDITION, fname).delete();
            return;
        }

        Commit headCommit = getTree().headCommit();
        if (headCommit.isTracked(fname)) {
            Utils.writeContents(Utils.join(REMOVAL, fname),
                    "staged for removal");
            File f = Utils.join(CWD, fname);
            if (f.exists()) {
                Utils.restrictedDelete(f);
            }
        } else {
            error("No reason to remove the file.");
        }
    }

    public static void log() {
        Commit c = getTree().headCommit();
        while (c != null) {
            System.out.println(c);
            c = c.parent();
        }
    }

    public static void globalLog() {
        List<String> x = Utils.plainFilenamesIn(COMMITS);
        if (x != null && !x.isEmpty()) {
            for (String y : x) {
                System.out.println(Commit.getCommit(y));
            }
        }
    }

    public static void find(String msg) {
        List<String> x = Utils.plainFilenamesIn(COMMITS);
        boolean found = false;
        if (x != null && !x.isEmpty()) {
            for (String y : x) {
                Commit c = Commit.getCommit(y);
                if (c.getMsg().equals(msg)) {
                    System.out.println(c.getId());
                    found = true;
                }
            }
        }

        if (!found) {
            error("Found no commit with that message.");
        }
    }

    public static void status() {
        System.out.println("=== Branches ===");
        CommitTree tree = getTree();
        System.out.println("*" + tree.getActiveBranch());
        for (String branch : tree.getBranchToCommit().keySet()) {
            if (!branch.equals(tree.getActiveBranch())) {
                System.out.println(branch);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        List<String> x = Utils.plainFilenamesIn(ADDITION);
        if (x != null && !x.isEmpty()) {
            for (String file : x) {
                System.out.println(file);
            }
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        x = Utils.plainFilenamesIn(REMOVAL);
        if (x != null && !x.isEmpty()) {
            for (String file : x) {
                System.out.println(file);
            }
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===\n");
        System.out.println("=== Untracked Files ===");
    }

    public static void checkoutf(Commit c, String fname) {
        if (!c.getFileToBlobID().containsKey(fname)) {
            error("File does not exist in that commit.");
        }
        byte[] contents = getBlob(c.getFileToBlobID().get(fname));
        Utils.writeContents(Utils.join(CWD, fname), contents);
    }

    public static void checkoutf(String commitID, String fname) {
        if (commitID.length() == 6) {
            commitID = getFullId(commitID);
        }
        if (!Utils.join(COMMITS, commitID).exists()) {
            error("No commit with that id exists.");
        }
        checkoutf(Commit.getCommit(commitID), fname);
    }

    public static void checkoutb(String branch) {
        CommitTree tree = getTree();
        if (!tree.getBranchToCommit().containsKey(branch)) {
            error("No such branch exists.");
        } else if (tree.getActiveBranch().equals(branch)) {
            error("No need to checkout the current branch.");
        }

        Commit commitFromBranch =
                Commit.getCommit(tree.getBranchToCommit().get(branch));
        checkoutCommit(commitFromBranch);

        tree.setActiveBranch(branch);
        saveTree(tree);
        clearStage();
    }

    public static void checkoutCommit(Commit c) {
        Commit headCommit = getTree().headCommit();
        List<String> x = Utils.plainFilenamesIn(CWD);
        if (x != null) {
            for (String f : x) {
                if (!headCommit.isTracked(f) && c.isTracked(f)) {
                    error("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
            for (String f : x) {
                if (headCommit.isTracked(f) && !c.isTracked(f)) {
                    Utils.restrictedDelete(Utils.join(CWD, f));
                }
            }
        }

        for (String file : c.getFileToBlobID().keySet()) {
            Utils.writeContents(Utils.join(CWD, file),
                    getBlob(c.getFileToBlobID().get(file)));
        }
    }

    public static void reset(String commitID) {
        if (commitID.length() == 6) {
            commitID = getFullId(commitID);
        }
        if (!Utils.join(COMMITS, commitID).exists()) {
            error("No commit with that id exists.");
        }
        CommitTree tree = getTree();
        checkoutCommit(Commit.getCommit(commitID));

        tree.getBranchToCommit().put(tree.getActiveBranch(), commitID);
        saveTree(tree);
        clearStage();
    }

    public static void branch(String branch) {
        CommitTree tree = getTree();
        if (tree.getBranchToCommit().containsKey(branch)) {
            error("A branch with that name already exists.");
        }
        tree.newBranch(branch);
        saveTree(tree);
    }

    public static void rmBranch(String branch) {
        CommitTree tree = getTree();
        if (!tree.getBranchToCommit().containsKey(branch)) {
            error("A branch with that name does not exist.");
        } else if (tree.getActiveBranch().equals(branch)) {
            error("Cannot remove the current branch.");
        }
        tree.getBranchToCommit().remove(branch);
        saveTree(tree);
    }

    public static void merge(String branch) {
        CommitTree tree = getTree();
        if (changesStaged()) {
            error("You have uncommitted changes.");
        } else if (!tree.getBranchToCommit().containsKey(branch)) {
            error("A branch with that name does not exist.");
        } else if (tree.getActiveBranch().equals(branch)) {
            error("Cannot merge a branch with itself.");
        }
        Commit curr = tree.headCommit(), given =
                Commit.getCommit(tree.getBranchToCommit().get(branch));
        Commit lca = lca(curr, given);
        if (lca.equals(given)) {
            error("Given branch is an ancestor of the current branch.");
        } else if (lca.equals(curr)) {
            checkoutb(branch);
            error("Current branch fast-forwarded.");
        }

        for (String file : lca.getFileToBlobID().keySet()) {
            if (!changesToFile(file, lca, curr)
                    && changesToFile(file, lca, given)) {
                if (given.getFileToBlobID().containsKey(file)) {
                    checkoutf(given, file);
                    add(file);
                } else {
                    rm(file);
                }
            }
        }

        HashSet<String> set = new HashSet<>();
        set.addAll(given.getFileToBlobID().keySet());
        set.addAll(curr.getFileToBlobID().keySet());

        for (String file : set) {
            if (!lca.getFileToBlobID().containsKey(file)
                    && !curr.getFileToBlobID().containsKey(file)
                    && given.getFileToBlobID().containsKey(file)) {
                checkoutf(given, file);
                add(file);
            } else if (changesToFile(file, given, curr)
                    && changesToFile(file, given, lca)
                    && changesToFile(file, curr, lca)) {
                mergeConflict(file, curr, given);
            }
        }

        if (!changesStaged()) {
            error("No changes added to the commit.");
        }

        tree.addCommit(String.format("Merged %s into %s.",
                branch, tree.getActiveBranch()), given);
        saveTree(tree);
    }

    public static void mergeConflict(String file, Commit c1, Commit c2) {
        byte[] x = new byte[] {}, y = new byte[] {};
        if (c1.getFileToBlobID().containsKey(file)) {
            x = getBlob(c1.getFileToBlobID().get(file));
        }
        if (c2.getFileToBlobID().containsKey(file)) {
            y = getBlob(c2.getFileToBlobID().get(file));
        }
        Utils.writeContents(Utils.join(CWD, file),
                "<<<<<<< HEAD\n", x, "=======\n", y, ">>>>>>>\n");
        add(file);
        System.out.println("Encountered a merge conflict.");
    }

    public static boolean changesToFile(String file, Commit c1, Commit c2) {
        if (c1.getFileToBlobID().containsKey(file)
                ^ c2.getFileToBlobID().containsKey(file)) {
            return true;
        } else if (!c1.getFileToBlobID().containsKey(file)
                && !c2.getFileToBlobID().containsKey(file)) {
            return false;
        }
        return !c1.getFileToBlobID().get(file).equals(
                c2.getFileToBlobID().get(file));
    }

    public static Commit lca(Commit curr, Commit given) {
        LinkedList<Commit> q = new LinkedList<>();
        HashSet<String> hashSet = new HashSet<>();
        q.add(given);
        while (!q.isEmpty()) {
            Commit c = q.poll();
            if (c == null) {
                continue;
            }
            hashSet.add(c.getId());
            q.add(c.parent());
            q.add(c.mergeParent());
        }

        q.add(curr);
        while (!q.isEmpty()) {
            Commit c = q.poll();
            if (c == null) {
                continue;
            }
            if (hashSet.contains(c.getId())) {
                return c;
            } else {
                q.add(c.parent());
                q.add(c.mergeParent());
            }
        }
        return curr;
    }

    public static void clearStage() {
        ADDITION.delete();
        REMOVAL.delete();
        ADDITION.mkdirs();
        REMOVAL.mkdirs();
    }

    public static byte[] getBlob(String blobID) {
        return Utils.readContents(Utils.join(BLOBS, blobID));
    }

    public static String dateFormat(Date d) {
        Formatter formatter = new Formatter();
        return formatter.format(FORMAT, d, d, d, d, d, d).toString();
    }

    public static CommitTree getTree() {
        return Utils.readObject(COMMIT_TREE, CommitTree.class);
    }

    public static String getFullId(String mini) {
        HashMap<String, String> h = getShortenedCommits();
        if (h.containsKey(mini)) {
            return h.get(mini);
        }
        return "not a real commit";
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, String> getShortenedCommits() {
        return Utils.readObject(SHORTENED_COMMITS, HashMap.class);
    }

    public static void saveShortenedCommits(HashMap<String, String> h) {
        Utils.writeObject(SHORTENED_COMMITS, h);
    }

    public static void saveTree(CommitTree tree) {
        Utils.writeObject(COMMIT_TREE, tree);
    }

    public static void error(String msg) {
        System.out.println(msg);
        System.exit(0);
    }
}
