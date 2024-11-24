package ru.vozov.moneystatbot.service.data;

public class MessageData {
    public static final String START_MESSAGE = """
                        \uD83D\uDC4BВас приветствует бот для учета финансов💵
                        Что он умеет?
                        ✅Сохранять информацию о ваших доходах и расходах📝
                        ✅Показывать статистические данные за разные промежутки времени📊
                        ✅Показывать историю сохранненых операций за разные промежутки времени🗂
                        """;

    public static final String FEEDBACK_MESSAGE = """   
                        ✉️По вопросам и предложениям Вы можете связаться с нами по данным контактам:
                        https://t.me/mintl0l
                        https://github.com/DmitryVozov
                        """;

    public static final String HELP_MESSAGE = """
                        В главном меню указаны основные доступные команды:
                        /income - добавление информации о операции дохода
                        /expense - добавление информации о операции расхода
                        /statistics - просмотр статистики Ваших доходов и расходов за все время, год, месяц или день
                        /history - просмотр истории Ваших доходов и расходов за все время, год, месяц или день
                        """;

    public static final String  UNSUPPORTED_COMMAND_MESSAGE = "Данная команда не поддерживается";

    public static final String OPERATION_START_MESSAGE = """
                        Здесь Вы можете сохранять Ваши %s
                        Нажмите продолжить для создания
                        """;
    public static final String OPERATION_EXISTS_IN_CREATION_MESSAGE = "У вас есть операция в процессе создания, для того чтобы создать новую необходимо отменить предыдущую. После отмены повторите попытку.";
    public static final String ASK_OPERATION_SUM_MESSAGE = "Введите сумму операции. Если число дробное, разделяйтя целую и дробную часть точкой. Например 243.21";
    public static final String INCORRECT_OPERATION_SUM_MESSAGE = "Некорректная сумма операции. Повторите попытку.";
    public static final String ASK_OPERATION_DATE_MESSAGE = """
                        Введите дату операции в данном формате: дд.мм.гггг
                    
                        Например 01.05.2000
                        """;
    public static final String INCORRECT_OPERATION_DATE_MESSAGE = "Некорректная дата операции. Повторите попытку.";
    public static final String ASK_OPERATION_CATEGORY_MESSAGE = "Выберите категорию операции.";
    public static final String ASK_OPERATION_DESCRIPTION_MESSAGE = """
                        Укажите описание операции.
                        Если в этом нет надобности, можете нажать кнопку Завершить для ее сохранения.
                        """;
    public static final String OPERATION_SUCCESS_MESSAGE = "Операция успешно сохранена✅";
    public static final String OPERATION_ALREADY_SAVED_MESSAGE = "Данная операция уже сохранена";
    public static final String OPERATION_CANCEL_MESSAGE = "Операция успешно отменена✅";

    public static final String ASK_STATISTICS_TYPE_MESSAGE = """
                        Здесь вы можете получить статистику по Вашим доходам и расходам📊
                        Выберите по какому типу операций Вы хотите получить статистику.
                        """;
    public static final String ASK_STATISTICS_PERIOD_MESSAGE = "Выберите за какой промежуток времени Вы хотите получить статистику\uD83D\uDCC6";
    public static final String NO_STATISTICS_DATA_MESSAGE = """
                        У Вас еще нет данных для получения статистики.
                        Для того чтобы добавить данные вызовите команду /income для добавления операции дохода или /expense для добавления операции расхода.
                        """;
    public static final String ASK_STATISTICS_YEAR_MESSAGE = "Выберите за какой год Вы хотите получить статистику";
    public static final String ASK_STATISTICS_MONTH_MESSAGE = "Выберите за какой месяц Вы хотите получить статистику";
    public static final String ASK_STATISTICS_DAY_MESSAGE = "Выберите за какой день Вы хотите получить статистику";
    public static final String STATISTICS_FOR_ALL_TIME_MESSAGE = "Ваша статистика по %s за все время📊\n\n";
    public static final String CATEGORY_STATISTICS_MESSAGE = "%s %.2f%% %.2f₽\n";
    public static final String STATISTICS_FOR_YEAR_MESSAGE = "Ваша статистика по %s за %d год📊\n\n";
    public static final String NO_STATISTICS_DATA_FOR_MONTH_MESSAGE = "За данный месяц у Вас не было %s.";
    public static final String STATISTICS_FOR_MONTH_MESSAGE = "Ваша статистика по %s за %s %d года📊\n\n";
    public static final String NO_STATISTICS_DATA_FOR_DAY_MESSAGE = "За данный день у Вас не было %s.";
    public static final String STATISTICS_FOR_DAY_MESSAGE = "Ваша статистика по %s за %s📊\n\n";

    public static final String ASK_HISTORY_TYPE_MESSAGE = """
                        Здесь вы можете получить историю по Вашим доходам и расходам🗂
                        Выберите по какому типу Вы хотите получить историю.
                        """;
    public static final String ASK_HISTORY_PERIOD_MESSAGE = "Выберите за какой промежуток времени Вы хотите получить историю\uD83D\uDCC6";
    public static final String NO_HISTORY_DATA_MESSAGE = """
                        У Вас еще нет данных для получения истории.
                        Для того чтобы добавить данные вызовите команду /income или /expense
                        """;
    public static final String ASK_HISTORY_YEAR_MESSAGE = "Выберите за какой год Вы хотите получить историю";
    public static final String ASK_HISTORY_MONTH_MESSAGE = "Выберите за какой месяц Вы хотите получить историю";
    public static final String ASK_HISTORY_DAY_MESSAGE = "Выберите за какой день Вы хотите получить историю";
    public static final String HISTORY_FOR_ALL_TIME_MESSAGE = "Ваша история по %s за все время🗂\n";
    public static final String HISTORY_FOR_YEAR_MESSAGE = "Ваша история по %s за %d год🗂\n\n";
    public static final String NO_HISTORY_DATA_FOR_MONTH_MESSAGE = "За данный месяц у Вас не было %s.";
    public static final String HISTORY_FOR_MONTH_MESSAGE = "Ваша история по %s за %s %d года🗂\n";
    public static final String NO_HISTORY_DATA_FOR_DAY_MESSAGE = "За данный день у Вас не было %s.";
    public static final String HISTORY_FOR_DAY_MESSAGE = "Ваша статистика по %s за %s🗂\n\n";
    public static final String HISTORY_DAY_MESSAGE = "\n%d %s %d г.\n";
    public static final String HISTORY_OPERATION_MESSAGE = "%s %.2f₽ %s\n";
}
