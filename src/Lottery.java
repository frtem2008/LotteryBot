import org.telegram.telegrambots.meta.api.objects.User;

import java.io.*;
import java.util.*;

public class Lottery implements Serializable {
    @Serial
    private static final long serialVersionUID = 11321L;

    private static final int MAX_ID = 100;
    private static final ArrayList<Integer> generatedIDS = new ArrayList<>();

    private static final File LOTTERY_SAVE_DIR = new File("Lotteries");
    private static final String LOTTERY_LIST_SAVE_FILE_PATH = LOTTERY_SAVE_DIR.getAbsolutePath() + "/lotteryList.dat";
    private static final String LOTTERY_ID_LIST_SAVE_FILE_PATH = LOTTERY_SAVE_DIR.getAbsolutePath() + "/lotteryIDList.dat";
    private static final String LOTTERY_CREATORS_LIST_SAVE_FILE_PATH = LOTTERY_SAVE_DIR.getAbsolutePath() + "/lotteryCreatorList.dat";

    private final int LOTTERY_ID;
    private final String lotteryPrize;
    private final ArrayList<User> participants;
    private final File saveFile;
    private User winner;

    public Lottery(String chatId, String lotteryPrize) {
        this.lotteryPrize = lotteryPrize;
        this.LOTTERY_ID = generateID();
        this.winner = null;

        this.participants = new ArrayList<>();
        this.saveFile = new File(LOTTERY_SAVE_DIR.getAbsolutePath() + "/" + LOTTERY_ID + ".lot");

        try {
            createLotteryStorageFile();
        } catch (IOException e) {
            System.err.println("CREATING STORAGE FILE FAILED");
            throw new RuntimeException(e);
        }
        save();
    }

    public static boolean generateSaveDir() {
        LOTTERY_SAVE_DIR.mkdirs();
        return LOTTERY_SAVE_DIR.exists();
    }

    public static void restoreLotteries(List<Lottery> toFill) {
        File[] lotteries = LOTTERY_SAVE_DIR.listFiles();
        System.out.println("Lottery save dir: " + LOTTERY_SAVE_DIR.getAbsolutePath());

        if (lotteries == null || lotteries.length == 0) {
            System.out.println("No lotteries to load!");
            return;
        }

        int restored = 0;
        System.out.println(Arrays.toString(lotteries));
        for (int i = 0; i < lotteries.length; i++) {
            Lottery temp = readFromFile(lotteries[i]);
            if (temp != null) {
                toFill.add(temp);
                restored++;
            }
        }

        System.out.println("Restored " + restored + " lotteries");
    }

    public static void createLotteryListsSaveFiles() throws IOException {
        System.out.println("Creating lottery lists save files...");
        new File(LOTTERY_LIST_SAVE_FILE_PATH).createNewFile();
        new File(LOTTERY_ID_LIST_SAVE_FILE_PATH).createNewFile();
        new File(LOTTERY_CREATORS_LIST_SAVE_FILE_PATH).createNewFile();
        System.out.println("Creating lottery lists save files done");
    }

    public static void saveLotteryLists(List<Lottery> lotteryList, List<Integer> lotteryIDS, List<Long> lotteryCreators) {
        try {
            createLotteryListsSaveFiles();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Saving lottery lists...");
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOTTERY_LIST_SAVE_FILE_PATH, false));
            oos.writeObject(lotteryList);
            oos.flush();
            oos.close();
            System.out.println("Lottery list saved");
            oos = new ObjectOutputStream(new FileOutputStream(LOTTERY_ID_LIST_SAVE_FILE_PATH, false));
            oos.writeObject(lotteryIDS);
            oos.flush();
            oos.close();
            System.out.println("Lottery id list saved");
            oos = new ObjectOutputStream(new FileOutputStream(LOTTERY_CREATORS_LIST_SAVE_FILE_PATH, false));
            oos.writeObject(lotteryCreators);
            oos.flush();
            oos.close();
            System.out.println("Lottery creators list saved");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static List<Lottery> restoreLotteryList() {
        System.out.println("Reading lottery list...");
        List<Lottery> res;
        try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(LOTTERY_LIST_SAVE_FILE_PATH))) {
            Object o;
            if ((o = oos.readObject()) instanceof List)
                res = (List<Lottery>) o;
            else {
                System.err.println("Failed to restore lottery list: read object is not a list");
                return new ArrayList<>();
            }
            System.out.println("Lottery list restored, contains " + res.size());
            return res;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<Integer> restoreLotteryIdList() {
        System.out.println("Reading lottery id list...");
        List<Integer> res;
        try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(LOTTERY_ID_LIST_SAVE_FILE_PATH))) {
            Object o;
            if ((o = oos.readObject()) instanceof List)
                res = (List<Integer>) o;
            else {
                System.err.println("Failed to restore lottery id list: read object is not a list");
                return new ArrayList<>();
            }
            System.out.println("Lottery id list restored, contains " + res.size());
            return res;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<Long> restoreLotteryCreatorsList() {
        System.out.println("Reading lottery creators list...");
        List<Long> res;
        try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(LOTTERY_CREATORS_LIST_SAVE_FILE_PATH))) {
            Object o;
            if ((o = oos.readObject()) instanceof List)
                res = (List<Long>) o;
            else {
                System.err.println("Failed to restore lottery creator list: read object is not a list");
                return new ArrayList<>();
            }
            System.out.println("Lottery creator list restored, contains " + res.size());
            return res;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static Lottery readFromFile(File file) {
        System.out.println("Reading lottery from " + file.getAbsolutePath() + "...");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object read = ois.readObject();
            if (read instanceof Lottery) {
                System.out.println("Read lottery from " + file.getAbsolutePath());
                return (Lottery) read;
            } else
                System.err.println("Failed to read lottery from " + file.getAbsolutePath() + ": object is not a lottery");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to read lottery from " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return null;
    }

    public static Lottery getById(List<Lottery> lotteries, int id) {
        if (id < 0 || id > MAX_ID || lotteries == null)
            return null;
        for (Lottery lottery : lotteries)
            if (lottery.LOTTERY_ID == id)
                return lottery;

        return null;
    }

    public static Lottery getByLotteryPrize(List<Lottery> lotteries, String lotteryPrize) {
        if (lotteryPrize == null || lotteries == null)
            return null;

        for (Lottery lottery : lotteries)
            if (lottery.lotteryPrize.equals(lotteryPrize))
                return lottery;

        return null;
    }

    public void createLotteryStorageFile() throws IOException {
        saveFile.createNewFile();
    }

    private void save() {
        System.out.println("Saving " + LOTTERY_ID + " to " + saveFile.getAbsolutePath() + "...");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile, false))) {
            oos.writeObject(this);
            oos.flush();
            System.out.println("Saved " + LOTTERY_ID + " to " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save LOTTERY!!!: " + LOTTERY_ID);
            e.printStackTrace();
        }
    }

    public boolean delete() {
        if (!isFinished())
            return false;

        System.out.println("saveFile = " + saveFile);
        return saveFile.delete();
    }

    private int generateID() {
        int generatedId;

        if (generatedIDS.size() == MAX_ID + 1)
            return -1;

        while (generatedIDS.contains(generatedId = (int) Math.round(Math.random() * MAX_ID))) ;

        generatedIDS.add(generatedId);

        return generatedId;
    }

    public int getID() {
        return LOTTERY_ID;
    }

    public Integer getIntegerId() {
        return LOTTERY_ID;
    }

    public boolean addUser(User toAdd) {
        if (!participants.contains(toAdd)) {
            participants.add(toAdd);
            save();
            return true;
        }
        return false;
    }

    public ArrayList<User> getParticipants() {
        return participants;
    }

    public void removeUser(User toRemove) {
        participants.remove(toRemove);
        save();
    }

    public double getWinChance() {
        if (participants.isEmpty())
            return 0;
        return 100.0 / participants.size();
    }

    public User getWinner() {
        if (winner != null)
            return winner;

        if (participants.isEmpty())
            return null;

        for (int i = 0; i < Math.random() * 9 + 1; i++) {
            Collections.reverse(participants);
            Collections.shuffle(participants);
            Collections.reverse(participants);
        }

        winner = participants.get(0);
        save();
        return winner;
    }

    public boolean isFinished() {
        return winner != null;
    }

    public String getLotteryPrize() {
        return lotteryPrize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lottery lottery)) return false;
        return Objects.equals(lotteryPrize, lottery.lotteryPrize);
    }

    @Override
    public String toString() {
        return "Lottery{" +
                "lotteryID='" + LOTTERY_ID + '\'' +
                ", lotteryPrize='" + lotteryPrize + '\'' +
                ", participants='" + participants.size() + '\'' +
                '}';
    }

    public int getParticipantCount() {
        return participants.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(lotteryPrize, participants);
    }
}
