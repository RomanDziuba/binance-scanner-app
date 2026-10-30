package com.arcteq.version02;

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
║ 🚀 ARCTEQ PRO COMBINER ENGINE v3.0 ULTRA - УНИВЕРСАЛЬНЫЙ СПРАВОЧНИК РЗА С ПОДМЕНЮ БЛОЧКОВ   ║
║ Автор: Dziuba Roman                                                                       ║
║ Описание: Многоуровневый комбайн с иерархией: Список приборов -> Меню категорий ->        ║
║ Подменю аппаратных блочков -> Глубокая детализация конфигурации каждого блока с примерами.  ║
╚═══════════════════════════════════════════════════════════════════════════════════════════╝
*/
public class MainActivity extends AppCompatActivity {

    // СТРУКТУРА ДАННЫХ АППАРАТНОГО БЛОКА С ДЕТАЛИЗАЦИЕЙ КОНФИГУРАЦИИ И ПРИМЕРАМИ
    static class HardwareBlock {
        String blockName;
        String composition;         // Что включает в себя модуль[span_0](start_span)[span_0](end_span)
        String configurationSteps;  // Как конфигурировать каждый блочек[span_1](start_span)[span_1](end_span)
        String practicalExample;    // Небольшой понятный пример из практики[span_2](start_span)[span_2](end_span)

        HardwareBlock(String blockName, String composition, String configurationSteps, String practicalExample) {
            this.blockName = blockName;
            this.composition = composition;
            this.configurationSteps = configurationSteps;
            this.practicalExample = practicalExample;
        }
    }

    // СТРУКТУРА ДАННЫХ ТЕРМИНАЛА
    static class Terminal {
        String name;
        String shortDescription;
        String fullDescription;
        String setupGuide;
        String proTips;
        boolean isFullyLoaded;
        List<HardwareBlock> hardwareBlocks = new ArrayList<>();

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
    
    // Элементы подменю выбора категорий для конкретного прибора
    private ScrollView submenuScrollView;
    private TextView submenuTitleView;
    private LinearLayout submenuButtonsLayout;

    // Элементы подменю выбора конкретного аппаратного блока
    private ScrollView hardwareBlocksMenuScrollView;
    private TextView hardwareBlocksMenuTitleView;
    private LinearLayout hardwareBlocksButtonsLayout;

    // Элементы просмотра детального контента выбранного раздела или блока
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

        // ПОСТРОЕНИЕ ИНТЕРФЕЙСА КОМБАЙНА
        buildCombinerUI();
    }

    /*
    ИНИЦИАЛИЗАЦИЯ БАЗЫ ДАННЫХ И ДЕТАЛЬНЫХ БЛОЧКОВ
    */
    private void initDatabase() {

        // --------------------------------------------------------------------------------------
        // [1] ТЕРМИНАЛ: AQ-F255 (Флагман РЗА 35 кВ) — ПОЛНОЦЕННАЯ БАЗА И ДЕТАЛИЗАЦИЯ БЛОКОВ
        // --------------------------------------------------------------------------------------
        Terminal aqF255 = new Terminal(
            "AQ-F255 (Флагман РЗА 35 кВ)",
            "Флагманский микропроцессорный терминал для комплексной защиты фидеров и трансформаторов 35 кВ.",
            /* ПОЛНОЕ ОПИСАНИЕ */
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ, ОБЛАСТЬ ПРИМЕНЕНИЯ И АРХИТЕКТУРА УСТРОЙСТВА\n\n" +
            "1.1. Область применения и позиционирование в энергосистеме:\n" +
            "Терминал серии AQ-F255 является флагманским микропроцессорным интеллектуальным электронным устройством (ИЭУ/IED) " +
            "линейки Arcteq Relays, спроектированным для управления, мониторинга, автоматики и комплексной защиты фидеров, " +
            "воздушных и кабельных линий электропередачи, а также понижающих и вольтодобавочных силовых трансформаторов в распределительных " +
            "электрических сетях номинальным напряжением 35 кВ[span_3](start_span)[span_3](end_span).\n\n" +
            "1.2. Поддержка топологий и конфигураций электрических сетей:\n" +
            "Конструктивные особенности и гибкость алгоритмов AQ-F255 позволяют использовать его в сетях со всеми известными типами " +
            "заземления нейтрали (изолированная, компенсированная через ДГР, резистивно-заземленная)[span_4](start_span)[span_4](end_span).\n\n" +
            "1.3. Полный перечень выполняемых защитных функций (ANSI/МЭК):\n" +
            "ANSI 50/51 (МТЗ), ANSI 67 (НМТЗ), ANSI 21 (ДЗ), ANSI 50N/51N (ЗЗ), ANSI 67N (НЗЗ), ANSI 51V, ANSI 46, ANSI 49F, ANSI 27/59, ANSI 81O/U, ANSI 25, ANSI 50BF (УРОВ)[span_5](start_span)[span_5](end_span).",
            /* РУКОВОДСТВО ПО КОНФИГУРАЦИИ */
            "РАЗДЕЛ II: ИНСТРУКЦИЯ ПО МОНТАЖУ, НАЛАДКЕ И СИСТЕМНОЙ КОНФИГУРАЦИИ В ПО AQTIVATE\n\n" +
            "2.1. Подготовительный этап и организация канала связи:\n" +
            "1. Монтаж в шкаф РЗА, подача оперативного питания (80-265 В AC/DC)[span_6](start_span)[span_6](end_span).\n" +
            "2. Подключение Ethernet-патч-корда в порт RJ45 на лицевой панели[span_7](start_span)[span_7](end_span).\n" +
            "3. Установка статического IP-адреса на ПК: 192.168.2.50 (Заводской IP прибора: 192.168.2.10)[span_8](start_span)[span_8](end_span).\n\n" +
            "2.2. Запуск ПО AQtivate 200:\n" +
            "Подключение через TCP/IP порт 20001, считывание конфигурации (File -> Read From Device) и сохранение резервной копии (.aqt)[span_9](start_span)[span_9](end_span).",
            /* ИНЖЕНЕРНЫЕ СЕКРЕТЫ */
            "РАЗДЕЛ III: ТОНКОСТИ НАСТРОЙКИ, ИНЖЕНЕРНЫЕ СЕКРЕТЫ И РЕШЕНИЕ НЕШТАТНЫХ СИТУАЦИЙ\n\n" +
            "3.1. Тонкости организации цепей направленной защиты от замыканий на землю (ЗЗ):\n" +
            "Опорная точка полярности ТТНП (Л1/P1) должна быть обращена к сборным шинам, а Л2/P2 — в сторону линии. " +
            "Заземляющий проводник брони кабеля ДОЛЖЕН быть пропущен обратно через окно ТТНП[span_10](start_span)[span_10](end_span).\n\n" +
            "3.2. Блокировка при неисправностях цепей напряжения (БНН / Fuse Failure):\n" +
            "Активация функции ANSI 60 (VTS) для предотвращения ложного срабатывания дистанционной защиты при обрыве цепей ТН[span_11](start_span)[span_11](end_span).",
            true
        );

        // --- ДЕТАЛЬНЫЕ АППАРАТНЫЕ БЛОЧКИ AQ-F255 С ПОДБОРКОЙ КОНФИГУРАЦИИ И ПРИМЕРОВ ---
        aqF255.hardwareBlocks.add(new HardwareBlock(
            "1. Модуль процессора и питания (Main CPU Card)",
            /* Что включает в себя */
            "• Высокопроизводительный RISC-микроконтроллер для математических расчетов защит и логики.\n" +
            "• Энергонезависимая Flash/NVRAM-память для хранения логов срабатываний и осциллограмм.\n" +
            "• Аппаратные часы реального времени (RTC) с супервизором питания.\n" +
            "• Коммуникационные интерфейсы: порт RJ45 Ethernet (на лицевой панели) и RS-485.\n" +
            "• Реле аппаратного отказа (IRF — Internal Relay Failure) для сигнализации об исчезновении питания или сбое ПО.",
            /* Как конфигурировать */
            "1. В ПО AQtivate перейдите во вкладку 'Device Settings' -> 'Communication & Network'.\n" +
            "2. Введите статический IP-адрес терминала (например, 192.168.2.10), маску подсети (255.255.255.0) и сетевой порт (по умолчанию 20001).\n" +
            "3. Перейдите в подраздел 'Time Synchronization' и выберите протокол синхронизации времени (SNTP или PTP / IEEE 1588) для точной привязки меток времени в журнале событий.\n" +
            "4. Настройте контакты реле аппаратного отказа (IRF) в матрице дискретных выходов для выдачи аварийного сигнала на центральную сигнализацию подстанции при пропадании питания.",
            /* Практический пример */
            "Пример настройки:\n" +
            "IP-адрес: 10.10.20.51, Маска: 255.255.255.0, Шлюз: 10.10.20.1. Сервер синхронизации времени SNTP: 10.10.20.10 с периодом опроса 60 секунд. При перезагрузке или зависании процессора реле IRF размыкает сухой контакт, зажигая красный светодиод 'Error' на передней панели."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "2. Измерительный аналоговый модуль (Analog Input Card)",
            /* Что включает в себя */
            "• Высокоточные аналого-цифровые преобразователи (АЦП) с дискретизацией до 64 точек на период[span_12](start_span)[span_12](end_span).\n" +
            "• 5 трансформаторов тока (ТТ): 3 фазных канала (IL1, IL2, IL3), 1 высокочувствительный нулевой последовательности (Io) для защиты от замыканий на землю, 1 резервный.\n" +
            "• 4 трансформатора напряжения (ТН): 3 фазных канала (U1, U2, U3) для контроля напряжений секции, 1 канал (3Uo) для контроля напряжения разомкнутого треугольника[span_13](start_span)[span_13](end_span).",
            /* Как конфигурировать */
            "1. В ПО AQtivate откройте раздел 'Analog Inputs' / 'Scaling[span_14](start_span)'[span_14](end_span).\n" +
            "2. Для фазных ТТ введите первичный и вторичный номинальный ток измерительного керна (например, Primary = 600 А, Secondary = 5 А)[span_15](start_span)[span_15](end_span).\n" +
            "3. Для канала тока нулевой последовательности (Io) введите коэффициент трансформации подключенного кабельного ТТ типа ТЗЛР (например, 30/1 А)[span_16](start_span)[span_16](end_span).\n" +
            "4. Для каналов ТН введите первичное и вторичное линейное/фазное напряжение (для сети 35 кВ: Primary = 35000 В, Secondary = 100 В)[span_17](start_span)[span_17](end_span).\n" +
            "5. Проверьте угол сдвига фаз и при необходимости активируйте инверсию фазы (Phase Invert = True)[span_18](start_span)[span_18](end_span).",
            /* Практический пример */
            "Пример настройки:\n" +
            "ТТ линии 35 кВ: 400/5 А (вводим в ПО: IL1-3 Prim = 400, Sec = 5). ТН 35 кВ: 35000/100 В (вводим: U Prim = 35000, Sec = 100). ТТНП (ТЗЛР): 30/1 А (вводим: Io Prim = 30, Sec = 1). Это обеспечивает корректный расчет токов и сопротивлений для МТЗ и дистанционной защиты."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "3. Дискретный модуль ввода-вывода (DI/DO Card)",
            /* Что включает в себя */
            "• 8 оптоизолированных дискретных входов (DI1–DI8) с фильтрацией дребезга контактов (номинал 110В / 220В DC/AC)[span_19](start_span)[span_19](end_span).\n" +
            "• 7 выходных электромеханических реле (DO1–DO7) с мощными замыкающими контактами, способными коммутировать токи до 30 А при кратковременной перегрузке[span_20](start_span)[span_20](end_span).\n" +
            "• Светодиодная индикация состояния каждого входа и выхода на печатной плате.",
            /* Как конфигурировать */
            "1. В ПО AQtivate зайдите в меню 'Binary Inputs' (Дискретные входы).\n" +
            "2. Назначьте функции входам (например, DI1 — 'Внешняя блокировка МТЗ', DI2 — 'Контроль положения выключателя ПВ', DI3 — 'Команда аварийного отключения').\n" +
            "3. Перейдите в матрицу логических связей ('Output Matrix').\n" +
            "4. Свяжите внутренние сигналы пуска и срабатывания защит (например, 'DMT1 Trip', 'Dist Zone1 Trip') с физическими выходными реле DO1...DO7 (реле DO1 традиционно назначается на отключение выключателя)[span_21](start_span)[span_21](end_span).",
            /* Практический пример */
            "Пример настройки:\n" +
            "Вход DI1 конфигурируется как 'External Trip' (Внешнее отключение от газового реле). Выходное реле DO1 назначается на выдачу команды отключения вакуумного выключателя 35 кВ при срабатывании МТЗ-1 или Дистанционной защиты."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "4. Плата связи и МЭК 61850 (COM Board)",
            /* Что включает в себя */
            "• Сменный коммуникационный Ethernet-модуль для интеграции в автоматизированные системы управления (АСУ ТП).\n" +
            "• Поддержка протоколов МЭК 61850-8-1 (MMS, GOOSE-сообщения), Modbus TCP, IEC 60870-5-104.\n" +
            "• Поддержка аппаратного кольцевого резервирования по протоколам PRP и HSR.",
            /* Как конфигурировать */
            "1. Экспортируйте или импортируйте SCL / CID файл конфигурации подстанции через модуль 'IEC 61850 Configurator' в AQtivate.\n" +
            "2. Настройте издателей (Publishers) и подписчиков (Subscribers) GOOSE-сообщений для реализации горизонтальных связей между терминалами (например, для логической защиты шин — ЛЗШ).\n" +
            "3. Укажите уникальный AppID и приоритет VLAN (например, VLAN Priority = 4) для пакетов GOOSE, чтобы коммутаторы подстанции пропускали их вне очереди.",
            /* Практический пример */
            "Пример настройки:\n" +
            "Настройка GOOSE-обмена между AQ-F255 и вводным выключателем 35 кВ для передачи сигнала блокировки ввода при КЗ на отходящей линии. AppID задается как 0x0001, VLAN ID = 10."
        ));
        
        database.add(aqF255);

        // --------------------------------------------------------------------------------------
        // [2] ТЕРМИНАЛ: AQ-101 (Центральный блок дуговой защиты) — ЗАГОТОВКА С БЛОЧКАМИ
        // --------------------------------------------------------------------------------------
        Terminal aq101 = new Terminal(
            "AQ-101 (Дуговая защита)",
            "Сверхбыстрое микропроцессорное устройство дуговой защиты для отсеков КРУ 6-35 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Терминал AQ-101 предназначен для сверхбыстрого обнаружения электрической дуги в отсеках КРУ и выдачи команды на отключение за 2-7 мс.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Настройка DIP-переключателями и кнопкой 'Auto Setup' для сканирования датчиков AQ-01.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Закрывайте датчики при ремонтных работах во избежание ложных срабатываний.",
            false
        );
        aq101.hardwareBlocks.add(new HardwareBlock(
            "1. Центральная плата фотодатчиков",
            "Содержит 4 оптических канала для подключения точечных датчиков типа AQ-01[span_22](start_span)[span_22](end_span).",
            "Подключение датчиков в клеммные разъемы X1-X4. Калибровка и запоминание датчиков кнопкой 'Auto Setup' на лицевой панели.",
            "Пример: Подключение 4 датчиков: отсек выключателя, кабельный отсек, отсек шин верхний, отсек шин нижний."
        ));
        aq101.hardwareBlocks.add(new HardwareBlock(
            "2. Модуль полупроводниковых ключей (Triac Out)",
            "Высокоскоростные тиристорные ключи для безинерционной выдачи команды на отключение выключателя[span_23](start_span)[span_23](end_span).",
            "Настройка матрицы отключения с помощью аппаратных DIP-переключателей SW2 на задней панели.",
            "Пример: При срабатывании оптического канала 1 замыкается тиристорный ключ Triac 1, выдавая сигнал на отключение вводного выключателя за 2.5 мс."
        ));
        database.add(aq101);

        // --------------------------------------------------------------------------------------
        // [3] ТЕРМИНАЛ: AQ-110P/F (Комбинированная дуговая защита) — ЗАГОТОВКА
        // --------------------------------------------------------------------------------------
        Terminal aq110 = new Terminal(
            "AQ-110P/F (Дуговая + Ток)",
            "Комбинированная дуговая защита с контролем фазных токов для исключения ложных срабатываний.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Реализует принцип двойного подтверждения: вспышка света + скачок тока по измерительным входам.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Подключение токовых цепей 1А/5А и настройка порогов токовой блокировки.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Отстраивайте уставку токовой блокировки от максимального рабочего тока нагрузки.",
            false
        );
        aq110.hardwareBlocks.add(new HardwareBlock(
            "1. Аналоговая плата токов КЗ",
            "Содержит три трансформатора тока для контроля фаз и логического совпадения с оптикой[span_24](start_span)[span_24](end_span).",
            "Подключение цепей ТТ 1А или 5А, установка токового порога срабатывания.",
            "Пример: Уставка токовой блокировки выставляется на 1.3 от номинального тока трансформатора."
        ));
        database.add(aq110);

        // --------------------------------------------------------------------------------------
        // [4] ТЕРМИНАЛ: AQ-M210 (Защита электродвигателей) — ЗАГОТОВКА
        // --------------------------------------------------------------------------------------
        Terminal aqM210 = new Terminal(
            "AQ-M210 (Защита двигателей)",
            "Специализированный терминал защиты асинхронных и синхронных электродвигателей 6-10 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Тепловая модель двигателя (ANSI 49M), защита от затянутого пуска, блокировки ротора и несимметрии.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Ввод паспортных данных двигателя через ПО AQtivate, настройка термисторов RTD.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Корректно задавайте К-фактор токов обратной последовательности.",
            false
        );
        aqM210.hardwareBlocks.add(new HardwareBlock(
            "1. Модуль RTD датчиков температуры",
            "Плата для подключения термосопротивлений Pt100 контроля подшипников и обмоток[span_25](start_span)[span_25](end_span).",
            "Подключение датчиков по 3-х проводной схеме, задание порогов Alarm (120°C) и Trip (140°C).",
            "Пример: Подключение датчиков Pt100 подшипников электродвигателя насосной станции."
        ));
        database.add(aqM210);

        // --------------------------------------------------------------------------------------
        // [5] ТЕРМИНАЛ: AQ-T215 (Защита трансформаторов) — ЗАГОТОВКА
        // --------------------------------------------------------------------------------------
        Terminal aqT215 = new Terminal(
            "AQ-T215 (Защита трансформаторов)",
            "Дифференциальная и резервная токовая защита двухобмоточных силовых трансформаторов.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Продольная дифференциальная защита (87T) с торможением и блокировкой по 2-й гармонике (БНТ).",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Ввод мощности, напряжений и группы соединения обмоток (например, Yd11).",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Обязательно снимайте векторную диаграмму под нагрузкой.",
            false
        );
        aqT215.hardwareBlocks.add(new HardwareBlock(
            "1. Прецизионный дифференциальный модуль",
            "Увеличенное число аналоговых входов для сторон ВН и НН трансформатора[span_26](start_span)[span_26](end_span).",
            "Настройка плеч дифференциальной защиты, выбор группы соединения и коэффициентов трансформации.",
            "Пример: Трансформатор 35/10 кВ, группа Yd11, настройка тормозной характеристики Idmin."
        ));
        database.add(aqT215);

        // --------------------------------------------------------------------------------------
        // [6] ТЕРМИНАЛ: AQ-L350 (Высоковольтная ДЗЛ) — ЗАГОТОВКА
        // --------------------------------------------------------------------------------------
        Terminal aqL350 = new Terminal(
            "AQ-L350 (Высоковольтная РЗА)",
            "Быстродействующая продольная дифференциальная защита линий (87L) 110-220 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\n\n" +
            "Сравнение векторов токов по оптоволоконному каналу связи с противоположным концом линии.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\n\n" +
            "Настройка оптических интерфейсов C37.94, синхронизация PTP / GPS.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ И НАЛАДКА\n\n" +
            "• Контролируйте симметрию оптических каналов связи.",
            false
        );
        aqL350.hardwareBlocks.add(new HardwareBlock(
            "1. Оптический модуль C37.94",
            "Интерфейс связи по волоконно-оптической линии до 120 км[span_27](start_span)[span_27](end_span).",
            "Настройка скорости передачи данных, выбор длины волны и компенсация задержки сигнала.",
            "Пример: Организация канала ДЗЛ между подстанциями 110 кВ по выделенному одномодовому волокну."
        ));
        database.add(aqL350);
    }

    /*
    ПОСТРОЕНИЕ ИНТЕРФЕЙСА КОМБАЙНА
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

        // 2. ЭКРАН ПОДМЕНЮ КАТЕГОРИЙ ПРИБОРА
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

        // 3. ЭКРАН ПОДМЕНЮ ВЫБОРА АППАРАТНОГО БЛОКА
        hardwareBlocksMenuScrollView = new ScrollView(this);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        LinearLayout blocksMenuRoot = new LinearLayout(this);
        blocksMenuRoot.setOrientation(LinearLayout.VERTICAL);

        Button btnBackToSubmenu = createStyledButton("← Вернуться в меню прибора", "#334155");
        btnBackToSubmenu.setOnClickListener(v -> showSubmenuScreen(currentSelectedTerminal));
        blocksMenuRoot.addView(btnBackToSubmenu);

        hardwareBlocksMenuTitleView = new TextView(this);
        hardwareBlocksMenuTitleView.setTextColor(android.graphics.Color.parseColor("#38BDF8"));
        hardwareBlocksMenuTitleView.setTextSize(18f);
        hardwareBlocksMenuTitleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        hardwareBlocksMenuTitleView.setPadding(0, 20, 0, 20);
        blocksMenuRoot.addView(hardwareBlocksMenuTitleView);

        hardwareBlocksButtonsLayout = new LinearLayout(this);
        hardwareBlocksButtonsLayout.setOrientation(LinearLayout.VERTICAL);
        blocksMenuRoot.addView(hardwareBlocksButtonsLayout);

        hardwareBlocksMenuScrollView.addView(blocksMenuRoot);
        mainLayout.addView(hardwareBlocksMenuScrollView);

        // 4. ЭКРАН ПРОСМОТРА ДЕТАЛЬНОГО КОНТЕНТА
        contentScrollView = new ScrollView(this);
        contentScrollView.setVisibility(View.GONE);
        LinearLayout contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);

        Button btnBackToBlocksMenu = createStyledButton("← Вернуться к списку блочков", "#334155");
        btnBackToBlocksMenu.setOnClickListener(v -> {
            if (currentSelectedTerminal != null && !currentSelectedTerminal.hardwareBlocks.isEmpty()) {
                showHardwareBlocksMenu(currentSelectedTerminal);
            } else {
                showSubmenuScreen(currentSelectedTerminal);
            }
        });
        contentRoot.addView(btnBackToBlocksMenu);

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

    private void showMainMenuScreen() {
        currentSelectedTerminal = null;
        submenuScrollView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        mainMenuListView.setVisibility(View.VISIBLE);
        setTitle("Arcteq Pro Combiner — Справочник РЗА");
    }

    private void showSubmenuScreen(Terminal terminal) {
        mainMenuListView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        submenuScrollView.setVisibility(View.VISIBLE);

        setTitle(terminal.name);
        submenuTitleView.setText("📌 Меню прибора: " + terminal.name);
        submenuButtonsLayout.removeAllViews();

        // Кнопка 1: Описание
        Button btn1 = createStyledButton("📋 1. Назначение и полный функционал (Описание)", "#1D4ED8");
        btn1.setOnClickListener(v -> showContentScreen(
            "📋 Общее описание и функции: " + terminal.name,
            terminal.fullDescription.isEmpty() ? "Информация для данного прибора пока не заполнена." : terminal.fullDescription
        ));
        submenuButtonsLayout.addView(btn1);

        // Кнопка 2: Конфигурация
        Button btn2 = createStyledButton("⚙️ 2. Руководство по конфигурации и наладке", "#0F766E");
        btn2.setOnClickListener(v -> showContentScreen(
            "⚙️ Конфигурация и ПО: " + terminal.name,
            terminal.setupGuide.isEmpty() ? "Раздел конфигурации находится в разработке." : terminal.setupGuide
        ));
        submenuButtonsLayout.addView(btn2);

        // Кнопка 3: Аппаратные блочки (Открывает подменю выбора блочков)
        Button btn3 = createStyledButton("🧱 3. Аппаратные блочки и модули (Конфигурация и состав)", "#6D28D9");
        btn3.setOnClickListener(v -> showHardwareBlocksMenu(terminal));
        submenuButtonsLayout.addView(btn3);

        // Кнопка 4: Секреты
        Button btn4 = createStyledButton("💡 4. Инженерные секреты, нюансы и решение проблем", "#B45309");
        btn4.setOnClickListener(v -> showContentScreen(
            "💡 Инженерные секреты: " + terminal.name,
            terminal.proTips.isEmpty() ? "Секреты наладки пока не добавлены." : terminal.proTips
        ));
        submenuButtonsLayout.addView(btn4);
    }

    /*
    ПОДМЕНЮ ВЫБОРА КОНКРЕТНОГО АППАРАТНОГО БЛОЧКА
    */
    private void showHardwareBlocksMenu(Terminal terminal) {
        submenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.VISIBLE);

        hardwareBlocksMenuTitleView.setText("🧱 Модули и блочки: " + terminal.name);
        hardwareBlocksButtonsLayout.removeAllViews();

        if (terminal.hardwareBlocks.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Для данного прибора аппаратные блочки еще не добавлены.");
            tvEmpty.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
            tvEmpty.setTextSize(14f);
            tvEmpty.setPadding(0, 20, 0, 20);
            hardwareBlocksButtonsLayout.addView(tvEmpty);
            return;
        }

        for (HardwareBlock block : terminal.hardwareBlocks) {
            Button btnBlock = createStyledButton("🔹 " + block.blockName, "#1E293B");
            btnBlock.setOnClickListener(v -> {
                // Формируем красивый структурированный детальный отчет по блочку
                StringBuilder sb = new StringBuilder();
                sb.append("📌 НАЗВАНИЕ МОДУЛЯ:\n").append(block.blockName).append("\n\n");
                sb.append("📋 ЧТО ВКЛЮЧАЕТ В СЕБЯ:\n").append(block.composition).append("\n\n");
                sb.append("⚙️ КАК КОНФИГУРИРОВАТЬ:\n").append(block.configurationSteps).append("\n\n");
                sb.append("💡 ПРАКТИЧЕСКИЙ ПРИМЕР / КЕЙС:\n").append(block.practicalExample);

                showContentScreen("🧱 Детализация блочка: " + block.blockName, sb.toString());
            });
            hardwareBlocksButtonsLayout.addView(btnBlock);
        }
    }

    private void showContentScreen(String title, String bodyText) {
        submenuScrollView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.VISIBLE);
        contentTitleView.setText(title);
        contentBodyView.setText(bodyText);
    }

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
            if (currentSelectedTerminal != null && !currentSelectedTerminal.hardwareBlocks.isEmpty()) {
                showHardwareBlocksMenu(currentSelectedTerminal);
            } else {
                showSubmenuScreen(currentSelectedTerminal);
            }
        } else if (hardwareBlocksMenuScrollView.getVisibility() == View.VISIBLE) {
            showSubmenuScreen(currentSelectedTerminal);
        } else if (submenuScrollView.getVisibility() == View.VISIBLE) {
            showMainMenuScreen();
        } else {
            super.onBackPressed();
        }
    }
}