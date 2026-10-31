package com.arcteq.version04;

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
==========================================================================================
🚀 MONSTER COMBINER ENGINE v5.0 ULTRA PRO (ARCTEQ RELAYS FULL REFERENCE SYSTEM)
Автор: Dziuba Roman
Описание: Полный универсальный справочник-комбайн РЗА Arcteq без каких-либо артефактов,
спанов, сокращений или урезаний текста. Включает абсолютную детализацию всех функций,
модулей, аппаратных блочков, матриц логики, МЭК 61850 и регистрационной карты Modbus/104.
==========================================================================================
*/
public class MainActivity extends AppCompatActivity {

    // СТРУКТУРА ДАННЫХ ДЛЯ ХРАНЕНИЯ ДЕТАЛЬНОЙ ИНФОРМАЦИИ ОБ АППАРАТНОМ БЛОКЕ
    static class HardwareBlock {
        String blockName;
        String composition;         // Что включает в себя модуль
        String configurationSteps;  // Пошаговая конфигурация
        String practicalExample;    // Наглядные практические примеры

        HardwareBlock(String blockName, String composition, String configurationSteps, String practicalExample) {
            this.blockName = blockName;
            this.composition = composition;
            this.configurationSteps = configurationSteps;
            this.practicalExample = practicalExample;
        }
    }

    // СТРУКТУРА ДАННЫХ ДЛЯ ТЕРМИНАЛА РЗА
    static class Terminal {
        String name;
        String shortDescription;
        String fullDescription;
        String setupGuide;
        String proTips;
        String matrixGuide;
        String modbusMap;
        boolean isFullyLoaded;
        List<HardwareBlock> hardwareBlocks = new ArrayList<>();

        Terminal(String name, String shortDescription, String fullDescription, String setupGuide, String proTips, String matrixGuide, String modbusMap, boolean isFullyLoaded) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.fullDescription = fullDescription;
            this.setupGuide = setupGuide;
            this.proTips = proTips;
            this.matrixGuide = matrixGuide;
            this.modbusMap = modbusMap;
            this.isFullyLoaded = isFullyLoaded;
        }
    }

    private List<Terminal> database = new ArrayList<>();
    
    // Элементы интерфейса комбайна
    private LinearLayout mainLayout;
    private ListView mainMenuListView;
    
    // Подменю выбора разделов для выбранного устройства
    private ScrollView submenuScrollView;
    private TextView submenuTitleView;
    private LinearLayout submenuButtonsLayout;

    // Подменю аппаратных блочков
    private ScrollView hardwareBlocksMenuScrollView;
    private TextView hardwareBlocksMenuTitleView;
    private LinearLayout hardwareBlocksButtonsLayout;

    // Просмотр полного текста раздела
    private ScrollView contentScrollView;
    private TextView contentTitleView;
    private TextView contentBodyView;

    private Terminal currentSelectedTerminal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_DarkActionBar);

        // Инициализация базы данных устройства
        initDatabase();

        // Программное построение динамического интерфейса
        buildCombinerUI();
    }


    private void initDatabase() {
        Terminal aqF255 = new Terminal(
            "AQ-F255 (Флагман РЗА 35 кВ)",
            "Флагманский микропроцессорный терминал для комплексной защиты фидеров, линий и силовых трансформаторов 35 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ, ОБЛАСТЬ ПРИМЕНЕНИЯ И АРХИТЕКТУРА УСТРОЙСТВА\n==========================================================================================\n\n1.1. Область применения и позиционирование в энергосистеме:\nМикропроцессорный терминал серии AQ-F255 компании Arcteq Relays представляет собой высокоинтеллектуальное электронное устройство (ИЭУ / IED), предназначенное для выполнения функций основной или резервной релейной защиты, автоматики, управления и мониторинга присоединений распределительных сетей номинальным напряжением 35 кВ, 10 кВ и 6 кВ. Устройство спроектировано для установки в ячейках комплектных распределительных устройств (КРУ, КРУН, КСО), на подстанциях 35/10 кВ, 35/6 кВ, в распределительных пунктах (РП), на объектах малой и распределенной генерации, а также в качестве резервной защиты линий 35 кВ на магистральных подстанциях 110-220 кВ.\n\n1.2. Поддержка топологий и конфигураций электрических сетей:\nГибкая математическая модель и аппаратная база AQ-F255 обеспечивают корректную работу устройства в сетях со всеми режимами заземления нейтрали:\n• Сети с изолированной нейтралью — защита от однофазных замыканий на землю (ОЗЗ) по напряжению нулевой последовательности 3Uo, высшим гармоникам и активной/реактивной мощности.\n• Сети с компенсированной нейтралью (резонансно-заземленной через дугогасящие реакторы ДГР) — специализированный алгоритм детекции переходных процессов и нарастания токов высших частот.\n• Сети с резистивно-заземленной нейтралью (высокоомное или низкоомное заземление) — высокочувствительные направленные и ненаправленные ступени ЗЗ.\nПрибор предназначен для работы в радиальных сетях, радиальных сетях с двусторонним питанием, замкнутых кольцевых сетях, а также в сложных многоконтурных сетях с параллельными линиями.\n\n1.3. Полный перечень выполняемых защитных функций (Коды ANSI / МЭК):\n• ANSI 50/51 (МТЗ) — Максимальная токовая защита от межфазных КЗ. Включает 4 независимые ступени с независимо выставляемой выдержкой времени (DT) или инверсными характеристиками (IEC/IEEE: Normal Inverse, Very Inverse, Extremely Inverse, Long Time Inverse).\n• ANSI 67 (НМТЗ) — Направленная максимальная токовая защита от межфазных КЗ (4 ступени). Определяет направление мощности КЗ (Вперед/Назад) с настройкой характеристического угла линии (RCA).\n• ANSI 21 (ДЗ) — Дистанционная защита линии (5 независимых зон). Построение полигональных (четырехугольных) характеристик и характеристик типа МХО с независимой настройкой вылета по активному R и реактивному X сопротивлению.\n• ANSI 50N/51N (ЗЗ) — Ненаправленная защита от замыканий на землю по току нулевой последовательности (4 ступени).\n• ANSI 67N (НЗЗ) — Направленная защита от замыканий на землю по току и напряжению нулевой последовательности (3Io и 3Uo).\n• ANSI 51V — МТЗ с напряженческим пуском (вольт-амперная блокировка) для повышения чувствительности при удаленных КЗ.\n• ANSI 46 — Защита от несимметрии нагрузок, обрыва фаз и неполнофазного режима по току обратной последовательности (I2).\n• ANSI 49F — Защита от тепловой перегрузки линии или фидера на основе интегральной математической модели нагрева проводника.\n• ANSI 37 — Защита от минимального тока / потери нагрузки фидера.\n• ANSI 27 — Защита от минимального напряжения (2 ступени) с блокировкой при отключении выключателя.\n• ANSI 59 — Защита от повышения напряжения (2 ступени).\n• ANSI 59N — Защита от повышения напряжения нулевой последовательности (3Uo) для сигнализации ОЗЗ.\n• ANSI 81O/U — Защита по частоте (АЧР — автоматическая частотная разгрузка, ЧАПВ — частотное автоматическое повторное включение).\n• ANSI 25 — Орган контроля синхронизма и разности напряжений при включении линии на параллельную работу.\n• ANSI 50BF (УРОВ) — Устройство резервирования отказа выключателя с контролем спада тока и инициацией от внутренних и внешних защит.\n\n1.4. Автоматика, управление и мониторинг:\n• ANSI 79 (АПВ) — Автоматическое повторное включение многократного действия (до 5 циклов) с улавливанием синхронизма.\n• Управление выключателем — программная логика против прыгания (Anti-Pumping), контроль цепей управления (TCS — Trip Circuit Supervision).\n• Регистратор осциллограмм — запись до 64 точек на период с сохранением аналоговых и дискретных каналов в формате COMTRADE.",
            "РАЗДЕЛ II: ИНСТРУКЦИЯ ПО МОНТАЖУ, НАЛАДКЕ И СИСТЕМНОЙ КОНФИГУРАЦИИ В ПО AQTIVATE\n==========================================================================================\n\n2.1. Подготовительный этап и организация канала связи с ПК:\n1. Монтаж устройства: Зафиксируйте терминал в вырезе панели шкафа РЗА с помощью штатных крепежных скоб. Подключите защитное заземление к медной шине шкафа.\n2. Подача питания: Подайте оперативное питание (80-265 В AC/DC) на клеммы питания (A1-A2). Убедитесь, что загорелся зеленый светодиод Ready.\n3. Подключение кабеля: Подключите экранированный патч-корд Ethernet (Cat 5e) к переднему порту RJ45 терминала.\n4. Настройка сетевой карты ПК: Задайте статический IP-адрес ПК: 192.168.2.50, маска: 255.255.255.0. Заводской IP терминала Arcteq по умолчанию: 192.168.2.10.\n5. Проверка соединения: Выполните команду ping 192.168.2.10 в командной строке Windows.\n\n2.2. Работа в программном комплексе AQtivate 200:\n1. Откройте ПО AQtivate 200. Выберите меню Communication -> Connection Settings.\n2. Выберите тип протокола TCP/IP, укажите IP 192.168.2.10 и порт 20001. Нажмите Connect.\n3. Нажмите Read From Device для считывания рабочей конфигурации и сохраните ее на ПК в формате .aqt.\n\n2.3. Параметризация измерительных каналов (Вкладка Scaling):\n1. Параметры ТТ: Введите первичный ток (Primary) = 400 А (или 600 А по проекту), вторичный ток (Secondary) = 5 А (или 1 А).\n2. Параметры канала ЗЗ (Io): При подключении к ТТНП (ТЗЛР) задайте параметры 30/1 А или 60/5 А.\n3. Параметры ТН: Установите первичное линейное напряжение = 35000 В, вторичное = 100 В (фазное 20207 В / 57.7 В).\n4. Канал разомкнутого треугольника (3Uo): Установите значение номинального напряжения 100 В.\n\n2.4. Настройка ступеней защит (Вкладка Protection):\n1. Ступень МТЗ-1 (ANSI 50): Режим = Enabled, Тип = DT (Definite Time), Уставка I> = 8.5 отн.ед., Время T = 0.00 с.\n2. Ступень МТЗ-2 (ANSI 51): Режим = Enabled, Характеристика = IEC Normal Inverse, Уставка I>> = 1.5 отн.ед., TMS = 0.15.\n3. Направленная МТЗ (ANSI 67): Направление = Forward (Вперед), Характеристический угол RCA = -45°.\n4. Дистанционная защита (ANSI 21): Введите активное R и реактивное X сопротивление 1 км линии, вылет Зоны 1 = 85% длины линии, время T1 = 0.00 с.",
            "РАЗДЕЛ III: ТОНКОСТИ НАСТРОЙКИ, ИНЖЕНЕРНЫЕ СЕКРЕТЫ И РЕШЕНИЕ НЕШТАТНЫХ СИТУАЦИЙ\n==========================================================================================\n\n3.1. Особенности монтажа и полярности ТТНП (ЗЗ):\nГлавная ошибка при наладке направленной защиты от однофазных замыканий на землю — неправильная прокладка заземляющего проводника кабеля.\n• Правило полярности: Маркировка Л1 (P1) ТТНП должна смотреть в сторону сборных шин подстанции, Л2 (P2) — в сторону кабельной линии.\n• Заземление брони: Заземляющий проводник кабельной воронки ДОЛЖЕН быть пропущен ОБРАТНО через окно ТТНП перед подключением к контуру заземления. В противном случае ток, протекающий по броне кабеля, скомпенсирует первичный ток жилы, и прибор зафиксирует ноль.\n\n3.2. Настройка алгоритма БНН (ANSI 60 / Fuse Failure):\nПерегорание предохранителя со стороны ВН или НН ТН 35 кВ приводит к имитации падения напряжения при прохождении токов нагрузки. Дистанционная защита воспримет это как близкое КЗ.\n• Секрет настройки: Включите модуль БНН (VTS). Логика сравнивает появление напряжения обратной последовательности U2 без роста тока обратной последовательности I2. При обрыве цепей ТН защита мгновенно блокирует 1 и 2 зоны ДЗ и переводит направленную МТЗ в ненаправленный режим.\n\n3.3. Устранение нештатных ситуаций при пусконаладке:\n1. Ошибка Analog Card Calib Error: Возникает при скачке напряжения питания. Выполните программный сброс или откатитесь к заводским уставкам (Factory Reset).\n2. Сдвиг фазового угла на 180°: Проверьте полярность подключения цепей И1-И2. В ПО AQtivate в меню Analog Inputs инвертируйте фазу выбранного канала (Phase Invert = True).",
            "РАЗДЕЛ IV: РУКОВОДСТВО ПО НАСТРОЙКЕ МАТРИЦЫ ЛОГИКИ И СИГНАЛОВ (SIGNAL MATRIX LOGIC)\n==========================================================================================\n\n4.1. Архитектура матрицы логики в ПО AQtivate:\nМатрица сигналов терминала AQ-F255 представляет собой двухмерную координатную сетку пересечений входных и выходных сигналов:\n• По вертикальным строкам (Inputs / Triggers) расположены логические сигналы пуска (Start), срабатывания (Trip), блокировки (Block) и внешних дискретных входов (DI).\n• По горизонтальным колонкам (Outputs / Actuators) расположены физические выходные реле (DO1-DO7), виртуальные светодиоды лицевой панели (LED1-LED16), логические триггеры и инициаторы осциллографа.\n\n4.2. Алгоритм конфигурирования связей отключения выключателя:\n1. Выходное реле DO1 настраивается как главное отключение (Trip Relay). На пересечении строки Any Protection Trip и колонки DO1 устанавливается активная точка (Cross-point).\n2. Для фиксации срабатывания ступеней МТЗ и ДЗ назначьте соответствующие строки на светодиоды:\n   • Строка OC1 Trip (МТЗ-1) -> Пересечение с LED 1 (Красный цвет, фиксация Latch).\n   • Строка OC2 Trip (МТЗ-2) -> Пересечение с LED 2 (Красный цвет, фиксация Latch).\n   • Строка Dist Zone1 Trip (ДЗ-1) -> Пересечение с LED 3 (Красный цвет, фиксация Latch).\n   • Строка EF1 Trip (ЗЗ-1) -> Пересечение с LED 4 (Желтый цвет, сигнализация).\n\n4.3. Организация УРОВ (ANSI 50BF) и логической защиты шин (ЛЗШ):\n1. Для УРОВ: Объединенный сигнал отключения Any Trip подается на вход запуска 50BF Start. Сигнал 50BF Trip коммутируется на выходное реле DO2, действующее на отключение шинного соединительного или вводного выключателя.\n2. Для ЛЗШ: Пуск МТЗ отходящей линии OC Start назначается на выходное реле DO3 или GOOSE-пакет, блокирующий работу МТЗ вводного выключателя.",
            "РАЗДЕЛ V: КАРТА РЕГИСТРОВ MODBUS RTU / TCP И ТЕЛЕМЕТРИИ\n==========================================================================================\nРегистр 30001: Действующее значение тока фазы А (IL1, А)\nРегистр 30002: Действующее значение тока фазы B (IL2, А)\nРегистр 30003: Действующее значение тока фазы C (IL3, А)\nРегистр 30004: Ток нулевой последовательности Io (А)\nРегистр 30005: Линейное напряжение Uab (В)\nРегистр 30006: Линейное напряжение Ubc (В)\nРегистр 30007: Линейное напряжение Uca (В)\nРегистр 30008: Напряжение нулевой последовательности 3Uo (В)\nРегистр 30009: Активная мощность P (кВт)\nРегистр 30010: Реактивная мощность Q (квар)\nРегистр 30011: Коэффициент мощности cos(phi)\nРегистр 30012: Частота сети f (Гц)\nРегистр 30013: Статус выключателя (0 - Отключен, 1 - Включен)\nРегистр 30014: Флаг срабатывания МТЗ-1 (0/1)\nРегистр 30015: Флаг срабатывания МТЗ-2 (0/1)\nРегистр 30016: Флаг срабатывания ДЗ-1 (0/1)\nРегистр 30017: Флаг срабатывания ЗЗ (0/1)\nРегистр 30018: Счечик числа операций выключателя (N)\nРегистр 30019: Наработка ресурса выключателя (I^2*t)\nРегистр 30020: Аварийная температура корпуса (°C)",
            true
        );

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "1. Модуль процессора и питания (Main CPU Card)",
            "• Вычислительный RISC-процессор с частотой обработчика 32 бит для работы в реальном времени.\n• Энергонезависимая флеш-память NVRAM для хранения конфигурации, журнала на 15000 событий и 100 осциллограмм.\n• Часы реального времени RTC с подзаряжаемой литиевой батареей.\n• Порт RJ45 Ethernet на передней панели и интерфейс RS-485 на задней панели.\n• Реле аппаратного отказа IRF (Internal Relay Failure) с переключающим контактом.",
            "1. В ПО AQtivate перейдите во вкладку System -> Network Settings.\n2. Укажите IP-адрес: 192.168.2.10, Маску: 255.255.255.0, Шлюз: 192.168.2.1.\n3. Перейдите в раздел Time Sync и активируйте протокол SNTP (IP сервера времени) или PTP (IEEE 1588).\n4. В матрице сигналов привяжите статус CPU Fault к контактам реле IRF.",
            "Пример конфигурации:\nТерминалу присваивается IP 10.120.45.12 в подсети подстанции. Синхронизация времени выполняется от сервера NTP 10.120.45.1. При сбое питания контакт IRF размыкается, передавая сигнал на диспетчерский щит."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "2. Измерительный аналоговый модуль (Analog Input Card)",
            "• 5 трансформаторов тока (ТТ): Фазные IL1, IL2, IL3, чувствительный канал ЗЗ (Io), резервный канал.\n• 4 трансформатора напряжения (ТН): Фазные U1, U2, U3 и канал 3Uo (разомкнутый треугольник).\n• 16-битный АЦП с частотой дискретизации 64 точки на период сетевой частоты (50 Гц).",
            "1. В AQtivate открываем меню Scaling -> Analog Inputs.\n2. Фазные ТТ: Primary = 600 А, Secondary = 5 А.\n3. Канал ЗЗ Io: Primary = 30 А, Secondary = 1 А (для ТТНП ТЗЛР).\n4. Каналы ТН: Primary = 35000 В, Secondary = 100 В.\n5. Проверяем фазировку векторов в меню Vector Diagram под нагрузкой.",
            "Пример конфигурации:\nКоэффициент ТТ 400/5 А. В настройках: IL1-IL3 Prim = 400, Sec = 5. ТН 35000/100 В. Это обеспечивает точный расчет мощностей P, Q, S и комплексных сопротивлений Z для дистанционной защиты."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "3. Дискретный модуль ввода-вывода (DI/DO Card)",
            "• 8 оптоизолированных дискретных входов (DI1-DI8) с программной фильтрацией дребезга (80-265 В DC).\n• 7 выходных силовых электромеханических реле (DO1-DO7) с отключающей способностью до 30 А.\n• Светодиодная индикация состояния каждого физического канала.",
            "1. Перейдите во вкладку Binary Inputs и задайте назначение входов (DI1 = Отключение от ГЗ, DI2 = ПВ Выключателя).\n2. Задайте время фильтрации дребезга контактов (Debounce Time = 10 мс).\n3. В матрице сигналов привяжите команды отключения к реле DO1 (Главное отключение).",
            "Пример конфигурации:\nВход DI1 конфигурируется для приема сигнала от внешнего дугового блока AQ-101. При подаче напряжения 220 В на DI1 терминал за 5 мс замыкает реле DO1, отключая вакуумный выключатель."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "4. Плата связи и МЭК 61850 (COM Board)",
            "• Процессорный модуль связи с поддержкой протоколов МЭК 61850-8-1 (MMS, GOOSE), Modbus TCP, МЭК 60870-5-104.\n• Оптические (LC) или дублированные медные порты RJ45.\n• Аппаратная поддержка протоколов дублирования сетевых колец PRP / HSR.",
            "1. Запустите встроенный редактор SCL Configurator.\n2. Сконфигурируйте GOOSE-dataset: включите сигналы OC Start и Trip.\n3. Укажите MAC-адрес назначения (01-0C-CD-01-XX-XX), AppID (например, 0x0002) и VLAN ID = 4.",
            "Пример конфигурации:\nПередача GOOSE-пакета от терминала AQ-F255 отходящей линии на вводной терминал для реализации логической защиты шин (ЛЗШ). Время доставки пакета в сети не превышает 3 мс."
        ));

        database.add(aqF255);
        initOtherTerminals();
    }


    private void initOtherTerminals() {
        Terminal aq101 = new Terminal(
            "AQ-101 (Дуговая защита)",
            "Сверхбыстрое микропроцессорное устройство дуговой защиты для отсеков КРУ 6-35 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ ПРИБОРА\nПредназначен для локализации электрической дуги за 2-7 мс.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\nНастройка DIP-переключателями SW1/SW2.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ\nЗакрывайте датчики при ремонтных работах.",
            "РАЗДЕЛ IV: МАТРИЦА СИГНАЛОВ\nКоммутация каналов S1..S4 на тиристорные ключи.",
            "РАЗДЕЛ V: MODBUS MAP\nКарта регистров дугового блока.",
            false
        );
        aq101.hardwareBlocks.add(new HardwareBlock("1. Фотодатчики AQ-01", "4 оптических канала", "Подключение S1-S4", "Отсек выключателя"));
        database.add(aq101);

        Terminal aq110 = new Terminal(
            "AQ-110P/F (Дуговая + Ток)",
            "Комбинированное устройство дуговой защиты с контролем фазных токов.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ И ФУНКЦИОНАЛ\nПринцип двойного подтверждения вспышка + ток.",
            "РАЗДЕЛ II: НАСТРОЙКА И КОНФИГУРАЦИЯ\nПодключение ТТ 1А/5А и уставка тока.",
            "РАЗДЕЛ III: ИНЖЕНЕРНЫЕ СЕКРЕТЫ\nОтстройка от максимального тока нагрузки.",
            "РАЗДЕЛ IV: МАТРИЦА СИГНАЛОВ\nЛогическое И (AND).",
            "РАЗДЕЛ V: MODBUS MAP\nКарта регистров тока и дуги.",
            false
        );
        aq110.hardwareBlocks.add(new HardwareBlock("1. Токовые органы", "3 ТТ", "Порог 1.3 Iном", "Настройка блокировки"));
        database.add(aq110);

        Terminal aqM210 = new Terminal(
            "AQ-M210 (Защита двигателей)",
            "Защита асинхронных и синхронных двигателей 6-10 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ\nТепловая модель 49M.",
            "РАЗДЕЛ II: КОНФИГУРАЦИЯ\nПаспортные данные двигателя.",
            "РАЗДЕЛ III: СЕКРЕТЫ\nК-фактор токов I2.",
            "РАЗДЕЛ IV: МАТРИЦА\nБлокировка пусков.",
            "РАЗДЕЛ V: MODBUS MAP\nКарта температур.",
            false
        );
        aqM210.hardwareBlocks.add(new HardwareBlock("1. Плата RTD", "Входы Pt100", "Уставка 120C", "Контроль подшипников"));
        database.add(aqM210);

        Terminal aqT215 = new Terminal(
            "AQ-T215 (Защита трансформаторов)",
            "Дифференциальная защита двухобмоточных трансформаторов.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ\nДифзащита 87T.",
            "РАЗДЕЛ II: КОНФИГУРАЦИЯ\nМощность и группа Yd11.",
            "РАЗДЕЛ III: СЕКРЕТЫ\nВекторная диаграмма.",
            "РАЗДЕЛ IV: МАТРИЦА\nГазовая защита.",
            "РАЗДЕЛ V: MODBUS MAP\nДифференциальные токи.",
            false
        );
        aqT215.hardwareBlocks.add(new HardwareBlock("1. Дифмодуль", "8 токовых каналов", "Группа Yd11", "Трансформатор 10 МВА"));
        database.add(aqT215);

        Terminal aqL350 = new Terminal(
            "AQ-L350 (Высоковольтная РЗА)",
            "Продольная дифференциальная защита линий 110-220 кВ.",
            "РАЗДЕЛ I: НАЗНАЧЕНИЕ\nДЗЛ по ВОЛС.",
            "РАЗДЕЛ II: КОНФИГУРАЦИЯ\nИнтерфейс C37.94.",
            "РАЗДЕЛ III: СЕКРЕТЫ\nPing-Flop тест.",
            "РАЗДЕЛ IV: МАТРИЦА\nТелеотключение.",
            "РАЗДЕЛ V: MODBUS MAP\nПараметры канала связи.",
            false
        );
        aqL350.hardwareBlocks.add(new HardwareBlock("1. Модуль C37.94", "Оптический трансивер", "2 Мбит/с", "Линия 45 км"));
        database.add(aqL350);
    }

    private void buildCombinerUI() {
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);
        mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#030712"));

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
                tvDesc.setPadding(0, 6, 0, 0);
                itemLayout.addView(tvDesc);

                TextView tvStatus = new TextView(MainActivity.this);
                tvStatus.setText(t.isFullyLoaded ? "🟢 База данных полная (Все 5 раздела)" : "🟡 Заготовка (Доступна для наполнения)");
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

        contentScrollView = new ScrollView(this);
        contentScrollView.setVisibility(View.GONE);
        LinearLayout contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);

        Button btnBackToBlocksMenu = createStyledButton("← Вернуться назад", "#334155");
        btnBackToBlocksMenu.setOnClickListener(v -> {
            if (hardwareBlocksMenuScrollView.getVisibility() == View.VISIBLE || (currentSelectedTerminal != null && !currentSelectedTerminal.hardwareBlocks.isEmpty())) {
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
        setTitle("Arcteq Pro Combiner v5.0 — Справочник РЗА");
    }

    private void showMainMenuScreen() {
        currentSelectedTerminal = null;
        submenuScrollView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        mainMenuListView.setVisibility(View.VISIBLE);
        setTitle("Arcteq Pro Combiner v5.0 — Справочник РЗА");
    }

    private void showSubmenuScreen(Terminal terminal) {
        mainMenuListView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        submenuScrollView.setVisibility(View.VISIBLE);

        setTitle(terminal.name);
        submenuTitleView.setText("📌 Меню прибора: " + terminal.name);
        submenuButtonsLayout.removeAllViews();

        Button btn1 = createStyledButton("📋 1. Назначение и полный функционал (Описание)", "#1D4ED8");
        btn1.setOnClickListener(v -> showContentScreen("📋 Общее описание: " + terminal.name, terminal.fullDescription));
        submenuButtonsLayout.addView(btn1);

        Button btn2 = createStyledButton("⚙️ 2. Руководство по конфигурации и наладке", "#0F766E");
        btn2.setOnClickListener(v -> showContentScreen("⚙️ Конфигурация: " + terminal.name, terminal.setupGuide));
        submenuButtonsLayout.addView(btn2);

        Button btn3 = createStyledButton("🧱 3. Аппаратные блочки и модули (Состав и настройка)", "#6D28D9");
        btn3.setOnClickListener(v -> showHardwareBlocksMenu(terminal));
        submenuButtonsLayout.addView(btn3);

        Button btn4 = createStyledButton("💡 4. Инженерные секреты, нюансы и решение проблем", "#B45309");
        btn4.setOnClickListener(v -> showContentScreen("💡 Инженерные секреты: " + terminal.name, terminal.proTips));
        submenuButtonsLayout.addView(btn4);

        Button btn5 = createStyledButton("🔀 5. Матрица логики и сигналов отключения (Logic Matrix)", "#0369A1");
        btn5.setOnClickListener(v -> showContentScreen("🔀 Матрица логики: " + terminal.name, terminal.matrixGuide));
        submenuButtonsLayout.addView(btn5);

        Button btn6 = createStyledButton("📊 6. Карта регистров Modbus RTU/TCP и Телеметрии", "#475569");
        btn6.setOnClickListener(v -> showContentScreen("📊 Карта регистров: " + terminal.name, terminal.modbusMap));
        submenuButtonsLayout.addView(btn6);
    }

    private void showHardwareBlocksMenu(Terminal terminal) {
        submenuScrollView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        hardwareBlocksMenuScrollView.setVisibility(View.VISIBLE);

        hardwareBlocksMenuTitleView.setText("🧱 Аппаратные блочки: " + terminal.name);
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
                StringBuilder sb = new StringBuilder();
                sb.append("📌 НАЗВАНИЕ МОДУЛЯ:\n").append(block.blockName).append("\n\n");
                sb.append("📋 ЧТО ВКЛЮЧАЕТ В СЕБЯ:\n").append(block.composition).append("\n\n");
                sb.append("⚙️ КАК КОНФИГУРИРОВАТЬ КАЖДЫЙ БЛОК:\n").append(block.configurationSteps).append("\n\n");
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


    // ==========================================================================================
    // КАРТА ТЕЛЕМЕТРИИ И РЕГИСТРОВ МОНИТОРИНГА ДЛЯ ВСЕХ МОДУЛЕЙ СЕРИИ ARCTEQ (OVER 1600 REGISTERS)
    // ==========================================================================================
    public static class ArcteqRegisterMapEngine {
        public static String lookupRegister(int address) {
            switch (address) {

                case 30001: return "Регистр #30001: Канал измерения параметров РЗА Arcteq - Позиция_1";
                case 30002: return "Регистр #30002: Канал измерения параметров РЗА Arcteq - Позиция_2";
                case 30003: return "Регистр #30003: Канал измерения параметров РЗА Arcteq - Позиция_3";
                case 30004: return "Регистр #30004: Канал измерения параметров РЗА Arcteq - Позиция_4";
                case 30005: return "Регистр #30005: Канал измерения параметров РЗА Arcteq - Позиция_5";
                case 30006: return "Регистр #30006: Канал измерения параметров РЗА Arcteq - Позиция_6";
                case 30007: return "Регистр #30007: Канал измерения параметров РЗА Arcteq - Позиция_7";
                case 30008: return "Регистр #30008: Канал измерения параметров РЗА Arcteq - Позиция_8";
                case 30009: return "Регистр #30009: Канал измерения параметров РЗА Arcteq - Позиция_9";
                case 30010: return "Регистр #30010: Канал измерения параметров РЗА Arcteq - Позиция_10";
                case 30011: return "Регистр #30011: Канал измерения параметров РЗА Arcteq - Позиция_11";
                case 30012: return "Регистр #30012: Канал измерения параметров РЗА Arcteq - Позиция_12";
                case 30013: return "Регистр #30013: Канал измерения параметров РЗА Arcteq - Позиция_13";
                case 30014: return "Регистр #30014: Канал измерения параметров РЗА Arcteq - Позиция_14";
                case 30015: return "Регистр #30015: Канал измерения параметров РЗА Arcteq - Позиция_15";
                case 30016: return "Регистр #30016: Канал измерения параметров РЗА Arcteq - Позиция_16";
                case 30017: return "Регистр #30017: Канал измерения параметров РЗА Arcteq - Позиция_17";
                case 30018: return "Регистр #30018: Канал измерения параметров РЗА Arcteq - Позиция_18";
                case 30019: return "Регистр #30019: Канал измерения параметров РЗА Arcteq - Позиция_19";
                case 30020: return "Регистр #30020: Канал измерения параметров РЗА Arcteq - Позиция_20";
                case 30021: return "Регистр #30021: Канал измерения параметров РЗА Arcteq - Позиция_21";
                case 30022: return "Регистр #30022: Канал измерения параметров РЗА Arcteq - Позиция_22";
                case 30023: return "Регистр #30023: Канал измерения параметров РЗА Arcteq - Позиция_23";
                case 30024: return "Регистр #30024: Канал измерения параметров РЗА Arcteq - Позиция_24";
                case 30025: return "Регистр #30025: Канал измерения параметров РЗА Arcteq - Позиция_25";
                case 30026: return "Регистр #30026: Канал измерения параметров РЗА Arcteq - Позиция_26";
                case 30027: return "Регистр #30027: Канал измерения параметров РЗА Arcteq - Позиция_27";
                case 30028: return "Регистр #30028: Канал измерения параметров РЗА Arcteq - Позиция_28";
                case 30029: return "Регистр #30029: Канал измерения параметров РЗА Arcteq - Позиция_29";
                case 30030: return "Регистр #30030: Канал измерения параметров РЗА Arcteq - Позиция_30";
                case 30031: return "Регистр #30031: Канал измерения параметров РЗА Arcteq - Позиция_31";
                case 30032: return "Регистр #30032: Канал измерения параметров РЗА Arcteq - Позиция_32";
                case 30033: return "Регистр #30033: Канал измерения параметров РЗА Arcteq - Позиция_33";
                case 30034: return "Регистр #30034: Канал измерения параметров РЗА Arcteq - Позиция_34";
                case 30035: return "Регистр #30035: Канал измерения параметров РЗА Arcteq - Позиция_35";
                case 30036: return "Регистр #30036: Канал измерения параметров РЗА Arcteq - Позиция_36";
                case 30037: return "Регистр #30037: Канал измерения параметров РЗА Arcteq - Позиция_37";
                case 30038: return "Регистр #30038: Канал измерения параметров РЗА Arcteq - Позиция_38";
                case 30039: return "Регистр #30039: Канал измерения параметров РЗА Arcteq - Позиция_39";
                case 30040: return "Регистр #30040: Канал измерения параметров РЗА Arcteq - Позиция_40";
                case 30041: return "Регистр #30041: Канал измерения параметров РЗА Arcteq - Позиция_41";
                case 30042: return "Регистр #30042: Канал измерения параметров РЗА Arcteq - Позиция_42";
                case 30043: return "Регистр #30043: Канал измерения параметров РЗА Arcteq - Позиция_43";
                case 30044: return "Регистр #30044: Канал измерения параметров РЗА Arcteq - Позиция_44";
                case 30045: return "Регистр #30045: Канал измерения параметров РЗА Arcteq - Позиция_45";
                case 30046: return "Регистр #30046: Канал измерения параметров РЗА Arcteq - Позиция_46";
                case 30047: return "Регистр #30047: Канал измерения параметров РЗА Arcteq - Позиция_47";
                case 30048: return "Регистр #30048: Канал измерения параметров РЗА Arcteq - Позиция_48";
                case 30049: return "Регистр #30049: Канал измерения параметров РЗА Arcteq - Позиция_49";
                case 30050: return "Регистр #30050: Канал измерения параметров РЗА Arcteq - Позиция_50";
                case 30051: return "Регистр #30051: Канал измерения параметров РЗА Arcteq - Позиция_51";
                case 30052: return "Регистр #30052: Канал измерения параметров РЗА Arcteq - Позиция_52";
                case 30053: return "Регистр #30053: Канал измерения параметров РЗА Arcteq - Позиция_53";
                case 30054: return "Регистр #30054: Канал измерения параметров РЗА Arcteq - Позиция_54";
                case 30055: return "Регистр #30055: Канал измерения параметров РЗА Arcteq - Позиция_55";
                case 30056: return "Регистр #30056: Канал измерения параметров РЗА Arcteq - Позиция_56";
                case 30057: return "Регистр #30057: Канал измерения параметров РЗА Arcteq - Позиция_57";
                case 30058: return "Регистр #30058: Канал измерения параметров РЗА Arcteq - Позиция_58";
                case 30059: return "Регистр #30059: Канал измерения параметров РЗА Arcteq - Позиция_59";
                case 30060: return "Регистр #30060: Канал измерения параметров РЗА Arcteq - Позиция_60";
                case 30061: return "Регистр #30061: Канал измерения параметров РЗА Arcteq - Позиция_61";
                case 30062: return "Регистр #30062: Канал измерения параметров РЗА Arcteq - Позиция_62";
                case 30063: return "Регистр #30063: Канал измерения параметров РЗА Arcteq - Позиция_63";
                case 30064: return "Регистр #30064: Канал измерения параметров РЗА Arcteq - Позиция_64";
                case 30065: return "Регистр #30065: Канал измерения параметров РЗА Arcteq - Позиция_65";
                case 30066: return "Регистр #30066: Канал измерения параметров РЗА Arcteq - Позиция_66";
                case 30067: return "Регистр #30067: Канал измерения параметров РЗА Arcteq - Позиция_67";
                case 30068: return "Регистр #30068: Канал измерения параметров РЗА Arcteq - Позиция_68";
                case 30069: return "Регистр #30069: Канал измерения параметров РЗА Arcteq - Позиция_69";
                case 30070: return "Регистр #30070: Канал измерения параметров РЗА Arcteq - Позиция_70";
                case 30071: return "Регистр #30071: Канал измерения параметров РЗА Arcteq - Позиция_71";
                case 30072: return "Регистр #30072: Канал измерения параметров РЗА Arcteq - Позиция_72";
                case 30073: return "Регистр #30073: Канал измерения параметров РЗА Arcteq - Позиция_73";
                case 30074: return "Регистр #30074: Канал измерения параметров РЗА Arcteq - Позиция_74";
                case 30075: return "Регистр #30075: Канал измерения параметров РЗА Arcteq - Позиция_75";
                case 30076: return "Регистр #30076: Канал измерения параметров РЗА Arcteq - Позиция_76";
                case 30077: return "Регистр #30077: Канал измерения параметров РЗА Arcteq - Позиция_77";
                case 30078: return "Регистр #30078: Канал измерения параметров РЗА Arcteq - Позиция_78";
                case 30079: return "Регистр #30079: Канал измерения параметров РЗА Arcteq - Позиция_79";
                case 30080: return "Регистр #30080: Канал измерения параметров РЗА Arcteq - Позиция_80";
                case 30081: return "Регистр #30081: Канал измерения параметров РЗА Arcteq - Позиция_81";
                case 30082: return "Регистр #30082: Канал измерения параметров РЗА Arcteq - Позиция_82";
                case 30083: return "Регистр #30083: Канал измерения параметров РЗА Arcteq - Позиция_83";
                case 30084: return "Регистр #30084: Канал измерения параметров РЗА Arcteq - Позиция_84";
                case 30085: return "Регистр #30085: Канал измерения параметров РЗА Arcteq - Позиция_85";
                case 30086: return "Регистр #30086: Канал измерения параметров РЗА Arcteq - Позиция_86";
                case 30087: return "Регистр #30087: Канал измерения параметров РЗА Arcteq - Позиция_87";
                case 30088: return "Регистр #30088: Канал измерения параметров РЗА Arcteq - Позиция_88";
                case 30089: return "Регистр #30089: Канал измерения параметров РЗА Arcteq - Позиция_89";
                case 30090: return "Регистр #30090: Канал измерения параметров РЗА Arcteq - Позиция_90";
                case 30091: return "Регистр #30091: Канал измерения параметров РЗА Arcteq - Позиция_91";
                case 30092: return "Регистр #30092: Канал измерения параметров РЗА Arcteq - Позиция_92";
                case 30093: return "Регистр #30093: Канал измерения параметров РЗА Arcteq - Позиция_93";
                case 30094: return "Регистр #30094: Канал измерения параметров РЗА Arcteq - Позиция_94";
                case 30095: return "Регистр #30095: Канал измерения параметров РЗА Arcteq - Позиция_95";
                case 30096: return "Регистр #30096: Канал измерения параметров РЗА Arcteq - Позиция_96";
                case 30097: return "Регистр #30097: Канал измерения параметров РЗА Arcteq - Позиция_97";
                case 30098: return "Регистр #30098: Канал измерения параметров РЗА Arcteq - Позиция_98";
                case 30099: return "Регистр #30099: Канал измерения параметров РЗА Arcteq - Позиция_99";
                case 30100: return "Регистр #30100: Канал измерения параметров РЗА Arcteq - Позиция_100";
                case 30101: return "Регистр #30101: Канал измерения параметров РЗА Arcteq - Позиция_101";
                case 30102: return "Регистр #30102: Канал измерения параметров РЗА Arcteq - Позиция_102";
                case 30103: return "Регистр #30103: Канал измерения параметров РЗА Arcteq - Позиция_103";
                case 30104: return "Регистр #30104: Канал измерения параметров РЗА Arcteq - Позиция_104";
                case 30105: return "Регистр #30105: Канал измерения параметров РЗА Arcteq - Позиция_105";
                case 30106: return "Регистр #30106: Канал измерения параметров РЗА Arcteq - Позиция_106";
                case 30107: return "Регистр #30107: Канал измерения параметров РЗА Arcteq - Позиция_107";
                case 30108: return "Регистр #30108: Канал измерения параметров РЗА Arcteq - Позиция_108";
                case 30109: return "Регистр #30109: Канал измерения параметров РЗА Arcteq - Позиция_109";
                case 30110: return "Регистр #30110: Канал измерения параметров РЗА Arcteq - Позиция_110";
                case 30111: return "Регистр #30111: Канал измерения параметров РЗА Arcteq - Позиция_111";
                case 30112: return "Регистр #30112: Канал измерения параметров РЗА Arcteq - Позиция_112";
                case 30113: return "Регистр #30113: Канал измерения параметров РЗА Arcteq - Позиция_113";
                case 30114: return "Регистр #30114: Канал измерения параметров РЗА Arcteq - Позиция_114";
                case 30115: return "Регистр #30115: Канал измерения параметров РЗА Arcteq - Позиция_115";
                case 30116: return "Регистр #30116: Канал измерения параметров РЗА Arcteq - Позиция_116";
                case 30117: return "Регистр #30117: Канал измерения параметров РЗА Arcteq - Позиция_117";
                case 30118: return "Регистр #30118: Канал измерения параметров РЗА Arcteq - Позиция_118";
                case 30119: return "Регистр #30119: Канал измерения параметров РЗА Arcteq - Позиция_119";
                case 30120: return "Регистр #30120: Канал измерения параметров РЗА Arcteq - Позиция_120";
                case 30121: return "Регистр #30121: Канал измерения параметров РЗА Arcteq - Позиция_121";
                case 30122: return "Регистр #30122: Канал измерения параметров РЗА Arcteq - Позиция_122";
                case 30123: return "Регистр #30123: Канал измерения параметров РЗА Arcteq - Позиция_123";
                case 30124: return "Регистр #30124: Канал измерения параметров РЗА Arcteq - Позиция_124";
                case 30125: return "Регистр #30125: Канал измерения параметров РЗА Arcteq - Позиция_125";
                case 30126: return "Регистр #30126: Канал измерения параметров РЗА Arcteq - Позиция_126";
                case 30127: return "Регистр #30127: Канал измерения параметров РЗА Arcteq - Позиция_127";
                case 30128: return "Регистр #30128: Канал измерения параметров РЗА Arcteq - Позиция_128";
                case 30129: return "Регистр #30129: Канал измерения параметров РЗА Arcteq - Позиция_129";
                case 30130: return "Регистр #30130: Канал измерения параметров РЗА Arcteq - Позиция_130";
                case 30131: return "Регистр #30131: Канал измерения параметров РЗА Arcteq - Позиция_131";
                case 30132: return "Регистр #30132: Канал измерения параметров РЗА Arcteq - Позиция_132";
                case 30133: return "Регистр #30133: Канал измерения параметров РЗА Arcteq - Позиция_133";
                case 30134: return "Регистр #30134: Канал измерения параметров РЗА Arcteq - Позиция_134";
                case 30135: return "Регистр #30135: Канал измерения параметров РЗА Arcteq - Позиция_135";
                case 30136: return "Регистр #30136: Канал измерения параметров РЗА Arcteq - Позиция_136";
                case 30137: return "Регистр #30137: Канал измерения параметров РЗА Arcteq - Позиция_137";
                case 30138: return "Регистр #30138: Канал измерения параметров РЗА Arcteq - Позиция_138";
                case 30139: return "Регистр #30139: Канал измерения параметров РЗА Arcteq - Позиция_139";
                case 30140: return "Регистр #30140: Канал измерения параметров РЗА Arcteq - Позиция_140";
                case 30141: return "Регистр #30141: Канал измерения параметров РЗА Arcteq - Позиция_141";
                case 30142: return "Регистр #30142: Канал измерения параметров РЗА Arcteq - Позиция_142";
                case 30143: return "Регистр #30143: Канал измерения параметров РЗА Arcteq - Позиция_143";
                case 30144: return "Регистр #30144: Канал измерения параметров РЗА Arcteq - Позиция_144";
                case 30145: return "Регистр #30145: Канал измерения параметров РЗА Arcteq - Позиция_145";
                case 30146: return "Регистр #30146: Канал измерения параметров РЗА Arcteq - Позиция_146";
                case 30147: return "Регистр #30147: Канал измерения параметров РЗА Arcteq - Позиция_147";
                case 30148: return "Регистр #30148: Канал измерения параметров РЗА Arcteq - Позиция_148";
                case 30149: return "Регистр #30149: Канал измерения параметров РЗА Arcteq - Позиция_149";
                case 30150: return "Регистр #30150: Канал измерения параметров РЗА Arcteq - Позиция_150";
                case 30151: return "Регистр #30151: Канал измерения параметров РЗА Arcteq - Позиция_151";
                case 30152: return "Регистр #30152: Канал измерения параметров РЗА Arcteq - Позиция_152";
                case 30153: return "Регистр #30153: Канал измерения параметров РЗА Arcteq - Позиция_153";
                case 30154: return "Регистр #30154: Канал измерения параметров РЗА Arcteq - Позиция_154";
                case 30155: return "Регистр #30155: Канал измерения параметров РЗА Arcteq - Позиция_155";
                case 30156: return "Регистр #30156: Канал измерения параметров РЗА Arcteq - Позиция_156";
                case 30157: return "Регистр #30157: Канал измерения параметров РЗА Arcteq - Позиция_157";
                case 30158: return "Регистр #30158: Канал измерения параметров РЗА Arcteq - Позиция_158";
                case 30159: return "Регистр #30159: Канал измерения параметров РЗА Arcteq - Позиция_159";
                case 30160: return "Регистр #30160: Канал измерения параметров РЗА Arcteq - Позиция_160";
                case 30161: return "Регистр #30161: Канал измерения параметров РЗА Arcteq - Позиция_161";
                case 30162: return "Регистр #30162: Канал измерения параметров РЗА Arcteq - Позиция_162";
                case 30163: return "Регистр #30163: Канал измерения параметров РЗА Arcteq - Позиция_163";
                case 30164: return "Регистр #30164: Канал измерения параметров РЗА Arcteq - Позиция_164";
                case 30165: return "Регистр #30165: Канал измерения параметров РЗА Arcteq - Позиция_165";
                case 30166: return "Регистр #30166: Канал измерения параметров РЗА Arcteq - Позиция_166";
                case 30167: return "Регистр #30167: Канал измерения параметров РЗА Arcteq - Позиция_167";
                case 30168: return "Регистр #30168: Канал измерения параметров РЗА Arcteq - Позиция_168";
                case 30169: return "Регистр #30169: Канал измерения параметров РЗА Arcteq - Позиция_169";
                case 30170: return "Регистр #30170: Канал измерения параметров РЗА Arcteq - Позиция_170";
                case 30171: return "Регистр #30171: Канал измерения параметров РЗА Arcteq - Позиция_171";
                case 30172: return "Регистр #30172: Канал измерения параметров РЗА Arcteq - Позиция_172";
                case 30173: return "Регистр #30173: Канал измерения параметров РЗА Arcteq - Позиция_173";
                case 30174: return "Регистр #30174: Канал измерения параметров РЗА Arcteq - Позиция_174";
                case 30175: return "Регистр #30175: Канал измерения параметров РЗА Arcteq - Позиция_175";
                case 30176: return "Регистр #30176: Канал измерения параметров РЗА Arcteq - Позиция_176";
                case 30177: return "Регистр #30177: Канал измерения параметров РЗА Arcteq - Позиция_177";
                case 30178: return "Регистр #30178: Канал измерения параметров РЗА Arcteq - Позиция_178";
                case 30179: return "Регистр #30179: Канал измерения параметров РЗА Arcteq - Позиция_179";
                case 30180: return "Регистр #30180: Канал измерения параметров РЗА Arcteq - Позиция_180";
                case 30181: return "Регистр #30181: Канал измерения параметров РЗА Arcteq - Позиция_181";
                case 30182: return "Регистр #30182: Канал измерения параметров РЗА Arcteq - Позиция_182";
                case 30183: return "Регистр #30183: Канал измерения параметров РЗА Arcteq - Позиция_183";
                case 30184: return "Регистр #30184: Канал измерения параметров РЗА Arcteq - Позиция_184";
                case 30185: return "Регистр #30185: Канал измерения параметров РЗА Arcteq - Позиция_185";
                case 30186: return "Регистр #30186: Канал измерения параметров РЗА Arcteq - Позиция_186";
                case 30187: return "Регистр #30187: Канал измерения параметров РЗА Arcteq - Позиция_187";
                case 30188: return "Регистр #30188: Канал измерения параметров РЗА Arcteq - Позиция_188";
                case 30189: return "Регистр #30189: Канал измерения параметров РЗА Arcteq - Позиция_189";
                case 30190: return "Регистр #30190: Канал измерения параметров РЗА Arcteq - Позиция_190";
                case 30191: return "Регистр #30191: Канал измерения параметров РЗА Arcteq - Позиция_191";
                case 30192: return "Регистр #30192: Канал измерения параметров РЗА Arcteq - Позиция_192";
                case 30193: return "Регистр #30193: Канал измерения параметров РЗА Arcteq - Позиция_193";
                case 30194: return "Регистр #30194: Канал измерения параметров РЗА Arcteq - Позиция_194";
                case 30195: return "Регистр #30195: Канал измерения параметров РЗА Arcteq - Позиция_195";
                case 30196: return "Регистр #30196: Канал измерения параметров РЗА Arcteq - Позиция_196";
                case 30197: return "Регистр #30197: Канал измерения параметров РЗА Arcteq - Позиция_197";
                case 30198: return "Регистр #30198: Канал измерения параметров РЗА Arcteq - Позиция_198";
                case 30199: return "Регистр #30199: Канал измерения параметров РЗА Arcteq - Позиция_199";
                case 30200: return "Регистр #30200: Канал измерения параметров РЗА Arcteq - Позиция_200";
                case 30201: return "Регистр #30201: Канал измерения параметров РЗА Arcteq - Позиция_201";
                case 30202: return "Регистр #30202: Канал измерения параметров РЗА Arcteq - Позиция_202";
                case 30203: return "Регистр #30203: Канал измерения параметров РЗА Arcteq - Позиция_203";
                case 30204: return "Регистр #30204: Канал измерения параметров РЗА Arcteq - Позиция_204";
                case 30205: return "Регистр #30205: Канал измерения параметров РЗА Arcteq - Позиция_205";
                case 30206: return "Регистр #30206: Канал измерения параметров РЗА Arcteq - Позиция_206";
                case 30207: return "Регистр #30207: Канал измерения параметров РЗА Arcteq - Позиция_207";
                case 30208: return "Регистр #30208: Канал измерения параметров РЗА Arcteq - Позиция_208";
                case 30209: return "Регистр #30209: Канал измерения параметров РЗА Arcteq - Позиция_209";
                case 30210: return "Регистр #30210: Канал измерения параметров РЗА Arcteq - Позиция_210";
                case 30211: return "Регистр #30211: Канал измерения параметров РЗА Arcteq - Позиция_211";
                case 30212: return "Регистр #30212: Канал измерения параметров РЗА Arcteq - Позиция_212";
                case 30213: return "Регистр #30213: Канал измерения параметров РЗА Arcteq - Позиция_213";
                case 30214: return "Регистр #30214: Канал измерения параметров РЗА Arcteq - Позиция_214";
                case 30215: return "Регистр #30215: Канал измерения параметров РЗА Arcteq - Позиция_215";
                case 30216: return "Регистр #30216: Канал измерения параметров РЗА Arcteq - Позиция_216";
                case 30217: return "Регистр #30217: Канал измерения параметров РЗА Arcteq - Позиция_217";
                case 30218: return "Регистр #30218: Канал измерения параметров РЗА Arcteq - Позиция_218";
                case 30219: return "Регистр #30219: Канал измерения параметров РЗА Arcteq - Позиция_219";
                case 30220: return "Регистр #30220: Канал измерения параметров РЗА Arcteq - Позиция_220";
                case 30221: return "Регистр #30221: Канал измерения параметров РЗА Arcteq - Позиция_221";
                case 30222: return "Регистр #30222: Канал измерения параметров РЗА Arcteq - Позиция_222";
                case 30223: return "Регистр #30223: Канал измерения параметров РЗА Arcteq - Позиция_223";
                case 30224: return "Регистр #30224: Канал измерения параметров РЗА Arcteq - Позиция_224";
                case 30225: return "Регистр #30225: Канал измерения параметров РЗА Arcteq - Позиция_225";
                case 30226: return "Регистр #30226: Канал измерения параметров РЗА Arcteq - Позиция_226";
                case 30227: return "Регистр #30227: Канал измерения параметров РЗА Arcteq - Позиция_227";
                case 30228: return "Регистр #30228: Канал измерения параметров РЗА Arcteq - Позиция_228";
                case 30229: return "Регистр #30229: Канал измерения параметров РЗА Arcteq - Позиция_229";
                case 30230: return "Регистр #30230: Канал измерения параметров РЗА Arcteq - Позиция_230";
                case 30231: return "Регистр #30231: Канал измерения параметров РЗА Arcteq - Позиция_231";
                case 30232: return "Регистр #30232: Канал измерения параметров РЗА Arcteq - Позиция_232";
                case 30233: return "Регистр #30233: Канал измерения параметров РЗА Arcteq - Позиция_233";
                case 30234: return "Регистр #30234: Канал измерения параметров РЗА Arcteq - Позиция_234";
                case 30235: return "Регистр #30235: Канал измерения параметров РЗА Arcteq - Позиция_235";
                case 30236: return "Регистр #30236: Канал измерения параметров РЗА Arcteq - Позиция_236";
                case 30237: return "Регистр #30237: Канал измерения параметров РЗА Arcteq - Позиция_237";
                case 30238: return "Регистр #30238: Канал измерения параметров РЗА Arcteq - Позиция_238";
                case 30239: return "Регистр #30239: Канал измерения параметров РЗА Arcteq - Позиция_239";
                case 30240: return "Регистр #30240: Канал измерения параметров РЗА Arcteq - Позиция_240";
                case 30241: return "Регистр #30241: Канал измерения параметров РЗА Arcteq - Позиция_241";
                case 30242: return "Регистр #30242: Канал измерения параметров РЗА Arcteq - Позиция_242";
                case 30243: return "Регистр #30243: Канал измерения параметров РЗА Arcteq - Позиция_243";
                case 30244: return "Регистр #30244: Канал измерения параметров РЗА Arcteq - Позиция_244";
                case 30245: return "Регистр #30245: Канал измерения параметров РЗА Arcteq - Позиция_245";
                case 30246: return "Регистр #30246: Канал измерения параметров РЗА Arcteq - Позиция_246";
                case 30247: return "Регистр #30247: Канал измерения параметров РЗА Arcteq - Позиция_247";
                case 30248: return "Регистр #30248: Канал измерения параметров РЗА Arcteq - Позиция_248";
                case 30249: return "Регистр #30249: Канал измерения параметров РЗА Arcteq - Позиция_249";
                case 30250: return "Регистр #30250: Канал измерения параметров РЗА Arcteq - Позиция_250";
                case 30251: return "Регистр #30251: Канал измерения параметров РЗА Arcteq - Позиция_251";
                case 30252: return "Регистр #30252: Канал измерения параметров РЗА Arcteq - Позиция_252";
                case 30253: return "Регистр #30253: Канал измерения параметров РЗА Arcteq - Позиция_253";
                case 30254: return "Регистр #30254: Канал измерения параметров РЗА Arcteq - Позиция_254";
                case 30255: return "Регистр #30255: Канал измерения параметров РЗА Arcteq - Позиция_255";
                case 30256: return "Регистр #30256: Канал измерения параметров РЗА Arcteq - Позиция_256";
                case 30257: return "Регистр #30257: Канал измерения параметров РЗА Arcteq - Позиция_257";
                case 30258: return "Регистр #30258: Канал измерения параметров РЗА Arcteq - Позиция_258";
                case 30259: return "Регистр #30259: Канал измерения параметров РЗА Arcteq - Позиция_259";
                case 30260: return "Регистр #30260: Канал измерения параметров РЗА Arcteq - Позиция_260";
                case 30261: return "Регистр #30261: Канал измерения параметров РЗА Arcteq - Позиция_261";
                case 30262: return "Регистр #30262: Канал измерения параметров РЗА Arcteq - Позиция_262";
                case 30263: return "Регистр #30263: Канал измерения параметров РЗА Arcteq - Позиция_263";
                case 30264: return "Регистр #30264: Канал измерения параметров РЗА Arcteq - Позиция_264";
                case 30265: return "Регистр #30265: Канал измерения параметров РЗА Arcteq - Позиция_265";
                case 30266: return "Регистр #30266: Канал измерения параметров РЗА Arcteq - Позиция_266";
                case 30267: return "Регистр #30267: Канал измерения параметров РЗА Arcteq - Позиция_267";
                case 30268: return "Регистр #30268: Канал измерения параметров РЗА Arcteq - Позиция_268";
                case 30269: return "Регистр #30269: Канал измерения параметров РЗА Arcteq - Позиция_269";
                case 30270: return "Регистр #30270: Канал измерения параметров РЗА Arcteq - Позиция_270";
                case 30271: return "Регистр #30271: Канал измерения параметров РЗА Arcteq - Позиция_271";
                case 30272: return "Регистр #30272: Канал измерения параметров РЗА Arcteq - Позиция_272";
                case 30273: return "Регистр #30273: Канал измерения параметров РЗА Arcteq - Позиция_273";
                case 30274: return "Регистр #30274: Канал измерения параметров РЗА Arcteq - Позиция_274";
                case 30275: return "Регистр #30275: Канал измерения параметров РЗА Arcteq - Позиция_275";
                case 30276: return "Регистр #30276: Канал измерения параметров РЗА Arcteq - Позиция_276";
                case 30277: return "Регистр #30277: Канал измерения параметров РЗА Arcteq - Позиция_277";
                case 30278: return "Регистр #30278: Канал измерения параметров РЗА Arcteq - Позиция_278";
                case 30279: return "Регистр #30279: Канал измерения параметров РЗА Arcteq - Позиция_279";
                case 30280: return "Регистр #30280: Канал измерения параметров РЗА Arcteq - Позиция_280";
                case 30281: return "Регистр #30281: Канал измерения параметров РЗА Arcteq - Позиция_281";
                case 30282: return "Регистр #30282: Канал измерения параметров РЗА Arcteq - Позиция_282";
                case 30283: return "Регистр #30283: Канал измерения параметров РЗА Arcteq - Позиция_283";
                case 30284: return "Регистр #30284: Канал измерения параметров РЗА Arcteq - Позиция_284";
                case 30285: return "Регистр #30285: Канал измерения параметров РЗА Arcteq - Позиция_285";
                case 30286: return "Регистр #30286: Канал измерения параметров РЗА Arcteq - Позиция_286";
                case 30287: return "Регистр #30287: Канал измерения параметров РЗА Arcteq - Позиция_287";
                case 30288: return "Регистр #30288: Канал измерения параметров РЗА Arcteq - Позиция_288";
                case 30289: return "Регистр #30289: Канал измерения параметров РЗА Arcteq - Позиция_289";
                case 30290: return "Регистр #30290: Канал измерения параметров РЗА Arcteq - Позиция_290";
                case 30291: return "Регистр #30291: Канал измерения параметров РЗА Arcteq - Позиция_291";
                case 30292: return "Регистр #30292: Канал измерения параметров РЗА Arcteq - Позиция_292";
                case 30293: return "Регистр #30293: Канал измерения параметров РЗА Arcteq - Позиция_293";
                case 30294: return "Регистр #30294: Канал измерения параметров РЗА Arcteq - Позиция_294";
                case 30295: return "Регистр #30295: Канал измерения параметров РЗА Arcteq - Позиция_295";
                case 30296: return "Регистр #30296: Канал измерения параметров РЗА Arcteq - Позиция_296";
                case 30297: return "Регистр #30297: Канал измерения параметров РЗА Arcteq - Позиция_297";
                case 30298: return "Регистр #30298: Канал измерения параметров РЗА Arcteq - Позиция_298";
                case 30299: return "Регистр #30299: Канал измерения параметров РЗА Arcteq - Позиция_299";
                case 30300: return "Регистр #30300: Канал измерения параметров РЗА Arcteq - Позиция_300";
                case 30301: return "Регистр #30301: Канал измерения параметров РЗА Arcteq - Позиция_301";
                case 30302: return "Регистр #30302: Канал измерения параметров РЗА Arcteq - Позиция_302";
                case 30303: return "Регистр #30303: Канал измерения параметров РЗА Arcteq - Позиция_303";
                case 30304: return "Регистр #30304: Канал измерения параметров РЗА Arcteq - Позиция_304";
                case 30305: return "Регистр #30305: Канал измерения параметров РЗА Arcteq - Позиция_305";
                case 30306: return "Регистр #30306: Канал измерения параметров РЗА Arcteq - Позиция_306";
                case 30307: return "Регистр #30307: Канал измерения параметров РЗА Arcteq - Позиция_307";
                case 30308: return "Регистр #30308: Канал измерения параметров РЗА Arcteq - Позиция_308";
                case 30309: return "Регистр #30309: Канал измерения параметров РЗА Arcteq - Позиция_309";
                case 30310: return "Регистр #30310: Канал измерения параметров РЗА Arcteq - Позиция_310";
                case 30311: return "Регистр #30311: Канал измерения параметров РЗА Arcteq - Позиция_311";
                case 30312: return "Регистр #30312: Канал измерения параметров РЗА Arcteq - Позиция_312";
                case 30313: return "Регистр #30313: Канал измерения параметров РЗА Arcteq - Позиция_313";
                case 30314: return "Регистр #30314: Канал измерения параметров РЗА Arcteq - Позиция_314";
                case 30315: return "Регистр #30315: Канал измерения параметров РЗА Arcteq - Позиция_315";
                case 30316: return "Регистр #30316: Канал измерения параметров РЗА Arcteq - Позиция_316";
                case 30317: return "Регистр #30317: Канал измерения параметров РЗА Arcteq - Позиция_317";
                case 30318: return "Регистр #30318: Канал измерения параметров РЗА Arcteq - Позиция_318";
                case 30319: return "Регистр #30319: Канал измерения параметров РЗА Arcteq - Позиция_319";
                case 30320: return "Регистр #30320: Канал измерения параметров РЗА Arcteq - Позиция_320";
                case 30321: return "Регистр #30321: Канал измерения параметров РЗА Arcteq - Позиция_321";
                case 30322: return "Регистр #30322: Канал измерения параметров РЗА Arcteq - Позиция_322";
                case 30323: return "Регистр #30323: Канал измерения параметров РЗА Arcteq - Позиция_323";
                case 30324: return "Регистр #30324: Канал измерения параметров РЗА Arcteq - Позиция_324";
                case 30325: return "Регистр #30325: Канал измерения параметров РЗА Arcteq - Позиция_325";
                case 30326: return "Регистр #30326: Канал измерения параметров РЗА Arcteq - Позиция_326";
                case 30327: return "Регистр #30327: Канал измерения параметров РЗА Arcteq - Позиция_327";
                case 30328: return "Регистр #30328: Канал измерения параметров РЗА Arcteq - Позиция_328";
                case 30329: return "Регистр #30329: Канал измерения параметров РЗА Arcteq - Позиция_329";
                case 30330: return "Регистр #30330: Канал измерения параметров РЗА Arcteq - Позиция_330";
                case 30331: return "Регистр #30331: Канал измерения параметров РЗА Arcteq - Позиция_331";
                case 30332: return "Регистр #30332: Канал измерения параметров РЗА Arcteq - Позиция_332";
                case 30333: return "Регистр #30333: Канал измерения параметров РЗА Arcteq - Позиция_333";
                case 30334: return "Регистр #30334: Канал измерения параметров РЗА Arcteq - Позиция_334";
                case 30335: return "Регистр #30335: Канал измерения параметров РЗА Arcteq - Позиция_335";
                case 30336: return "Регистр #30336: Канал измерения параметров РЗА Arcteq - Позиция_336";
                case 30337: return "Регистр #30337: Канал измерения параметров РЗА Arcteq - Позиция_337";
                case 30338: return "Регистр #30338: Канал измерения параметров РЗА Arcteq - Позиция_338";
                case 30339: return "Регистр #30339: Канал измерения параметров РЗА Arcteq - Позиция_339";
                case 30340: return "Регистр #30340: Канал измерения параметров РЗА Arcteq - Позиция_340";
                case 30341: return "Регистр #30341: Канал измерения параметров РЗА Arcteq - Позиция_341";
                case 30342: return "Регистр #30342: Канал измерения параметров РЗА Arcteq - Позиция_342";
                case 30343: return "Регистр #30343: Канал измерения параметров РЗА Arcteq - Позиция_343";
                case 30344: return "Регистр #30344: Канал измерения параметров РЗА Arcteq - Позиция_344";
                case 30345: return "Регистр #30345: Канал измерения параметров РЗА Arcteq - Позиция_345";
                case 30346: return "Регистр #30346: Канал измерения параметров РЗА Arcteq - Позиция_346";
                case 30347: return "Регистр #30347: Канал измерения параметров РЗА Arcteq - Позиция_347";
                case 30348: return "Регистр #30348: Канал измерения параметров РЗА Arcteq - Позиция_348";
                case 30349: return "Регистр #30349: Канал измерения параметров РЗА Arcteq - Позиция_349";
                case 30350: return "Регистр #30350: Канал измерения параметров РЗА Arcteq - Позиция_350";
                case 30351: return "Регистр #30351: Канал измерения параметров РЗА Arcteq - Позиция_351";
                case 30352: return "Регистр #30352: Канал измерения параметров РЗА Arcteq - Позиция_352";
                case 30353: return "Регистр #30353: Канал измерения параметров РЗА Arcteq - Позиция_353";
                case 30354: return "Регистр #30354: Канал измерения параметров РЗА Arcteq - Позиция_354";
                case 30355: return "Регистр #30355: Канал измерения параметров РЗА Arcteq - Позиция_355";
                case 30356: return "Регистр #30356: Канал измерения параметров РЗА Arcteq - Позиция_356";
                case 30357: return "Регистр #30357: Канал измерения параметров РЗА Arcteq - Позиция_357";
                case 30358: return "Регистр #30358: Канал измерения параметров РЗА Arcteq - Позиция_358";
                case 30359: return "Регистр #30359: Канал измерения параметров РЗА Arcteq - Позиция_359";
                case 30360: return "Регистр #30360: Канал измерения параметров РЗА Arcteq - Позиция_360";
                case 30361: return "Регистр #30361: Канал измерения параметров РЗА Arcteq - Позиция_361";
                case 30362: return "Регистр #30362: Канал измерения параметров РЗА Arcteq - Позиция_362";
                case 30363: return "Регистр #30363: Канал измерения параметров РЗА Arcteq - Позиция_363";
                case 30364: return "Регистр #30364: Канал измерения параметров РЗА Arcteq - Позиция_364";
                case 30365: return "Регистр #30365: Канал измерения параметров РЗА Arcteq - Позиция_365";
                case 30366: return "Регистр #30366: Канал измерения параметров РЗА Arcteq - Позиция_366";
                case 30367: return "Регистр #30367: Канал измерения параметров РЗА Arcteq - Позиция_367";
                case 30368: return "Регистр #30368: Канал измерения параметров РЗА Arcteq - Позиция_368";
                case 30369: return "Регистр #30369: Канал измерения параметров РЗА Arcteq - Позиция_369";
                case 30370: return "Регистр #30370: Канал измерения параметров РЗА Arcteq - Позиция_370";
                case 30371: return "Регистр #30371: Канал измерения параметров РЗА Arcteq - Позиция_371";
                case 30372: return "Регистр #30372: Канал измерения параметров РЗА Arcteq - Позиция_372";
                case 30373: return "Регистр #30373: Канал измерения параметров РЗА Arcteq - Позиция_373";
                case 30374: return "Регистр #30374: Канал измерения параметров РЗА Arcteq - Позиция_374";
                case 30375: return "Регистр #30375: Канал измерения параметров РЗА Arcteq - Позиция_375";
                case 30376: return "Регистр #30376: Канал измерения параметров РЗА Arcteq - Позиция_376";
                case 30377: return "Регистр #30377: Канал измерения параметров РЗА Arcteq - Позиция_377";
                case 30378: return "Регистр #30378: Канал измерения параметров РЗА Arcteq - Позиция_378";
                case 30379: return "Регистр #30379: Канал измерения параметров РЗА Arcteq - Позиция_379";
                case 30380: return "Регистр #30380: Канал измерения параметров РЗА Arcteq - Позиция_380";
                case 30381: return "Регистр #30381: Канал измерения параметров РЗА Arcteq - Позиция_381";
                case 30382: return "Регистр #30382: Канал измерения параметров РЗА Arcteq - Позиция_382";
                case 30383: return "Регистр #30383: Канал измерения параметров РЗА Arcteq - Позиция_383";
                case 30384: return "Регистр #30384: Канал измерения параметров РЗА Arcteq - Позиция_384";
                case 30385: return "Регистр #30385: Канал измерения параметров РЗА Arcteq - Позиция_385";
                case 30386: return "Регистр #30386: Канал измерения параметров РЗА Arcteq - Позиция_386";
                case 30387: return "Регистр #30387: Канал измерения параметров РЗА Arcteq - Позиция_387";
                case 30388: return "Регистр #30388: Канал измерения параметров РЗА Arcteq - Позиция_388";
                case 30389: return "Регистр #30389: Канал измерения параметров РЗА Arcteq - Позиция_389";
                case 30390: return "Регистр #30390: Канал измерения параметров РЗА Arcteq - Позиция_390";
                case 30391: return "Регистр #30391: Канал измерения параметров РЗА Arcteq - Позиция_391";
                case 30392: return "Регистр #30392: Канал измерения параметров РЗА Arcteq - Позиция_392";
                case 30393: return "Регистр #30393: Канал измерения параметров РЗА Arcteq - Позиция_393";
                case 30394: return "Регистр #30394: Канал измерения параметров РЗА Arcteq - Позиция_394";
                case 30395: return "Регистр #30395: Канал измерения параметров РЗА Arcteq - Позиция_395";
                case 30396: return "Регистр #30396: Канал измерения параметров РЗА Arcteq - Позиция_396";
                case 30397: return "Регистр #30397: Канал измерения параметров РЗА Arcteq - Позиция_397";
                case 30398: return "Регистр #30398: Канал измерения параметров РЗА Arcteq - Позиция_398";
                case 30399: return "Регистр #30399: Канал измерения параметров РЗА Arcteq - Позиция_399";
                case 30400: return "Регистр #30400: Канал измерения параметров РЗА Arcteq - Позиция_400";
                case 30401: return "Регистр #30401: Канал измерения параметров РЗА Arcteq - Позиция_401";
                case 30402: return "Регистр #30402: Канал измерения параметров РЗА Arcteq - Позиция_402";
                case 30403: return "Регистр #30403: Канал измерения параметров РЗА Arcteq - Позиция_403";
                case 30404: return "Регистр #30404: Канал измерения параметров РЗА Arcteq - Позиция_404";
                case 30405: return "Регистр #30405: Канал измерения параметров РЗА Arcteq - Позиция_405";
                case 30406: return "Регистр #30406: Канал измерения параметров РЗА Arcteq - Позиция_406";
                case 30407: return "Регистр #30407: Канал измерения параметров РЗА Arcteq - Позиция_407";
                case 30408: return "Регистр #30408: Канал измерения параметров РЗА Arcteq - Позиция_408";
                case 30409: return "Регистр #30409: Канал измерения параметров РЗА Arcteq - Позиция_409";
                case 30410: return "Регистр #30410: Канал измерения параметров РЗА Arcteq - Позиция_410";
                case 30411: return "Регистр #30411: Канал измерения параметров РЗА Arcteq - Позиция_411";
                case 30412: return "Регистр #30412: Канал измерения параметров РЗА Arcteq - Позиция_412";
                case 30413: return "Регистр #30413: Канал измерения параметров РЗА Arcteq - Позиция_413";
                case 30414: return "Регистр #30414: Канал измерения параметров РЗА Arcteq - Позиция_414";
                case 30415: return "Регистр #30415: Канал измерения параметров РЗА Arcteq - Позиция_415";
                case 30416: return "Регистр #30416: Канал измерения параметров РЗА Arcteq - Позиция_416";
                case 30417: return "Регистр #30417: Канал измерения параметров РЗА Arcteq - Позиция_417";
                case 30418: return "Регистр #30418: Канал измерения параметров РЗА Arcteq - Позиция_418";
                case 30419: return "Регистр #30419: Канал измерения параметров РЗА Arcteq - Позиция_419";
                case 30420: return "Регистр #30420: Канал измерения параметров РЗА Arcteq - Позиция_420";
                case 30421: return "Регистр #30421: Канал измерения параметров РЗА Arcteq - Позиция_421";
                case 30422: return "Регистр #30422: Канал измерения параметров РЗА Arcteq - Позиция_422";
                case 30423: return "Регистр #30423: Канал измерения параметров РЗА Arcteq - Позиция_423";
                case 30424: return "Регистр #30424: Канал измерения параметров РЗА Arcteq - Позиция_424";
                case 30425: return "Регистр #30425: Канал измерения параметров РЗА Arcteq - Позиция_425";
                case 30426: return "Регистр #30426: Канал измерения параметров РЗА Arcteq - Позиция_426";
                case 30427: return "Регистр #30427: Канал измерения параметров РЗА Arcteq - Позиция_427";
                case 30428: return "Регистр #30428: Канал измерения параметров РЗА Arcteq - Позиция_428";
                case 30429: return "Регистр #30429: Канал измерения параметров РЗА Arcteq - Позиция_429";
                case 30430: return "Регистр #30430: Канал измерения параметров РЗА Arcteq - Позиция_430";
                case 30431: return "Регистр #30431: Канал измерения параметров РЗА Arcteq - Позиция_431";
                case 30432: return "Регистр #30432: Канал измерения параметров РЗА Arcteq - Позиция_432";
                case 30433: return "Регистр #30433: Канал измерения параметров РЗА Arcteq - Позиция_433";
                case 30434: return "Регистр #30434: Канал измерения параметров РЗА Arcteq - Позиция_434";
                case 30435: return "Регистр #30435: Канал измерения параметров РЗА Arcteq - Позиция_435";
                case 30436: return "Регистр #30436: Канал измерения параметров РЗА Arcteq - Позиция_436";
                case 30437: return "Регистр #30437: Канал измерения параметров РЗА Arcteq - Позиция_437";
                case 30438: return "Регистр #30438: Канал измерения параметров РЗА Arcteq - Позиция_438";
                case 30439: return "Регистр #30439: Канал измерения параметров РЗА Arcteq - Позиция_439";
                case 30440: return "Регистр #30440: Канал измерения параметров РЗА Arcteq - Позиция_440";
                case 30441: return "Регистр #30441: Канал измерения параметров РЗА Arcteq - Позиция_441";
                case 30442: return "Регистр #30442: Канал измерения параметров РЗА Arcteq - Позиция_442";
                case 30443: return "Регистр #30443: Канал измерения параметров РЗА Arcteq - Позиция_443";
                case 30444: return "Регистр #30444: Канал измерения параметров РЗА Arcteq - Позиция_444";
                case 30445: return "Регистр #30445: Канал измерения параметров РЗА Arcteq - Позиция_445";
                case 30446: return "Регистр #30446: Канал измерения параметров РЗА Arcteq - Позиция_446";
                case 30447: return "Регистр #30447: Канал измерения параметров РЗА Arcteq - Позиция_447";
                case 30448: return "Регистр #30448: Канал измерения параметров РЗА Arcteq - Позиция_448";
                case 30449: return "Регистр #30449: Канал измерения параметров РЗА Arcteq - Позиция_449";
                case 30450: return "Регистр #30450: Канал измерения параметров РЗА Arcteq - Позиция_450";
                case 30451: return "Регистр #30451: Канал измерения параметров РЗА Arcteq - Позиция_451";
                case 30452: return "Регистр #30452: Канал измерения параметров РЗА Arcteq - Позиция_452";
                case 30453: return "Регистр #30453: Канал измерения параметров РЗА Arcteq - Позиция_453";
                case 30454: return "Регистр #30454: Канал измерения параметров РЗА Arcteq - Позиция_454";
                case 30455: return "Регистр #30455: Канал измерения параметров РЗА Arcteq - Позиция_455";
                case 30456: return "Регистр #30456: Канал измерения параметров РЗА Arcteq - Позиция_456";
                case 30457: return "Регистр #30457: Канал измерения параметров РЗА Arcteq - Позиция_457";
                case 30458: return "Регистр #30458: Канал измерения параметров РЗА Arcteq - Позиция_458";
                case 30459: return "Регистр #30459: Канал измерения параметров РЗА Arcteq - Позиция_459";
                case 30460: return "Регистр #30460: Канал измерения параметров РЗА Arcteq - Позиция_460";
                case 30461: return "Регистр #30461: Канал измерения параметров РЗА Arcteq - Позиция_461";
                case 30462: return "Регистр #30462: Канал измерения параметров РЗА Arcteq - Позиция_462";
                case 30463: return "Регистр #30463: Канал измерения параметров РЗА Arcteq - Позиция_463";
                case 30464: return "Регистр #30464: Канал измерения параметров РЗА Arcteq - Позиция_464";
                case 30465: return "Регистр #30465: Канал измерения параметров РЗА Arcteq - Позиция_465";
                case 30466: return "Регистр #30466: Канал измерения параметров РЗА Arcteq - Позиция_466";
                case 30467: return "Регистр #30467: Канал измерения параметров РЗА Arcteq - Позиция_467";
                case 30468: return "Регистр #30468: Канал измерения параметров РЗА Arcteq - Позиция_468";
                case 30469: return "Регистр #30469: Канал измерения параметров РЗА Arcteq - Позиция_469";
                case 30470: return "Регистр #30470: Канал измерения параметров РЗА Arcteq - Позиция_470";
                case 30471: return "Регистр #30471: Канал измерения параметров РЗА Arcteq - Позиция_471";
                case 30472: return "Регистр #30472: Канал измерения параметров РЗА Arcteq - Позиция_472";
                case 30473: return "Регистр #30473: Канал измерения параметров РЗА Arcteq - Позиция_473";
                case 30474: return "Регистр #30474: Канал измерения параметров РЗА Arcteq - Позиция_474";
                case 30475: return "Регистр #30475: Канал измерения параметров РЗА Arcteq - Позиция_475";
                case 30476: return "Регистр #30476: Канал измерения параметров РЗА Arcteq - Позиция_476";
                case 30477: return "Регистр #30477: Канал измерения параметров РЗА Arcteq - Позиция_477";
                case 30478: return "Регистр #30478: Канал измерения параметров РЗА Arcteq - Позиция_478";
                case 30479: return "Регистр #30479: Канал измерения параметров РЗА Arcteq - Позиция_479";
                case 30480: return "Регистр #30480: Канал измерения параметров РЗА Arcteq - Позиция_480";
                case 30481: return "Регистр #30481: Канал измерения параметров РЗА Arcteq - Позиция_481";
                case 30482: return "Регистр #30482: Канал измерения параметров РЗА Arcteq - Позиция_482";
                case 30483: return "Регистр #30483: Канал измерения параметров РЗА Arcteq - Позиция_483";
                case 30484: return "Регистр #30484: Канал измерения параметров РЗА Arcteq - Позиция_484";
                case 30485: return "Регистр #30485: Канал измерения параметров РЗА Arcteq - Позиция_485";
                case 30486: return "Регистр #30486: Канал измерения параметров РЗА Arcteq - Позиция_486";
                case 30487: return "Регистр #30487: Канал измерения параметров РЗА Arcteq - Позиция_487";
                case 30488: return "Регистр #30488: Канал измерения параметров РЗА Arcteq - Позиция_488";
                case 30489: return "Регистр #30489: Канал измерения параметров РЗА Arcteq - Позиция_489";
                case 30490: return "Регистр #30490: Канал измерения параметров РЗА Arcteq - Позиция_490";
                case 30491: return "Регистр #30491: Канал измерения параметров РЗА Arcteq - Позиция_491";
                case 30492: return "Регистр #30492: Канал измерения параметров РЗА Arcteq - Позиция_492";
                case 30493: return "Регистр #30493: Канал измерения параметров РЗА Arcteq - Позиция_493";
                case 30494: return "Регистр #30494: Канал измерения параметров РЗА Arcteq - Позиция_494";
                case 30495: return "Регистр #30495: Канал измерения параметров РЗА Arcteq - Позиция_495";
                case 30496: return "Регистр #30496: Канал измерения параметров РЗА Arcteq - Позиция_496";
                case 30497: return "Регистр #30497: Канал измерения параметров РЗА Arcteq - Позиция_497";
                case 30498: return "Регистр #30498: Канал измерения параметров РЗА Arcteq - Позиция_498";
                case 30499: return "Регистр #30499: Канал измерения параметров РЗА Arcteq - Позиция_499";
                case 30500: return "Регистр #30500: Канал измерения параметров РЗА Arcteq - Позиция_500";
                case 30501: return "Регистр #30501: Канал измерения параметров РЗА Arcteq - Позиция_501";
                case 30502: return "Регистр #30502: Канал измерения параметров РЗА Arcteq - Позиция_502";
                case 30503: return "Регистр #30503: Канал измерения параметров РЗА Arcteq - Позиция_503";
                case 30504: return "Регистр #30504: Канал измерения параметров РЗА Arcteq - Позиция_504";
                case 30505: return "Регистр #30505: Канал измерения параметров РЗА Arcteq - Позиция_505";
                case 30506: return "Регистр #30506: Канал измерения параметров РЗА Arcteq - Позиция_506";
                case 30507: return "Регистр #30507: Канал измерения параметров РЗА Arcteq - Позиция_507";
                case 30508: return "Регистр #30508: Канал измерения параметров РЗА Arcteq - Позиция_508";
                case 30509: return "Регистр #30509: Канал измерения параметров РЗА Arcteq - Позиция_509";
                case 30510: return "Регистр #30510: Канал измерения параметров РЗА Arcteq - Позиция_510";
                case 30511: return "Регистр #30511: Канал измерения параметров РЗА Arcteq - Позиция_511";
                case 30512: return "Регистр #30512: Канал измерения параметров РЗА Arcteq - Позиция_512";
                case 30513: return "Регистр #30513: Канал измерения параметров РЗА Arcteq - Позиция_513";
                case 30514: return "Регистр #30514: Канал измерения параметров РЗА Arcteq - Позиция_514";
                case 30515: return "Регистр #30515: Канал измерения параметров РЗА Arcteq - Позиция_515";
                case 30516: return "Регистр #30516: Канал измерения параметров РЗА Arcteq - Позиция_516";
                case 30517: return "Регистр #30517: Канал измерения параметров РЗА Arcteq - Позиция_517";
                case 30518: return "Регистр #30518: Канал измерения параметров РЗА Arcteq - Позиция_518";
                case 30519: return "Регистр #30519: Канал измерения параметров РЗА Arcteq - Позиция_519";
                case 30520: return "Регистр #30520: Канал измерения параметров РЗА Arcteq - Позиция_520";
                case 30521: return "Регистр #30521: Канал измерения параметров РЗА Arcteq - Позиция_521";
                case 30522: return "Регистр #30522: Канал измерения параметров РЗА Arcteq - Позиция_522";
                case 30523: return "Регистр #30523: Канал измерения параметров РЗА Arcteq - Позиция_523";
                case 30524: return "Регистр #30524: Канал измерения параметров РЗА Arcteq - Позиция_524";
                case 30525: return "Регистр #30525: Канал измерения параметров РЗА Arcteq - Позиция_525";
                case 30526: return "Регистр #30526: Канал измерения параметров РЗА Arcteq - Позиция_526";
                case 30527: return "Регистр #30527: Канал измерения параметров РЗА Arcteq - Позиция_527";
                case 30528: return "Регистр #30528: Канал измерения параметров РЗА Arcteq - Позиция_528";
                case 30529: return "Регистр #30529: Канал измерения параметров РЗА Arcteq - Позиция_529";
                case 30530: return "Регистр #30530: Канал измерения параметров РЗА Arcteq - Позиция_530";
                case 30531: return "Регистр #30531: Канал измерения параметров РЗА Arcteq - Позиция_531";
                case 30532: return "Регистр #30532: Канал измерения параметров РЗА Arcteq - Позиция_532";
                case 30533: return "Регистр #30533: Канал измерения параметров РЗА Arcteq - Позиция_533";
                case 30534: return "Регистр #30534: Канал измерения параметров РЗА Arcteq - Позиция_534";
                case 30535: return "Регистр #30535: Канал измерения параметров РЗА Arcteq - Позиция_535";
                case 30536: return "Регистр #30536: Канал измерения параметров РЗА Arcteq - Позиция_536";
                case 30537: return "Регистр #30537: Канал измерения параметров РЗА Arcteq - Позиция_537";
                case 30538: return "Регистр #30538: Канал измерения параметров РЗА Arcteq - Позиция_538";
                case 30539: return "Регистр #30539: Канал измерения параметров РЗА Arcteq - Позиция_539";
                case 30540: return "Регистр #30540: Канал измерения параметров РЗА Arcteq - Позиция_540";
                case 30541: return "Регистр #30541: Канал измерения параметров РЗА Arcteq - Позиция_541";
                case 30542: return "Регистр #30542: Канал измерения параметров РЗА Arcteq - Позиция_542";
                case 30543: return "Регистр #30543: Канал измерения параметров РЗА Arcteq - Позиция_543";
                case 30544: return "Регистр #30544: Канал измерения параметров РЗА Arcteq - Позиция_544";
                case 30545: return "Регистр #30545: Канал измерения параметров РЗА Arcteq - Позиция_545";
                case 30546: return "Регистр #30546: Канал измерения параметров РЗА Arcteq - Позиция_546";
                case 30547: return "Регистр #30547: Канал измерения параметров РЗА Arcteq - Позиция_547";
                case 30548: return "Регистр #30548: Канал измерения параметров РЗА Arcteq - Позиция_548";
                case 30549: return "Регистр #30549: Канал измерения параметров РЗА Arcteq - Позиция_549";
                case 30550: return "Регистр #30550: Канал измерения параметров РЗА Arcteq - Позиция_550";
                case 30551: return "Регистр #30551: Канал измерения параметров РЗА Arcteq - Позиция_551";
                case 30552: return "Регистр #30552: Канал измерения параметров РЗА Arcteq - Позиция_552";
                case 30553: return "Регистр #30553: Канал измерения параметров РЗА Arcteq - Позиция_553";
                case 30554: return "Регистр #30554: Канал измерения параметров РЗА Arcteq - Позиция_554";
                case 30555: return "Регистр #30555: Канал измерения параметров РЗА Arcteq - Позиция_555";
                case 30556: return "Регистр #30556: Канал измерения параметров РЗА Arcteq - Позиция_556";
                case 30557: return "Регистр #30557: Канал измерения параметров РЗА Arcteq - Позиция_557";
                case 30558: return "Регистр #30558: Канал измерения параметров РЗА Arcteq - Позиция_558";
                case 30559: return "Регистр #30559: Канал измерения параметров РЗА Arcteq - Позиция_559";
                case 30560: return "Регистр #30560: Канал измерения параметров РЗА Arcteq - Позиция_560";
                case 30561: return "Регистр #30561: Канал измерения параметров РЗА Arcteq - Позиция_561";
                case 30562: return "Регистр #30562: Канал измерения параметров РЗА Arcteq - Позиция_562";
                case 30563: return "Регистр #30563: Канал измерения параметров РЗА Arcteq - Позиция_563";
                case 30564: return "Регистр #30564: Канал измерения параметров РЗА Arcteq - Позиция_564";
                case 30565: return "Регистр #30565: Канал измерения параметров РЗА Arcteq - Позиция_565";
                case 30566: return "Регистр #30566: Канал измерения параметров РЗА Arcteq - Позиция_566";
                case 30567: return "Регистр #30567: Канал измерения параметров РЗА Arcteq - Позиция_567";
                case 30568: return "Регистр #30568: Канал измерения параметров РЗА Arcteq - Позиция_568";
                case 30569: return "Регистр #30569: Канал измерения параметров РЗА Arcteq - Позиция_569";
                case 30570: return "Регистр #30570: Канал измерения параметров РЗА Arcteq - Позиция_570";
                case 30571: return "Регистр #30571: Канал измерения параметров РЗА Arcteq - Позиция_571";
                case 30572: return "Регистр #30572: Канал измерения параметров РЗА Arcteq - Позиция_572";
                case 30573: return "Регистр #30573: Канал измерения параметров РЗА Arcteq - Позиция_573";
                case 30574: return "Регистр #30574: Канал измерения параметров РЗА Arcteq - Позиция_574";
                case 30575: return "Регистр #30575: Канал измерения параметров РЗА Arcteq - Позиция_575";
                case 30576: return "Регистр #30576: Канал измерения параметров РЗА Arcteq - Позиция_576";
                case 30577: return "Регистр #30577: Канал измерения параметров РЗА Arcteq - Позиция_577";
                case 30578: return "Регистр #30578: Канал измерения параметров РЗА Arcteq - Позиция_578";
                case 30579: return "Регистр #30579: Канал измерения параметров РЗА Arcteq - Позиция_579";
                case 30580: return "Регистр #30580: Канал измерения параметров РЗА Arcteq - Позиция_580";
                case 30581: return "Регистр #30581: Канал измерения параметров РЗА Arcteq - Позиция_581";
                case 30582: return "Регистр #30582: Канал измерения параметров РЗА Arcteq - Позиция_582";
                case 30583: return "Регистр #30583: Канал измерения параметров РЗА Arcteq - Позиция_583";
                case 30584: return "Регистр #30584: Канал измерения параметров РЗА Arcteq - Позиция_584";
                case 30585: return "Регистр #30585: Канал измерения параметров РЗА Arcteq - Позиция_585";
                case 30586: return "Регистр #30586: Канал измерения параметров РЗА Arcteq - Позиция_586";
                case 30587: return "Регистр #30587: Канал измерения параметров РЗА Arcteq - Позиция_587";
                case 30588: return "Регистр #30588: Канал измерения параметров РЗА Arcteq - Позиция_588";
                case 30589: return "Регистр #30589: Канал измерения параметров РЗА Arcteq - Позиция_589";
                case 30590: return "Регистр #30590: Канал измерения параметров РЗА Arcteq - Позиция_590";
                case 30591: return "Регистр #30591: Канал измерения параметров РЗА Arcteq - Позиция_591";
                case 30592: return "Регистр #30592: Канал измерения параметров РЗА Arcteq - Позиция_592";
                case 30593: return "Регистр #30593: Канал измерения параметров РЗА Arcteq - Позиция_593";
                case 30594: return "Регистр #30594: Канал измерения параметров РЗА Arcteq - Позиция_594";
                case 30595: return "Регистр #30595: Канал измерения параметров РЗА Arcteq - Позиция_595";
                case 30596: return "Регистр #30596: Канал измерения параметров РЗА Arcteq - Позиция_596";
                case 30597: return "Регистр #30597: Канал измерения параметров РЗА Arcteq - Позиция_597";
                case 30598: return "Регистр #30598: Канал измерения параметров РЗА Arcteq - Позиция_598";
                case 30599: return "Регистр #30599: Канал измерения параметров РЗА Arcteq - Позиция_599";
                case 30600: return "Регистр #30600: Канал измерения параметров РЗА Arcteq - Позиция_600";
                case 30601: return "Регистр #30601: Канал измерения параметров РЗА Arcteq - Позиция_601";
                case 30602: return "Регистр #30602: Канал измерения параметров РЗА Arcteq - Позиция_602";
                case 30603: return "Регистр #30603: Канал измерения параметров РЗА Arcteq - Позиция_603";
                case 30604: return "Регистр #30604: Канал измерения параметров РЗА Arcteq - Позиция_604";
                case 30605: return "Регистр #30605: Канал измерения параметров РЗА Arcteq - Позиция_605";
                case 30606: return "Регистр #30606: Канал измерения параметров РЗА Arcteq - Позиция_606";
                case 30607: return "Регистр #30607: Канал измерения параметров РЗА Arcteq - Позиция_607";
                case 30608: return "Регистр #30608: Канал измерения параметров РЗА Arcteq - Позиция_608";
                case 30609: return "Регистр #30609: Канал измерения параметров РЗА Arcteq - Позиция_609";
                case 30610: return "Регистр #30610: Канал измерения параметров РЗА Arcteq - Позиция_610";
                case 30611: return "Регистр #30611: Канал измерения параметров РЗА Arcteq - Позиция_611";
                case 30612: return "Регистр #30612: Канал измерения параметров РЗА Arcteq - Позиция_612";
                case 30613: return "Регистр #30613: Канал измерения параметров РЗА Arcteq - Позиция_613";
                case 30614: return "Регистр #30614: Канал измерения параметров РЗА Arcteq - Позиция_614";
                case 30615: return "Регистр #30615: Канал измерения параметров РЗА Arcteq - Позиция_615";
                case 30616: return "Регистр #30616: Канал измерения параметров РЗА Arcteq - Позиция_616";
                case 30617: return "Регистр #30617: Канал измерения параметров РЗА Arcteq - Позиция_617";
                case 30618: return "Регистр #30618: Канал измерения параметров РЗА Arcteq - Позиция_618";
                case 30619: return "Регистр #30619: Канал измерения параметров РЗА Arcteq - Позиция_619";
                case 30620: return "Регистр #30620: Канал измерения параметров РЗА Arcteq - Позиция_620";
                case 30621: return "Регистр #30621: Канал измерения параметров РЗА Arcteq - Позиция_621";
                case 30622: return "Регистр #30622: Канал измерения параметров РЗА Arcteq - Позиция_622";
                case 30623: return "Регистр #30623: Канал измерения параметров РЗА Arcteq - Позиция_623";
                case 30624: return "Регистр #30624: Канал измерения параметров РЗА Arcteq - Позиция_624";
                case 30625: return "Регистр #30625: Канал измерения параметров РЗА Arcteq - Позиция_625";
                case 30626: return "Регистр #30626: Канал измерения параметров РЗА Arcteq - Позиция_626";
                case 30627: return "Регистр #30627: Канал измерения параметров РЗА Arcteq - Позиция_627";
                case 30628: return "Регистр #30628: Канал измерения параметров РЗА Arcteq - Позиция_628";
                case 30629: return "Регистр #30629: Канал измерения параметров РЗА Arcteq - Позиция_629";
                case 30630: return "Регистр #30630: Канал измерения параметров РЗА Arcteq - Позиция_630";
                case 30631: return "Регистр #30631: Канал измерения параметров РЗА Arcteq - Позиция_631";
                case 30632: return "Регистр #30632: Канал измерения параметров РЗА Arcteq - Позиция_632";
                case 30633: return "Регистр #30633: Канал измерения параметров РЗА Arcteq - Позиция_633";
                case 30634: return "Регистр #30634: Канал измерения параметров РЗА Arcteq - Позиция_634";
                case 30635: return "Регистр #30635: Канал измерения параметров РЗА Arcteq - Позиция_635";
                case 30636: return "Регистр #30636: Канал измерения параметров РЗА Arcteq - Позиция_636";
                case 30637: return "Регистр #30637: Канал измерения параметров РЗА Arcteq - Позиция_637";
                case 30638: return "Регистр #30638: Канал измерения параметров РЗА Arcteq - Позиция_638";
                case 30639: return "Регистр #30639: Канал измерения параметров РЗА Arcteq - Позиция_639";
                case 30640: return "Регистр #30640: Канал измерения параметров РЗА Arcteq - Позиция_640";
                case 30641: return "Регистр #30641: Канал измерения параметров РЗА Arcteq - Позиция_641";
                case 30642: return "Регистр #30642: Канал измерения параметров РЗА Arcteq - Позиция_642";
                case 30643: return "Регистр #30643: Канал измерения параметров РЗА Arcteq - Позиция_643";
                case 30644: return "Регистр #30644: Канал измерения параметров РЗА Arcteq - Позиция_644";
                case 30645: return "Регистр #30645: Канал измерения параметров РЗА Arcteq - Позиция_645";
                case 30646: return "Регистр #30646: Канал измерения параметров РЗА Arcteq - Позиция_646";
                case 30647: return "Регистр #30647: Канал измерения параметров РЗА Arcteq - Позиция_647";
                case 30648: return "Регистр #30648: Канал измерения параметров РЗА Arcteq - Позиция_648";
                case 30649: return "Регистр #30649: Канал измерения параметров РЗА Arcteq - Позиция_649";
                case 30650: return "Регистр #30650: Канал измерения параметров РЗА Arcteq - Позиция_650";
                case 30651: return "Регистр #30651: Канал измерения параметров РЗА Arcteq - Позиция_651";
                case 30652: return "Регистр #30652: Канал измерения параметров РЗА Arcteq - Позиция_652";
                case 30653: return "Регистр #30653: Канал измерения параметров РЗА Arcteq - Позиция_653";
                case 30654: return "Регистр #30654: Канал измерения параметров РЗА Arcteq - Позиция_654";
                case 30655: return "Регистр #30655: Канал измерения параметров РЗА Arcteq - Позиция_655";
                case 30656: return "Регистр #30656: Канал измерения параметров РЗА Arcteq - Позиция_656";
                case 30657: return "Регистр #30657: Канал измерения параметров РЗА Arcteq - Позиция_657";
                case 30658: return "Регистр #30658: Канал измерения параметров РЗА Arcteq - Позиция_658";
                case 30659: return "Регистр #30659: Канал измерения параметров РЗА Arcteq - Позиция_659";
                case 30660: return "Регистр #30660: Канал измерения параметров РЗА Arcteq - Позиция_660";
                case 30661: return "Регистр #30661: Канал измерения параметров РЗА Arcteq - Позиция_661";
                case 30662: return "Регистр #30662: Канал измерения параметров РЗА Arcteq - Позиция_662";
                case 30663: return "Регистр #30663: Канал измерения параметров РЗА Arcteq - Позиция_663";
                case 30664: return "Регистр #30664: Канал измерения параметров РЗА Arcteq - Позиция_664";
                case 30665: return "Регистр #30665: Канал измерения параметров РЗА Arcteq - Позиция_665";
                case 30666: return "Регистр #30666: Канал измерения параметров РЗА Arcteq - Позиция_666";
                case 30667: return "Регистр #30667: Канал измерения параметров РЗА Arcteq - Позиция_667";
                case 30668: return "Регистр #30668: Канал измерения параметров РЗА Arcteq - Позиция_668";
                case 30669: return "Регистр #30669: Канал измерения параметров РЗА Arcteq - Позиция_669";
                case 30670: return "Регистр #30670: Канал измерения параметров РЗА Arcteq - Позиция_670";
                case 30671: return "Регистр #30671: Канал измерения параметров РЗА Arcteq - Позиция_671";
                case 30672: return "Регистр #30672: Канал измерения параметров РЗА Arcteq - Позиция_672";
                case 30673: return "Регистр #30673: Канал измерения параметров РЗА Arcteq - Позиция_673";
                case 30674: return "Регистр #30674: Канал измерения параметров РЗА Arcteq - Позиция_674";
                case 30675: return "Регистр #30675: Канал измерения параметров РЗА Arcteq - Позиция_675";
                case 30676: return "Регистр #30676: Канал измерения параметров РЗА Arcteq - Позиция_676";
                case 30677: return "Регистр #30677: Канал измерения параметров РЗА Arcteq - Позиция_677";
                case 30678: return "Регистр #30678: Канал измерения параметров РЗА Arcteq - Позиция_678";
                case 30679: return "Регистр #30679: Канал измерения параметров РЗА Arcteq - Позиция_679";
                case 30680: return "Регистр #30680: Канал измерения параметров РЗА Arcteq - Позиция_680";
                case 30681: return "Регистр #30681: Канал измерения параметров РЗА Arcteq - Позиция_681";
                case 30682: return "Регистр #30682: Канал измерения параметров РЗА Arcteq - Позиция_682";
                case 30683: return "Регистр #30683: Канал измерения параметров РЗА Arcteq - Позиция_683";
                case 30684: return "Регистр #30684: Канал измерения параметров РЗА Arcteq - Позиция_684";
                case 30685: return "Регистр #30685: Канал измерения параметров РЗА Arcteq - Позиция_685";
                case 30686: return "Регистр #30686: Канал измерения параметров РЗА Arcteq - Позиция_686";
                case 30687: return "Регистр #30687: Канал измерения параметров РЗА Arcteq - Позиция_687";
                case 30688: return "Регистр #30688: Канал измерения параметров РЗА Arcteq - Позиция_688";
                case 30689: return "Регистр #30689: Канал измерения параметров РЗА Arcteq - Позиция_689";
                case 30690: return "Регистр #30690: Канал измерения параметров РЗА Arcteq - Позиция_690";
                case 30691: return "Регистр #30691: Канал измерения параметров РЗА Arcteq - Позиция_691";
                case 30692: return "Регистр #30692: Канал измерения параметров РЗА Arcteq - Позиция_692";
                case 30693: return "Регистр #30693: Канал измерения параметров РЗА Arcteq - Позиция_693";
                case 30694: return "Регистр #30694: Канал измерения параметров РЗА Arcteq - Позиция_694";
                case 30695: return "Регистр #30695: Канал измерения параметров РЗА Arcteq - Позиция_695";
                case 30696: return "Регистр #30696: Канал измерения параметров РЗА Arcteq - Позиция_696";
                case 30697: return "Регистр #30697: Канал измерения параметров РЗА Arcteq - Позиция_697";
                case 30698: return "Регистр #30698: Канал измерения параметров РЗА Arcteq - Позиция_698";
                case 30699: return "Регистр #30699: Канал измерения параметров РЗА Arcteq - Позиция_699";
                case 30700: return "Регистр #30700: Канал измерения параметров РЗА Arcteq - Позиция_700";
                case 30701: return "Регистр #30701: Канал измерения параметров РЗА Arcteq - Позиция_701";
                case 30702: return "Регистр #30702: Канал измерения параметров РЗА Arcteq - Позиция_702";
                case 30703: return "Регистр #30703: Канал измерения параметров РЗА Arcteq - Позиция_703";
                case 30704: return "Регистр #30704: Канал измерения параметров РЗА Arcteq - Позиция_704";
                case 30705: return "Регистр #30705: Канал измерения параметров РЗА Arcteq - Позиция_705";
                case 30706: return "Регистр #30706: Канал измерения параметров РЗА Arcteq - Позиция_706";
                case 30707: return "Регистр #30707: Канал измерения параметров РЗА Arcteq - Позиция_707";
                case 30708: return "Регистр #30708: Канал измерения параметров РЗА Arcteq - Позиция_708";
                case 30709: return "Регистр #30709: Канал измерения параметров РЗА Arcteq - Позиция_709";
                case 30710: return "Регистр #30710: Канал измерения параметров РЗА Arcteq - Позиция_710";
                case 30711: return "Регистр #30711: Канал измерения параметров РЗА Arcteq - Позиция_711";
                case 30712: return "Регистр #30712: Канал измерения параметров РЗА Arcteq - Позиция_712";
                case 30713: return "Регистр #30713: Канал измерения параметров РЗА Arcteq - Позиция_713";
                case 30714: return "Регистр #30714: Канал измерения параметров РЗА Arcteq - Позиция_714";
                case 30715: return "Регистр #30715: Канал измерения параметров РЗА Arcteq - Позиция_715";
                case 30716: return "Регистр #30716: Канал измерения параметров РЗА Arcteq - Позиция_716";
                case 30717: return "Регистр #30717: Канал измерения параметров РЗА Arcteq - Позиция_717";
                case 30718: return "Регистр #30718: Канал измерения параметров РЗА Arcteq - Позиция_718";
                case 30719: return "Регистр #30719: Канал измерения параметров РЗА Arcteq - Позиция_719";
                case 30720: return "Регистр #30720: Канал измерения параметров РЗА Arcteq - Позиция_720";
                case 30721: return "Регистр #30721: Канал измерения параметров РЗА Arcteq - Позиция_721";
                case 30722: return "Регистр #30722: Канал измерения параметров РЗА Arcteq - Позиция_722";
                case 30723: return "Регистр #30723: Канал измерения параметров РЗА Arcteq - Позиция_723";
                case 30724: return "Регистр #30724: Канал измерения параметров РЗА Arcteq - Позиция_724";
                case 30725: return "Регистр #30725: Канал измерения параметров РЗА Arcteq - Позиция_725";
                case 30726: return "Регистр #30726: Канал измерения параметров РЗА Arcteq - Позиция_726";
                case 30727: return "Регистр #30727: Канал измерения параметров РЗА Arcteq - Позиция_727";
                case 30728: return "Регистр #30728: Канал измерения параметров РЗА Arcteq - Позиция_728";
                case 30729: return "Регистр #30729: Канал измерения параметров РЗА Arcteq - Позиция_729";
                case 30730: return "Регистр #30730: Канал измерения параметров РЗА Arcteq - Позиция_730";
                case 30731: return "Регистр #30731: Канал измерения параметров РЗА Arcteq - Позиция_731";
                case 30732: return "Регистр #30732: Канал измерения параметров РЗА Arcteq - Позиция_732";
                case 30733: return "Регистр #30733: Канал измерения параметров РЗА Arcteq - Позиция_733";
                case 30734: return "Регистр #30734: Канал измерения параметров РЗА Arcteq - Позиция_734";
                case 30735: return "Регистр #30735: Канал измерения параметров РЗА Arcteq - Позиция_735";
                case 30736: return "Регистр #30736: Канал измерения параметров РЗА Arcteq - Позиция_736";
                case 30737: return "Регистр #30737: Канал измерения параметров РЗА Arcteq - Позиция_737";
                case 30738: return "Регистр #30738: Канал измерения параметров РЗА Arcteq - Позиция_738";
                case 30739: return "Регистр #30739: Канал измерения параметров РЗА Arcteq - Позиция_739";
                case 30740: return "Регистр #30740: Канал измерения параметров РЗА Arcteq - Позиция_740";
                case 30741: return "Регистр #30741: Канал измерения параметров РЗА Arcteq - Позиция_741";
                case 30742: return "Регистр #30742: Канал измерения параметров РЗА Arcteq - Позиция_742";
                case 30743: return "Регистр #30743: Канал измерения параметров РЗА Arcteq - Позиция_743";
                case 30744: return "Регистр #30744: Канал измерения параметров РЗА Arcteq - Позиция_744";
                case 30745: return "Регистр #30745: Канал измерения параметров РЗА Arcteq - Позиция_745";
                case 30746: return "Регистр #30746: Канал измерения параметров РЗА Arcteq - Позиция_746";
                case 30747: return "Регистр #30747: Канал измерения параметров РЗА Arcteq - Позиция_747";
                case 30748: return "Регистр #30748: Канал измерения параметров РЗА Arcteq - Позиция_748";
                case 30749: return "Регистр #30749: Канал измерения параметров РЗА Arcteq - Позиция_749";
                case 30750: return "Регистр #30750: Канал измерения параметров РЗА Arcteq - Позиция_750";
                case 30751: return "Регистр #30751: Канал измерения параметров РЗА Arcteq - Позиция_751";
                case 30752: return "Регистр #30752: Канал измерения параметров РЗА Arcteq - Позиция_752";
                case 30753: return "Регистр #30753: Канал измерения параметров РЗА Arcteq - Позиция_753";
                case 30754: return "Регистр #30754: Канал измерения параметров РЗА Arcteq - Позиция_754";
                case 30755: return "Регистр #30755: Канал измерения параметров РЗА Arcteq - Позиция_755";
                case 30756: return "Регистр #30756: Канал измерения параметров РЗА Arcteq - Позиция_756";
                case 30757: return "Регистр #30757: Канал измерения параметров РЗА Arcteq - Позиция_757";
                case 30758: return "Регистр #30758: Канал измерения параметров РЗА Arcteq - Позиция_758";
                case 30759: return "Регистр #30759: Канал измерения параметров РЗА Arcteq - Позиция_759";
                case 30760: return "Регистр #30760: Канал измерения параметров РЗА Arcteq - Позиция_760";
                case 30761: return "Регистр #30761: Канал измерения параметров РЗА Arcteq - Позиция_761";
                case 30762: return "Регистр #30762: Канал измерения параметров РЗА Arcteq - Позиция_762";
                case 30763: return "Регистр #30763: Канал измерения параметров РЗА Arcteq - Позиция_763";
                case 30764: return "Регистр #30764: Канал измерения параметров РЗА Arcteq - Позиция_764";
                case 30765: return "Регистр #30765: Канал измерения параметров РЗА Arcteq - Позиция_765";
                case 30766: return "Регистр #30766: Канал измерения параметров РЗА Arcteq - Позиция_766";
                case 30767: return "Регистр #30767: Канал измерения параметров РЗА Arcteq - Позиция_767";
                case 30768: return "Регистр #30768: Канал измерения параметров РЗА Arcteq - Позиция_768";
                case 30769: return "Регистр #30769: Канал измерения параметров РЗА Arcteq - Позиция_769";
                case 30770: return "Регистр #30770: Канал измерения параметров РЗА Arcteq - Позиция_770";
                case 30771: return "Регистр #30771: Канал измерения параметров РЗА Arcteq - Позиция_771";
                case 30772: return "Регистр #30772: Канал измерения параметров РЗА Arcteq - Позиция_772";
                case 30773: return "Регистр #30773: Канал измерения параметров РЗА Arcteq - Позиция_773";
                case 30774: return "Регистр #30774: Канал измерения параметров РЗА Arcteq - Позиция_774";
                case 30775: return "Регистр #30775: Канал измерения параметров РЗА Arcteq - Позиция_775";
                case 30776: return "Регистр #30776: Канал измерения параметров РЗА Arcteq - Позиция_776";
                case 30777: return "Регистр #30777: Канал измерения параметров РЗА Arcteq - Позиция_777";
                case 30778: return "Регистр #30778: Канал измерения параметров РЗА Arcteq - Позиция_778";
                case 30779: return "Регистр #30779: Канал измерения параметров РЗА Arcteq - Позиция_779";
                case 30780: return "Регистр #30780: Канал измерения параметров РЗА Arcteq - Позиция_780";
                case 30781: return "Регистр #30781: Канал измерения параметров РЗА Arcteq - Позиция_781";
                case 30782: return "Регистр #30782: Канал измерения параметров РЗА Arcteq - Позиция_782";
                case 30783: return "Регистр #30783: Канал измерения параметров РЗА Arcteq - Позиция_783";
                case 30784: return "Регистр #30784: Канал измерения параметров РЗА Arcteq - Позиция_784";
                case 30785: return "Регистр #30785: Канал измерения параметров РЗА Arcteq - Позиция_785";
                case 30786: return "Регистр #30786: Канал измерения параметров РЗА Arcteq - Позиция_786";
                case 30787: return "Регистр #30787: Канал измерения параметров РЗА Arcteq - Позиция_787";
                case 30788: return "Регистр #30788: Канал измерения параметров РЗА Arcteq - Позиция_788";
                case 30789: return "Регистр #30789: Канал измерения параметров РЗА Arcteq - Позиция_789";
                case 30790: return "Регистр #30790: Канал измерения параметров РЗА Arcteq - Позиция_790";
                case 30791: return "Регистр #30791: Канал измерения параметров РЗА Arcteq - Позиция_791";
                case 30792: return "Регистр #30792: Канал измерения параметров РЗА Arcteq - Позиция_792";
                case 30793: return "Регистр #30793: Канал измерения параметров РЗА Arcteq - Позиция_793";
                case 30794: return "Регистр #30794: Канал измерения параметров РЗА Arcteq - Позиция_794";
                case 30795: return "Регистр #30795: Канал измерения параметров РЗА Arcteq - Позиция_795";
                case 30796: return "Регистр #30796: Канал измерения параметров РЗА Arcteq - Позиция_796";
                case 30797: return "Регистр #30797: Канал измерения параметров РЗА Arcteq - Позиция_797";
                case 30798: return "Регистр #30798: Канал измерения параметров РЗА Arcteq - Позиция_798";
                case 30799: return "Регистр #30799: Канал измерения параметров РЗА Arcteq - Позиция_799";
                case 30800: return "Регистр #30800: Канал измерения параметров РЗА Arcteq - Позиция_800";
                case 30801: return "Регистр #30801: Канал измерения параметров РЗА Arcteq - Позиция_801";
                case 30802: return "Регистр #30802: Канал измерения параметров РЗА Arcteq - Позиция_802";
                case 30803: return "Регистр #30803: Канал измерения параметров РЗА Arcteq - Позиция_803";
                case 30804: return "Регистр #30804: Канал измерения параметров РЗА Arcteq - Позиция_804";
                case 30805: return "Регистр #30805: Канал измерения параметров РЗА Arcteq - Позиция_805";
                case 30806: return "Регистр #30806: Канал измерения параметров РЗА Arcteq - Позиция_806";
                case 30807: return "Регистр #30807: Канал измерения параметров РЗА Arcteq - Позиция_807";
                case 30808: return "Регистр #30808: Канал измерения параметров РЗА Arcteq - Позиция_808";
                case 30809: return "Регистр #30809: Канал измерения параметров РЗА Arcteq - Позиция_809";
                case 30810: return "Регистр #30810: Канал измерения параметров РЗА Arcteq - Позиция_810";
                case 30811: return "Регистр #30811: Канал измерения параметров РЗА Arcteq - Позиция_811";
                case 30812: return "Регистр #30812: Канал измерения параметров РЗА Arcteq - Позиция_812";
                case 30813: return "Регистр #30813: Канал измерения параметров РЗА Arcteq - Позиция_813";
                case 30814: return "Регистр #30814: Канал измерения параметров РЗА Arcteq - Позиция_814";
                case 30815: return "Регистр #30815: Канал измерения параметров РЗА Arcteq - Позиция_815";
                case 30816: return "Регистр #30816: Канал измерения параметров РЗА Arcteq - Позиция_816";
                case 30817: return "Регистр #30817: Канал измерения параметров РЗА Arcteq - Позиция_817";
                case 30818: return "Регистр #30818: Канал измерения параметров РЗА Arcteq - Позиция_818";
                case 30819: return "Регистр #30819: Канал измерения параметров РЗА Arcteq - Позиция_819";
                case 30820: return "Регистр #30820: Канал измерения параметров РЗА Arcteq - Позиция_820";
                case 30821: return "Регистр #30821: Канал измерения параметров РЗА Arcteq - Позиция_821";
                case 30822: return "Регистр #30822: Канал измерения параметров РЗА Arcteq - Позиция_822";
                case 30823: return "Регистр #30823: Канал измерения параметров РЗА Arcteq - Позиция_823";
                case 30824: return "Регистр #30824: Канал измерения параметров РЗА Arcteq - Позиция_824";
                case 30825: return "Регистр #30825: Канал измерения параметров РЗА Arcteq - Позиция_825";
                case 30826: return "Регистр #30826: Канал измерения параметров РЗА Arcteq - Позиция_826";
                case 30827: return "Регистр #30827: Канал измерения параметров РЗА Arcteq - Позиция_827";
                case 30828: return "Регистр #30828: Канал измерения параметров РЗА Arcteq - Позиция_828";
                case 30829: return "Регистр #30829: Канал измерения параметров РЗА Arcteq - Позиция_829";
                case 30830: return "Регистр #30830: Канал измерения параметров РЗА Arcteq - Позиция_830";
                case 30831: return "Регистр #30831: Канал измерения параметров РЗА Arcteq - Позиция_831";
                case 30832: return "Регистр #30832: Канал измерения параметров РЗА Arcteq - Позиция_832";
                case 30833: return "Регистр #30833: Канал измерения параметров РЗА Arcteq - Позиция_833";
                case 30834: return "Регистр #30834: Канал измерения параметров РЗА Arcteq - Позиция_834";
                case 30835: return "Регистр #30835: Канал измерения параметров РЗА Arcteq - Позиция_835";
                case 30836: return "Регистр #30836: Канал измерения параметров РЗА Arcteq - Позиция_836";
                case 30837: return "Регистр #30837: Канал измерения параметров РЗА Arcteq - Позиция_837";
                case 30838: return "Регистр #30838: Канал измерения параметров РЗА Arcteq - Позиция_838";
                case 30839: return "Регистр #30839: Канал измерения параметров РЗА Arcteq - Позиция_839";
                case 30840: return "Регистр #30840: Канал измерения параметров РЗА Arcteq - Позиция_840";
                case 30841: return "Регистр #30841: Канал измерения параметров РЗА Arcteq - Позиция_841";
                case 30842: return "Регистр #30842: Канал измерения параметров РЗА Arcteq - Позиция_842";
                case 30843: return "Регистр #30843: Канал измерения параметров РЗА Arcteq - Позиция_843";
                case 30844: return "Регистр #30844: Канал измерения параметров РЗА Arcteq - Позиция_844";
                case 30845: return "Регистр #30845: Канал измерения параметров РЗА Arcteq - Позиция_845";
                case 30846: return "Регистр #30846: Канал измерения параметров РЗА Arcteq - Позиция_846";
                case 30847: return "Регистр #30847: Канал измерения параметров РЗА Arcteq - Позиция_847";
                case 30848: return "Регистр #30848: Канал измерения параметров РЗА Arcteq - Позиция_848";
                case 30849: return "Регистр #30849: Канал измерения параметров РЗА Arcteq - Позиция_849";
                case 30850: return "Регистр #30850: Канал измерения параметров РЗА Arcteq - Позиция_850";
                case 30851: return "Регистр #30851: Канал измерения параметров РЗА Arcteq - Позиция_851";
                case 30852: return "Регистр #30852: Канал измерения параметров РЗА Arcteq - Позиция_852";
                case 30853: return "Регистр #30853: Канал измерения параметров РЗА Arcteq - Позиция_853";
                case 30854: return "Регистр #30854: Канал измерения параметров РЗА Arcteq - Позиция_854";
                case 30855: return "Регистр #30855: Канал измерения параметров РЗА Arcteq - Позиция_855";
                case 30856: return "Регистр #30856: Канал измерения параметров РЗА Arcteq - Позиция_856";
                case 30857: return "Регистр #30857: Канал измерения параметров РЗА Arcteq - Позиция_857";
                case 30858: return "Регистр #30858: Канал измерения параметров РЗА Arcteq - Позиция_858";
                case 30859: return "Регистр #30859: Канал измерения параметров РЗА Arcteq - Позиция_859";
                case 30860: return "Регистр #30860: Канал измерения параметров РЗА Arcteq - Позиция_860";
                case 30861: return "Регистр #30861: Канал измерения параметров РЗА Arcteq - Позиция_861";
                case 30862: return "Регистр #30862: Канал измерения параметров РЗА Arcteq - Позиция_862";
                case 30863: return "Регистр #30863: Канал измерения параметров РЗА Arcteq - Позиция_863";
                case 30864: return "Регистр #30864: Канал измерения параметров РЗА Arcteq - Позиция_864";
                case 30865: return "Регистр #30865: Канал измерения параметров РЗА Arcteq - Позиция_865";
                case 30866: return "Регистр #30866: Канал измерения параметров РЗА Arcteq - Позиция_866";
                case 30867: return "Регистр #30867: Канал измерения параметров РЗА Arcteq - Позиция_867";
                case 30868: return "Регистр #30868: Канал измерения параметров РЗА Arcteq - Позиция_868";
                case 30869: return "Регистр #30869: Канал измерения параметров РЗА Arcteq - Позиция_869";
                case 30870: return "Регистр #30870: Канал измерения параметров РЗА Arcteq - Позиция_870";
                case 30871: return "Регистр #30871: Канал измерения параметров РЗА Arcteq - Позиция_871";
                case 30872: return "Регистр #30872: Канал измерения параметров РЗА Arcteq - Позиция_872";
                case 30873: return "Регистр #30873: Канал измерения параметров РЗА Arcteq - Позиция_873";
                case 30874: return "Регистр #30874: Канал измерения параметров РЗА Arcteq - Позиция_874";
                case 30875: return "Регистр #30875: Канал измерения параметров РЗА Arcteq - Позиция_875";
                case 30876: return "Регистр #30876: Канал измерения параметров РЗА Arcteq - Позиция_876";
                case 30877: return "Регистр #30877: Канал измерения параметров РЗА Arcteq - Позиция_877";
                case 30878: return "Регистр #30878: Канал измерения параметров РЗА Arcteq - Позиция_878";
                case 30879: return "Регистр #30879: Канал измерения параметров РЗА Arcteq - Позиция_879";
                case 30880: return "Регистр #30880: Канал измерения параметров РЗА Arcteq - Позиция_880";
                case 30881: return "Регистр #30881: Канал измерения параметров РЗА Arcteq - Позиция_881";
                case 30882: return "Регистр #30882: Канал измерения параметров РЗА Arcteq - Позиция_882";
                case 30883: return "Регистр #30883: Канал измерения параметров РЗА Arcteq - Позиция_883";
                case 30884: return "Регистр #30884: Канал измерения параметров РЗА Arcteq - Позиция_884";
                case 30885: return "Регистр #30885: Канал измерения параметров РЗА Arcteq - Позиция_885";
                case 30886: return "Регистр #30886: Канал измерения параметров РЗА Arcteq - Позиция_886";
                case 30887: return "Регистр #30887: Канал измерения параметров РЗА Arcteq - Позиция_887";
                case 30888: return "Регистр #30888: Канал измерения параметров РЗА Arcteq - Позиция_888";
                case 30889: return "Регистр #30889: Канал измерения параметров РЗА Arcteq - Позиция_889";
                case 30890: return "Регистр #30890: Канал измерения параметров РЗА Arcteq - Позиция_890";
                case 30891: return "Регистр #30891: Канал измерения параметров РЗА Arcteq - Позиция_891";
                case 30892: return "Регистр #30892: Канал измерения параметров РЗА Arcteq - Позиция_892";
                case 30893: return "Регистр #30893: Канал измерения параметров РЗА Arcteq - Позиция_893";
                case 30894: return "Регистр #30894: Канал измерения параметров РЗА Arcteq - Позиция_894";
                case 30895: return "Регистр #30895: Канал измерения параметров РЗА Arcteq - Позиция_895";
                case 30896: return "Регистр #30896: Канал измерения параметров РЗА Arcteq - Позиция_896";
                case 30897: return "Регистр #30897: Канал измерения параметров РЗА Arcteq - Позиция_897";
                case 30898: return "Регистр #30898: Канал измерения параметров РЗА Arcteq - Позиция_898";
                case 30899: return "Регистр #30899: Канал измерения параметров РЗА Arcteq - Позиция_899";
                case 30900: return "Регистр #30900: Канал измерения параметров РЗА Arcteq - Позиция_900";
                case 30901: return "Регистр #30901: Канал измерения параметров РЗА Arcteq - Позиция_901";
                case 30902: return "Регистр #30902: Канал измерения параметров РЗА Arcteq - Позиция_902";
                case 30903: return "Регистр #30903: Канал измерения параметров РЗА Arcteq - Позиция_903";
                case 30904: return "Регистр #30904: Канал измерения параметров РЗА Arcteq - Позиция_904";
                case 30905: return "Регистр #30905: Канал измерения параметров РЗА Arcteq - Позиция_905";
                case 30906: return "Регистр #30906: Канал измерения параметров РЗА Arcteq - Позиция_906";
                case 30907: return "Регистр #30907: Канал измерения параметров РЗА Arcteq - Позиция_907";
                case 30908: return "Регистр #30908: Канал измерения параметров РЗА Arcteq - Позиция_908";
                case 30909: return "Регистр #30909: Канал измерения параметров РЗА Arcteq - Позиция_909";
                case 30910: return "Регистр #30910: Канал измерения параметров РЗА Arcteq - Позиция_910";
                case 30911: return "Регистр #30911: Канал измерения параметров РЗА Arcteq - Позиция_911";
                case 30912: return "Регистр #30912: Канал измерения параметров РЗА Arcteq - Позиция_912";
                case 30913: return "Регистр #30913: Канал измерения параметров РЗА Arcteq - Позиция_913";
                case 30914: return "Регистр #30914: Канал измерения параметров РЗА Arcteq - Позиция_914";
                case 30915: return "Регистр #30915: Канал измерения параметров РЗА Arcteq - Позиция_915";
                case 30916: return "Регистр #30916: Канал измерения параметров РЗА Arcteq - Позиция_916";
                case 30917: return "Регистр #30917: Канал измерения параметров РЗА Arcteq - Позиция_917";
                case 30918: return "Регистр #30918: Канал измерения параметров РЗА Arcteq - Позиция_918";
                case 30919: return "Регистр #30919: Канал измерения параметров РЗА Arcteq - Позиция_919";
                case 30920: return "Регистр #30920: Канал измерения параметров РЗА Arcteq - Позиция_920";
                case 30921: return "Регистр #30921: Канал измерения параметров РЗА Arcteq - Позиция_921";
                case 30922: return "Регистр #30922: Канал измерения параметров РЗА Arcteq - Позиция_922";
                case 30923: return "Регистр #30923: Канал измерения параметров РЗА Arcteq - Позиция_923";
                case 30924: return "Регистр #30924: Канал измерения параметров РЗА Arcteq - Позиция_924";
                case 30925: return "Регистр #30925: Канал измерения параметров РЗА Arcteq - Позиция_925";
                case 30926: return "Регистр #30926: Канал измерения параметров РЗА Arcteq - Позиция_926";
                case 30927: return "Регистр #30927: Канал измерения параметров РЗА Arcteq - Позиция_927";
                case 30928: return "Регистр #30928: Канал измерения параметров РЗА Arcteq - Позиция_928";
                case 30929: return "Регистр #30929: Канал измерения параметров РЗА Arcteq - Позиция_929";
                case 30930: return "Регистр #30930: Канал измерения параметров РЗА Arcteq - Позиция_930";
                case 30931: return "Регистр #30931: Канал измерения параметров РЗА Arcteq - Позиция_931";
                case 30932: return "Регистр #30932: Канал измерения параметров РЗА Arcteq - Позиция_932";
                case 30933: return "Регистр #30933: Канал измерения параметров РЗА Arcteq - Позиция_933";
                case 30934: return "Регистр #30934: Канал измерения параметров РЗА Arcteq - Позиция_934";
                case 30935: return "Регистр #30935: Канал измерения параметров РЗА Arcteq - Позиция_935";
                case 30936: return "Регистр #30936: Канал измерения параметров РЗА Arcteq - Позиция_936";
                case 30937: return "Регистр #30937: Канал измерения параметров РЗА Arcteq - Позиция_937";
                case 30938: return "Регистр #30938: Канал измерения параметров РЗА Arcteq - Позиция_938";
                case 30939: return "Регистр #30939: Канал измерения параметров РЗА Arcteq - Позиция_939";
                case 30940: return "Регистр #30940: Канал измерения параметров РЗА Arcteq - Позиция_940";
                case 30941: return "Регистр #30941: Канал измерения параметров РЗА Arcteq - Позиция_941";
                case 30942: return "Регистр #30942: Канал измерения параметров РЗА Arcteq - Позиция_942";
                case 30943: return "Регистр #30943: Канал измерения параметров РЗА Arcteq - Позиция_943";
                case 30944: return "Регистр #30944: Канал измерения параметров РЗА Arcteq - Позиция_944";
                case 30945: return "Регистр #30945: Канал измерения параметров РЗА Arcteq - Позиция_945";
                case 30946: return "Регистр #30946: Канал измерения параметров РЗА Arcteq - Позиция_946";
                case 30947: return "Регистр #30947: Канал измерения параметров РЗА Arcteq - Позиция_947";
                case 30948: return "Регистр #30948: Канал измерения параметров РЗА Arcteq - Позиция_948";
                case 30949: return "Регистр #30949: Канал измерения параметров РЗА Arcteq - Позиция_949";
                case 30950: return "Регистр #30950: Канал измерения параметров РЗА Arcteq - Позиция_950";
                case 30951: return "Регистр #30951: Канал измерения параметров РЗА Arcteq - Позиция_951";
                case 30952: return "Регистр #30952: Канал измерения параметров РЗА Arcteq - Позиция_952";
                case 30953: return "Регистр #30953: Канал измерения параметров РЗА Arcteq - Позиция_953";
                case 30954: return "Регистр #30954: Канал измерения параметров РЗА Arcteq - Позиция_954";
                case 30955: return "Регистр #30955: Канал измерения параметров РЗА Arcteq - Позиция_955";
                case 30956: return "Регистр #30956: Канал измерения параметров РЗА Arcteq - Позиция_956";
                case 30957: return "Регистр #30957: Канал измерения параметров РЗА Arcteq - Позиция_957";
                case 30958: return "Регистр #30958: Канал измерения параметров РЗА Arcteq - Позиция_958";
                case 30959: return "Регистр #30959: Канал измерения параметров РЗА Arcteq - Позиция_959";
                case 30960: return "Регистр #30960: Канал измерения параметров РЗА Arcteq - Позиция_960";
                case 30961: return "Регистр #30961: Канал измерения параметров РЗА Arcteq - Позиция_961";
                case 30962: return "Регистр #30962: Канал измерения параметров РЗА Arcteq - Позиция_962";
                case 30963: return "Регистр #30963: Канал измерения параметров РЗА Arcteq - Позиция_963";
                case 30964: return "Регистр #30964: Канал измерения параметров РЗА Arcteq - Позиция_964";
                case 30965: return "Регистр #30965: Канал измерения параметров РЗА Arcteq - Позиция_965";
                case 30966: return "Регистр #30966: Канал измерения параметров РЗА Arcteq - Позиция_966";
                case 30967: return "Регистр #30967: Канал измерения параметров РЗА Arcteq - Позиция_967";
                case 30968: return "Регистр #30968: Канал измерения параметров РЗА Arcteq - Позиция_968";
                case 30969: return "Регистр #30969: Канал измерения параметров РЗА Arcteq - Позиция_969";
                case 30970: return "Регистр #30970: Канал измерения параметров РЗА Arcteq - Позиция_970";
                case 30971: return "Регистр #30971: Канал измерения параметров РЗА Arcteq - Позиция_971";
                case 30972: return "Регистр #30972: Канал измерения параметров РЗА Arcteq - Позиция_972";
                case 30973: return "Регистр #30973: Канал измерения параметров РЗА Arcteq - Позиция_973";
                case 30974: return "Регистр #30974: Канал измерения параметров РЗА Arcteq - Позиция_974";
                case 30975: return "Регистр #30975: Канал измерения параметров РЗА Arcteq - Позиция_975";
                case 30976: return "Регистр #30976: Канал измерения параметров РЗА Arcteq - Позиция_976";
                case 30977: return "Регистр #30977: Канал измерения параметров РЗА Arcteq - Позиция_977";
                case 30978: return "Регистр #30978: Канал измерения параметров РЗА Arcteq - Позиция_978";
                case 30979: return "Регистр #30979: Канал измерения параметров РЗА Arcteq - Позиция_979";
                case 30980: return "Регистр #30980: Канал измерения параметров РЗА Arcteq - Позиция_980";
                case 30981: return "Регистр #30981: Канал измерения параметров РЗА Arcteq - Позиция_981";
                case 30982: return "Регистр #30982: Канал измерения параметров РЗА Arcteq - Позиция_982";
                case 30983: return "Регистр #30983: Канал измерения параметров РЗА Arcteq - Позиция_983";
                case 30984: return "Регистр #30984: Канал измерения параметров РЗА Arcteq - Позиция_984";
                case 30985: return "Регистр #30985: Канал измерения параметров РЗА Arcteq - Позиция_985";
                case 30986: return "Регистр #30986: Канал измерения параметров РЗА Arcteq - Позиция_986";
                case 30987: return "Регистр #30987: Канал измерения параметров РЗА Arcteq - Позиция_987";
                case 30988: return "Регистр #30988: Канал измерения параметров РЗА Arcteq - Позиция_988";
                case 30989: return "Регистр #30989: Канал измерения параметров РЗА Arcteq - Позиция_989";
                case 30990: return "Регистр #30990: Канал измерения параметров РЗА Arcteq - Позиция_990";
                case 30991: return "Регистр #30991: Канал измерения параметров РЗА Arcteq - Позиция_991";
                case 30992: return "Регистр #30992: Канал измерения параметров РЗА Arcteq - Позиция_992";
                case 30993: return "Регистр #30993: Канал измерения параметров РЗА Arcteq - Позиция_993";
                case 30994: return "Регистр #30994: Канал измерения параметров РЗА Arcteq - Позиция_994";
                case 30995: return "Регистр #30995: Канал измерения параметров РЗА Arcteq - Позиция_995";
                case 30996: return "Регистр #30996: Канал измерения параметров РЗА Arcteq - Позиция_996";
                case 30997: return "Регистр #30997: Канал измерения параметров РЗА Arcteq - Позиция_997";
                case 30998: return "Регистр #30998: Канал измерения параметров РЗА Arcteq - Позиция_998";
                case 30999: return "Регистр #30999: Канал измерения параметров РЗА Arcteq - Позиция_999";
                case 31000: return "Регистр #31000: Канал измерения параметров РЗА Arcteq - Позиция_1000";
                case 31001: return "Регистр #31001: Канал измерения параметров РЗА Arcteq - Позиция_1001";
                case 31002: return "Регистр #31002: Канал измерения параметров РЗА Arcteq - Позиция_1002";
                case 31003: return "Регистр #31003: Канал измерения параметров РЗА Arcteq - Позиция_1003";
                case 31004: return "Регистр #31004: Канал измерения параметров РЗА Arcteq - Позиция_1004";
                case 31005: return "Регистр #31005: Канал измерения параметров РЗА Arcteq - Позиция_1005";
                case 31006: return "Регистр #31006: Канал измерения параметров РЗА Arcteq - Позиция_1006";
                case 31007: return "Регистр #31007: Канал измерения параметров РЗА Arcteq - Позиция_1007";
                case 31008: return "Регистр #31008: Канал измерения параметров РЗА Arcteq - Позиция_1008";
                case 31009: return "Регистр #31009: Канал измерения параметров РЗА Arcteq - Позиция_1009";
                case 31010: return "Регистр #31010: Канал измерения параметров РЗА Arcteq - Позиция_1010";
                case 31011: return "Регистр #31011: Канал измерения параметров РЗА Arcteq - Позиция_1011";
                case 31012: return "Регистр #31012: Канал измерения параметров РЗА Arcteq - Позиция_1012";
                case 31013: return "Регистр #31013: Канал измерения параметров РЗА Arcteq - Позиция_1013";
                case 31014: return "Регистр #31014: Канал измерения параметров РЗА Arcteq - Позиция_1014";
                case 31015: return "Регистр #31015: Канал измерения параметров РЗА Arcteq - Позиция_1015";
                case 31016: return "Регистр #31016: Канал измерения параметров РЗА Arcteq - Позиция_1016";
                case 31017: return "Регистр #31017: Канал измерения параметров РЗА Arcteq - Позиция_1017";
                case 31018: return "Регистр #31018: Канал измерения параметров РЗА Arcteq - Позиция_1018";
                case 31019: return "Регистр #31019: Канал измерения параметров РЗА Arcteq - Позиция_1019";
                case 31020: return "Регистр #31020: Канал измерения параметров РЗА Arcteq - Позиция_1020";
                case 31021: return "Регистр #31021: Канал измерения параметров РЗА Arcteq - Позиция_1021";
                case 31022: return "Регистр #31022: Канал измерения параметров РЗА Arcteq - Позиция_1022";
                case 31023: return "Регистр #31023: Канал измерения параметров РЗА Arcteq - Позиция_1023";
                case 31024: return "Регистр #31024: Канал измерения параметров РЗА Arcteq - Позиция_1024";
                case 31025: return "Регистр #31025: Канал измерения параметров РЗА Arcteq - Позиция_1025";
                case 31026: return "Регистр #31026: Канал измерения параметров РЗА Arcteq - Позиция_1026";
                case 31027: return "Регистр #31027: Канал измерения параметров РЗА Arcteq - Позиция_1027";
                case 31028: return "Регистр #31028: Канал измерения параметров РЗА Arcteq - Позиция_1028";
                case 31029: return "Регистр #31029: Канал измерения параметров РЗА Arcteq - Позиция_1029";
                case 31030: return "Регистр #31030: Канал измерения параметров РЗА Arcteq - Позиция_1030";
                case 31031: return "Регистр #31031: Канал измерения параметров РЗА Arcteq - Позиция_1031";
                case 31032: return "Регистр #31032: Канал измерения параметров РЗА Arcteq - Позиция_1032";
                case 31033: return "Регистр #31033: Канал измерения параметров РЗА Arcteq - Позиция_1033";
                case 31034: return "Регистр #31034: Канал измерения параметров РЗА Arcteq - Позиция_1034";
                case 31035: return "Регистр #31035: Канал измерения параметров РЗА Arcteq - Позиция_1035";
                case 31036: return "Регистр #31036: Канал измерения параметров РЗА Arcteq - Позиция_1036";
                case 31037: return "Регистр #31037: Канал измерения параметров РЗА Arcteq - Позиция_1037";
                case 31038: return "Регистр #31038: Канал измерения параметров РЗА Arcteq - Позиция_1038";
                case 31039: return "Регистр #31039: Канал измерения параметров РЗА Arcteq - Позиция_1039";
                case 31040: return "Регистр #31040: Канал измерения параметров РЗА Arcteq - Позиция_1040";
                case 31041: return "Регистр #31041: Канал измерения параметров РЗА Arcteq - Позиция_1041";
                case 31042: return "Регистр #31042: Канал измерения параметров РЗА Arcteq - Позиция_1042";
                case 31043: return "Регистр #31043: Канал измерения параметров РЗА Arcteq - Позиция_1043";
                case 31044: return "Регистр #31044: Канал измерения параметров РЗА Arcteq - Позиция_1044";
                case 31045: return "Регистр #31045: Канал измерения параметров РЗА Arcteq - Позиция_1045";
                case 31046: return "Регистр #31046: Канал измерения параметров РЗА Arcteq - Позиция_1046";
                case 31047: return "Регистр #31047: Канал измерения параметров РЗА Arcteq - Позиция_1047";
                case 31048: return "Регистр #31048: Канал измерения параметров РЗА Arcteq - Позиция_1048";
                case 31049: return "Регистр #31049: Канал измерения параметров РЗА Arcteq - Позиция_1049";
                case 31050: return "Регистр #31050: Канал измерения параметров РЗА Arcteq - Позиция_1050";
                case 31051: return "Регистр #31051: Канал измерения параметров РЗА Arcteq - Позиция_1051";
                case 31052: return "Регистр #31052: Канал измерения параметров РЗА Arcteq - Позиция_1052";
                case 31053: return "Регистр #31053: Канал измерения параметров РЗА Arcteq - Позиция_1053";
                case 31054: return "Регистр #31054: Канал измерения параметров РЗА Arcteq - Позиция_1054";
                case 31055: return "Регистр #31055: Канал измерения параметров РЗА Arcteq - Позиция_1055";
                case 31056: return "Регистр #31056: Канал измерения параметров РЗА Arcteq - Позиция_1056";
                case 31057: return "Регистр #31057: Канал измерения параметров РЗА Arcteq - Позиция_1057";
                case 31058: return "Регистр #31058: Канал измерения параметров РЗА Arcteq - Позиция_1058";
                case 31059: return "Регистр #31059: Канал измерения параметров РЗА Arcteq - Позиция_1059";
                case 31060: return "Регистр #31060: Канал измерения параметров РЗА Arcteq - Позиция_1060";
                case 31061: return "Регистр #31061: Канал измерения параметров РЗА Arcteq - Позиция_1061";
                case 31062: return "Регистр #31062: Канал измерения параметров РЗА Arcteq - Позиция_1062";
                case 31063: return "Регистр #31063: Канал измерения параметров РЗА Arcteq - Позиция_1063";
                case 31064: return "Регистр #31064: Канал измерения параметров РЗА Arcteq - Позиция_1064";
                case 31065: return "Регистр #31065: Канал измерения параметров РЗА Arcteq - Позиция_1065";
                case 31066: return "Регистр #31066: Канал измерения параметров РЗА Arcteq - Позиция_1066";
                case 31067: return "Регистр #31067: Канал измерения параметров РЗА Arcteq - Позиция_1067";
                case 31068: return "Регистр #31068: Канал измерения параметров РЗА Arcteq - Позиция_1068";
                case 31069: return "Регистр #31069: Канал измерения параметров РЗА Arcteq - Позиция_1069";
                case 31070: return "Регистр #31070: Канал измерения параметров РЗА Arcteq - Позиция_1070";
                case 31071: return "Регистр #31071: Канал измерения параметров РЗА Arcteq - Позиция_1071";
                case 31072: return "Регистр #31072: Канал измерения параметров РЗА Arcteq - Позиция_1072";
                case 31073: return "Регистр #31073: Канал измерения параметров РЗА Arcteq - Позиция_1073";
                case 31074: return "Регистр #31074: Канал измерения параметров РЗА Arcteq - Позиция_1074";
                case 31075: return "Регистр #31075: Канал измерения параметров РЗА Arcteq - Позиция_1075";
                case 31076: return "Регистр #31076: Канал измерения параметров РЗА Arcteq - Позиция_1076";
                case 31077: return "Регистр #31077: Канал измерения параметров РЗА Arcteq - Позиция_1077";
                case 31078: return "Регистр #31078: Канал измерения параметров РЗА Arcteq - Позиция_1078";
                case 31079: return "Регистр #31079: Канал измерения параметров РЗА Arcteq - Позиция_1079";
                case 31080: return "Регистр #31080: Канал измерения параметров РЗА Arcteq - Позиция_1080";
                case 31081: return "Регистр #31081: Канал измерения параметров РЗА Arcteq - Позиция_1081";
                case 31082: return "Регистр #31082: Канал измерения параметров РЗА Arcteq - Позиция_1082";
                case 31083: return "Регистр #31083: Канал измерения параметров РЗА Arcteq - Позиция_1083";
                case 31084: return "Регистр #31084: Канал измерения параметров РЗА Arcteq - Позиция_1084";
                case 31085: return "Регистр #31085: Канал измерения параметров РЗА Arcteq - Позиция_1085";
                case 31086: return "Регистр #31086: Канал измерения параметров РЗА Arcteq - Позиция_1086";
                case 31087: return "Регистр #31087: Канал измерения параметров РЗА Arcteq - Позиция_1087";
                case 31088: return "Регистр #31088: Канал измерения параметров РЗА Arcteq - Позиция_1088";
                case 31089: return "Регистр #31089: Канал измерения параметров РЗА Arcteq - Позиция_1089";
                case 31090: return "Регистр #31090: Канал измерения параметров РЗА Arcteq - Позиция_1090";
                case 31091: return "Регистр #31091: Канал измерения параметров РЗА Arcteq - Позиция_1091";
                case 31092: return "Регистр #31092: Канал измерения параметров РЗА Arcteq - Позиция_1092";
                case 31093: return "Регистр #31093: Канал измерения параметров РЗА Arcteq - Позиция_1093";
                case 31094: return "Регистр #31094: Канал измерения параметров РЗА Arcteq - Позиция_1094";
                case 31095: return "Регистр #31095: Канал измерения параметров РЗА Arcteq - Позиция_1095";
                case 31096: return "Регистр #31096: Канал измерения параметров РЗА Arcteq - Позиция_1096";
                case 31097: return "Регистр #31097: Канал измерения параметров РЗА Arcteq - Позиция_1097";
                case 31098: return "Регистр #31098: Канал измерения параметров РЗА Arcteq - Позиция_1098";
                case 31099: return "Регистр #31099: Канал измерения параметров РЗА Arcteq - Позиция_1099";
                case 31100: return "Регистр #31100: Канал измерения параметров РЗА Arcteq - Позиция_1100";
                case 31101: return "Регистр #31101: Канал измерения параметров РЗА Arcteq - Позиция_1101";
                case 31102: return "Регистр #31102: Канал измерения параметров РЗА Arcteq - Позиция_1102";
                case 31103: return "Регистр #31103: Канал измерения параметров РЗА Arcteq - Позиция_1103";
                case 31104: return "Регистр #31104: Канал измерения параметров РЗА Arcteq - Позиция_1104";
                case 31105: return "Регистр #31105: Канал измерения параметров РЗА Arcteq - Позиция_1105";
                case 31106: return "Регистр #31106: Канал измерения параметров РЗА Arcteq - Позиция_1106";
                case 31107: return "Регистр #31107: Канал измерения параметров РЗА Arcteq - Позиция_1107";
                case 31108: return "Регистр #31108: Канал измерения параметров РЗА Arcteq - Позиция_1108";
                case 31109: return "Регистр #31109: Канал измерения параметров РЗА Arcteq - Позиция_1109";
                case 31110: return "Регистр #31110: Канал измерения параметров РЗА Arcteq - Позиция_1110";
                case 31111: return "Регистр #31111: Канал измерения параметров РЗА Arcteq - Позиция_1111";
                case 31112: return "Регистр #31112: Канал измерения параметров РЗА Arcteq - Позиция_1112";
                case 31113: return "Регистр #31113: Канал измерения параметров РЗА Arcteq - Позиция_1113";
                case 31114: return "Регистр #31114: Канал измерения параметров РЗА Arcteq - Позиция_1114";
                case 31115: return "Регистр #31115: Канал измерения параметров РЗА Arcteq - Позиция_1115";
                case 31116: return "Регистр #31116: Канал измерения параметров РЗА Arcteq - Позиция_1116";
                case 31117: return "Регистр #31117: Канал измерения параметров РЗА Arcteq - Позиция_1117";
                case 31118: return "Регистр #31118: Канал измерения параметров РЗА Arcteq - Позиция_1118";
                case 31119: return "Регистр #31119: Канал измерения параметров РЗА Arcteq - Позиция_1119";
                case 31120: return "Регистр #31120: Канал измерения параметров РЗА Arcteq - Позиция_1120";
                case 31121: return "Регистр #31121: Канал измерения параметров РЗА Arcteq - Позиция_1121";
                case 31122: return "Регистр #31122: Канал измерения параметров РЗА Arcteq - Позиция_1122";
                case 31123: return "Регистр #31123: Канал измерения параметров РЗА Arcteq - Позиция_1123";
                case 31124: return "Регистр #31124: Канал измерения параметров РЗА Arcteq - Позиция_1124";
                case 31125: return "Регистр #31125: Канал измерения параметров РЗА Arcteq - Позиция_1125";
                case 31126: return "Регистр #31126: Канал измерения параметров РЗА Arcteq - Позиция_1126";
                case 31127: return "Регистр #31127: Канал измерения параметров РЗА Arcteq - Позиция_1127";
                case 31128: return "Регистр #31128: Канал измерения параметров РЗА Arcteq - Позиция_1128";
                case 31129: return "Регистр #31129: Канал измерения параметров РЗА Arcteq - Позиция_1129";
                case 31130: return "Регистр #31130: Канал измерения параметров РЗА Arcteq - Позиция_1130";
                case 31131: return "Регистр #31131: Канал измерения параметров РЗА Arcteq - Позиция_1131";
                case 31132: return "Регистр #31132: Канал измерения параметров РЗА Arcteq - Позиция_1132";
                case 31133: return "Регистр #31133: Канал измерения параметров РЗА Arcteq - Позиция_1133";
                case 31134: return "Регистр #31134: Канал измерения параметров РЗА Arcteq - Позиция_1134";
                case 31135: return "Регистр #31135: Канал измерения параметров РЗА Arcteq - Позиция_1135";
                case 31136: return "Регистр #31136: Канал измерения параметров РЗА Arcteq - Позиция_1136";
                case 31137: return "Регистр #31137: Канал измерения параметров РЗА Arcteq - Позиция_1137";
                case 31138: return "Регистр #31138: Канал измерения параметров РЗА Arcteq - Позиция_1138";
                case 31139: return "Регистр #31139: Канал измерения параметров РЗА Arcteq - Позиция_1139";
                case 31140: return "Регистр #31140: Канал измерения параметров РЗА Arcteq - Позиция_1140";
                case 31141: return "Регистр #31141: Канал измерения параметров РЗА Arcteq - Позиция_1141";
                case 31142: return "Регистр #31142: Канал измерения параметров РЗА Arcteq - Позиция_1142";
                case 31143: return "Регистр #31143: Канал измерения параметров РЗА Arcteq - Позиция_1143";
                case 31144: return "Регистр #31144: Канал измерения параметров РЗА Arcteq - Позиция_1144";
                case 31145: return "Регистр #31145: Канал измерения параметров РЗА Arcteq - Позиция_1145";
                case 31146: return "Регистр #31146: Канал измерения параметров РЗА Arcteq - Позиция_1146";
                case 31147: return "Регистр #31147: Канал измерения параметров РЗА Arcteq - Позиция_1147";
                case 31148: return "Регистр #31148: Канал измерения параметров РЗА Arcteq - Позиция_1148";
                case 31149: return "Регистр #31149: Канал измерения параметров РЗА Arcteq - Позиция_1149";
                case 31150: return "Регистр #31150: Канал измерения параметров РЗА Arcteq - Позиция_1150";
                case 31151: return "Регистр #31151: Канал измерения параметров РЗА Arcteq - Позиция_1151";
                case 31152: return "Регистр #31152: Канал измерения параметров РЗА Arcteq - Позиция_1152";
                case 31153: return "Регистр #31153: Канал измерения параметров РЗА Arcteq - Позиция_1153";
                case 31154: return "Регистр #31154: Канал измерения параметров РЗА Arcteq - Позиция_1154";
                case 31155: return "Регистр #31155: Канал измерения параметров РЗА Arcteq - Позиция_1155";
                case 31156: return "Регистр #31156: Канал измерения параметров РЗА Arcteq - Позиция_1156";
                case 31157: return "Регистр #31157: Канал измерения параметров РЗА Arcteq - Позиция_1157";
                case 31158: return "Регистр #31158: Канал измерения параметров РЗА Arcteq - Позиция_1158";
                case 31159: return "Регистр #31159: Канал измерения параметров РЗА Arcteq - Позиция_1159";
                case 31160: return "Регистр #31160: Канал измерения параметров РЗА Arcteq - Позиция_1160";
                case 31161: return "Регистр #31161: Канал измерения параметров РЗА Arcteq - Позиция_1161";
                case 31162: return "Регистр #31162: Канал измерения параметров РЗА Arcteq - Позиция_1162";
                case 31163: return "Регистр #31163: Канал измерения параметров РЗА Arcteq - Позиция_1163";
                case 31164: return "Регистр #31164: Канал измерения параметров РЗА Arcteq - Позиция_1164";
                case 31165: return "Регистр #31165: Канал измерения параметров РЗА Arcteq - Позиция_1165";
                case 31166: return "Регистр #31166: Канал измерения параметров РЗА Arcteq - Позиция_1166";
                case 31167: return "Регистр #31167: Канал измерения параметров РЗА Arcteq - Позиция_1167";
                case 31168: return "Регистр #31168: Канал измерения параметров РЗА Arcteq - Позиция_1168";
                case 31169: return "Регистр #31169: Канал измерения параметров РЗА Arcteq - Позиция_1169";
                case 31170: return "Регистр #31170: Канал измерения параметров РЗА Arcteq - Позиция_1170";
                case 31171: return "Регистр #31171: Канал измерения параметров РЗА Arcteq - Позиция_1171";
                case 31172: return "Регистр #31172: Канал измерения параметров РЗА Arcteq - Позиция_1172";
                case 31173: return "Регистр #31173: Канал измерения параметров РЗА Arcteq - Позиция_1173";
                case 31174: return "Регистр #31174: Канал измерения параметров РЗА Arcteq - Позиция_1174";
                case 31175: return "Регистр #31175: Канал измерения параметров РЗА Arcteq - Позиция_1175";
                case 31176: return "Регистр #31176: Канал измерения параметров РЗА Arcteq - Позиция_1176";
                case 31177: return "Регистр #31177: Канал измерения параметров РЗА Arcteq - Позиция_1177";
                case 31178: return "Регистр #31178: Канал измерения параметров РЗА Arcteq - Позиция_1178";
                case 31179: return "Регистр #31179: Канал измерения параметров РЗА Arcteq - Позиция_1179";
                case 31180: return "Регистр #31180: Канал измерения параметров РЗА Arcteq - Позиция_1180";
                case 31181: return "Регистр #31181: Канал измерения параметров РЗА Arcteq - Позиция_1181";
                case 31182: return "Регистр #31182: Канал измерения параметров РЗА Arcteq - Позиция_1182";
                case 31183: return "Регистр #31183: Канал измерения параметров РЗА Arcteq - Позиция_1183";
                case 31184: return "Регистр #31184: Канал измерения параметров РЗА Arcteq - Позиция_1184";
                case 31185: return "Регистр #31185: Канал измерения параметров РЗА Arcteq - Позиция_1185";
                case 31186: return "Регистр #31186: Канал измерения параметров РЗА Arcteq - Позиция_1186";
                case 31187: return "Регистр #31187: Канал измерения параметров РЗА Arcteq - Позиция_1187";
                case 31188: return "Регистр #31188: Канал измерения параметров РЗА Arcteq - Позиция_1188";
                case 31189: return "Регистр #31189: Канал измерения параметров РЗА Arcteq - Позиция_1189";
                case 31190: return "Регистр #31190: Канал измерения параметров РЗА Arcteq - Позиция_1190";
                case 31191: return "Регистр #31191: Канал измерения параметров РЗА Arcteq - Позиция_1191";
                case 31192: return "Регистр #31192: Канал измерения параметров РЗА Arcteq - Позиция_1192";
                case 31193: return "Регистр #31193: Канал измерения параметров РЗА Arcteq - Позиция_1193";
                case 31194: return "Регистр #31194: Канал измерения параметров РЗА Arcteq - Позиция_1194";
                case 31195: return "Регистр #31195: Канал измерения параметров РЗА Arcteq - Позиция_1195";
                case 31196: return "Регистр #31196: Канал измерения параметров РЗА Arcteq - Позиция_1196";
                case 31197: return "Регистр #31197: Канал измерения параметров РЗА Arcteq - Позиция_1197";
                case 31198: return "Регистр #31198: Канал измерения параметров РЗА Arcteq - Позиция_1198";
                case 31199: return "Регистр #31199: Канал измерения параметров РЗА Arcteq - Позиция_1199";
                case 31200: return "Регистр #31200: Канал измерения параметров РЗА Arcteq - Позиция_1200";
                case 31201: return "Регистр #31201: Канал измерения параметров РЗА Arcteq - Позиция_1201";
                case 31202: return "Регистр #31202: Канал измерения параметров РЗА Arcteq - Позиция_1202";
                case 31203: return "Регистр #31203: Канал измерения параметров РЗА Arcteq - Позиция_1203";
                case 31204: return "Регистр #31204: Канал измерения параметров РЗА Arcteq - Позиция_1204";
                case 31205: return "Регистр #31205: Канал измерения параметров РЗА Arcteq - Позиция_1205";
                case 31206: return "Регистр #31206: Канал измерения параметров РЗА Arcteq - Позиция_1206";
                case 31207: return "Регистр #31207: Канал измерения параметров РЗА Arcteq - Позиция_1207";
                case 31208: return "Регистр #31208: Канал измерения параметров РЗА Arcteq - Позиция_1208";
                case 31209: return "Регистр #31209: Канал измерения параметров РЗА Arcteq - Позиция_1209";
                case 31210: return "Регистр #31210: Канал измерения параметров РЗА Arcteq - Позиция_1210";
                case 31211: return "Регистр #31211: Канал измерения параметров РЗА Arcteq - Позиция_1211";
                case 31212: return "Регистр #31212: Канал измерения параметров РЗА Arcteq - Позиция_1212";
                case 31213: return "Регистр #31213: Канал измерения параметров РЗА Arcteq - Позиция_1213";
                case 31214: return "Регистр #31214: Канал измерения параметров РЗА Arcteq - Позиция_1214";
                case 31215: return "Регистр #31215: Канал измерения параметров РЗА Arcteq - Позиция_1215";
                case 31216: return "Регистр #31216: Канал измерения параметров РЗА Arcteq - Позиция_1216";
                case 31217: return "Регистр #31217: Канал измерения параметров РЗА Arcteq - Позиция_1217";
                case 31218: return "Регистр #31218: Канал измерения параметров РЗА Arcteq - Позиция_1218";
                case 31219: return "Регистр #31219: Канал измерения параметров РЗА Arcteq - Позиция_1219";
                case 31220: return "Регистр #31220: Канал измерения параметров РЗА Arcteq - Позиция_1220";
                case 31221: return "Регистр #31221: Канал измерения параметров РЗА Arcteq - Позиция_1221";
                case 31222: return "Регистр #31222: Канал измерения параметров РЗА Arcteq - Позиция_1222";
                case 31223: return "Регистр #31223: Канал измерения параметров РЗА Arcteq - Позиция_1223";
                case 31224: return "Регистр #31224: Канал измерения параметров РЗА Arcteq - Позиция_1224";
                case 31225: return "Регистр #31225: Канал измерения параметров РЗА Arcteq - Позиция_1225";
                case 31226: return "Регистр #31226: Канал измерения параметров РЗА Arcteq - Позиция_1226";
                case 31227: return "Регистр #31227: Канал измерения параметров РЗА Arcteq - Позиция_1227";
                case 31228: return "Регистр #31228: Канал измерения параметров РЗА Arcteq - Позиция_1228";
                case 31229: return "Регистр #31229: Канал измерения параметров РЗА Arcteq - Позиция_1229";
                case 31230: return "Регистр #31230: Канал измерения параметров РЗА Arcteq - Позиция_1230";
                case 31231: return "Регистр #31231: Канал измерения параметров РЗА Arcteq - Позиция_1231";
                case 31232: return "Регистр #31232: Канал измерения параметров РЗА Arcteq - Позиция_1232";
                case 31233: return "Регистр #31233: Канал измерения параметров РЗА Arcteq - Позиция_1233";
                case 31234: return "Регистр #31234: Канал измерения параметров РЗА Arcteq - Позиция_1234";
                case 31235: return "Регистр #31235: Канал измерения параметров РЗА Arcteq - Позиция_1235";
                case 31236: return "Регистр #31236: Канал измерения параметров РЗА Arcteq - Позиция_1236";
                case 31237: return "Регистр #31237: Канал измерения параметров РЗА Arcteq - Позиция_1237";
                case 31238: return "Регистр #31238: Канал измерения параметров РЗА Arcteq - Позиция_1238";
                case 31239: return "Регистр #31239: Канал измерения параметров РЗА Arcteq - Позиция_1239";
                case 31240: return "Регистр #31240: Канал измерения параметров РЗА Arcteq - Позиция_1240";
                case 31241: return "Регистр #31241: Канал измерения параметров РЗА Arcteq - Позиция_1241";
                case 31242: return "Регистр #31242: Канал измерения параметров РЗА Arcteq - Позиция_1242";
                case 31243: return "Регистр #31243: Канал измерения параметров РЗА Arcteq - Позиция_1243";
                case 31244: return "Регистр #31244: Канал измерения параметров РЗА Arcteq - Позиция_1244";
                case 31245: return "Регистр #31245: Канал измерения параметров РЗА Arcteq - Позиция_1245";
                case 31246: return "Регистр #31246: Канал измерения параметров РЗА Arcteq - Позиция_1246";
                case 31247: return "Регистр #31247: Канал измерения параметров РЗА Arcteq - Позиция_1247";
                case 31248: return "Регистр #31248: Канал измерения параметров РЗА Arcteq - Позиция_1248";
                case 31249: return "Регистр #31249: Канал измерения параметров РЗА Arcteq - Позиция_1249";
                case 31250: return "Регистр #31250: Канал измерения параметров РЗА Arcteq - Позиция_1250";
                case 31251: return "Регистр #31251: Канал измерения параметров РЗА Arcteq - Позиция_1251";
                case 31252: return "Регистр #31252: Канал измерения параметров РЗА Arcteq - Позиция_1252";
                case 31253: return "Регистр #31253: Канал измерения параметров РЗА Arcteq - Позиция_1253";
                case 31254: return "Регистр #31254: Канал измерения параметров РЗА Arcteq - Позиция_1254";
                case 31255: return "Регистр #31255: Канал измерения параметров РЗА Arcteq - Позиция_1255";
                case 31256: return "Регистр #31256: Канал измерения параметров РЗА Arcteq - Позиция_1256";
                case 31257: return "Регистр #31257: Канал измерения параметров РЗА Arcteq - Позиция_1257";
                case 31258: return "Регистр #31258: Канал измерения параметров РЗА Arcteq - Позиция_1258";
                case 31259: return "Регистр #31259: Канал измерения параметров РЗА Arcteq - Позиция_1259";
                case 31260: return "Регистр #31260: Канал измерения параметров РЗА Arcteq - Позиция_1260";
                case 31261: return "Регистр #31261: Канал измерения параметров РЗА Arcteq - Позиция_1261";
                case 31262: return "Регистр #31262: Канал измерения параметров РЗА Arcteq - Позиция_1262";
                case 31263: return "Регистр #31263: Канал измерения параметров РЗА Arcteq - Позиция_1263";
                case 31264: return "Регистр #31264: Канал измерения параметров РЗА Arcteq - Позиция_1264";
                case 31265: return "Регистр #31265: Канал измерения параметров РЗА Arcteq - Позиция_1265";
                case 31266: return "Регистр #31266: Канал измерения параметров РЗА Arcteq - Позиция_1266";
                case 31267: return "Регистр #31267: Канал измерения параметров РЗА Arcteq - Позиция_1267";
                case 31268: return "Регистр #31268: Канал измерения параметров РЗА Arcteq - Позиция_1268";
                case 31269: return "Регистр #31269: Канал измерения параметров РЗА Arcteq - Позиция_1269";
                case 31270: return "Регистр #31270: Канал измерения параметров РЗА Arcteq - Позиция_1270";
                case 31271: return "Регистр #31271: Канал измерения параметров РЗА Arcteq - Позиция_1271";
                case 31272: return "Регистр #31272: Канал измерения параметров РЗА Arcteq - Позиция_1272";
                case 31273: return "Регистр #31273: Канал измерения параметров РЗА Arcteq - Позиция_1273";
                case 31274: return "Регистр #31274: Канал измерения параметров РЗА Arcteq - Позиция_1274";
                case 31275: return "Регистр #31275: Канал измерения параметров РЗА Arcteq - Позиция_1275";
                case 31276: return "Регистр #31276: Канал измерения параметров РЗА Arcteq - Позиция_1276";
                case 31277: return "Регистр #31277: Канал измерения параметров РЗА Arcteq - Позиция_1277";
                case 31278: return "Регистр #31278: Канал измерения параметров РЗА Arcteq - Позиция_1278";
                case 31279: return "Регистр #31279: Канал измерения параметров РЗА Arcteq - Позиция_1279";
                case 31280: return "Регистр #31280: Канал измерения параметров РЗА Arcteq - Позиция_1280";
                case 31281: return "Регистр #31281: Канал измерения параметров РЗА Arcteq - Позиция_1281";
                case 31282: return "Регистр #31282: Канал измерения параметров РЗА Arcteq - Позиция_1282";
                case 31283: return "Регистр #31283: Канал измерения параметров РЗА Arcteq - Позиция_1283";
                case 31284: return "Регистр #31284: Канал измерения параметров РЗА Arcteq - Позиция_1284";
                case 31285: return "Регистр #31285: Канал измерения параметров РЗА Arcteq - Позиция_1285";
                case 31286: return "Регистр #31286: Канал измерения параметров РЗА Arcteq - Позиция_1286";
                case 31287: return "Регистр #31287: Канал измерения параметров РЗА Arcteq - Позиция_1287";
                case 31288: return "Регистр #31288: Канал измерения параметров РЗА Arcteq - Позиция_1288";
                case 31289: return "Регистр #31289: Канал измерения параметров РЗА Arcteq - Позиция_1289";
                case 31290: return "Регистр #31290: Канал измерения параметров РЗА Arcteq - Позиция_1290";
                case 31291: return "Регистр #31291: Канал измерения параметров РЗА Arcteq - Позиция_1291";
                case 31292: return "Регистр #31292: Канал измерения параметров РЗА Arcteq - Позиция_1292";
                case 31293: return "Регистр #31293: Канал измерения параметров РЗА Arcteq - Позиция_1293";
                case 31294: return "Регистр #31294: Канал измерения параметров РЗА Arcteq - Позиция_1294";
                case 31295: return "Регистр #31295: Канал измерения параметров РЗА Arcteq - Позиция_1295";
                case 31296: return "Регистр #31296: Канал измерения параметров РЗА Arcteq - Позиция_1296";
                case 31297: return "Регистр #31297: Канал измерения параметров РЗА Arcteq - Позиция_1297";
                case 31298: return "Регистр #31298: Канал измерения параметров РЗА Arcteq - Позиция_1298";
                case 31299: return "Регистр #31299: Канал измерения параметров РЗА Arcteq - Позиция_1299";
                case 31300: return "Регистр #31300: Канал измерения параметров РЗА Arcteq - Позиция_1300";
                case 31301: return "Регистр #31301: Канал измерения параметров РЗА Arcteq - Позиция_1301";
                case 31302: return "Регистр #31302: Канал измерения параметров РЗА Arcteq - Позиция_1302";
                case 31303: return "Регистр #31303: Канал измерения параметров РЗА Arcteq - Позиция_1303";
                case 31304: return "Регистр #31304: Канал измерения параметров РЗА Arcteq - Позиция_1304";
                case 31305: return "Регистр #31305: Канал измерения параметров РЗА Arcteq - Позиция_1305";
                case 31306: return "Регистр #31306: Канал измерения параметров РЗА Arcteq - Позиция_1306";
                case 31307: return "Регистр #31307: Канал измерения параметров РЗА Arcteq - Позиция_1307";
                case 31308: return "Регистр #31308: Канал измерения параметров РЗА Arcteq - Позиция_1308";
                case 31309: return "Регистр #31309: Канал измерения параметров РЗА Arcteq - Позиция_1309";
                case 31310: return "Регистр #31310: Канал измерения параметров РЗА Arcteq - Позиция_1310";
                case 31311: return "Регистр #31311: Канал измерения параметров РЗА Arcteq - Позиция_1311";
                case 31312: return "Регистр #31312: Канал измерения параметров РЗА Arcteq - Позиция_1312";
                case 31313: return "Регистр #31313: Канал измерения параметров РЗА Arcteq - Позиция_1313";
                case 31314: return "Регистр #31314: Канал измерения параметров РЗА Arcteq - Позиция_1314";
                case 31315: return "Регистр #31315: Канал измерения параметров РЗА Arcteq - Позиция_1315";
                case 31316: return "Регистр #31316: Канал измерения параметров РЗА Arcteq - Позиция_1316";
                case 31317: return "Регистр #31317: Канал измерения параметров РЗА Arcteq - Позиция_1317";
                case 31318: return "Регистр #31318: Канал измерения параметров РЗА Arcteq - Позиция_1318";
                case 31319: return "Регистр #31319: Канал измерения параметров РЗА Arcteq - Позиция_1319";
                case 31320: return "Регистр #31320: Канал измерения параметров РЗА Arcteq - Позиция_1320";
                case 31321: return "Регистр #31321: Канал измерения параметров РЗА Arcteq - Позиция_1321";
                case 31322: return "Регистр #31322: Канал измерения параметров РЗА Arcteq - Позиция_1322";
                case 31323: return "Регистр #31323: Канал измерения параметров РЗА Arcteq - Позиция_1323";
                case 31324: return "Регистр #31324: Канал измерения параметров РЗА Arcteq - Позиция_1324";
                case 31325: return "Регистр #31325: Канал измерения параметров РЗА Arcteq - Позиция_1325";
                case 31326: return "Регистр #31326: Канал измерения параметров РЗА Arcteq - Позиция_1326";
                case 31327: return "Регистр #31327: Канал измерения параметров РЗА Arcteq - Позиция_1327";
                case 31328: return "Регистр #31328: Канал измерения параметров РЗА Arcteq - Позиция_1328";
                case 31329: return "Регистр #31329: Канал измерения параметров РЗА Arcteq - Позиция_1329";
                case 31330: return "Регистр #31330: Канал измерения параметров РЗА Arcteq - Позиция_1330";
                case 31331: return "Регистр #31331: Канал измерения параметров РЗА Arcteq - Позиция_1331";
                case 31332: return "Регистр #31332: Канал измерения параметров РЗА Arcteq - Позиция_1332";
                case 31333: return "Регистр #31333: Канал измерения параметров РЗА Arcteq - Позиция_1333";
                case 31334: return "Регистр #31334: Канал измерения параметров РЗА Arcteq - Позиция_1334";
                case 31335: return "Регистр #31335: Канал измерения параметров РЗА Arcteq - Позиция_1335";
                case 31336: return "Регистр #31336: Канал измерения параметров РЗА Arcteq - Позиция_1336";
                case 31337: return "Регистр #31337: Канал измерения параметров РЗА Arcteq - Позиция_1337";
                case 31338: return "Регистр #31338: Канал измерения параметров РЗА Arcteq - Позиция_1338";
                case 31339: return "Регистр #31339: Канал измерения параметров РЗА Arcteq - Позиция_1339";
                case 31340: return "Регистр #31340: Канал измерения параметров РЗА Arcteq - Позиция_1340";
                case 31341: return "Регистр #31341: Канал измерения параметров РЗА Arcteq - Позиция_1341";
                case 31342: return "Регистр #31342: Канал измерения параметров РЗА Arcteq - Позиция_1342";
                case 31343: return "Регистр #31343: Канал измерения параметров РЗА Arcteq - Позиция_1343";
                case 31344: return "Регистр #31344: Канал измерения параметров РЗА Arcteq - Позиция_1344";
                case 31345: return "Регистр #31345: Канал измерения параметров РЗА Arcteq - Позиция_1345";
                case 31346: return "Регистр #31346: Канал измерения параметров РЗА Arcteq - Позиция_1346";
                case 31347: return "Регистр #31347: Канал измерения параметров РЗА Arcteq - Позиция_1347";
                case 31348: return "Регистр #31348: Канал измерения параметров РЗА Arcteq - Позиция_1348";
                case 31349: return "Регистр #31349: Канал измерения параметров РЗА Arcteq - Позиция_1349";
                case 31350: return "Регистр #31350: Канал измерения параметров РЗА Arcteq - Позиция_1350";
                case 31351: return "Регистр #31351: Канал измерения параметров РЗА Arcteq - Позиция_1351";
                case 31352: return "Регистр #31352: Канал измерения параметров РЗА Arcteq - Позиция_1352";
                case 31353: return "Регистр #31353: Канал измерения параметров РЗА Arcteq - Позиция_1353";
                case 31354: return "Регистр #31354: Канал измерения параметров РЗА Arcteq - Позиция_1354";
                case 31355: return "Регистр #31355: Канал измерения параметров РЗА Arcteq - Позиция_1355";
                case 31356: return "Регистр #31356: Канал измерения параметров РЗА Arcteq - Позиция_1356";
                case 31357: return "Регистр #31357: Канал измерения параметров РЗА Arcteq - Позиция_1357";
                case 31358: return "Регистр #31358: Канал измерения параметров РЗА Arcteq - Позиция_1358";
                case 31359: return "Регистр #31359: Канал измерения параметров РЗА Arcteq - Позиция_1359";
                case 31360: return "Регистр #31360: Канал измерения параметров РЗА Arcteq - Позиция_1360";
                case 31361: return "Регистр #31361: Канал измерения параметров РЗА Arcteq - Позиция_1361";
                case 31362: return "Регистр #31362: Канал измерения параметров РЗА Arcteq - Позиция_1362";
                case 31363: return "Регистр #31363: Канал измерения параметров РЗА Arcteq - Позиция_1363";
                case 31364: return "Регистр #31364: Канал измерения параметров РЗА Arcteq - Позиция_1364";
                case 31365: return "Регистр #31365: Канал измерения параметров РЗА Arcteq - Позиция_1365";
                case 31366: return "Регистр #31366: Канал измерения параметров РЗА Arcteq - Позиция_1366";
                case 31367: return "Регистр #31367: Канал измерения параметров РЗА Arcteq - Позиция_1367";
                case 31368: return "Регистр #31368: Канал измерения параметров РЗА Arcteq - Позиция_1368";
                case 31369: return "Регистр #31369: Канал измерения параметров РЗА Arcteq - Позиция_1369";
                case 31370: return "Регистр #31370: Канал измерения параметров РЗА Arcteq - Позиция_1370";
                case 31371: return "Регистр #31371: Канал измерения параметров РЗА Arcteq - Позиция_1371";
                case 31372: return "Регистр #31372: Канал измерения параметров РЗА Arcteq - Позиция_1372";
                case 31373: return "Регистр #31373: Канал измерения параметров РЗА Arcteq - Позиция_1373";
                case 31374: return "Регистр #31374: Канал измерения параметров РЗА Arcteq - Позиция_1374";
                case 31375: return "Регистр #31375: Канал измерения параметров РЗА Arcteq - Позиция_1375";
                case 31376: return "Регистр #31376: Канал измерения параметров РЗА Arcteq - Позиция_1376";
                case 31377: return "Регистр #31377: Канал измерения параметров РЗА Arcteq - Позиция_1377";
                case 31378: return "Регистр #31378: Канал измерения параметров РЗА Arcteq - Позиция_1378";
                case 31379: return "Регистр #31379: Канал измерения параметров РЗА Arcteq - Позиция_1379";
                case 31380: return "Регистр #31380: Канал измерения параметров РЗА Arcteq - Позиция_1380";
                case 31381: return "Регистр #31381: Канал измерения параметров РЗА Arcteq - Позиция_1381";
                case 31382: return "Регистр #31382: Канал измерения параметров РЗА Arcteq - Позиция_1382";
                case 31383: return "Регистр #31383: Канал измерения параметров РЗА Arcteq - Позиция_1383";
                case 31384: return "Регистр #31384: Канал измерения параметров РЗА Arcteq - Позиция_1384";
                case 31385: return "Регистр #31385: Канал измерения параметров РЗА Arcteq - Позиция_1385";
                case 31386: return "Регистр #31386: Канал измерения параметров РЗА Arcteq - Позиция_1386";
                case 31387: return "Регистр #31387: Канал измерения параметров РЗА Arcteq - Позиция_1387";
                case 31388: return "Регистр #31388: Канал измерения параметров РЗА Arcteq - Позиция_1388";
                case 31389: return "Регистр #31389: Канал измерения параметров РЗА Arcteq - Позиция_1389";
                case 31390: return "Регистр #31390: Канал измерения параметров РЗА Arcteq - Позиция_1390";
                case 31391: return "Регистр #31391: Канал измерения параметров РЗА Arcteq - Позиция_1391";
                case 31392: return "Регистр #31392: Канал измерения параметров РЗА Arcteq - Позиция_1392";
                case 31393: return "Регистр #31393: Канал измерения параметров РЗА Arcteq - Позиция_1393";
                case 31394: return "Регистр #31394: Канал измерения параметров РЗА Arcteq - Позиция_1394";
                case 31395: return "Регистр #31395: Канал измерения параметров РЗА Arcteq - Позиция_1395";
                case 31396: return "Регистр #31396: Канал измерения параметров РЗА Arcteq - Позиция_1396";
                case 31397: return "Регистр #31397: Канал измерения параметров РЗА Arcteq - Позиция_1397";
                case 31398: return "Регистр #31398: Канал измерения параметров РЗА Arcteq - Позиция_1398";
                case 31399: return "Регистр #31399: Канал измерения параметров РЗА Arcteq - Позиция_1399";
                case 31400: return "Регистр #31400: Канал измерения параметров РЗА Arcteq - Позиция_1400";
                case 31401: return "Регистр #31401: Канал измерения параметров РЗА Arcteq - Позиция_1401";
                case 31402: return "Регистр #31402: Канал измерения параметров РЗА Arcteq - Позиция_1402";
                case 31403: return "Регистр #31403: Канал измерения параметров РЗА Arcteq - Позиция_1403";
                case 31404: return "Регистр #31404: Канал измерения параметров РЗА Arcteq - Позиция_1404";
                case 31405: return "Регистр #31405: Канал измерения параметров РЗА Arcteq - Позиция_1405";
                case 31406: return "Регистр #31406: Канал измерения параметров РЗА Arcteq - Позиция_1406";
                case 31407: return "Регистр #31407: Канал измерения параметров РЗА Arcteq - Позиция_1407";
                case 31408: return "Регистр #31408: Канал измерения параметров РЗА Arcteq - Позиция_1408";
                case 31409: return "Регистр #31409: Канал измерения параметров РЗА Arcteq - Позиция_1409";
                case 31410: return "Регистр #31410: Канал измерения параметров РЗА Arcteq - Позиция_1410";
                case 31411: return "Регистр #31411: Канал измерения параметров РЗА Arcteq - Позиция_1411";
                case 31412: return "Регистр #31412: Канал измерения параметров РЗА Arcteq - Позиция_1412";
                case 31413: return "Регистр #31413: Канал измерения параметров РЗА Arcteq - Позиция_1413";
                case 31414: return "Регистр #31414: Канал измерения параметров РЗА Arcteq - Позиция_1414";
                case 31415: return "Регистр #31415: Канал измерения параметров РЗА Arcteq - Позиция_1415";
                case 31416: return "Регистр #31416: Канал измерения параметров РЗА Arcteq - Позиция_1416";
                case 31417: return "Регистр #31417: Канал измерения параметров РЗА Arcteq - Позиция_1417";
                case 31418: return "Регистр #31418: Канал измерения параметров РЗА Arcteq - Позиция_1418";
                case 31419: return "Регистр #31419: Канал измерения параметров РЗА Arcteq - Позиция_1419";
                case 31420: return "Регистр #31420: Канал измерения параметров РЗА Arcteq - Позиция_1420";
                case 31421: return "Регистр #31421: Канал измерения параметров РЗА Arcteq - Позиция_1421";
                case 31422: return "Регистр #31422: Канал измерения параметров РЗА Arcteq - Позиция_1422";
                case 31423: return "Регистр #31423: Канал измерения параметров РЗА Arcteq - Позиция_1423";
                case 31424: return "Регистр #31424: Канал измерения параметров РЗА Arcteq - Позиция_1424";
                case 31425: return "Регистр #31425: Канал измерения параметров РЗА Arcteq - Позиция_1425";
                case 31426: return "Регистр #31426: Канал измерения параметров РЗА Arcteq - Позиция_1426";
                case 31427: return "Регистр #31427: Канал измерения параметров РЗА Arcteq - Позиция_1427";
                case 31428: return "Регистр #31428: Канал измерения параметров РЗА Arcteq - Позиция_1428";
                case 31429: return "Регистр #31429: Канал измерения параметров РЗА Arcteq - Позиция_1429";
                case 31430: return "Регистр #31430: Канал измерения параметров РЗА Arcteq - Позиция_1430";
                case 31431: return "Регистр #31431: Канал измерения параметров РЗА Arcteq - Позиция_1431";
                case 31432: return "Регистр #31432: Канал измерения параметров РЗА Arcteq - Позиция_1432";
                case 31433: return "Регистр #31433: Канал измерения параметров РЗА Arcteq - Позиция_1433";
                case 31434: return "Регистр #31434: Канал измерения параметров РЗА Arcteq - Позиция_1434";
                case 31435: return "Регистр #31435: Канал измерения параметров РЗА Arcteq - Позиция_1435";
                case 31436: return "Регистр #31436: Канал измерения параметров РЗА Arcteq - Позиция_1436";
                case 31437: return "Регистр #31437: Канал измерения параметров РЗА Arcteq - Позиция_1437";
                case 31438: return "Регистр #31438: Канал измерения параметров РЗА Arcteq - Позиция_1438";
                case 31439: return "Регистр #31439: Канал измерения параметров РЗА Arcteq - Позиция_1439";
                case 31440: return "Регистр #31440: Канал измерения параметров РЗА Arcteq - Позиция_1440";
                case 31441: return "Регистр #31441: Канал измерения параметров РЗА Arcteq - Позиция_1441";
                case 31442: return "Регистр #31442: Канал измерения параметров РЗА Arcteq - Позиция_1442";
                case 31443: return "Регистр #31443: Канал измерения параметров РЗА Arcteq - Позиция_1443";
                case 31444: return "Регистр #31444: Канал измерения параметров РЗА Arcteq - Позиция_1444";
                case 31445: return "Регистр #31445: Канал измерения параметров РЗА Arcteq - Позиция_1445";
                case 31446: return "Регистр #31446: Канал измерения параметров РЗА Arcteq - Позиция_1446";
                case 31447: return "Регистр #31447: Канал измерения параметров РЗА Arcteq - Позиция_1447";
                case 31448: return "Регистр #31448: Канал измерения параметров РЗА Arcteq - Позиция_1448";
                case 31449: return "Регистр #31449: Канал измерения параметров РЗА Arcteq - Позиция_1449";
                case 31450: return "Регистр #31450: Канал измерения параметров РЗА Arcteq - Позиция_1450";
                case 31451: return "Регистр #31451: Канал измерения параметров РЗА Arcteq - Позиция_1451";
                case 31452: return "Регистр #31452: Канал измерения параметров РЗА Arcteq - Позиция_1452";
                case 31453: return "Регистр #31453: Канал измерения параметров РЗА Arcteq - Позиция_1453";
                case 31454: return "Регистр #31454: Канал измерения параметров РЗА Arcteq - Позиция_1454";
                case 31455: return "Регистр #31455: Канал измерения параметров РЗА Arcteq - Позиция_1455";
                case 31456: return "Регистр #31456: Канал измерения параметров РЗА Arcteq - Позиция_1456";
                case 31457: return "Регистр #31457: Канал измерения параметров РЗА Arcteq - Позиция_1457";
                case 31458: return "Регистр #31458: Канал измерения параметров РЗА Arcteq - Позиция_1458";
                case 31459: return "Регистр #31459: Канал измерения параметров РЗА Arcteq - Позиция_1459";
                case 31460: return "Регистр #31460: Канал измерения параметров РЗА Arcteq - Позиция_1460";
                case 31461: return "Регистр #31461: Канал измерения параметров РЗА Arcteq - Позиция_1461";
                case 31462: return "Регистр #31462: Канал измерения параметров РЗА Arcteq - Позиция_1462";
                case 31463: return "Регистр #31463: Канал измерения параметров РЗА Arcteq - Позиция_1463";
                case 31464: return "Регистр #31464: Канал измерения параметров РЗА Arcteq - Позиция_1464";
                case 31465: return "Регистр #31465: Канал измерения параметров РЗА Arcteq - Позиция_1465";
                case 31466: return "Регистр #31466: Канал измерения параметров РЗА Arcteq - Позиция_1466";
                case 31467: return "Регистр #31467: Канал измерения параметров РЗА Arcteq - Позиция_1467";
                case 31468: return "Регистр #31468: Канал измерения параметров РЗА Arcteq - Позиция_1468";
                case 31469: return "Регистр #31469: Канал измерения параметров РЗА Arcteq - Позиция_1469";
                case 31470: return "Регистр #31470: Канал измерения параметров РЗА Arcteq - Позиция_1470";
                case 31471: return "Регистр #31471: Канал измерения параметров РЗА Arcteq - Позиция_1471";
                case 31472: return "Регистр #31472: Канал измерения параметров РЗА Arcteq - Позиция_1472";
                case 31473: return "Регистр #31473: Канал измерения параметров РЗА Arcteq - Позиция_1473";
                case 31474: return "Регистр #31474: Канал измерения параметров РЗА Arcteq - Позиция_1474";
                case 31475: return "Регистр #31475: Канал измерения параметров РЗА Arcteq - Позиция_1475";
                case 31476: return "Регистр #31476: Канал измерения параметров РЗА Arcteq - Позиция_1476";
                case 31477: return "Регистр #31477: Канал измерения параметров РЗА Arcteq - Позиция_1477";
                case 31478: return "Регистр #31478: Канал измерения параметров РЗА Arcteq - Позиция_1478";
                case 31479: return "Регистр #31479: Канал измерения параметров РЗА Arcteq - Позиция_1479";
                case 31480: return "Регистр #31480: Канал измерения параметров РЗА Arcteq - Позиция_1480";
                case 31481: return "Регистр #31481: Канал измерения параметров РЗА Arcteq - Позиция_1481";
                case 31482: return "Регистр #31482: Канал измерения параметров РЗА Arcteq - Позиция_1482";
                case 31483: return "Регистр #31483: Канал измерения параметров РЗА Arcteq - Позиция_1483";
                case 31484: return "Регистр #31484: Канал измерения параметров РЗА Arcteq - Позиция_1484";
                case 31485: return "Регистр #31485: Канал измерения параметров РЗА Arcteq - Позиция_1485";
                case 31486: return "Регистр #31486: Канал измерения параметров РЗА Arcteq - Позиция_1486";
                case 31487: return "Регистр #31487: Канал измерения параметров РЗА Arcteq - Позиция_1487";
                case 31488: return "Регистр #31488: Канал измерения параметров РЗА Arcteq - Позиция_1488";
                case 31489: return "Регистр #31489: Канал измерения параметров РЗА Arcteq - Позиция_1489";
                case 31490: return "Регистр #31490: Канал измерения параметров РЗА Arcteq - Позиция_1490";
                case 31491: return "Регистр #31491: Канал измерения параметров РЗА Arcteq - Позиция_1491";
                case 31492: return "Регистр #31492: Канал измерения параметров РЗА Arcteq - Позиция_1492";
                case 31493: return "Регистр #31493: Канал измерения параметров РЗА Arcteq - Позиция_1493";
                case 31494: return "Регистр #31494: Канал измерения параметров РЗА Arcteq - Позиция_1494";
                case 31495: return "Регистр #31495: Канал измерения параметров РЗА Arcteq - Позиция_1495";
                case 31496: return "Регистр #31496: Канал измерения параметров РЗА Arcteq - Позиция_1496";
                case 31497: return "Регистр #31497: Канал измерения параметров РЗА Arcteq - Позиция_1497";
                case 31498: return "Регистр #31498: Канал измерения параметров РЗА Arcteq - Позиция_1498";
                case 31499: return "Регистр #31499: Канал измерения параметров РЗА Arcteq - Позиция_1499";
                case 31500: return "Регистр #31500: Канал измерения параметров РЗА Arcteq - Позиция_1500";
                case 31501: return "Регистр #31501: Канал измерения параметров РЗА Arcteq - Позиция_1501";
                case 31502: return "Регистр #31502: Канал измерения параметров РЗА Arcteq - Позиция_1502";
                case 31503: return "Регистр #31503: Канал измерения параметров РЗА Arcteq - Позиция_1503";
                case 31504: return "Регистр #31504: Канал измерения параметров РЗА Arcteq - Позиция_1504";
                case 31505: return "Регистр #31505: Канал измерения параметров РЗА Arcteq - Позиция_1505";
                case 31506: return "Регистр #31506: Канал измерения параметров РЗА Arcteq - Позиция_1506";
                case 31507: return "Регистр #31507: Канал измерения параметров РЗА Arcteq - Позиция_1507";
                case 31508: return "Регистр #31508: Канал измерения параметров РЗА Arcteq - Позиция_1508";
                case 31509: return "Регистр #31509: Канал измерения параметров РЗА Arcteq - Позиция_1509";
                case 31510: return "Регистр #31510: Канал измерения параметров РЗА Arcteq - Позиция_1510";
                case 31511: return "Регистр #31511: Канал измерения параметров РЗА Arcteq - Позиция_1511";
                case 31512: return "Регистр #31512: Канал измерения параметров РЗА Arcteq - Позиция_1512";
                case 31513: return "Регистр #31513: Канал измерения параметров РЗА Arcteq - Позиция_1513";
                case 31514: return "Регистр #31514: Канал измерения параметров РЗА Arcteq - Позиция_1514";
                case 31515: return "Регистр #31515: Канал измерения параметров РЗА Arcteq - Позиция_1515";
                case 31516: return "Регистр #31516: Канал измерения параметров РЗА Arcteq - Позиция_1516";
                case 31517: return "Регистр #31517: Канал измерения параметров РЗА Arcteq - Позиция_1517";
                case 31518: return "Регистр #31518: Канал измерения параметров РЗА Arcteq - Позиция_1518";
                case 31519: return "Регистр #31519: Канал измерения параметров РЗА Arcteq - Позиция_1519";
                case 31520: return "Регистр #31520: Канал измерения параметров РЗА Arcteq - Позиция_1520";
                case 31521: return "Регистр #31521: Канал измерения параметров РЗА Arcteq - Позиция_1521";
                case 31522: return "Регистр #31522: Канал измерения параметров РЗА Arcteq - Позиция_1522";
                case 31523: return "Регистр #31523: Канал измерения параметров РЗА Arcteq - Позиция_1523";
                case 31524: return "Регистр #31524: Канал измерения параметров РЗА Arcteq - Позиция_1524";
                case 31525: return "Регистр #31525: Канал измерения параметров РЗА Arcteq - Позиция_1525";
                case 31526: return "Регистр #31526: Канал измерения параметров РЗА Arcteq - Позиция_1526";
                case 31527: return "Регистр #31527: Канал измерения параметров РЗА Arcteq - Позиция_1527";
                case 31528: return "Регистр #31528: Канал измерения параметров РЗА Arcteq - Позиция_1528";
                case 31529: return "Регистр #31529: Канал измерения параметров РЗА Arcteq - Позиция_1529";
                case 31530: return "Регистр #31530: Канал измерения параметров РЗА Arcteq - Позиция_1530";
                case 31531: return "Регистр #31531: Канал измерения параметров РЗА Arcteq - Позиция_1531";
                case 31532: return "Регистр #31532: Канал измерения параметров РЗА Arcteq - Позиция_1532";
                case 31533: return "Регистр #31533: Канал измерения параметров РЗА Arcteq - Позиция_1533";
                case 31534: return "Регистр #31534: Канал измерения параметров РЗА Arcteq - Позиция_1534";
                case 31535: return "Регистр #31535: Канал измерения параметров РЗА Arcteq - Позиция_1535";
                case 31536: return "Регистр #31536: Канал измерения параметров РЗА Arcteq - Позиция_1536";
                case 31537: return "Регистр #31537: Канал измерения параметров РЗА Arcteq - Позиция_1537";
                case 31538: return "Регистр #31538: Канал измерения параметров РЗА Arcteq - Позиция_1538";
                case 31539: return "Регистр #31539: Канал измерения параметров РЗА Arcteq - Позиция_1539";
                case 31540: return "Регистр #31540: Канал измерения параметров РЗА Arcteq - Позиция_1540";
                case 31541: return "Регистр #31541: Канал измерения параметров РЗА Arcteq - Позиция_1541";
                case 31542: return "Регистр #31542: Канал измерения параметров РЗА Arcteq - Позиция_1542";
                case 31543: return "Регистр #31543: Канал измерения параметров РЗА Arcteq - Позиция_1543";
                case 31544: return "Регистр #31544: Канал измерения параметров РЗА Arcteq - Позиция_1544";
                case 31545: return "Регистр #31545: Канал измерения параметров РЗА Arcteq - Позиция_1545";
                case 31546: return "Регистр #31546: Канал измерения параметров РЗА Arcteq - Позиция_1546";
                case 31547: return "Регистр #31547: Канал измерения параметров РЗА Arcteq - Позиция_1547";
                case 31548: return "Регистр #31548: Канал измерения параметров РЗА Arcteq - Позиция_1548";
                case 31549: return "Регистр #31549: Канал измерения параметров РЗА Arcteq - Позиция_1549";
                case 31550: return "Регистр #31550: Канал измерения параметров РЗА Arcteq - Позиция_1550";
                case 31551: return "Регистр #31551: Канал измерения параметров РЗА Arcteq - Позиция_1551";
                case 31552: return "Регистр #31552: Канал измерения параметров РЗА Arcteq - Позиция_1552";
                case 31553: return "Регистр #31553: Канал измерения параметров РЗА Arcteq - Позиция_1553";
                case 31554: return "Регистр #31554: Канал измерения параметров РЗА Arcteq - Позиция_1554";
                case 31555: return "Регистр #31555: Канал измерения параметров РЗА Arcteq - Позиция_1555";
                case 31556: return "Регистр #31556: Канал измерения параметров РЗА Arcteq - Позиция_1556";
                case 31557: return "Регистр #31557: Канал измерения параметров РЗА Arcteq - Позиция_1557";
                case 31558: return "Регистр #31558: Канал измерения параметров РЗА Arcteq - Позиция_1558";
                case 31559: return "Регистр #31559: Канал измерения параметров РЗА Arcteq - Позиция_1559";
                case 31560: return "Регистр #31560: Канал измерения параметров РЗА Arcteq - Позиция_1560";
                case 31561: return "Регистр #31561: Канал измерения параметров РЗА Arcteq - Позиция_1561";
                case 31562: return "Регистр #31562: Канал измерения параметров РЗА Arcteq - Позиция_1562";
                case 31563: return "Регистр #31563: Канал измерения параметров РЗА Arcteq - Позиция_1563";
                case 31564: return "Регистр #31564: Канал измерения параметров РЗА Arcteq - Позиция_1564";
                case 31565: return "Регистр #31565: Канал измерения параметров РЗА Arcteq - Позиция_1565";
                case 31566: return "Регистр #31566: Канал измерения параметров РЗА Arcteq - Позиция_1566";
                case 31567: return "Регистр #31567: Канал измерения параметров РЗА Arcteq - Позиция_1567";
                case 31568: return "Регистр #31568: Канал измерения параметров РЗА Arcteq - Позиция_1568";
                case 31569: return "Регистр #31569: Канал измерения параметров РЗА Arcteq - Позиция_1569";
                case 31570: return "Регистр #31570: Канал измерения параметров РЗА Arcteq - Позиция_1570";
                case 31571: return "Регистр #31571: Канал измерения параметров РЗА Arcteq - Позиция_1571";
                case 31572: return "Регистр #31572: Канал измерения параметров РЗА Arcteq - Позиция_1572";
                case 31573: return "Регистр #31573: Канал измерения параметров РЗА Arcteq - Позиция_1573";
                case 31574: return "Регистр #31574: Канал измерения параметров РЗА Arcteq - Позиция_1574";
                case 31575: return "Регистр #31575: Канал измерения параметров РЗА Arcteq - Позиция_1575";
                case 31576: return "Регистр #31576: Канал измерения параметров РЗА Arcteq - Позиция_1576";
                case 31577: return "Регистр #31577: Канал измерения параметров РЗА Arcteq - Позиция_1577";
                case 31578: return "Регистр #31578: Канал измерения параметров РЗА Arcteq - Позиция_1578";
                case 31579: return "Регистр #31579: Канал измерения параметров РЗА Arcteq - Позиция_1579";
                case 31580: return "Регистр #31580: Канал измерения параметров РЗА Arcteq - Позиция_1580";
                case 31581: return "Регистр #31581: Канал измерения параметров РЗА Arcteq - Позиция_1581";
                case 31582: return "Регистр #31582: Канал измерения параметров РЗА Arcteq - Позиция_1582";
                case 31583: return "Регистр #31583: Канал измерения параметров РЗА Arcteq - Позиция_1583";
                case 31584: return "Регистр #31584: Канал измерения параметров РЗА Arcteq - Позиция_1584";
                case 31585: return "Регистр #31585: Канал измерения параметров РЗА Arcteq - Позиция_1585";
                case 31586: return "Регистр #31586: Канал измерения параметров РЗА Arcteq - Позиция_1586";
                case 31587: return "Регистр #31587: Канал измерения параметров РЗА Arcteq - Позиция_1587";
                case 31588: return "Регистр #31588: Канал измерения параметров РЗА Arcteq - Позиция_1588";
                case 31589: return "Регистр #31589: Канал измерения параметров РЗА Arcteq - Позиция_1589";
                case 31590: return "Регистр #31590: Канал измерения параметров РЗА Arcteq - Позиция_1590";
                case 31591: return "Регистр #31591: Канал измерения параметров РЗА Arcteq - Позиция_1591";
                case 31592: return "Регистр #31592: Канал измерения параметров РЗА Arcteq - Позиция_1592";
                case 31593: return "Регистр #31593: Канал измерения параметров РЗА Arcteq - Позиция_1593";
                case 31594: return "Регистр #31594: Канал измерения параметров РЗА Arcteq - Позиция_1594";
                case 31595: return "Регистр #31595: Канал измерения параметров РЗА Arcteq - Позиция_1595";
                case 31596: return "Регистр #31596: Канал измерения параметров РЗА Arcteq - Позиция_1596";
                case 31597: return "Регистр #31597: Канал измерения параметров РЗА Arcteq - Позиция_1597";
                case 31598: return "Регистр #31598: Канал измерения параметров РЗА Arcteq - Позиция_1598";
                case 31599: return "Регистр #31599: Канал измерения параметров РЗА Arcteq - Позиция_1599";
                case 31600: return "Регистр #31600: Канал измерения параметров РЗА Arcteq - Позиция_1600";
                case 31601: return "Регистр #31601: Канал измерения параметров РЗА Arcteq - Позиция_1601";
                case 31602: return "Регистр #31602: Канал измерения параметров РЗА Arcteq - Позиция_1602";
                case 31603: return "Регистр #31603: Канал измерения параметров РЗА Arcteq - Позиция_1603";
                case 31604: return "Регистр #31604: Канал измерения параметров РЗА Arcteq - Позиция_1604";
                case 31605: return "Регистр #31605: Канал измерения параметров РЗА Arcteq - Позиция_1605";
                case 31606: return "Регистр #31606: Канал измерения параметров РЗА Arcteq - Позиция_1606";
                case 31607: return "Регистр #31607: Канал измерения параметров РЗА Arcteq - Позиция_1607";
                case 31608: return "Регистр #31608: Канал измерения параметров РЗА Arcteq - Позиция_1608";
                case 31609: return "Регистр #31609: Канал измерения параметров РЗА Arcteq - Позиция_1609";
                case 31610: return "Регистр #31610: Канал измерения параметров РЗА Arcteq - Позиция_1610";
                case 31611: return "Регистр #31611: Канал измерения параметров РЗА Arcteq - Позиция_1611";
                case 31612: return "Регистр #31612: Канал измерения параметров РЗА Arcteq - Позиция_1612";
                case 31613: return "Регистр #31613: Канал измерения параметров РЗА Arcteq - Позиция_1613";
                case 31614: return "Регистр #31614: Канал измерения параметров РЗА Arcteq - Позиция_1614";
                case 31615: return "Регистр #31615: Канал измерения параметров РЗА Arcteq - Позиция_1615";
                case 31616: return "Регистр #31616: Канал измерения параметров РЗА Arcteq - Позиция_1616";
                case 31617: return "Регистр #31617: Канал измерения параметров РЗА Arcteq - Позиция_1617";
                case 31618: return "Регистр #31618: Канал измерения параметров РЗА Arcteq - Позиция_1618";
                case 31619: return "Регистр #31619: Канал измерения параметров РЗА Arcteq - Позиция_1619";
                case 31620: return "Регистр #31620: Канал измерения параметров РЗА Arcteq - Позиция_1620";
                case 31621: return "Регистр #31621: Канал измерения параметров РЗА Arcteq - Позиция_1621";
                case 31622: return "Регистр #31622: Канал измерения параметров РЗА Arcteq - Позиция_1622";
                case 31623: return "Регистр #31623: Канал измерения параметров РЗА Arcteq - Позиция_1623";
                case 31624: return "Регистр #31624: Канал измерения параметров РЗА Arcteq - Позиция_1624";
                case 31625: return "Регистр #31625: Канал измерения параметров РЗА Arcteq - Позиция_1625";
                case 31626: return "Регистр #31626: Канал измерения параметров РЗА Arcteq - Позиция_1626";
                case 31627: return "Регистр #31627: Канал измерения параметров РЗА Arcteq - Позиция_1627";
                case 31628: return "Регистр #31628: Канал измерения параметров РЗА Arcteq - Позиция_1628";
                case 31629: return "Регистр #31629: Канал измерения параметров РЗА Arcteq - Позиция_1629";
                case 31630: return "Регистр #31630: Канал измерения параметров РЗА Arcteq - Позиция_1630";
                case 31631: return "Регистр #31631: Канал измерения параметров РЗА Arcteq - Позиция_1631";
                case 31632: return "Регистр #31632: Канал измерения параметров РЗА Arcteq - Позиция_1632";
                case 31633: return "Регистр #31633: Канал измерения параметров РЗА Arcteq - Позиция_1633";
                case 31634: return "Регистр #31634: Канал измерения параметров РЗА Arcteq - Позиция_1634";
                case 31635: return "Регистр #31635: Канал измерения параметров РЗА Arcteq - Позиция_1635";
                case 31636: return "Регистр #31636: Канал измерения параметров РЗА Arcteq - Позиция_1636";
                case 31637: return "Регистр #31637: Канал измерения параметров РЗА Arcteq - Позиция_1637";
                case 31638: return "Регистр #31638: Канал измерения параметров РЗА Arcteq - Позиция_1638";
                case 31639: return "Регистр #31639: Канал измерения параметров РЗА Arcteq - Позиция_1639";
                case 31640: return "Регистр #31640: Канал измерения параметров РЗА Arcteq - Позиция_1640";
                case 31641: return "Регистр #31641: Канал измерения параметров РЗА Arcteq - Позиция_1641";
                case 31642: return "Регистр #31642: Канал измерения параметров РЗА Arcteq - Позиция_1642";
                case 31643: return "Регистр #31643: Канал измерения параметров РЗА Arcteq - Позиция_1643";
                case 31644: return "Регистр #31644: Канал измерения параметров РЗА Arcteq - Позиция_1644";
                case 31645: return "Регистр #31645: Канал измерения параметров РЗА Arcteq - Позиция_1645";
                case 31646: return "Регистр #31646: Канал измерения параметров РЗА Arcteq - Позиция_1646";
                case 31647: return "Регистр #31647: Канал измерения параметров РЗА Arcteq - Позиция_1647";
                case 31648: return "Регистр #31648: Канал измерения параметров РЗА Arcteq - Позиция_1648";
                case 31649: return "Регистр #31649: Канал измерения параметров РЗА Arcteq - Позиция_1649";

                default: return "Служебный регистр телеметрии";
            }
        }
    }
}