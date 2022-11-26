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
            Команды для управления ботом:
            Все команды лучше писать боту в лс, чтобы не засорять основной чат
            1) /newlottery <Предмет> — создание нового розыгрыша
            2) /dellottery <id> или /dellottery <Предмет> - удалить розыгрыш *Удалять можно только завершённые розыгрыши!!!
            3) /winner <id> или /winner <Предмет> — завершить розыгрыш и определить победителя
            4) /participate <id> или /participate <Предмет> — участвовать в розыгрыше
            5) /lotteries — список розыгрышей
            6) /help - это сообщение
                        
            * команды 1, 2, 3 доступны только для админов бота (админом никак не стать, это не обойти, не пытайтесь)
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
            this.lotteryCreators.add(1280356300L); //я
        if (!lotteryCreators.contains(5290295786L))
            this.lotteryCreators.add(5290295786L); //Лисы топ

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
                response = new StringBuilder("НЕ В ЭТОТ ЧАТ ПОЖАЛУЙСТА (пишите боту в лс)!!!");
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
                    response = new StringBuilder("У вас нет права создавать розыгрыши!");
                } else {
                    System.out.println("A user: @" + userName + " attempted to create a new lottery!");
                    String[] words = messageText.split("/newlottery");

                    if (words.length <= 1)
                        response = new StringBuilder("Укажите разыгрываемый предмет");
                    else {
                        words[1] = words[1].trim();
                        Lottery newLottery = new Lottery(chatId, words[1]);

                        if (lotteryList.contains(newLottery))
                            response = new StringBuilder("Розыгрыш предмета: " + words[1] + " уже есть в этом чате!\nИтендификатор для быстрого доступа: " + lotteryList.get(lotteryList.indexOf(newLottery)).getID());
                        else if (newLottery.getID() == -1) {
                            response = new StringBuilder("Достигнуто максимальное количество различных розыгрышей\nУдалите существующий или попробуйте позже");
                        } else {
                            lotteryList.add(newLottery);
                            lotteryIDS.add(newLottery.getID());
                            System.out.println("New lottery " + newLottery + "created");
                            response = new StringBuilder("Розыгрыш предмета: " + newLottery.getLotteryPrize() + " начался!\nИтендификатор розыгрыша: " + newLottery.getID() + " (используйте для обращения в дальнейшем)");
                        }
                    }
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/participate")) {
                String[] words = messageText.split("/participate");

                if (words.length <= 1)
                    response = new StringBuilder("Укажите, в каком розыгрыше вы хотите принять участие");
                else {
                    words[1] = words[1].trim();
                    if (words[1].matches("\\d*")) {
                        int id = Integer.parseInt(words[1]);

                        if (lotteryIDS.contains(id)) {
                            Lottery toOperate = Lottery.getById(lotteryList, id);
                            if (toOperate == null)
                                response = new StringBuilder("Розыгрыша с итендификатором " + id + " не существует");
                            else {
                                if (toOperate.addUser(message.getFrom()))
                                    response = new StringBuilder("Теперь пользователь " + userName + " принимает участие в розыгрыше предмета: " + toOperate.getLotteryPrize() + "\nЖелаем удачи)");
                                else
                                    response = new StringBuilder("Вы уже участвуете в розыгрыше предмета: " + toOperate.getLotteryPrize());
                            }
                        } else
                            response = new StringBuilder("Розыгрыша с итендификатором " + id + " не существует");
                    } else {
                        Lottery toOperate = Lottery.getByLotteryPrize(lotteryList, words[1]);
                        if (toOperate == null)
                            response = new StringBuilder("Розыгрыша предмета: " + words[1] + " не существует");
                        else {
                            if (toOperate.addUser(message.getFrom()))
                                response = new StringBuilder("Теперь пользователь " + userName + " принимает участие в розыгрыше предмета: " + toOperate.getLotteryPrize() + "\nЖелаем удачи)");
                            else
                                response = new StringBuilder("Вы уже участвуете в розыгрыше предмета: " + toOperate.getLotteryPrize());
                        }
                    }
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/winner")) {
                String[] words = messageText.split("/winner");
                if (!lotteryCreators.contains(message.getFrom().getId())) {
                    System.out.println("A user: " + userName + " attempted to end a lottery with no permission!");
                    response = new StringBuilder("У вас нет права завершать розыгрыши!");
                } else if (words.length <= 1)
                    response = new StringBuilder("Укажите id или название разыгрываемого предмета, чтобы опредилить победителя");
                else {
                    words[1] = words[1].trim();

                    if (words[1].matches("\\d*")) {
                        int id = Integer.parseInt(words[1]);

                        if (lotteryIDS.contains(id)) {
                            Lottery toOperate = Lottery.getById(lotteryList, id);
                            if (toOperate == null)
                                response = new StringBuilder("Розыгрыша с итендификатором " + id + " не существует");
                            else if (toOperate.isFinished())
                                response = new StringBuilder("Этот розыгрыш уже завершён!\nПобедителем был " + userName);
                            else if (toOperate.getWinChance() == 0) {
                                response = new StringBuilder("В этом розыгрыше пока не принимает участие ни один пользователь!\nСтаньте первым! (пропишите /participate " + toOperate.getID() + ")");
                            } else {
                                response = new StringBuilder("Пользователь " + userName +
                                        " становится победителем розыгрыша предмета: " + toOperate.getLotteryPrize() +
                                        "\nШанс выиграть предмет: " + toOperate.getLotteryPrize() +
                                        " был " + toOperate.getWinChance() + "%\n" +
                                        "Остальные — не расстраивайтесь, мы надеемся, что в следующий раз повезёт именно вам!");
                            }
                        } else
                            response = new StringBuilder("Розыгрыша с итендификатором " + id + " не существует");
                    } else {
                        Lottery toOperate = Lottery.getByLotteryPrize(lotteryList, words[1]);
                        if (toOperate == null)
                            response = new StringBuilder("Розыгрыша предмета: " + words[1] + " не существует");
                        else if (toOperate.isFinished())
                            response = new StringBuilder("Этот розыгрыш уже завершён!\nПобедителем был " + userName);
                        else if (toOperate.getWinChance() == 0) {
                            response = new StringBuilder("В этом розыгрыше пока не принимает участие ни один пользователь!\nСтаньте первым! (пропишите /participate " + toOperate.getID() + ")");
                        } else {
                            userName = toOperate.getWinner().getUserName() == null ? toOperate.getWinner().getFirstName() + " " + toOperate.getWinner().getLastName() : "@" + toOperate.getWinner().getUserName();
                            response = new StringBuilder("Пользователь @" + userName +
                                    " становится победителем розыгрыша предмета: " + toOperate.getLotteryPrize() +
                                    "\nШанс выиграть предмет: " + toOperate.getLotteryPrize() +
                                    " был " + toOperate.getWinChance() + "%\n" +
                                    "Остальные — не расстраивайтесь, мы надеемся, что в следующий раз повезёт именно вам!");
                        }
                    }
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/lotteries")) {
                response = new StringBuilder("Сейчас проводится " + lotteryList.size() + " розыгрышей: \n");
                for (int i = 0; i < lotteryList.size(); i++) {
                    response.append(i + 1).append(")").append(lotteryList.get(i).getLotteryPrize()).append("(id = ").append(lotteryList.get(i).getID()).append(")").append("          ").append(lotteryList.get(i).getParticipantCount()).append(" человек уже участвуют!").append("\n");
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/dellottery")) {
                String[] words = messageText.split("/dellottery");
                if (!lotteryCreators.contains(message.getFrom().getId())) {
                    System.out.println("A user: " + userName + " attempted to delete a lottery with no permission!");
                    response = new StringBuilder("У вас нет права удалять розыгрыши!");
                } else if (words.length <= 1)
                    response = new StringBuilder("Укажите id или название предмета, чтобы удалить розыгрыш");
                else {
                    words[1] = words[1].trim();

                    if (words[1].matches("\\d*")) {
                        int id = Integer.parseInt(words[1]);

                        if (lotteryIDS.contains(id)) {
                            Lottery toOperate = Lottery.getById(lotteryList, id);
                            if (toOperate == null)
                                response = new StringBuilder("Розыгрыша с итендификатором " + id + " не существует");
                            else {
                                if (!toOperate.delete())
                                    if (toOperate.isFinished())
                                        response = new StringBuilder("Не удалось удалить розыгрыш предмета " + toOperate.getLotteryPrize() + ": файла не существует");
                                    else
                                        response = new StringBuilder("Не удалось удалить розыгрыш предмета " + toOperate.getLotteryPrize() + ": этот розыгрыш ещё не завершён");
                                else {
                                    lotteryList.remove(toOperate);
                                    lotteryIDS.remove(toOperate.getIntegerId());
                                    response = new StringBuilder("Розыгрыш предмета: " + toOperate.getLotteryPrize() + " был удалён\n");

                                    response.append("Сейчас проводится ").append(lotteryList.size()).append(" розыгрышей: \n");

                                    for (int i = 0; i < lotteryList.size(); i++) {
                                        response.append(i + 1).append(")").append(lotteryList.get(i).getLotteryPrize()).append("(id = ").append(lotteryList.get(i).getID()).append(")").append("          ").append(lotteryList.get(i).getParticipantCount()).append(" человек уже участвуют!").append("\n");
                                    }
                                }
                            }
                        } else
                            response = new StringBuilder("Розыгрыша с итендификатором " + id + " не существует");
                    } else {
                        Lottery toOperate = Lottery.getByLotteryPrize(lotteryList, words[1]);
                        if (toOperate == null)
                            response = new StringBuilder("Розыгрыша предмета: " + words[1] + " не существует");
                        else {
                            if (!toOperate.delete())
                                if (toOperate.isFinished())
                                    response = new StringBuilder("Не удалось удалить розыгрыш предмета " + toOperate.getLotteryPrize() + ": файла не существует");
                                else
                                    response = new StringBuilder("Не удалось удалить розыгрыш предмета " + toOperate.getLotteryPrize() + ": этот розыгрыш ещё не завершён");
                            else {
                                lotteryList.remove(toOperate);
                                lotteryIDS.remove(toOperate.getIntegerId());
                                response = new StringBuilder("Розыгрыш предмета: " + toOperate.getLotteryPrize() + " был удалён\n");
                                response.append("Сейчас проводится ").append(lotteryList.size()).append(" розыгрышей: \n");
                                for (int i = 0; i < lotteryList.size(); i++) {
                                    response.append(i + 1).append(")").append(lotteryList.get(i).getLotteryPrize()).append("(id = ").append(lotteryList.get(i).getID()).append(")").append("          ").append(lotteryList.get(i).getParticipantCount()).append(" человек уже участвуют!").append("\n");
                                }
                            }
                        }
                    }
                }
            } else if (messageText.toLowerCase(Locale.ROOT).startsWith("/help")) {
                response = HELP_MESSAGE;
            } else if (messageText.startsWith("/"))
                response = new StringBuilder("Неизвестная команда!");
        }

        if (response.toString().isBlank())
            return null;

        resultMessage.setChatId(chatId);
        resultMessage.setText(response.toString());

        Lottery.saveLotteryLists(lotteryList, lotteryIDS, lotteryCreators);

        return resultMessage;
    }
}
