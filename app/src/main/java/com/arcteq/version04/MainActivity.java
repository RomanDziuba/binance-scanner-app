package com.arcteq.version04;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import java.util.List;

/*
===================================================================================================
  ARCTEQ PRO COMBINER ENGINE v3.0 ULTRA - PROFESSIONAL RELAY PROTECTION REFERENCE & GUIDE
  Developer: Dziuba Roman
  Year: 2026
  Encoding: UTF-8
  Description: Exhaustive reference manual & interactive configuration guide for Arcteq Relays
  (AQ-F255, AQ-101, AQ-110P/F, AQ-M210, AQ-T215, AQ-L350) targeting 35 kV and MV substations.
===================================================================================================
*/

public class MainActivity extends AppCompatActivity {

    // STRUCT: HARDWARE BLOCK MODULE DETAILS
    static class HardwareBlock {
        String blockName;
        String composition;
        String configurationSteps;
        String practicalExample;

        HardwareBlock(String blockName, String composition, String configurationSteps, String practicalExample) {
            this.blockName = blockName;
            this.composition = composition;
            this.configurationSteps = configurationSteps;
            this.practicalExample = practicalExample;
        }
    }

    // STRUCT: TERMINAL DATABASE ENTRY
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

    private final List<Terminal> database = new ArrayList<>();

    // UI ELEMENTS
    private LinearLayout mainLayout;
    private ListView mainMenuListView;

    private ScrollView submenuScrollView;
    private TextView submenuTitleView;
    private LinearLayout submenuButtonsLayout;

    private ScrollView hardwareBlocksMenuScrollView;
    private TextView hardwareBlocksMenuTitleView;
    private LinearLayout hardwareBlocksButtonsLayout;

    private ScrollView contentScrollView;
    private TextView contentTitleView;
    private TextView contentBodyView;

    private Terminal currentSelectedTerminal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_DarkActionBar);

        initDatabase();
        buildCombinerUI();
    }

    /* =========================================================================================
       1. DATABASE INITIALIZATION (AQ-F255 & COMPANION TERMINALS WITH EXPANDED DETAILS)
       ========================================================================================= */
    private void initDatabase() {

        // --------------------------------------------------------------------------------------
        // [1] TERMINAL: AQ-F255 (ФЛАГМАН РЗА 35 кВ / 10 кВ - МАКСИМАЛЬНО ПОЛНАЯ БАЗА)
        // --------------------------------------------------------------------------------------
        Terminal aqF255 = new Terminal(
            "AQ-F255 (Флагман РЗА 35 кВ / 10 кВ)",
            "Универсальный микропроцессорный терминал защиты фидеров, трансформаторов, контроля выключателя и измерений.",

            /* =========================================================================
               РАЗДЕЛ I: НАЗНАЧЕНИЕ, ОБЛАСТЬ ПРИМЕНЕНИЯ И АРХИТЕКТУРА УСТРОЙСТВА
               ========================================================================= */
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ, ОБЛАСТЬ ПРИМЕНЕНИЯ И АРХИТЕКТУРА УСТРОЙСТВА AQ-F255

" +
            "1.1. Область применения и Позиционирование в Энергосистеме:
" +
            "Микропроцессорный терминал Arcteq AQ-F255 предназначен для построения высоконадежных систем релейной защиты, " +
            "автоматики (АВР, АПВ, УРОВ, АЧР/ЧАПВ), управления, мониторинга изоляции и коммерческого/технического учета электроэнергии. " +
            "Устройство рассчитано на применение в распределительных сетях номинальным напряжением 35 кВ, 10 кВ и 6 кВ, а также " +
            "на объектах генерации и крупных промышленных предприятиях.

" +
            "1.2. Режимы Заземления Нейтрали и Поддерживаемые Топологии Сетей:
" +
            "• Сети с изолированной нейтралью: точный векторный расчет I0, U0 и угла 3I0/3U0 для селективной защиты от однофазных замыканий на землю (ОЗЗ).
" +
            "• Сети с компенсацией емкостного тока (через ДГР / дугогасящий реактор): поддержка направленной защиты по высшим гармоникам и переходным процессам (ANSI 67YN / Admittance).
" +
            "• Сети с резистивным заземлением нейтрали (высокоомным или низкоомным): прямое измерение токов ОЗЗ чувствительными входами 0.2...1 А.
" +
            "• Сети с глухозаземленной нейтралью (110/35 кВ): полноценная защита от однофазных КЗ на землю (ANSI 50N/51N).

" +
            "1.3. Полный Спектр Выполняемых Защитных Функций (ANSI / МЭК):
" +
            "• ANSI 50/51 (NOC): Токовая отсечка (ТО) и ненаправленная МТЗ (4 независимые ступени с независимой или инверсной выдержкой времени IEC/IEEE).
" +
            "• ANSI 67 (DOC): Направленная максимальная токовая защита (фазная НМТЗ) с настраиваемым углом максимальной чувствительности (MTA = -180°...+180°).
" +
            "• ANSI 50N/51N (EOC): Токовая защита от замыканий на землю (ЗЗ) по каналу тока нулевой последовательности.
" +
            "• ANSI 67N (DEC): Направленная защита от замыканий на землю (НЗЗ) с секторальным разделением активной и реактивной составляющей тока.
" +
            "• ANSI 67YN: Направленная защита от ОЗЗ по адмитансному принципу (полной проводимости Y0, активной G0 и реактивной B0).
" +
            "• ANSI 46 (IOC): Защита от несимметрии токов и обрыва фаз по току обратной последовательности I2/I1.
" +
            "• ANSI 49F (TOL): Тепловая модель кабеля и силового трансформатора с учетом коэффициента нагрузки и постоянной времени нагрева/охлаждения.
" +
            "• ANSI 27 / 59 (UV/OV): Трехступенчатые защиты минимального (ЗМН) и максимального (ЗМК) напряжения (по фазным и линейным значениям).
" +
            "• ANSI 59N (ROV): Защита по напряжению нулевой последовательности 3U0 (контроль целостности изоляции сети).
" +
            "• ANSI 81O / 81U / 81R: Частотная защита (АЧР/ЧАПВ) и защита по скорости изменения частоты (df/dt) для предотвращения системных аварий.
" +
            "• ANSI 25 (SYN): Модуль контроля синхронизма и разности напряжений при включении выключателя.
" +
            "• ANSI 50BF (CBFP): Устройство резервирования отказа выключателя (УРОВ) с контролем тока и состояний вспомогательных контактов ПВ.
" +
            "• ANSI 79 (AR): Автоматическое повторное включение (АПВ) до 4 ступеней с бестрансформаторным контролем готовности выключателя.
" +
            "• ANSI 60 (VTS): Блокировка при неисправностях цепей напряжения (БНН / Fuse Failure) по соотношению токов и напряжений обратной последовательности.
" +
            "• ANSI 74TC / 74CC: Постоянный аппаратный контроль целостности цепей отключения и включения выключателя (TCS/CCS).",

            /* =========================================================================
               РАЗДЕЛ II: ИНСТРУКЦИЯ ПО МОНТАЖУ, НАЛАДКЕ И СИСТЕМНОЙ КОНФИГУРАЦИИ В ПО AQTIVATE
               ========================================================================= */
            "РАЗДЕЛ II: ИНСТРУКЦИЯ ПО МОНТАЖУ, НАЛАДКЕ И СИСТЕМНОЙ КОНФИГУРАЦИИ В ПО AQTIVATE 200

" +
            "2.1. Физический Монтаж и Сетевое Подключение:
" +
            "1. Закрепите корпус терминала AQ-F255 в вырезе панели шкафа РЗА, затяните боковые фиксаторы с моментом 1.2 Нм.
" +
            "2. Выполните заземление корпуса медным шинками через заземляющий штырь M4 (сопротивление контура заземления < 0.1 Ом).
" +
            "3. Подайте оперативный ток на клеммы питания X1: 80–265 V AC/DC. Проверьте загорание зелёного светодиода 'Ready'.
" +
            "4. Подключите Ethernet-кабель Cat.5e/6 от ПК к переднему RJ45 порту обслуживания.
" +
            "5. В сетевых настройках Windows задайте IP ПК: 192.168.2.50, Маска: 255.255.255.0. Заводской IP AQ-F255: 192.168.2.10.

" +
            "2.2. Пошаговая Работа с Сервисным ПО AQtivate 200:
" +
            "1. Запустите ПО AQtivate 200 -> перейдите во вкладку 'Communication' -> 'TCP/IP Connection'.
" +
            "2. Укажите IP-адрес 192.168.2.10, порт 20001. Нажмите 'Connect'.
" +
            "3. Нажмите 'Read Configuration from Device' (Считать данные с терминала). Пароль по умолчанию: 0000 или 1234.
" +
            "4. Сохраните резервную копию заводских установок (File -> Save As -> Factory_Backup.aqt).

" +
            "2.3. Пошаговая Конфигурация Масштабирования Измерительных Каналов (Scaling):
" +
            "• Перейдите в дерево параметров: 'System Parameters' -> 'Analog Inputs'.
" +
            "• Phase CT1-3: Задайте Primary Current = 400 А, Secondary Current = 5 А (или 1 А).
" +
            "• Earth CT4 (Канал I0 / ТТНП): Задайте Primary Current = 30 А, Secondary Current = 1 А (для кабельного ТТНП типа ТЗЛР-0.8).
" +
            "• Phase VT1-3: Задайте Primary Line Voltage = 35000 V, Secondary Line Voltage = 100 V.
" +
            "• Earth VT4 (Канал 3U0): Задайте Primary Voltage = 35000/V3 V, Secondary Voltage = 100/3 V (или 100 V для разомкнутого треугольника).

" +
            "2.4. Пошаговая Настройка Матрицы Выходных Реле и Сигналов (Logic Output Matrix):
" +
            "1. Откройте подраздел 'Matrix Configuration' -> 'Output Matrix'.
" +
            "2. Строки матрицы представляют собой виртуальные логические пуски и срабатывания функций (например, NOC1 Trip, DOC1 Trip, Arc Zone1 Trip, Fault Reset).
" +
            "3. Столбцы представляют собой физические выходные реле (Out 1 ... Out 8), тиристорные ключи (Triac 1..2) и светодиодные индикаторы (LED 1...16).
" +
            "4. Назначение отключения: Установите точку пересечения NOC1 Trip -> Out 1 (Реле отключения YAT) + LED 1 (Красный 'МТЗ-1').
" +
            "5. Выбор режима контактов: Задайте Latching (с удержанием до сброса) для отключения выключателя, и Non-Latching (импульс) для внешних блокировок.

" +
            "2.5. Настройка Аварийного Регистратора Осциллограмм (Disturbance Recorder):
" +
            "1. Перейдите в раздел 'Tools' -> 'Disturbance Recorder'.
" +
            "2. Задайте частоту дискретизации: 32 точки на период (1600 Гц при 50 Гц).
" +
            "3. Настройте длительность записи: Pre-trigger = 200 мс, Post-trigger = 1800 мс (общая длина 2 секунды).
" +
            "4. Включите триггеры запуска: срабатывание МТЗ, срабатывание дуговой защиты, изменение состояния дискретных входов DI1-DI8.",

            /* =========================================================================
               РАЗДЕЛ III: ТОНКОСТИ НАСТРОЙКИ, ИНЖЕНЕРНЫЕ СЕКРЕТЫ И РЕШЕНИЕ НЕШТАТНЫХ СИТУАЦИЙ
               ========================================================================= */
            "РАЗДЕЛ III: ТОНКОСТИ НАСТРОЙКИ, ИНЖЕНЕРНЫЕ СЕКРЕТЫ И РЕШЕНИЕ НЕШТАТНЫХ СИТУАЦИЙ

" +
            "3.1. Организация и Настройка Цепей Направленной ЗЗ (ANSI 67N):
" +
            "• Монтаж ТТНП: Маркировка Л1 (P1) должна смотреть в сторону сборных шин подстанции, Л2 (P2) — в сторону кабельной линии.
" +
            "• Проводник заземления экрана: Заземляющий проводник металлической брони и экрана кабеля ОБЯЗАТЕЛЬНО должен быть пропущен обратно через окно ТТНП перед подключением к контуру заземления подстанции! В противном случае емкостный ток экрана скомпенсирует ток жилы и защита не сработает.
" +
            "• Угол максимальной чувствительности (MTA):
" +
            "  - Для сетей с изолированной нейтралью set MTA = -90° (емкостный ток).
" +
            "  - Для сетей с активным резистором set MTA = 0° (активный ток).

" +
            "3.2. Логическая Блокировка Неисправностей Цепей Напряжения (БНН / Fuse Failure):
" +
            " При перегорании предохранителя на стороне ВН/НН ТН появляется напряжение U0 при отсутствии тока I0. " +
            "Функция БНН мгновенно блокирует работы направленных МТЗ (67) и защиты минимального напряжения (27), предотвращая ложное отключение отходящего фидера.

" +
            "3.3. Решение Нештатных Ситуаций и Диагностика Ошибок:
" +
            "• Ошибка 'Phase Sequence Error': Возникает при обратной последовательности фаз (А-С-В вместо А-В-С). Устраняется программной инверсией в меню 'Analog Inputs' -> 'Phase Rotation = BAC' или физической перекоммутацией вторичных жил ТТ.
" +
            "• Ошибка 'GOOSE Communication Offline': Проверьте совпадение VLAN ID на управляемом коммутаторе, соответствие GOOSE APPID и наличие корректного файла конфигурации SCL/SCD.
" +
            "• Сброс системных часов при отключении питания: Необходима замена литиевой батарейки CR2032 на процессорном модуле.",
            true
        );

        // --- ДЕТАЛИЗИРОВАННЫЕ АППАРАТНЫЕ БЛОКИ ДЛЯ AQ-F255 ---
        aqF255.hardwareBlocks.add(new HardwareBlock(
            "1. Модуль Процессора и Питания (Main CPU & Power Supply Card)",
            "• Высокопроизводительный 32-разрядный RISC-микропроцессор ARM Cortex с частотой 400 МГц.
" +
            "• Встроенный импульсный источник питания: 80–265 V AC/DC (опция 18–72 V DC).
" +
            "• Аппаратный аппаратный Watchdog таймер и реле самодиагностики IRF (Internal Relay Failure).
" +
            "• Коммуникационные порты: Front Ethernet RJ45, Rear RS485 (Modbus RTU, IEC 60870-5-103).
" +
            "• Энергонезависимая память FRAM для хранения 5000 событий и 100 осциллограмм.",
            "1. Зайдите в ПО AQtivate -> 'Device Settings' -> 'Network Setup'.
" +
            "2. Укажите IP-адрес: 10.120.34.15, Маску: 255.255.255.0, Шлюз: 10.120.34.1.
" +
            "3. Активируйте протокол синхронизации времени SNTP: IP сервера = 10.120.34.2, период опроса = 60 с.
" +
            "4. В матрице сигналов привяжите статус 'Internal Error' к контакту реле IRF (клеммы X1: 3-4).",
            "Пример настройки:
" +
            "При глубокой посадке оперативного напряжения ниже 70 В реле IRF размыкает контакт, выдавая сигнал 'Неисправность РЗА' на щит центральной сигнализации подстанции."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "2. Измерительный Аналоговый Модуль Тока и Напряжения (Analog Input Card)",
            "• 5 Измерительных каналов тока (IL1, IL2, IL3, I01 высокочувствительный 0.2..1А, I02 стандартный 1..5А).
" +
            "• 4 Измерительных канала напряжения (UL1, UL2, UL3, U0 / разомкнутый треугольник).
" +
            "• Дискретизация 64 точки на период, АЦП 16-бит с гальванической развязкой 4 кВ.",
            "1. Откройте раздел меню 'Analog Inputs' -> 'Scaling'.
" +
            "2. Введите Phase CT Primary = 600 A, Secondary = 5 A.
" +
            "3. Введите Earth CT Primary = 30 A, Secondary = 1 A (для кабельного ТТНП типа ТЗЛР-0.8).
" +
            "4. Введите Phase VT Primary = 35000 V, Secondary = 100 V.
" +
            "5. Выполните программный замер векторной диаграммы под нагрузкой для проверки фазировки.",
            "Пример настройки:
" +
            "Подключение фидера 35 кВ: Ток фазы А = 320 А (вторичный 4.0 А), Ток фазы B = 318 А, Ток фазы C = 321 А. Рассчитанный вектор I0 равен 0.02 А (нормальное состояние изоляции)."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "3. Модуль Дискретных Входов и Выходов (DI/DO Card Module A/B)",
            "• 8 Оптоизолированных дискретных входов DI (программный выбор порога: 24V, 110V, 220V AC/DC).
" +
            "• 8 Выходных электромеханических реле DO (из них 2 высокоскоростных тиристорных ключа Triac).
" +
            "• Коммутационная способность контактов: 30 А (в течение 0.2 с), 5 А длительно при 250 V AC.",
            "1. Перейдите в меню 'Digital Inputs'. Выберите вход DI1 и присвойте имя 'ПОЛОЖЕНИЕ ВЫКЛЮЧАТЕЛЯ ВКЛ'.
" +
            "2. Выставьте программный фильтр дребезга контактов Debounce Time = 15 мс.
" +
            "3. Перейдите в 'Output Matrix'. Назначьте выходу DO1 сигнал 'NOC1 TRIP' с удержанием (Latching).",
            "Пример настройки:
" +
            "Сигнал от блокировки электромагнитной двери ячейки КРУ подается на DI4, автоматически вызывая программный запрет АПВ выключателя в матрице логики."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "4. Плата Коммуникации и МЭК 61850 (IEC 61850 Communication Card)",
            "• 2x Ethernet порта (Optical LC / RJ45) с аппаратной поддержкой протоколов резервирования PRP и HSR.
" +
            "• Полный стекирование МЭК 61850-8-1 (MMS, GOOSE-сообщения) и МЭК 61850-9-2 (Sampled Values).",
            "1. Экспортируйте SCL/ICD файл устройства через AQtivate IEC Tool.
" +
            "2. Сконфигурируйте подписку GOOSE-пакетов (GOOSE Subscriber) на сигналы УРОВ от смежных фидеров.
" +
            "3. Задайте APPID = 0x4001, VLAN ID = 10, VLAN Priority = 4 для наивысшего приоритета в сети подстанции.",
            "Пример настройки:
" +
            "При КЗ на отходящей линии терминал AQ-F255 передает GOOSE-сообщение на вводной терминал за 2.2 мс для реализации системы Логической Защиты Шины (ЛЗШ)."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "5. Плата Оптической Дуговой Зашиты (Arc Protection Card)",
            "• 4 Оптических входа для точечных датчиков светa AQ-01/AQ-02 или петлевых волоконных датчиков AQ-07.
" +
            "• Время отклика оптического канала: 1 мс, время замыкания тиристорного ключа: < 2.5 мс.",
            "1. Подключите точечные датчики AQ-01 к портам Arc 1...Arc 4.
" +
            "2. В меню 'Arc Protection' выберите логику работы: (Arc Sensor 1) AND (Phase Current > 1.5 Iн).
" +
            "3. Назначьте срабатывание на тиристорный выход Triac 1 (DO1) для безинерционного отключения.",
            "Пример настройки:
" +
            "Возникновение дуговой вспышки в отсеке выключателя фиксируется датчиком AQ-01. Терминал подтверждает наличие тока КЗ и выдает команду TRIP за 2.1 мс."
        ));

        database.add(aqF255);

        // --------------------------------------------------------------------------------------
        // [2] ТЕРМИНАЛ: AQ-101 (Центральный блок дуговой защиты)
        // --------------------------------------------------------------------------------------
        Terminal aq101 = new Terminal(
            "AQ-101 (Центральный блок дуговой защиты)",
            "Сверхбыстрое автономное устройство дуговой защиты для КРУ 6-35 кВ.",
            "РАЗДЕЛ I: ОБЗОР УСТРОЙСТВА AQ-101

" +
            "Устройство AQ-101 предназначено для локализации дуговых замыканий в отсеках выключателя, шин и кабелей КРУ. " +
            "Время срабатывания составляет 2-7 мс, что полностью предотвращает разрушение ячеек КРУ.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ

" +
            "• Настройка осуществляется с помощью DIP-переключателей SW1, SW2 на задней стенке прибора.
" +
            "• Нажмите и удерживайте кнопку 'Auto Setup' в течение 3 секунд для калибровки и автоконфигурации датчиков.",
            "РАЗДЕЛ III: СЕКРЕТЫ ЭКСПЛУАТАЦИИ

" +
            "• При проведении ремонтных и сварочных работ закрывайте линзы датчиков чехлами во избежание срабатываний от вспышек сварки.",
            true
        );
        aq101.hardwareBlocks.add(new HardwareBlock(
            "1. Модуль Оптических Каналов Sensor Inputs",
            "4 независимых входа для датчиков AQ-01 (точечный) или AQ-06 (волоконный).",
            "Подключите датчики, подайте питание и нажмите кнопку 'Auto Setup' на передней панели.",
            "Пример: Датчик 1 - Кабельный отсек, Датчик 2 - Отсек выключателя."
        ));
        aq101.hardwareBlocks.add(new HardwareBlock(
            "2. Высокоскоростные Тиристорные Выходы (Triac Outputs)",
            "2 полупроводниковых тиристорных ключа с током коммутации до 250 А (10 мс).",
            "Установите переключатели DIP SW2 в положение 'ON' для привязки каналов 1-4 к ключу High Speed 1.",
            "Пример: Прямая команда на отключение отключающего соленоида YAT за 2.5 мс."
        ));
        database.add(aq101);

        // --------------------------------------------------------------------------------------
        // [3] ТЕРМИНАЛ: AQ-110P/F (Комбинированная дуговая защита)
        // --------------------------------------------------------------------------------------
        Terminal aq110 = new Terminal(
            "AQ-110P/F (Комбинированная дуговая + токовая защита)",
            "Защита от дуговых КЗ с двойным подтверждением (Оптика + Скачок Тока).",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ

Исключает ложные срабатывания от сторонних источников света благодаря блокировке по току КЗ.",
            "РАЗДЕЛ II: НАСТРОЙКА

Регулировка порогов тока I> и I0> потенциометрами или в программном меню.",
            "РАЗДЕЛ III: РЕКОМЕНДАЦИИ

Уставка I> выставляется на 20-30% выше максимального рабочего тока фидера.",
            true
        );
        aq110.hardwareBlocks.add(new HardwareBlock(
            "1. Измерительный Токовый Блок I>",
            "3 входа фазных ТТ (1А/5А) + 1 вход ТТНП.",
            "Задайте уставку срабатывания токового органа в пределах 0.5 - 5.0 Iн.",
            "Пример: При I > 2.5 Iн и вспышке света выдается команда TRIP."
        ));
        database.add(aq110);

        // --------------------------------------------------------------------------------------
        // [4] ТЕРМИНАЛ: AQ-M210 (Защита электродвигателей)
        // --------------------------------------------------------------------------------------
        Terminal aqM210 = new Terminal(
            "AQ-M210 (Защита высоковольтных электродвигателей)",
            "Терминал защиты асинхронных и синхронных двигателей 6-10 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ

Комплексная защита двигателей насосов, компрессоров, дымососов и мельниц.",
            "РАЗДЕЛ II: НАСТРОЙКА

Ввод паспортных данных мотора: Iном, Uном, Pном, Tstart, K-factor.",
            "РАЗДЕЛ III: СЕКРЕТЫ

Термодатчики RTD требуют 3-х проводного подключения для компенсации сопротивления жил.",
            true
        );
        aqM210.hardwareBlocks.add(new HardwareBlock(
            "1. Модуль Термопреобразователей RTD (Pt100)",
            "8 входов для температурных датчиков Pt100/Cu50 контроля обмоток и подшипников.",
            "В ПО AQtivate задайте пороги Alarm (120°C) и Trip (140°C).",
            "Пример: Мониторинг температуры подшипника насоса питательной воды."
        ));
        database.add(aqM210);

        // --------------------------------------------------------------------------------------
        // [5] ТЕРМИНАЛ: AQ-T215 (Защита трансформаторов)
        // --------------------------------------------------------------------------------------
        Terminal aqT215 = new Terminal(
            "AQ-T215 (Защита двухтрансформаторных установок)",
            "Продольная дифференциальная защита силовых трансформаторов 35/10 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ

Основная быстродействующая защита от всех видов КЗ внутри бака трансформатора.",
            "РАЗДЕЛ II: НАСТРОЙКА

Выбор группы соединений обмоток (Y/D-11), выравнивание токов ВН и НН.",
            "РАЗДЕЛ III: СЕКРЕТЫ

Блокировка по 2-й гармонике исключает ложное срабатывание при броске тока намагничивания.",
            true
        );
        aqT215.hardwareBlocks.add(new HardwareBlock(
            "1. Дифференциальный Блок Измерения (Idiff / Ibias)",
            "Аналоговые входы для плеч ВН и НН с выравниванием коэффициентов.",
            "Задайте I-bias торможение и уставку Idiff> = 0.2 - 0.3 Iн.",
            "Пример: Защита трансформатора ТДНС-10000/35."
        ));
        database.add(aqT215);

        // --------------------------------------------------------------------------------------
        // [6] ТЕРМИНАЛ: AQ-L350 (Высоковольтная ДЗЛ)
        // --------------------------------------------------------------------------------------
        Terminal aqL350 = new Terminal(
            "AQ-L350 (Продольная дифференциальная защита линий)",
            "Высокоскоростная ДЗЛ (87L) для линий 110-220 кВ по оптоволокну.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ

Абсолютно селективная защита протяженных ЛЭП с обменом векторами токов.",
            "РАЗДЕЛ II: НАСТРОЙКА

Синхронизация по PTP (IEEE 1588) или GPS, настройка C37.94.",
            "РАЗДЕЛ III: СЕКРЕТЫ

Контролируйте задержку сигнала в ВОЛС (Ping delay < 5 мс).",
            true
        );
        aqL350.hardwareBlocks.add(new HardwareBlock(
            "1. Модуль Оптического Интерфейса C37.94",
            "Оптический трансивер IEEE C37.94 для связи с противоположным концом ЛЭП.",
            "Установите скорость 64 кбит/с ... 2 Мбит/с и проверьте баланс оптической мощности.",
            "Пример: Канал ДЗЛ между ПС 110 кВ 'Северная' и ПС 110 кВ 'Южная'."
        ));
        database.add(aqL350);
    }

    /* =========================================================================================
       2. USER INTERFACE CONSTRUCTION & STYLING ENGINE
       ========================================================================================= */
    private void buildCombinerUI() {
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);
        mainLayout.setBackgroundColor(Color.parseColor("#030712"));

        // HEADER: BRANDING & CREDITS
        TextView tvHeader = new TextView(this);
        tvHeader.setText("🚀 ARCTEQ PRO COMBINER v3.0 ULTRA\nDeveloper: Dziuba Roman | Year: 2026");
        tvHeader.setTextColor(Color.parseColor("#38BDF8"));
        tvHeader.setTextSize(13f);
        tvHeader.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvHeader.setPadding(0, 0, 0, 20);
        mainLayout.addView(tvHeader);

        // 1. MAIN MENU LIST VIEW
        mainMenuListView = new ListView(this);
        mainMenuListView.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return database.size(); }
            @Override public Object getItem(int position) { return database.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                Terminal t = database.get(position);
                LinearLayout itemLayout = new LinearLayout(MainActivity.this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setPadding(24, 28, 24, 28);
                
                GradientDrawable drawable = new GradientDrawable();
                drawable.setColor(Color.parseColor("#0F172A"));
                drawable.setCornerRadius(12f);
                drawable.setStroke(2, Color.parseColor("#1E293B"));
                itemLayout.setBackground(drawable);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 18);
                itemLayout.setLayoutParams(params);

                TextView tvName = new TextView(MainActivity.this);
                tvName.setText(t.name);
                tvName.setTextColor(Color.parseColor("#38BDF8"));
                tvName.setTextSize(16f);
                tvName.setTypeface(Typeface.DEFAULT_BOLD);
                itemLayout.addView(tvName);

                TextView tvDesc = new TextView(MainActivity.this);
                tvDesc.setText(t.shortDescription);
                tvDesc.setTextColor(Color.parseColor("#94A3B8"));
                tvDesc.setTextSize(12f);
                tvDesc.setPadding(0, 6, 0, 0);
                itemLayout.addView(tvDesc);

                TextView tvStatus = new TextView(MainActivity.this);
                tvStatus.setText(t.isFullyLoaded ? "🟢 Полноценная база (Все разделы развернуты)" : "🟡 Базовая конфигурация");
                tvStatus.setTextColor(Color.parseColor(t.isFullyLoaded ? "#10B981" : "#F59E0B"));
                tvStatus.setTextSize(11f);
                tvStatus.setPadding(0, 10, 0, 0);
                itemLayout.addView(tvStatus);

                return itemLayout;
            }
        });

        mainMenuListView.setOnItemClickListener((parent, view, position, id) -> {
            currentSelectedTerminal = database.get(position);
            showSubmenuScreen(currentSelectedTerminal);
        });

        mainLayout.addView(mainMenuListView);

        // 2. SUBMENU SCREEN (CATEGORIES)
        submenuScrollView = new ScrollView(this);
        submenuScrollView.setVisibility(View.GONE);
        LinearLayout submenuRoot = new LinearLayout(this);
        submenuRoot.setOrientation(LinearLayout.VERTICAL);

        Button btnBackToMain = createStyledButton("← Вернуться к списку всех приборов", "#334155");
        btnBackToMain.setOnClickListener(v -> showMainMenuScreen());
        submenuRoot.addView(btnBackToMain);

        submenuTitleView = new TextView(this);
        submenuTitleView.setTextColor(Color.parseColor("#F8FAFC"));
        submenuTitleView.setTextSize(18f);
        submenuTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        submenuTitleView.setPadding(0, 20, 0, 20);
        submenuRoot.addView(submenuTitleView);

        submenuButtonsLayout = new LinearLayout(this);
        submenuButtonsLayout.setOrientation(LinearLayout.VERTICAL);
        submenuRoot.addView(submenuButtonsLayout);

        submenuScrollView.addView(submenuRoot);
        mainLayout.addView(submenuScrollView);

        // 3. HARDWARE BLOCKS SUBMENU SCREEN
        hardwareBlocksMenuScrollView = new ScrollView(this);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        LinearLayout blocksMenuRoot = new LinearLayout(this);
        blocksMenuRoot.setOrientation(LinearLayout.VERTICAL);

        Button btnBackToSubmenu = createStyledButton("← Вернуться в меню прибора", "#334155");
        btnBackToSubmenu.setOnClickListener(v -> showSubmenuScreen(currentSelectedTerminal));
        blocksMenuRoot.addView(btnBackToSubmenu);

        hardwareBlocksMenuTitleView = new TextView(this);
        hardwareBlocksMenuTitleView.setTextColor(Color.parseColor("#38BDF8"));
        hardwareBlocksMenuTitleView.setTextSize(18f);
        hardwareBlocksMenuTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        hardwareBlocksMenuTitleView.setPadding(0, 20, 0, 20);
        blocksMenuRoot.addView(hardwareBlocksMenuTitleView);

        hardwareBlocksButtonsLayout = new LinearLayout(this);
        hardwareBlocksButtonsLayout.setOrientation(LinearLayout.VERTICAL);
        blocksMenuRoot.addView(hardwareBlocksButtonsLayout);

        hardwareBlocksMenuScrollView.addView(blocksMenuRoot);
        mainLayout.addView(hardwareBlocksMenuScrollView);

        // 4. DETAILED CONTENT VIEW SCREEN
        contentScrollView = new ScrollView(this);
        contentScrollView.setVisibility(View.GONE);
        LinearLayout contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);

        Button btnBackToBlocksMenu = createStyledButton("← Вернуться в меню", "#334155");
        btnBackToBlocksMenu.setOnClickListener(v -> {
            if (currentSelectedTerminal != null && !currentSelectedTerminal.hardwareBlocks.isEmpty()) {
                showHardwareBlocksMenu(currentSelectedTerminal);
            } else {
                showSubmenuScreen(currentSelectedTerminal);
            }
        });
        contentRoot.addView(btnBackToBlocksMenu);

        contentTitleView = new TextView(this);
        contentTitleView.setTextColor(Color.parseColor("#38BDF8"));
        contentTitleView.setTextSize(17f);
        contentTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        contentTitleView.setPadding(0, 20, 0, 10);
        contentRoot.addView(contentTitleView);

        contentBodyView = new TextView(this);
        contentBodyView.setTextColor(Color.parseColor("#E2E8F0"));
        contentBodyView.setTextSize(14f);
        contentBodyView.setLineSpacing(6f, 1.25f);
        contentBodyView.setPadding(0, 10, 0, 40);
        contentRoot.addView(contentBodyView);

        contentScrollView.addView(contentRoot);
        mainLayout.addView(contentScrollView);

        setContentView(mainLayout);
        setTitle("Arcteq Pro Combiner v3.0");
    }

    /* =========================================================================================
       3. NAVIGATION & SCREEN CONTROLLER METHODS
       ========================================================================================= */
    private void showMainMenuScreen() {
        currentSelectedTerminal = null;
        submenuScrollView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        mainMenuListView.setVisibility(View.VISIBLE);
        setTitle("Arcteq Pro Combiner v3.0");
    }

    private void showSubmenuScreen(Terminal terminal) {
        mainMenuListView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        submenuScrollView.setVisibility(View.VISIBLE);

        setTitle(terminal.name);
        submenuTitleView.setText("📌 Меню прибора: " + terminal.name);
        submenuButtonsLayout.removeAllViews();

        // BUTTON 1: GENERAL DESCRIPTION
        Button btn1 = createStyledButton("📋 1. Назначение и полный функционал (Описание)", "#1D4ED8");
        btn1.setOnClickListener(v -> showContentScreen(
            "📋 Общее описание и функции: " + terminal.name,
            terminal.fullDescription.isEmpty() ? "Информация обновляется." : terminal.fullDescription
        ));
        submenuButtonsLayout.addView(btn1);

        // BUTTON 2: SETUP GUIDE
        Button btn2 = createStyledButton("⚙️ 2. Руководство по конфигурации и наладке (AQtivate)", "#0F766E");
        btn2.setOnClickListener(v -> showContentScreen(
            "⚙️ Конфигурация и ПО: " + terminal.name,
            terminal.setupGuide.isEmpty() ? "Раздел в разработке." : terminal.setupGuide
        ));
        submenuButtonsLayout.addView(btn2);

        // BUTTON 3: HARDWARE BLOCKS SUBMENU
        Button btn3 = createStyledButton("🧱 3. Аппаратные блочки и модули (Детализация и приклады)", "#6D28D9");
        btn3.setOnClickListener(v -> showHardwareBlocksMenu(terminal));
        submenuButtonsLayout.addView(btn3);

        // BUTTON 4: PRO TIPS AND SECRETS
        Button btn4 = createStyledButton("💡 4. Инженерные секреты, нюансы и решение проблем", "#B45309");
        btn4.setOnClickListener(v -> showContentScreen(
            "💡 Инженерные секреты: " + terminal.name,
            terminal.proTips.isEmpty() ? "Секреты добавлены." : terminal.proTips
        ));
        submenuButtonsLayout.addView(btn4);
    }

    private void showHardwareBlocksMenu(Terminal terminal) {
        submenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.VISIBLE);

        hardwareBlocksMenuTitleView.setText("🧱 Аппаратные модули: " + terminal.name);
        hardwareBlocksButtonsLayout.removeAllViews();

        if (terminal.hardwareBlocks.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Для данного устройства список аппаратных блочков пока пуст.");
            tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
            tvEmpty.setTextSize(14f);
            tvEmpty.setPadding(0, 20, 0, 20);
            hardwareBlocksButtonsLayout.addView(tvEmpty);
            return;
        }

        for (HardwareBlock block : terminal.hardwareBlocks) {
            Button btnBlock = createStyledButton("🔹 " + block.blockName, "#1E293B");
            btnBlock.setOnClickListener(v -> {
                StringBuilder sb = new StringBuilder();
                sb.append("📌 НАЗВАНИЕ МОДУЛЯ:\n").append(block.blockName).append("\n\n");
                sb.append("📋 ЧТО ВКЛЮЧАЕТ В СЕБЯ:\n").append(block.composition).append("\n\n");
                sb.append("⚙️ КАК КОНФИГУРИРОВАТЬ (AQtivate):\n").append(block.configurationSteps).append("\n\n");
                sb.append("💡 ПРАКТИЧЕСКИЙ ПРИМЕР И КЕЙС:\n").append(block.practicalExample);

                showContentScreen("🧱 Детализация: " + block.blockName, sb.toString());
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
        btn.setTextColor(Color.parseColor("#F8FAFC"));
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setCornerRadius(8f);
        btn.setBackground(drawable);
        
        btn.setPadding(20, 24, 20, 24);
        
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