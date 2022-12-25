import org.telegram.telegrambots.meta.api.objects.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Test {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        List<Lottery> list1;
        List<Lottery> list2;
        List<Lottery> list3;

        ObjectInputStream ois1 = new ObjectInputStream(new FileInputStream("I:\\LotteryBot\\Lotteries\\lotteryList.dat"));
        list1 = (List<Lottery>) ois1.readObject();
        ObjectInputStream ois2 = new ObjectInputStream(new FileInputStream("I:\\LotteryBot\\out\\artifacts\\LotteryBot_jar\\Lotteries\\lotteryList.dat"));
        list2 = (List<Lottery>) ois2.readObject();

        System.out.println("____________LIST 1_____________");
        for (Lottery lottery : list1) {
            System.out.println(lottery);
        }
        System.out.println("____________LIST 2_____________");
        for (Lottery lottery : list2) {
            System.out.println(lottery);
        }
        System.out.println("____________LIST 3_____________");
        list3 = new ArrayList<>(list1);
        list3.add(new Lottery("1", "1"));
        list3.get(0).getParticipants().addAll(list1.get(0).getParticipants());

        for (Lottery lottery : list3) {
            System.out.println(lottery);
        }

        for (int i = 0; i < list3.get(0).getParticipants().size(); i++) {
            System.out.println(list3.get(0).getParticipants().get(i));
        }

        for (int i = 0; i < list2.get(0).getParticipants().size(); i++) {
            if (!list3.get(0).getParticipants().contains(list2.get(0).getParticipants().get(i)))
                list3.get(0).getParticipants().add(list2.get(0).getParticipants().get(i));
        }

        HashSet<User> set = new HashSet<>();
        set.addAll(list1.get(0).getParticipants());
        set.addAll(list2.get(0).getParticipants());

        System.out.println("____________LIST 3_____________");
        int i = 0;
        for (User lottery : set) {
            i++;
            System.out.println(lottery);
        }

        list1.get(0).getParticipants().clear();
        list1.get(0).getParticipants().addAll(set);

        File aba = new File("lotteryList.dat");
        aba.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(aba,false));
        oos.writeObject(list1);
        System.out.println("LIST SIZE = " + list1.get(0).getParticipants().size());
        oos.close();
        System.out.println(aba.getAbsolutePath());

        System.out.println("SIZE IS: " + i);
    }
}
