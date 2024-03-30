package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import pro.sky.telegrambot.service.TelegramBotSender;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final Pattern RECEIVED_MESSAGE_PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");

    private final DateTimeFormatter NOTIFICATION_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final TelegramBot telegramBot;

    private final TelegramBotSender telegramBotSender;

    private final NotificationTaskRepository notificationTaskRepository;

    private final String GREETING_MESSAGE = "Привет! Это бот для твоих напоминаний.";

    private final String SUCCESSFULLY_ADDED_TASK = "Задача успешно добавлена";

    public TelegramBotUpdatesListener(TelegramBot telegramBot, TelegramBotSender telegramBotSender, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.telegramBotSender = telegramBotSender;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            String message = update.message().text();
            Long chatID = update.message().chat().id();
            if (message.equals("/start")) {
                logger.info("Получили сообщение о запуске бота с текстом" + message);
                telegramBotSender.send(chatID, GREETING_MESSAGE);
            }
            else {
                Matcher matcher = RECEIVED_MESSAGE_PATTERN.matcher(message);
                if (matcher.matches()) {
                    logger.info("Get new message: " + message);
                    String rawDateTime = matcher.group(1);
                    String notificationText = matcher.group(3);
                    NotificationTask notificationTask = new NotificationTask(
                            chatID,
                            notificationText,
                            LocalDateTime.parse(rawDateTime, NOTIFICATION_DATE_TIME_FORMAT)
                    );
                    notificationTaskRepository.save(notificationTask);
                    telegramBotSender.send(chatID, SUCCESSFULLY_ADDED_TASK);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
