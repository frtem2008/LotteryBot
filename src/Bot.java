import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Bot extends TelegramLongPollingBot {
    private static final StringBuilder HELP_MESSAGE = new StringBuilder("""
            ������� ��� ���������� �����:
            ��� ������� ����� ������ ���� � ��, ����� �� �������� �������� ���
            1) /newlottery <�������> � �������� ������ ���������
            2) /dellottery <id> ��� /dellottery <�������> - ������� �������� *������� ����� ������ ����������� ���������!!!
            3) /winner <id> ��� /winner <�������> � ��������� �������� � ���������� ����������
            4) /participate <id> ��� /participate <�������> � ����������� � ���������
            5) /lotteries � ������ ����������
            6) /help - ��� ���������
                        
            * ������� 1, 2, 3 �������� ������ ��� ������� ���� (������� ����� �� �����, ��� �� ������, �� ���������)
            """);
    private final List<Lottery> lotteryList;
    private final List<Integer> lotteryIDS;
    private final List<Long> lotteryCreators;

    public Bot() {
        try {
            Lottery.createLotteryListsSaveFiles();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        lotteryList = Lottery.restoreLotteryList();
        lotteryIDS = Lottery.restoreLotteryIdList();
        lotteryCreators = Lottery.restoreLotteryCreatorsList();

        if (!lotteryCreators.contains(1280356300L))
            this.lotteryCreators.add(1280356300L); //�
        if (!lotteryCreators.contains(5290295786L))
            this.lotteryCreators.add(5290295786L); //���� ���

        Lottery.saveLotteryLists(lotteryList, lotteryIDS, lotteryCreators);

        System.out.println("LOTTERIES: ");
        for (Lottery lottery : lotteryList) {
            System.out.println(lottery);
        }
    }

    @Override
    public String getBotUsername() {
        return "LiveLotteryBot";
    }

    @Override
    public String getBotToken() {
        return "5941794780:AAF31j0STWEuFmFlUr4dn9pVRXM8iuHnddk";
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();

                SendMessage toSend = parseMessage(message);
                System.out.println("message.getChatId() = " + message.getChatId());
                System.out.println(message.getFrom());
                System.out.println("message = " + message.getText());

                if (toSend != null) {
                    toSend.setReplyToMessageId(message.getMessageId());
                    execute(toSend);
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private SendMessage parseMessage(Message message) {
        SendMessage resultMessage = new SendMessage();

        String chatId = message.getChatId().toString();
        String messageText = null;

        String userName = message.getFrom().getUserName() == null ? message.getFrom().getFirstName() + " " + message.getFrom().getLastName() : "@" + message.getFrom().getUserName();

        StringBuilder response = new StringBuilder();

        if (message.hasText())
            messageText = message.getText();

        if (messageText != null) {
            if (messageText.startsWith("/") && message.getChatId() == -1001897131533L && !lotteryCreators.contains(message.getFrom().getId())) {
                response = new StringBuilder("�� � ���� ��� ���������� (������ ���� � ��)!!!");
                DeleteMessage autoRemove1 = new DeleteMessage(String.valueOf(message.getChatId()), message.getMessageId());

                new Thread(() -> {
                    try {
                        Thread.sleep(10000);
                        execute(autoRemove1);
                    } catch (TelegramApiException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            else if (messageText.toLowerCase(Locale.ROOT).startsWith("/newlottery")) {
                if (!lotteryCreators.contains(message.getFrom().getId())) {
                    System.out.println("A user: @" + userName + " attempted to create a new lottery with no permission!");
                    response = new StringBuilder("� ��� ��� ����� ��������� ���������!");
                } else {
                    System.out.println("A user: @" + userName + " attempted to create a new lottery!");
                    String[] words = messageText.split("/newlottery");

                    if (words.length <= 1)
                        response = new StringBuilder("������� ������������� �������");
                    else {
                        words[1] = words[1].trim();
                        Lottery newLottery = new Lottery(chatId, words[1]);

                        if (lotteryList.contains(newLottery))
                            response = new StringBuilder("�������� ��������: " + words[1] + " ��� ���� � ���� ����!\n������������� ��� �������� �������: " + lotteryList.get(lotteryList.indexOf(newLottery)).getID());
                        else if (newLottery.getID() == -1) {
                            response = new StringBuilder("���������� ������������ ���������� ��������� ����������\n������� ������������ ��� ���������� �����");
                        } else {
                            lotteryList.add(newLottery);
                            lotteryIDS.add(newLottery.getID());
                            System.out.println("New lottery " + newLottery + "created");
                            response = new StringBuilder("�������� ��������: " + newLottery.getLotteryPrize() + " �������!\n������������� ���������: " + newLottery.getID() + " (����������� ��� ��������� � ����������)");
                        }
                    }
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/participate")) {
                String[] words = messageText.split("/participate");

                if (words.length <= 1)
                    response = new StringBuilder("�������, � ����� ��������� �� ������ ������� �������");
                else {
                    words[1] = words[1].trim();
                    if (words[1].matches("\\d*")) {
                        int id = Integer.parseInt(words[1]);

                        if (lotteryIDS.contains(id)) {
                            Lottery toOperate = Lottery.getById(lotteryList, id);
                            if (toOperate == null)
                                response = new StringBuilder("��������� � ��������������� " + id + " �� ����������");
                            else {
                                if (toOperate.addUser(message.getFrom()))
                                    response = new StringBuilder("������ ������������ " + userName + " ��������� ������� � ��������� ��������: " + toOperate.getLotteryPrize() + "\n������ �����)");
                                else
                                    response = new StringBuilder("�� ��� ���������� � ��������� ��������: " + toOperate.getLotteryPrize());
                            }
                        } else
                            response = new StringBuilder("��������� � ��������������� " + id + " �� ����������");
                    } else {
                        Lottery toOperate = Lottery.getByLotteryPrize(lotteryList, words[1]);
                        if (toOperate == null)
                            response = new StringBuilder("��������� ��������: " + words[1] + " �� ����������");
                        else {
                            if (toOperate.addUser(message.getFrom()))
                                response = new StringBuilder("������ ������������ " + userName + " ��������� ������� � ��������� ��������: " + toOperate.getLotteryPrize() + "\n������ �����)");
                            else
                                response = new StringBuilder("�� ��� ���������� � ��������� ��������: " + toOperate.getLotteryPrize());
                        }
                    }
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/winner")) {
                String[] words = messageText.split("/winner");
                if (!lotteryCreators.contains(message.getFrom().getId())) {
                    System.out.println("A user: " + userName + " attempted to end a lottery with no permission!");
                    response = new StringBuilder("� ��� ��� ����� ��������� ���������!");
                } else if (words.length <= 1)
                    response = new StringBuilder("������� id ��� �������� �������������� ��������, ����� ���������� ����������");
                else {
                    words[1] = words[1].trim();

                    if (words[1].matches("\\d*")) {
                        int id = Integer.parseInt(words[1]);

                        if (lotteryIDS.contains(id)) {
                            Lottery toOperate = Lottery.getById(lotteryList, id);
                            if (toOperate == null)
                                response = new StringBuilder("��������� � ��������������� " + id + " �� ����������");
                            else if (toOperate.isFinished())
                                response = new StringBuilder("���� �������� ��� ��������!\n����������� ��� " + userName);
                            else if (toOperate.getWinChance() == 0) {
                                response = new StringBuilder("� ���� ��������� ���� �� ��������� ������� �� ���� ������������!\n������� ������! (��������� /participate " + toOperate.getID() + ")");
                            } else {
                                response = new StringBuilder("������������ " + userName +
                                        " ���������� ����������� ��������� ��������: " + toOperate.getLotteryPrize() +
                                        "\n���� �������� �������: " + toOperate.getLotteryPrize() +
                                        " ��� " + toOperate.getWinChance() + "%\n" +
                                        "��������� � �� ���������������, �� ��������, ��� � ��������� ��� ������ ������ ���!");
                            }
                        } else
                            response = new StringBuilder("��������� � ��������������� " + id + " �� ����������");
                    } else {
                        Lottery toOperate = Lottery.getByLotteryPrize(lotteryList, words[1]);
                        if (toOperate == null)
                            response = new StringBuilder("��������� ��������: " + words[1] + " �� ����������");
                        else if (toOperate.isFinished())
                            response = new StringBuilder("���� �������� ��� ��������!\n����������� ��� " + userName);
                        else if (toOperate.getWinChance() == 0) {
                            response = new StringBuilder("� ���� ��������� ���� �� ��������� ������� �� ���� ������������!\n������� ������! (��������� /participate " + toOperate.getID() + ")");
                        } else {
                            userName = toOperate.getWinner().getUserName() == null ? toOperate.getWinner().getFirstName() + " " + toOperate.getWinner().getLastName() : "@" + toOperate.getWinner().getUserName();
                            response = new StringBuilder("������������ @" + userName +
                                    " ���������� ����������� ��������� ��������: " + toOperate.getLotteryPrize() +
                                    "\n���� �������� �������: " + toOperate.getLotteryPrize() +
                                    " ��� " + toOperate.getWinChance() + "%\n" +
                                    "��������� � �� ���������������, �� ��������, ��� � ��������� ��� ������ ������ ���!");
                        }
                    }
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/lotteries")) {
                response = new StringBuilder("������ ���������� " + lotteryList.size() + " ����������: \n");
                for (int i = 0; i < lotteryList.size(); i++) {
                    response.append(i + 1).append(")").append(lotteryList.get(i).getLotteryPrize()).append("(id = ").append(lotteryList.get(i).getID()).append(")").append("          ").append(lotteryList.get(i).getParticipantCount()).append(" ������� ��� ���������!").append("\n");
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/dellottery")) {
                String[] words = messageText.split("/dellottery");
                if (!lotteryCreators.contains(message.getFrom().getId())) {
                    System.out.println("A user: " + userName + " attempted to delete a lottery with no permission!");
                    response = new StringBuilder("� ��� ��� ����� ������� ���������!");
                } else if (words.length <= 1)
                    response = new StringBuilder("������� id ��� �������� ��������, ����� ������� ��������");
                else {
                    words[1] = words[1].trim();

                    if (words[1].matches("\\d*")) {
                        int id = Integer.parseInt(words[1]);

                        if (lotteryIDS.contains(id)) {
                            Lottery toOperate = Lottery.getById(lotteryList, id);
                            if (toOperate == null)
                                response = new StringBuilder("��������� � ��������������� " + id + " �� ����������");
                            else {
                                if (!toOperate.delete())
                                    if (toOperate.isFinished())
                                        response = new StringBuilder("�� ������� ������� �������� �������� " + toOperate.getLotteryPrize() + ": ����� �� ����������");
                                    else
                                        response = new StringBuilder("�� ������� ������� �������� �������� " + toOperate.getLotteryPrize() + ": ���� �������� ��� �� ��������");
                                else {
                                    lotteryList.remove(toOperate);
                                    lotteryIDS.remove(toOperate.getIntegerId());
                                    response = new StringBuilder("�������� ��������: " + toOperate.getLotteryPrize() + " ��� �����\n");

                                    response.append("������ ���������� ").append(lotteryList.size()).append(" ����������: \n");

                                    for (int i = 0; i < lotteryList.size(); i++) {
                                        response.append(i + 1).append(")").append(lotteryList.get(i).getLotteryPrize()).append("(id = ").append(lotteryList.get(i).getID()).append(")").append("          ").append(lotteryList.get(i).getParticipantCount()).append(" ������� ��� ���������!").append("\n");
                                    }
                                }
                            }
                        } else
                            response = new StringBuilder("��������� � ��������������� " + id + " �� ����������");
                    } else {
                        Lottery toOperate = Lottery.getByLotteryPrize(lotteryList, words[1]);
                        if (toOperate == null)
                            response = new StringBuilder("��������� ��������: " + words[1] + " �� ����������");
                        else {
                            if (!toOperate.delete())
                                if (toOperate.isFinished())
                                    response = new StringBuilder("�� ������� ������� �������� �������� " + toOperate.getLotteryPrize() + ": ����� �� ����������");
                                else
                                    response = new StringBuilder("�� ������� ������� �������� �������� " + toOperate.getLotteryPrize() + ": ���� �������� ��� �� ��������");
                            else {
                                lotteryList.remove(toOperate);
                                lotteryIDS.remove(toOperate.getIntegerId());
                                response = new StringBuilder("�������� ��������: " + toOperate.getLotteryPrize() + " ��� �����\n");
                                response.append("������ ���������� ").append(lotteryList.size()).append(" ����������: \n");
                                for (int i = 0; i < lotteryList.size(); i++) {
                                    response.append(i + 1).append(")").append(lotteryList.get(i).getLotteryPrize()).append("(id = ").append(lotteryList.get(i).getID()).append(")").append("          ").append(lotteryList.get(i).getParticipantCount()).append(" ������� ��� ���������!").append("\n");
                                }
                            }
                        }
                    }
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/help")) {
                response = HELP_MESSAGE;
            } else if (messageText.startsWith("/"))
                response = new StringBuilder("����������� �������!");
        }

        if (response.toString().isBlank())
            return null;

        resultMessage.setChatId(chatId);
        resultMessage.setText(response.toString());

        Lottery.saveLotteryLists(lotteryList, lotteryIDS, lotteryCreators);

        return resultMessage;
    }
}
