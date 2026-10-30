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
║ 🚀 ARCTEQ PRO COMBINER ENGINE v3.1 ULTRA - УНИВЕРСАЛЬНЫЙ СПРАВОЧНИК РЗА С ПОДМЕНЮ БЛОЧКОВ   ║
║ Автор: Dziuba Roman                                                                       ║
║ Описание: Многоуровневый комбайн с иерархией: Список приборов -> Меню категорий ->        ║
║ Подменю аппаратных блочков -> Глубокая детализация конфигурации каждого блока с примерами.  ║
║ Исправлены все артефакты разметки, текст очищен и максимально расширен для инженеров РЗА.  ║
╚═══════════════════════════════════════════════════════════════════════════════════════════╝
*/
public class MainActivity extends AppCompatActivity {

    // СТРУКТУРА ДАННЫХ АППАРАТНОГО БЛОКА С ДЕТАЛИЗАЦИЕЙ КОНФИГУРАЦИИ И ПРИМЕРАМИ
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
    ИНИЦИАЛИЗАЦИЯ БАЗЫ ДАННЫХ И ДЕТАЛЬНЫХ БЛОЧКОВ (БЕЗ АРТЕФАКТОВ И С МАКСИМАЛЬНЫМ РАСШИРЕНИЕМ)
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
            "Аппаратный комплекс и встроенное микропрограммное обеспечение реализуют следующий расширенный массив защит:\n" +
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
            /* РУКОВОДСТВО ПО КОНФИГУРАЦИИ */
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
            /* ИНЖЕНЕРНЫЕ СЕКРЕТЫ */
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

        // --- ДЕТАЛЬНЫЕ АППАРАТНЫЕ БЛОЧКИ AQ-F255 С МАКСИМАЛЬНЫМ РАСШИРЕНИЕМ ПОДПУНКТОВ ---
        aqF255.hardwareBlocks.add(new HardwareBlock(
            "1. Модуль процессора и питания (Main CPU Card)",
            /* Что включает в себя */
            "• Высокопроизводительный 32-разрядный RISC-микроконтроллер с аппаратным ускорителем вычислений для обработки матриц защит и логики в режиме реального времени.\n" +
            "• Энергонезависимая Flash и NVRAM память: предназначена для хранения операционной системы, файлов конфигурации (.aqt), журнала аварийных событий (Event Log) емкостью до 10 000 записей и осциллограмм аварий (COMTRADE).\n" +
            "• Аппаратные часы реального времени (RTC) с супервизором питания и резервной литиевой батареей (хранение хода часов при полном пропадании оперативного тока до 5 лет).\n" +
            "• Интерфейсы связи на плате: выделенный сервисный порт RJ45 Ethernet на лицевой панели (для подключения ноутбука с ПО AQtivate) и тыловой изолированный интерфейс RS-485 (протоколы Modbus RTU, IEC 60870-5-103).\n" +
            "• Специализированное аппаратное реле системного отказа (IRF — Internal Relay Failure): электромеханический сухой контакт, непрерывно замкнутый при нормальной работе и размыкающийся при критическом сбое ПО, зависании процессора или исчезновении питающих напряжений.",
            /* Как конфигурировать */
            "1. В программном комплексе AQtivate откройте раздел 'Device Settings' -> 'Communication & Network Parameters'.\n" +
            "2. Установите статический IP-адрес терминала (например, 192.168.2.10), маску подсети (255.255.255.0) и основной шлюз в соответствии с сетевой архитектурой подстанции.\n" +
            "3. В подразделе 'Time Synchronization' выберите источник синхронизации времени: SNTP сервер по сети Ethernet или импульсный вход IRIG-B/PPS для обеспечения единой шкалы времени с АСУ ТП (погрешность не более 1 мс).\n" +
            "4. В матрице дискретных выходов ('Output Matrix') найдите сигнал аппаратного отказа ('System Error / IRF') и назначьте его на свободное дискретное реле (например, DO7) для вывода аварии на щит диспетчера.",
            /* Практический пример */
            "Практический кейс настройки:\n" +
            "Настройка IP-адреса узла: IP = 10.20.30.50, Маска = 255.255.255.0, Порт связи = 20001. Синхронизация времени настроена по протоколу SNTP от сервера подстанции 10.20.30.1 с периодом опроса 60 секунд. При внутреннем сбое контроллера контакт реле IRF размыкается, включая общую лампу 'Неисправность РЗА'."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "2. Измерительный аналоговый модуль (Analog Input Card)",
            /* Что включает в себя */
            "• Высокоточные многоканальные аналого-цифровые преобразователи (АЦП) с частотой дискретизации 64 точки на период промышленной частоты (50/60 Гц), обеспечивающие высокую точность выделения гармоник.\n" +
            "• 5 измерительных каналов тока (ТТ): 3 фазных канала (IL1, IL2, IL3) с номиналами 1А или 5А и перегрузочной способностью до 100*In в течение 1 секунды; 1 высокочувствительный канал нулевой последовательности (Io) для селективной защиты от замыканий на землю; 1 резервный измерительный канал.\n" +
            "• 4 измерительных канала напряжения (ТН): 3 фазных канала (U1, U2, U3) для контроля междуфазных и фазных напряжений защищаемой секции; 1 выделенный канал (3Uo) для подключения обмотки разомкнутого треугольника измерительных ТН.",
            /* Как конфигурировать */
            "1. В ПО AQtivate перейдите в дерево конфигурации во вкладку 'Analog Inputs' -> 'Scaling & Ratings'.\n" +
            "2. Для фазных трансформаторов тока (IL1-IL3) введите номинальные первичные и вторичные токи измерительного ядра (например, Первичный = 600 А, Вторичный = 5 А).\n" +
            "3. Для канала тока нулевой последовательности (Io) укажите точный коэффициент трансформации внешнего кабельного ТТ (ТЗЛР), например 30/1 А или 50/5 А.\n" +
            "4. Для каналов напряжения (U1-U3) задайте номинальные коэффициенты трансформации силовых ТН (например, Первичное напряжение = 35000 В, Вторичное = 100 В).\n" +
            "5. При необходимости коррекции угловой погрешности измерительных трансформаторов задайте угол поворота векторов в настройках фазировки каналов.",
            /* Практический пример */
            "Практический кейс настройки:\n" +
            "Параметры для линии 35 кВ: ТТ линии 600/5 А (в ПО: IL1-3 Prim = 600, Sec = 5). ТН 35 кВ: 35000/100 В (в ПО: U Prim = 35000, Sec = 100). Кабельный ТТНП (ТЗЛР): 30/1 А (в ПО: Io Prim = 30, Sec = 1). Это позволяет микропроцессору корректно оперировать абсолютными значениями первичных токов и напряжений во всех алгоритмах защит."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "3. Дискретный модуль ввода-вывода (DI/DO Card)",
            /* Что включает в себя */
            "• 8 дискретных входов (DI1–DI8) с гальванической развязкой на оптопарах, защитой от импульсных наводок и настраиваемым программным фильтром дребезга контактов (номинальное напряжение срабатывания 110В / 220В DC/AC).\n" +
            "• 7 выходных электромеханических реле (DO1–DO7) с мощными переключающими и замыкающими контактами, способными коммутировать постоянный ток до 8 А при индуктивной нагрузке и выдерживать кратковременные броски до 30 А.\n" +
            "• Светодиодная индикация на печатной плате для визуального контроля срабатывания каждого дискретного входа и выхода в реальном времени.",
            /* Как конфигурировать */
            "1. В ПО AQtivate откройте раздел 'Binary Inputs' (Дискретные входы) и назначьте логическое имя каждому входу (например, DI1 — 'Команда отключения от газового реле', DI2 — 'Контроль положения выключателя ПВ', DI3 — 'Блокировка АПВ').\n" +
            "2. Установите время фильтрации дребезга контактов (типовое значение 10–20 мс для отсечения наводок в длинных кабельных линиях связи со щитом управления).\n" +
            "3. Перейдите в матрицу логических связей ('Output Matrix / Signal Matrix').\n" +
            "4. Соедините пусковые органы и выходные ступени защит (например, 'DMT1 Trip', 'Dist Zone1 Trip', 'EF1 Trip') с физическими выходными реле DO1...DO7. Учтите, что реле DO1 всегда выделяется под основную цепь отключения выключателя.",
            /* Практический пример */
            "Практический кейс настройки:\n" +
            "Вход DI1 конфигурируется как 'External Trip' (Внешний аварийный останов). В матрице логики сигнал 'DMT2 Trip' (Срабатывание второй ступени МТЗ) связывается с реле DO1 (отключение выключателя) и реле DO2 (пуск устройства резервирования отказа выключателя — УРОВ)."
        ));

        aqF255.hardwareBlocks.add(new HardwareBlock(
            "4. Плата связи и МЭК 61850 (COM Board)",
            /* Что включает в себя */
            "• Сменный высокоскоростной коммуникационный Ethernet-модуль для интеграции терминала в цифровые подстанции стандарта МЭК 61850.\n" +
            "• Поддержка стека протоколов МЭК 61850-8-1 (передача данных по протоколу MMS для АСУ ТП, обмен горизонтальными GOOSE-сообщениями между терминалами).\n" +
            "• Поддержка резервированных сетей Ethernet по протоколам PRP (Parallel Redundancy Protocol) и HSR (High-availability Seamless Redundancy) с нулевым временем восстановления при обрыве кабеля.\n" +
            "• Поддержка классических протоколов телемеханики Modbus TCP и IEC 60870-5-104 для работы со сторонними SCADA-системами.",
            /* Как конфигурировать */
            "1. Импортируйте или экспортируйте стандартный SCL / CID файл конфигурации подстанции с помощью встроенного модуля 'IEC 61850 Configurator' в ПО AQtivate.\n" +
            "2. Настройте параметры издателей (Publishers) и подписчиков (Subscribers) GOOSE-сообщений для реализации быстрых межтерминальных блокировок (например, логическая защита шин — ЛЗШ, или запрет АПВ при внешнем КЗ).\n" +
            "3. Присвойте уникальный идентификатор AppID и настройте приоритет тегов VLAN (рекомендуется приоритет 4 или 5 для трафика РЗА), чтобы сетевые коммутаторы подстанции обрабатывали пакеты релейной защиты вне очереди.",
            /* Практический пример */
            "Практический кейс настройки:\n" +
            "Настройка GOOSE-канала между терминалом AQ-F255 отходящей линии и терминалом ввода 35 кВ для передачи сигнала блокировки ввода при повреждении на линии. GOOSE AppID задается как 0x0001, VLAN ID = 20, время повтора сообщения (MinTime = 2ms, MaxTime = 1000ms)."
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
            "Содержит 4 оптических канала для подключения точечных датчиков типа AQ-01.",
            "Подключение датчиков в клеммные разъемы X1-X4. Калибровка и запоминание датчиков кнопкой 'Auto Setup' на лицевой панели.",
            "Пример: Подключение 4 датчиков: отсек выключателя, кабельный отсек, отсек шин верхний, отсек шин нижний."
        ));
        aq101.hardwareBlocks.add(new HardwareBlock(
            "2. Модуль полупроводниковых ключей (Triac Out)",
            "Высокоскоростные тиристорные ключи для безинерционной выдачи команды на отключение выключателя.",
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
            "Содержит три трансформатора тока для контроля фаз и логического совпадения с оптикой.",
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
            "Плата для подключения термосопротивлений Pt100 контроля подшипников и обмоток.",
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
            "Увеличенное число аналоговых входов для сторон ВН и НН трансформатора.",
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
            "Интерфейс связи по волоконно-оптической линии до 120 км.",
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