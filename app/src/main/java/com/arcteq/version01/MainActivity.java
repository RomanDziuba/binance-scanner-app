package com.arcteq.version01;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
╔═══════════════════════════════════════════════════════════════════════════════════════════╗
║ 🚀 ARCTEQ PRO COMBINER ENGINE v2.0 ULTRA - УНИВЕРСАЛЬНЫЙ СПРАВОЧНИК РЗА                      ║
║ Автор: Dziuba Roman                                                                       ║
║ Описание: Архитектура комбайна с иерархическим меню (Выбор прибора -> Подменю категорий -> ║
║ Полная информация по блочкам, конфигурации, наладке и инженерным секретам).               ║
╚═══════════════════════════════════════════════════════════════════════════════════════════╝
*/
public class MainActivity extends AppCompatActivity {

    // СТРУКТУРА ДАННЫХ ДЛЯ ХРАНЕНИЯ ОПИСАНИЙ И БЛОЧКОВ ТЕРМИНАЛОВ
    static class Terminal {
        String name;
        String shortDescription;
        String fullDescription;
        String setupGuide;
        String proTips;
        boolean isFullyLoaded; // Флаг заполненности (true для AQ-F255, false для остальных заготовок)
        Map<String, String> blocks = new HashMap<>();

        Terminal(String name, String shortDescription, String fullDescription, String setupGuide, String proTips, boolean isFullyLoaded) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.fullDescription = fullDescription;
            this.setupGuide = setupGuide;
            this.proTips = proTips;
            this.isFullyLoaded = isFullyLoaded;
        }
    }

    private List<Terminal> database = new ArrayList<>();
    
    // Элементы UI для комбайна
    private LinearLayout mainLayout;
    private ListView mainMenuListView;
    
    // Элементы подменю выбора разделов для конкретного прибора
    private ScrollView submenuScrollView;
    private TextView submenuTitleView;
    private LinearLayout submenuButtonsLayout;

    // Элементы просмотра детального контента выбранного раздела
    private ScrollView contentScrollView;
    private TextView contentTitleView;
    private TextView contentBodyView;

    private Terminal currentSelectedTerminal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_DarkActionBar);

        // ИНИЦИАЛИЗАЦИЯ БАЗЫ ДАННЫХ КОМБАЙНА
        initDatabase();

        // ПОСТРОЕНИЕ ИНТЕРФЕЙСА (КОМБАЙН С ДИНАМИЧЕСКИМИ ЭКРАНАМИ И ПОДМЕНЮ)
        buildCombinerUI();
    }

    /*
    ==========================================================================================
    МЕТОД ИНИЦИАЛИЗАЦИИ БАЗЫ ДАННЫХ.
    ИНСТРУКЦИЯ ПО ДОБАВЛЕНИЮ НОВЫХ ПРИБОРОВ:
    1. Скопируйте шаблон создания терминала ниже.
    2. Заполните имя, описания, руководство, секреты и аппаратные блочки через .blocks.put().
    3. Добавьте созданный объект в database с помощью database.add(yourDevice).
    ==========================================================================================
    */
    private void initDatabase() {

        // --------------------------------------------------------------------------------------
        // [1] ТЕРМИНАЛ: AQ-F255 (Флагман РЗА 35 кВ) — ПОЛНОЦЕННАЯ БАЗА ДАННЫХ СО ВСЕМИ РАЗДЕЛАМИ
        // --------------------------------------------------------------------------------------
        Terminal aqF255 = new Terminal(
            "AQ-F255 (Флагман РЗА 35 кВ)",
            "Флагманский микропроцессорный терминал для комплексной защиты фидеров и трансформаторов 35 кВ.",
            /* ПОЛНОЕ ОПИСАНИЕ И НАЗНАЧЕНИЕ */
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ, ОБЛАСТЬ ПРИМЕНЕНИЯ И АРХИТЕКТУРА УСТРОЙСТВА\n\n" +
            "1.1. Область применения и позиционирование в энергосистеме:\n" +
            "Терминал серии AQ-F255 является флагманским микропроцессорным интеллектуальным электронным устройством (ИЭУ/IED) " +
            "линейки Arcteq Relays, спроектированным для управления, мониторинга, автоматики и комплексной защиты фидеров, " +
            "воздушных и кабельных линий электропередачи, а также понижающих и вольтодобавочных силовых трансформаторов в распределительных " +
            "электрических сетях номинальным напряжением 35 кВ. Устройство ориентировано на применение на узловых подстанциях (ПС) " +
            "35/10 кВ и 35/6 кВ, распределительных пунктах (РП), объектах малой генерации, а также на магистральных подстанциях 110-220 кВ " +
            "в качестве резервной защиты линий 35 кВ.\n\n" +
            "1.2. Поддержка топологий и конфигураций электрических сетей:\n" +
            "Конструктивные особенности и гибкость алгоритмов AQ-F255 позволяют использовать его в сетях со всеми известными типами " +
            "заземления нейтрали, характерными для класса напряжения 35 кВ:\n" +
            "• Сети с изолированной нейтралью (классическая схема, требующая контроля высших гармоник и направленных органов).\n" +
            "• Сети с резонансно-заземленной (компенсированной) нейтралью через дугогасящие реакторы (ДГР), включая схемы с автоматической подстройкой.\n" +
            "• Сети с резистивным заземлением нейтрали (высокоомным или низкоомным через заземляющий резистор).\n" +
            "Терминал идеально функционирует в радиальных сетях, радиальных сетях с двухсторонним питанием, замкнутых кольцевых сетях, " +
            "а также в сложных многоконтурных кольцах, где направление мощности короткого замыкания имеет критическое значение.\n\n" +
            "1.3. Полный перечень выполняемых защитных функций (Коды ANSI/МЭК):\n" +
            "Аппаратный комплекс и встроенное микропрограммное обеспечение реализуют следующий массив защит:\n" +
            "• ANSI 50/51 (МТЗ) — Максимальная токовая защита от межфазных КЗ с независимой и инверсной выдержкой времени (4 независимые ступени).\n" +
            "• ANSI 67 (НМТЗ) — Направленная максимальная токовая защита (4 ступени). Предотвращает ложные отключения при КЗ на смежных линиях.\n" +
            "• ANSI 21 (ДЗ) — Дистанционная защита (5 зон срабатывания) с возможностью построения полигональных (четырехугольных) характеристик " +
            "и характеристик типа МХО. Эффективно компенсирует переходное сопротивление дуги.\n" +
            "• ANSI 50N/51N (ЗЗ) — Ненаправленная защита от замыканий на землю (4 ступени).\n" +
            "• ANSI 67N (НЗЗ) — Направленная защита от замыканий на землю по току и напряжению нулевой последовательности (3Io и 3Uo).\n" +
            "• ANSI 51V — Вольт-амперная (токовая с вольтметровым пуском) МТЗ для повышения чувствительности при глубоких посадках напряжения.\n" +
            "• ANSI 46 — Защита от несимметрии токов нагрузки и обрыва фаз (по току обратной последовательности I2).\n" +
            "• ANSI 49F — Тепловая защита фидера / перегрузка линии на основе математической интегральной модели нагрева.\n" +
            "• ANSI 37 — Защита от недогрузки / потери нагрузки (актуально при контроле специализированных фидеров).\n" +
            "• ANSI 27 — Защита от снижения (понижения) линейных и фазных напряжений (2 ступени).\n" +
            "• ANSI 59 — Защита от повышения напряжения (2 ступени).\n" +
            "• ANSI 59N — Защита от повышения напряжения нулевой последовательности (3Uo) для сигнализации или отключения ОЗЗ.\n" +
            "• ANSI 81O/U (АЧР/ЧАР) — Защита по частоте: автоматическая частотная разгрузка при падении частоты и частотное автоматическое повторное включение (ЧАПВ).\n" +
            "• ANSI 25 — Контроль синхронизма и проверка условий параллельной работы двух энергосистем.\n" +
            "• ANSI 50BF (УРОВ) — Устройство резервирования отказов выключателя с контролем тока и инициацией от смежных защит.",
            /* РУКОВОДСТВО ПО КОНФИГУРАЦИИ И ПОДКЛЮЧЕНИЮ */
            "РАЗДЕЛ II: ИНСТРУКЦИЯ ПО МОНТАЖУ, НАЛАДКЕ И СИСТЕМНОЙ КОНФИГУРАЦИИ В ПО AQTIVATE\n\n" +
            "2.1. Подготовительный этап и организация канала связи с ПК:\n" +
            "1. Извлеките терминал из транспортной упаковки и убедитесь в отсутствии физических повреждений корпуса и клеммных колодок.\n" +
            "2. Выполните монтаж устройства в вырез панели шкафа РЗА (утопленный монтаж) с помощью штатных фиксирующих скоб.\n" +
            "3. Подайте оперативный ток на клеммы питания (допустимый диапазон указан на шильдике, обычно 80-265 В AC/DC).\n" +
            "4. Возьмите стандартный экранированный патч-корд Ethernet (кат. 5e или выше) и подключите его к разъему RJ45 на лицевой панели.\n" +
            "5. На инженерном ноутбуке зайдите в параметры сетевого адаптера и жестко пропишите статический IP-адрес: 192.168.2.50 с маской подсети 255.255.255.0. " +
            "Заводской IP-адрес лицевого порта терминалов Arcteq по умолчанию всегда равен 192.168.2.10.\n" +
            "6. Проверьте связь с прибором с помощью утилиты ping через командную строку (команда: ping 192.168.2.10).\n\n" +
            "2.2. Запуск ПО AQtivate 200 и считывание текущих настроек:\n" +
            "1. Запустите на ноутбуке специализированный программный комплекс AQtivate 200 (или актуальную версию AQtivate для серии 250).\n" +
            "2. Перейдите в верхнее меню 'Communication' -> 'Connection Settings'. Выберите протокол связи TCP/IP и введите IP-адрес: 192.168.2.10, порт: 20001 (или стандартный HTTP-порт в зависимости от прошивки).\n" +
            "3. Нажмите кнопку 'Connect' на панели инструментов. Статус-бар внизу экрана должен сменить цвет на зеленый, сигнализируя об успешном сопряжении.\n" +
            "4. Перейдите в меню 'File' -> 'Read From Device'. Программа загрузит текущую конфигурацию, структуру входов/выходов и матрицу логики. Сохраните этот исходный файл на жесткий диск как резервную копию (.aqt).\n\n" +
            "2.3. Конфигурирование аналоговых измерительных каналов (Вкладка 'General'):\n" +
            "1. Разверните дерево конфигурации и перейдите в меню 'Scaling' или 'Analog Inputs'.\n" +
            "2. В строке параметров фазных трансформаторов тока (ТТ) введите номинальные параметры измерительного керна. Для сети 35 кВ это обычно: " +
            "Первичный ток (IL1, IL2, IL3 Primary) = 400 А, 600 А или 1000 А (в соответствии с проектом), Вторичный ток (Secondary) = 5 А (или 1 А).\n" +
            "3. Настройте канал тока нулевой последовательности (Io, канал 4). При подключении к кабельному трансформатору тока типа ТЗЛР укажите его номинальный коэффициент, например 30/1 или 60/5. Это критично для точности ЗЗ.\n" +
            "4. Перейдите к параметрам трансформаторов напряжения (ТН). Для сети 35 кВ номинальное первичное линейное напряжение составляет 35000 В, фазное — 20207 В. " +
            "Номинальное вторичное линейное напряжение составляет 100 В, фазное — 57.7 В. Введите данные уставки: Primary Voltage = 35000 V, Secondary Voltage = 100 V.\n" +
            "5. Настройте канал расчетного или физического напряжения разомкнутого треугольника (Uo, канал 4 для ТН) на номинал 100 В.\n\n" +
            "2.4. Параметризация ступеней защит (Вкладка 'Protection'):\n" +
            "1. Зайдите в раздел 'Protection functions'. Перед вами откроется список доступных программных модулей защит.\n" +
            "2. Настройка МТЗ-1 (ANSI 50/51, Ступень отсечки): Активируйте модуль (состояние 'Enabled'). Установите тип выдержки времени — Независимая (DT). " +
            "Введите уставку по току срабатывания в относительных (отн.ед. Io/In) или абсолютных амперах. Задайте выдержку времени T = 0.00 с.\n" +
            "3. Настройка МТЗ-2 (ANSI 51, Ступень с выдержкой времени): Активируйте модуль. Выберите тип кривой — IEC Normal Inverse или DT.\n" +
            "4. Настройка Направленной МТЗ (ANSI 67): Задайте Характеристический Угол Линии (RCA) от -30 до -45 градусов.\n" +
            "5. Настройка Дистанционной защиты (ANSI 21): Введите активные (R) и реактивные (X) сопротивления линии в Ом/км, задайте полигональную характеристику.",
            /* ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА */
            "РАЗДЕЛ III: ТОНКОСТИ НАСТРОЙКИ, ИНЖЕНЕРНЫЕ СЕКРЕТЫ И РЕШЕНИЕ НЕШТАТНЫХ СИТУАЦИЙ\n\n" +
            "3.1. Тонкости организации цепей направленной защиты от замыканий на землю (ЗЗ):\n" +
            "В сетях 35 кВ емкостные токи малы. Главная ошибка наладчиков — неверная ориентация трансформатора тока нулевой последовательности (ТТНП, 'бублика').\n" +
            "• Правило монтажа: Опорная точка полярности ТТНП (Л1/P1) должна быть обращена к сборным шинам, а Л2/P2 — в сторону линии. " +
            "Заземляющий проводник брони кабеля ДОЛЖЕН быть пропущен обратно через окно ТТНП перед фиксацией на контуре заземления подстанции. " +
            "Иначе ток брони скомпенсирует первичный ток жилы, и AQ-F255 зафиксирует ноль.\n\n" +
            "3.2. Блокировка при неисправностях цепей напряжения (БНН / Fuse Failure):\n" +
            "Обрыв предохранителя ТН 35 кВ вызовет ложное срабатывание дистанционной защиты. Обязательно активируйте функцию БНН (ANSI 60 / VTS). " +
            "Алгоритм сравнивает появление U2 без скачка I2 и мгновенно блокирует ДЗ, переводя направленную МТЗ в ненаправленный режим.\n\n" +
            "3.3. Устранение частых нештатных ситуаций:\n" +
            "1. Симптом: Ошибка 'Analog Card Calib Error'.\n" +
            "   Решение: Перезагрузка по питанию либо Factory Reset через сервисное меню AQtivate.\n" +
            "2. Симптом: Фазовый угол мощности смещен на 180°.\n" +
            "   Решение: Перекоммутировать токовые цепи на клеммнике или инвертировать полярность канала (Phase Invert = True).",
            true
        );
        // Заполнение аппаратных блочков для AQ-F255
        aqF255.blocks.put(
            "Модуль процессора и питания (Main CPU Card)", 
            "Центральный вычислительный модуль на базе RISC-архитектуры. Отвечает за расчет математических матриц защит, осциллографирование и логику. " +
            "Содержит порт RJ45 (Ethernet), интерфейс RS-485 и реле аппаратного отказа (IRF)."
        );
        aqF255.blocks.put(
            "Измерительный аналоговый модуль токов и напряжений", 
            "Высокоточная плата АЦП. Включает 5 трансформаторов тока (3 фазных, 1 чувствительный для ЗЗ, 1 резервный) с динамическим диапазоном до 40*In, " +
            "и 4 трансформатора напряжения (3 фазных для контроля секции, 1 для разомкнутого треугольника 3Uo)."
        );
        aqF255.blocks.put(
            "Дискретный модуль ввода-вывода (DI/DO Card)", 
            "Модуль расширения логического интерфейса ячейки КРУ 35 кВ. Содержит 8 оптоизолированных дискретных входов со стиранием дребезга контактов " +
            "(номинал 110В/220В) и 7 выходных электромеханических реле, способных коммутировать токи до 30 А при замыкании."
        );
        aqF255.blocks.put(
            "Плата связи и протоколов МЭК 61850 (COM Board)", 
            "Сменный коммуникационный модуль для поддержки цифровой подстанции. Обеспечивает обмен GOOSE-сообщениями, поддержку протоколов MMS, SNTP и резервирование по кольцу (PRP/HSR)."
        );
        database.add(aqF255);

        // --------------------------------------------------------------------------------------
        // [2] ТЕРМИНАЛ: AQ-101 (Центральный блок дуговой защиты) — ЗАГОТОВКА КОМБАЙНА
        // --------------------------------------------------------------------------------------
        Terminal aq101 = new Terminal(
            "AQ-101 (Дуговая защита)",
            "Сверхбыстрое микропроцессорное устройство дуговой защиты для отсеков КРУ 6-35 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Терминал AQ-101 предназначен для сверхбыстрого обнаружения электрической дуги в отсеках КРУ и выдачи команды на отключение за 2-7 мс.\n" +
            "Контролирует до 4 точечных оптических датчиков типа AQ-01.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Настройка осуществляется аппаратно с помощью DIP-переключателей на задней панели прибора.\n" +
            "Кнопка 'Auto Setup' используется для автоматического сканирования и запоминания подключенных оптических датчиков.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• При проведении ремонтных работ в ячейке КРУ закрывайте датчики во избежание ложных срабатываний от вспышек переносных ламп или сварки.\n" +
            "• Тестирование датчиков выполняйте только фирменным тестовым фонарем Arcteq AQ-TEST.",
            false // Заготовка
        );
        aq101.blocks.put("Центральная плата фотодатчиков", "Плата с 4 оптическими каналами для датчиков AQ-01.");
        aq101.blocks.put("Модуль полупроводниковых ключей (Triac Out)", "Быстродействующие тиристорные ключи выдачи команды на отключение.");
        database.add(aq101);

        // --------------------------------------------------------------------------------------
        // [3] ТЕРМИНАЛ: AQ-110P/F (Комбинированная дуговая защита с током)
        // --------------------------------------------------------------------------------------
        Terminal aq110 = new Terminal(
            "AQ-110P/F (Дуговая + Ток)",
            "Комбинированная дуговая защита с контролем фазных токов для исключения ложных срабатываний.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Реализует принцип двойного подтверждения: вспышка света + скачок тока по измерительным входам.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Подключение токовых цепей 1А/5А, установка порогов блокировки по току.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Тщательно отстраивайте уставку токовой блокировки от максимального рабочего тока нагрузки.",
            false
        );
        aq110.blocks.put("Аналоговая плата токов КЗ", "Содержит три трансформатора тока для контроля фаз.");
        aq110.blocks.put("Оптический трансивер петли (для AQ-110F)", "Контроль целостности непрерывного волоконно-оптического петлевого датчика.");
        database.add(aq110);

        // --------------------------------------------------------------------------------------
        // [4] ТЕРМИНАЛ: AQ-M210 (Защита электродвигателей)
        // --------------------------------------------------------------------------------------
        Terminal aqM210 = new Terminal(
            "AQ-M210 (Защита двигателей)",
            "Специализированный терминал защиты асинхронных и синхронных электродвигателей 6-10 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Тепловая модель двигателя (ANSI 49M), защита от затянутого пуска, блокировки ротора и несимметрии.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Ввод паспортных данных двигателя через ПО AQtivate, настройка термисторов RTD.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Корректно задавайте К-фактор токов обратной последовательности для защиты ротора.",
            false
        );
        aqM210.blocks.put("Модуль RTD датчиков температуры", "Плата для подключения термосопротивлений Pt100 контроля подшипников.");
        database.add(aqM210);

        // --------------------------------------------------------------------------------------
        // [5] ТЕРМИНАЛ: AQ-T215 (Защита трансформаторов)
        // --------------------------------------------------------------------------------------
        Terminal aqT215 = new Terminal(
            "AQ-T215 (Защита трансформаторов)",
            "Дифференциальная и резервная токовая защита двухобмоточных силовых трансформаторов.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Продольная дифференциальная защита (87T) с торможением и блокировкой по 2-й гармонике (БНТ).",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Ввод мощности, напряжений и группы соединения обмоток (например, Yd11).",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Обязательно снимайте векторную диаграмму под нагрузкой для проверки правильности фазировки ТТ.",
            false
        );
        aqT215.blocks.put("Прецизионный дифференциальный модуль", "Увеличенное число аналоговых входов для сторон ВН и НН.");
        database.add(aqT215);

        // --------------------------------------------------------------------------------------
        // [6] ТЕРМИНАЛ: AQ-L350 (Высоковольтная дифференциальная защита линий)
        // --------------------------------------------------------------------------------------
        Terminal aqL350 = new Terminal(
            "AQ-L350 (Высоковольтная РЗА)",
            "Быстродействующая продольная дифференциальная защита линий (87L) 110-220 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Сравнение векторов токов по оптоволоконному каналу связи с противоположным концом линии.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Настройка оптических интерфейсов C37.94, синхронизация PTP / GPS.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Контролируйте симметрию оптических каналов связи для исключения ложных срабатываний.",
            false
        );
        aqL350.blocks.put("Оптический модуль C37.94", "Интерфейс связи по волоконно-оптической линии до 120 км.");
        database.add(aqL350);
    }

    /*
    ПОСТРОЕНИЕ ГРАФИЧЕСКОГО ИНТЕРФЕЙСА КОМБАЙНА (БЕЗ XML-ФАЙЛОВ РАЗМЕТКИ)
    */
    private void buildCombinerUI() {
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);
        mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#030712"));

        // 1. ГЛАВНОЕ МЕНЮ: СПИСОК ПРИБОРОВ
        mainMenuListView = new ListView(this);
        mainMenuListView.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return database.size(); }
            @Override public Object getItem(int position) { return database.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                Terminal t = database.get(position);
                LinearLayout itemLayout = new LinearLayout(MainActivity.this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setPadding(20, 25, 20, 25);
                itemLayout.setBackgroundColor(android.graphics.Color.parseColor("#0F172A"));

                TextView tvName = new TextView(MainActivity.this);
                tvName.setText(t.name);
                tvName.setTextColor(android.graphics.Color.parseColor("#38BDF8"));
                tvName.setTextSize(16f);
                tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                itemLayout.addView(tvName);

                TextView tvDesc = new TextView(MainActivity.this);
                tvDesc.setText(t.shortDescription);
                tvDesc.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
                tvDesc.setTextSize(12f);
                tvDesc.setPadding(0, 5, 0, 0);
                itemLayout.addView(tvDesc);

                // Индикатор готовности базы
                TextView tvStatus = new TextView(MainActivity.this);
                tvStatus.setText(t.isFullyLoaded ? "🟢 База данных полная (Все разделы)" : "🟡 Заготовка (Доступна для наполнения)");
                tvStatus.setTextColor(android.graphics.Color.parseColor(t.isFullyLoaded ? "#10B981" : "#F59E0B"));
                tvStatus.setTextSize(10f);
                tvStatus.setPadding(0, 8, 0, 0);
                itemLayout.addView(tvStatus);

                return itemLayout;
            }
        });

        mainMenuListView.setOnItemClickListener((parent, view, position, id) -> {
            currentSelectedTerminal = database.get(position);
            showSubmenuScreen(currentSelectedTerminal);
        });

        mainLayout.addView(mainMenuListView);

        // 2. ИНИЦИАЛИЗАЦИЯ ЭКРАНА ПОДМЕНЮ (СКРЫТ ПО УМОЛЧАНИЮ)
        submenuScrollView = new ScrollView(this);
        submenuScrollView.setVisibility(View.GONE);
        LinearLayout submenuRoot = new LinearLayout(this);
        submenuRoot.setOrientation(LinearLayout.VERTICAL);

        Button btnBackToMain = createStyledButton("← Вернуться к списку всех приборов", "#334155");
        btnBackToMain.setOnClickListener(v -> showMainMenuScreen());
        submenuRoot.addView(btnBackToMain);

        submenuTitleView = new TextView(this);
        submenuTitleView.setTextColor(android.graphics.Color.parseColor("#F8FAFC"));
        submenuTitleView.setTextSize(18f);
        submenuTitleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        submenuTitleView.setPadding(0, 20, 0, 20);
        submenuRoot.addView(submenuTitleView);

        submenuButtonsLayout = new LinearLayout(this);
        submenuButtonsLayout.setOrientation(LinearLayout.VERTICAL);
        submenuRoot.addView(submenuButtonsLayout);

        submenuScrollView.addView(submenuRoot);
        mainLayout.addView(submenuScrollView);

        // 3. ИНИЦИАЛИЗАЦИЯ ЭКРАНА ПРОСМОТРА ДЕТАЛЬНОГО КОНТЕНТА (СКРЫТ ПО УМОЛЧАНИЮ)
        contentScrollView = new ScrollView(this);
        contentScrollView.setVisibility(View.GONE);
        LinearLayout contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);

        Button btnBackToSubmenu = createStyledButton("← Вернуться в подменю прибора", "#334155");
        btnBackToSubmenu.setOnClickListener(v -> showSubmenuScreen(currentSelectedTerminal));
        contentRoot.addView(btnBackToSubmenu);

        contentTitleView = new TextView(this);
        contentTitleView.setTextColor(android.graphics.Color.parseColor("#38BDF8"));
        contentTitleView.setTextSize(16f);
        contentTitleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        contentTitleView.setPadding(0, 20, 0, 10);
        contentRoot.addView(contentTitleView);

        contentBodyView = new TextView(this);
        contentBodyView.setTextColor(android.graphics.Color.parseColor("#E2E8F0"));
        contentBodyView.setTextSize(14f);
        contentBodyView.setLineSpacing(6f, 1.2f);
        contentBodyView.setPadding(0, 10, 0, 40);
        contentRoot.addView(contentBodyView);

        contentScrollView.addView(contentRoot);
        mainLayout.addView(contentScrollView);

        setContentView(mainLayout);
        setTitle("Arcteq Pro Combiner — Справочник РЗА");
    }

    /*
    ПОКАЗАТЬ ГЛАВНОЕ МЕНЮ
    */
    private void showMainMenuScreen() {
        currentSelectedTerminal = null;
        submenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        mainMenuListView.setVisibility(View.VISIBLE);
        setTitle("Arcteq Pro Combiner — Справочник РЗА");
    }

    /*
    ПОКАЗАТЬ ПОДМЕНЮ ДЛЯ ВЫБРАННОГО ПРИБОРА
    Здесь создаются необходимые кнопки категорий: Описание, Конфигурация, Аппаратные блочки, Секреты.
    */
    private void showSubmenuScreen(Terminal terminal) {
        mainMenuListView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        submenuScrollView.setVisibility(View.VISIBLE);

        setTitle(terminal.name);
        submenuTitleView.setText("📌 Меню прибора: " + terminal.name);
        submenuButtonsLayout.removeAllViews();

        // Кнопка 1: Общее описание и функции
        Button btn1 = createStyledButton("📋 1. Назначение и полный функционал (Описание)", "#1D4ED8");
        btn1.setOnClickListener(v -> showContentScreen(
            "📋 Общее описание и функции: " + terminal.name,
            terminal.fullDescription.isEmpty() ? "Информация для данного прибора пока не заполнена. Вы можете добавить ее в коде в методе initDatabase()." : terminal.fullDescription
        ));
        submenuButtonsLayout.addView(btn1);

        // Кнопка 2: Конфигурация и ПО
        Button btn2 = createStyledButton("⚙️ 2. Руководство по конфигурации и наладке", "#0F766E");
        btn2.setOnClickListener(v -> showContentScreen(
            "⚙️ Конфигурация и ПО: " + terminal.name,
            terminal.setupGuide.isEmpty() ? "Раздел конфигурации находится в разработке." : terminal.setupGuide
        ));
        submenuButtonsLayout.addView(btn2);

        // Кнопка 3: Аппаратные блочки и модули (С динамической генерацией из Map)
        Button btn3 = createStyledButton("🧱 3. Аппаратные блочки и модули (Состав и описание)", "#6D28D9");
        btn3.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            sb.append("КОНСТРУКТИВНЫЙ СОСТАВ И МОДУЛИ ДЛЯ ").append(terminal.name).append("\n\n");
            if (terminal.blocks.isEmpty()) {
                sb.append("Информация по аппаратным блокам пока не добавлена.");
            } else {
                int index = 1;
                for (Map.Entry<String, String> entry : terminal.blocks.entrySet()) {
                    sb.append(index++).append(". ").append(entry.getKey()).append(":\n");
                    sb.append("   ↳ ").append(entry.getValue()).append("\n\n");
                }
            }
            showContentScreen("🧱 Аппаратные блочки и модули", sb.toString());
        });
        submenuButtonsLayout.addView(btn3);

        // Кнопка 4: Инженерные секреты и наладка
        Button btn4 = createStyledButton("💡 4. Инженерные секреты, нюансы и решение проблем", "#B45309");
        btn4.setOnClickListener(v -> showContentScreen(
            "💡 Инженерные секреты: " + terminal.name,
            terminal.proTips.isEmpty() ? "Секреты наладки для данного прибора пока не добавлены." : terminal.proTips
        ));
        submenuButtonsLayout.addView(btn4);
    }

    /*
    ПОКАЗАТЬ ЭКРАН ПРОСМОТРА ТЕКСТА КОНТЕНТА
    */
    private void showContentScreen(String title, String bodyText) {
        submenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.VISIBLE);
        contentTitleView.setText(title);
        contentBodyView.setText(bodyText);
    }

    /*
    ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ СОЗДАНИЯ СТИЛИЗОВАННЫХ КНОПОК
    */
    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(android.graphics.Color.parseColor("#F8FAFC"));
        btn.setTextSize(13f);
        btn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(android.graphics.Color.parseColor(colorHex));
        btn.setPadding(20, 25, 20, 25);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        btn.setLayoutParams(params);
        return btn;
    }

    @Override
    public void onBackPressed() {
        if (contentScrollView.getVisibility() == View.VISIBLE) {
            showSubmenuScreen(currentSelectedTerminal);
        } else if (submenuScrollView.getVisibility() == View.VISIBLE) {
            showMainMenuScreen();
        } else {
            super.onBackPressed();
        }
    }
}