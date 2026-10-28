package com.novijkompiljator.app;
​import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.;
import org.json.JSONArray;
import org.json.JSONObject;
​import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
​/
​MONSTER COMPILER CI/CD ENGINE v2.1 AUTOMATED PRO
​Pure ASCII String Constants & Strict Sanitization Engine
​Author: Dziuba Roman
*/
public class MainActivity extends Activity {
​private static final String PREFS_NAME = "AppConfigMonster";
private static final int PICK_ICON_REQUEST = 1001;
private static final int PICK_JAVA_REQUEST = 1002;
private static final int PICK_APK_REQUEST = 1003;
private static final int PICK_FILE_TO_FOLDER_REQUEST = 1004;
​private String githubToken = "";
private String botToken = "" + new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT)) + "";
private String chatId = "1434233986";
private String repoOwner = "RomanDziuba";
private String repoName = "binance-scanner-app";
​private String telegramApkTitle = "RomaMonsterApp";
private String appVersion = "001";
private int versionCode = 1;
private String packageName = "com.roman.version001";
private String currentJavaFileName = "CustomApp";
private boolean iconCustomized = true;
private String targetUploadFolder = "";
​private int minSdk = 21;
private int compileSdk = 34;
private String javaVersion = "8";
private String activeCodeContent = "";
private long latestRunId = -1;
​private List<Integer> errorLineNumbers = new ArrayList<>();
​private TextView statusTextView, infoTextView;
private EditText logEditText;
private Button btnBuild, btnStopBuild, btnUploadJava, btnDecompileApk, btnTitle, btnVersion, btnPackageName, btnIcon, btnSettings, btnCopyLogs, btnCodeEditor, btnFileManager, btnRetryUpload;
​private Handler handler = new Handler(Looper.getMainLooper());
private volatile boolean isMonitoring = false;
private String lastErrorLog = "";
private byte[] lastFailedBytes = null;
private String lastFailedPath = "";
​public static class RepoItem {
public String path;
public String type;
public String sha;
public long size;
​public RepoItem(String path, String type, String sha, long size) {
this.path = path;
this.type = type;
this.sha = sha;
this.size = size;
}
}
​@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
loadSettings();
​if (activeCodeContent.isEmpty()) {
activeCodeContent = generateClockTemplate();
}
​updatePackageNameFromCode(activeCodeContent);
activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
​LinearLayout rootLayout = new LinearLayout(this);
rootLayout.setOrientation(LinearLayout.VERTICAL);
rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
rootLayout.setBackgroundColor(Color.parseColor("#030712"));
rootLayout.setPadding(16, 16, 16, 16);
​ScrollView mainScroll = new ScrollView(this);
mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
));
​LinearLayout container = new LinearLayout(this);
container.setOrientation(LinearLayout.VERTICAL);
container.setGravity(Gravity.CENTER_HORIZONTAL);
​TextView authorView = new TextView(this);
authorView.setText("Dziuba Roman - Monster CI/CD Engine v2.1 Pro Auto");
authorView.setTextColor(Color.parseColor("#F59E0B"));
authorView.setTextSize(15f);
authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
authorView.setGravity(Gravity.CENTER);
authorView.setPadding(0, 5, 0, 8);
container.addView(authorView);
​LinearLayout card = new LinearLayout(this);
card.setOrientation(LinearLayout.VERTICAL);
card.setPadding(20, 20, 20, 20);
​GradientDrawable cardBg = new GradientDrawable();
cardBg.setColor(Color.parseColor("#0F172A"));
cardBg.setCornerRadius(24f);
cardBg.setStroke(2, Color.parseColor("#1E293B"));
card.setBackground(cardBg);
​LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
);
cardParams.setMargins(0, 0, 0, 16);
card.setLayoutParams(cardParams);
​TextView titleView = new TextView(this);
titleView.setText("Android Monster Compiler (Auto-Healing Engine)");
titleView.setTextColor(Color.parseColor("#38BDF8"));
titleView.setTextSize(16f);
titleView.setTypeface(Typeface.DEFAULT_BOLD);
titleView.setGravity(Gravity.CENTER);
titleView.setPadding(0, 0, 0, 10);
card.addView(titleView);
​infoTextView = new TextView(this);
infoTextView.setTextColor(Color.parseColor("#94A3B8"));
infoTextView.setTextSize(11f);
infoTextView.setPadding(12, 10, 12, 10);
​GradientDrawable infoBg = new GradientDrawable();
infoBg.setColor(Color.parseColor("#1E293B"));
infoBg.setCornerRadius(12f);
infoTextView.setBackground(infoBg);
​LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
);
infoParams.setMargins(0, 0, 0, 10);
infoTextView.setLayoutParams(infoParams);
card.addView(infoTextView);
​btnBuild = createStyledButton("", "#1D4ED8");
btnBuild.setOnClickListener(v -> triggerGitHubBuild());
card.addView(btnBuild);
​btnVersion = createStyledButton("", "#334155");
btnVersion.setOnClickListener(v -> showEditVersionDialog());
card.addView(btnVersion);
​btnTitle = createStyledButton("", "#334155");
btnTitle.setOnClickListener(v -> showEditTitleDialog());
card.addView(btnTitle);
​btnPackageName = createStyledButton("", "#0F766E");
btnPackageName.setOnClickListener(v -> showEditPackageDialog());
card.addView(btnPackageName);
​btnIcon = createStyledButton("", "#5B21B6");
btnIcon.setOnClickListener(v -> openIconFilePicker());
card.addView(btnIcon);
​btnCodeEditor = createStyledButton("[EDIT] Redaktor koda (Horiz-Scroll)", "#0F172A");
btnCodeEditor.setOnClickListener(v -> openCodeEditorDialog());
card.addView(btnCodeEditor);
​btnUploadJava = createStyledButton("", "#115E59");
btnUploadJava.setOnClickListener(v -> openJavaFilePicker());
card.addView(btnUploadJava);
​btnRetryUpload = createStyledButton("[RETRY] Povtorit otpravku fayla na server", "#B45309");
btnRetryUpload.setVisibility(View.GONE);
btnRetryUpload.setOnClickListener(v -> retryLastUpload());
card.addView(btnRetryUpload);
​btnDecompileApk = createStyledButton("[DEC] Dekompilirovat fayl ili APK v kod", "#475569");
btnDecompileApk.setOnClickListener(v -> openUniversalFilePicker());
card.addView(btnDecompileApk);
​btnStopBuild = createStyledButton("[STOP] Ekstrennyy stop sborki", "#B91C1C");
btnStopBuild.setOnClickListener(v -> emergencyStopBuild());
card.addView(btnStopBuild);
​btnFileManager = createStyledButton("[FILES] Faylovyy menedzher repozitoriya", "#6D28D9");
btnFileManager.setOnClickListener(v -> openRepositoryFileManager());
card.addView(btnFileManager);
​btnSettings = createStyledButton("[SET] Nastroyki GitHub, Telegram i SDK", "#1E293B");
btnSettings.setOnClickListener(v -> showSettingsDialog());
card.addView(btnSettings);
​statusTextView = new TextView(this);
statusTextView.setText("Status: Gotov k rabote");
statusTextView.setTextColor(Color.parseColor("#10B981"));
statusTextView.setTextSize(12f);
statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
statusTextView.setGravity(Gravity.CENTER);
statusTextView.setPadding(0, 10, 0, 6);
card.addView(statusTextView);
​TextView logLabel = new TextView(this);
logLabel.setText("Professionalnyy terminal i otchet ob oshibkakh:");
logLabel.setTextColor(Color.parseColor("#64748B"));
logLabel.setTextSize(11f);
logLabel.setPadding(0, 6, 0, 4);
card.addView(logLabel);
​logEditText = new EditText(this);
logEditText.setHint("Zdes vyvodyatsya strukturirovannye logi...");
logEditText.setTextSize(10f);
logEditText.setTextColor(Color.parseColor("#E2E8F0"));
logEditText.setHintTextColor(Color.parseColor("#475569"));
logEditText.setBackgroundColor(Color.parseColor("#030712"));
logEditText.setPadding(10, 10, 10, 10);
logEditText.setGravity(Gravity.TOP);
logEditText.setMinLines(8);
logEditText.setMaxLines(15);
card.addView(logEditText);
​btnCopyLogs = createStyledButton("[COPY] Skopirovat log v bufer", "#334155");
btnCopyLogs.setOnClickListener(v -> {
ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
ClipData clip = ClipData.newPlainText("Monster Logs", logEditText.getText().toString());
clipboard.setPrimaryClip(clip);
Toast.makeText(this, "Logi skopirovany!", Toast.LENGTH_SHORT).show();
});
card.addView(btnCopyLogs);
​container.addView(card);
mainScroll.addView(container);
rootLayout.addView(mainScroll);
setContentView(rootLayout);
​updateAllButtonLabels();
logEditText.setText("=== MONSTER COMPILER AUTOMATED ENGINE V2.1 ===\n" +
"* Imya dlya Telegram APK: " + telegramApkTitle + "\n" +
"* Paket: " + packageName + "\n" +
"* Versiya: " + appVersion + " (Code: " + versionCode + ")\n" +
"* GitHub Token: " + (githubToken.isEmpty() ? "[!] Ne vveden" : "[OK] Zagruzhen"));
​if (!githubToken.isEmpty()) {
fetchRemoteRepositoryInfo();
}
}
​private Button createStyledButton(String text, String colorHex) {
Button btn = new Button(this);
btn.setText(text);
btn.setTextColor(Color.parseColor("#F8FAFC"));
btn.setTextSize(11f);
btn.setTypeface(Typeface.DEFAULT_BOLD);
btn.setBackgroundColor(Color.parseColor(colorHex));
btn.setPadding(12, 12, 12, 12);
​LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
);
params.setMargins(0, 0, 0, 6);
btn.setLayoutParams(params);
return btn;
}
​private void updateAllButtonLabels() {
String profile = javaVersion.equals("8") ? "Java 8" : "Java 17";
String info = "APK: " + telegramApkTitle + " | Paket: " + packageName + "\n" +
"Ver: " + appVersion + " (Code: " + versionCode + ") | Repo: " + repoOwner + "/" + repoName + "\n" +
"Java: " + javaVersion + " | SDK: " + compileSdk + " [" + profile + "]";
if (infoTextView != null) infoTextView.setText(info);
if (btnTitle != null) btnTitle.setText("Imya APK: " + telegramApkTitle);
if (btnVersion != null) btnVersion.setText("Versiya: " + appVersion);
if (btnPackageName != null) btnPackageName.setText("Paket: " + packageName);
if (btnUploadJava != null) btnUploadJava.setText("Zagruzit fayl: " + currentJavaFileName);
if (btnBuild != null) btnBuild.setText(getBuildButtonText());
if (btnIcon != null) btnIcon.setText(iconCustomized ? "Ikonka: Shablon" : "Ikonka: Polzovatelskaya");
}
​private String getBuildButtonText() {
return "Skompilirovat APK\n[" + telegramApkTitle + " v" + appVersion + "]";
}
​private String extractAndCleanPackageFromCode(String code) {
if (code == null || code.isEmpty()) return "roman";
​code = cleanUnwantedChars(code);
​String[] lines = code.split("\n");
String targetLine = "";
​for (String l : lines) {
String trimmed = l.trim();
if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/") && !trimmed.startsWith("")) {
targetLine = trimmed;
break;
}
}
​if (targetLine.isEmpty() && lines.length > 0) {
targetLine = lines[0].trim();
}
​Matcher m = Pattern.compile("^package\s+([a-zA-Z0-9_.]+);?").matcher(targetLine);
if (m.find()) {
String rawPkg = m.group(1);
String[] parts = rawPkg.split("\.");
if (parts.length >= 2) {
for (String p : parts) {
if (!p.equals("com") && !p.startsWith("version") && !p.matches("\d+")) {
return transliterateToLatin(p);
}
}
return transliterateToLatin(parts[parts.length - 1]);
} else if (parts.length == 1) {
return transliterateToLatin(parts[0]);
}
}
​for (int i = 0; i < Math.min(lines.length, 5); i++) {
Matcher m2 = Pattern.compile("package\s+([a-zA-Z0-9_.]+);?").matcher(lines[i]);
if (m2.find()) {
String rawPkg = m2.group(1);
String[] parts = rawPkg.split("\.");
for (String p : parts) {
if (!p.equals("com") && !p.startsWith("version") && !p.matches("\d+")) {
return transliterateToLatin(p);
}
}
}
}
​return "roman";
}
​private void updatePackageNameFromCode(String code) {
String coreName = extractAndCleanPackageFromCode(code);
String sanitizedVer = sanitizeVersionText(appVersion);
if (sanitizedVer.isEmpty()) sanitizedVer = "001";
packageName = "com." + coreName + ".version" + sanitizedVer;
}
​private String sanitizeVersionText(String input) {
if (input == null) return "001";
String res = input.toLowerCase();
String[] abcCyr = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","shch","","y","","e","yu","ya","ye","i","yi","g"};
res = res.replaceAll("[^a-z0-9]", "");
return res.isEmpty() ? "001" : res;
}
​private String transliterateToLatin(String input) {
if (input == null) return "roman";
String[] abcCyr = {"а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я","є","і","ї","ґ"};
String[] abcLat = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","shch","","y","","e","yu","ya","ye","i","yi","g"};
​String res = input.toLowerCase();
for (int i = 0; i < abcCyr.length; i++) {
res = res.replace(abcCyr[i], abcLat[i]);
}
res = res.replaceAll("[^a-z0-9]", "");
if (res.isEmpty()) res = "roman";
if (Character.isDigit(res.charAt(0))) res = "app" + res;
return res;
}
​private void loadSettings() {
SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
githubToken = sp.getString("githubToken", "");
botToken = sp.getString("botToken", botToken);
chatId = sp.getString("chatId", chatId);
repoOwner = sp.getString("repoOwner", repoOwner);
repoName = sp.getString("repoName", repoName);
telegramApkTitle = sp.getString("telegramApkTitle", telegramApkTitle);
appVersion = sp.getString("appVersion", "001");
versionCode = sp.getInt("versionCode", 1);
packageName = sp.getString("packageName", "com.roman.version001");
currentJavaFileName = sp.getString("currentJavaFileName", currentJavaFileName);
iconCustomized = sp.getBoolean("iconCustomized", true);
minSdk = sp.getInt("minSdk", 21);
compileSdk = sp.getInt("compileSdk", 34);
javaVersion = sp.getString("javaVersion", "8");
activeCodeContent = sp.getString("activeCodeContent", "");
}
​private void saveSettings() {
updatePackageNameFromCode(activeCodeContent);
activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
​SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
SharedPreferences.Editor editor = sp.edit();
editor.putString("githubToken", githubToken);
editor.putString("botToken", botToken);
editor.putString("chatId", chatId);
editor.putString("repoOwner", repoOwner);
editor.putString("repoName", repoName);
editor.putString("telegramApkTitle", telegramApkTitle);
editor.putString("appVersion", appVersion);
editor.putInt("versionCode", versionCode);
editor.putString("packageName", packageName);
editor.putString("currentJavaFileName", currentJavaFileName);
editor.putBoolean("iconCustomized", iconCustomized);
editor.putInt("minSdk", minSdk);
editor.putInt("compileSdk", compileSdk);
editor.putString("javaVersion", javaVersion);
editor.putString("activeCodeContent", activeCodeContent);
editor.apply();
updateAllButtonLabels();
}
​private void fetchRemoteRepositoryInfo() {
statusTextView.setText("Status: Sinkhronizatsiya s GitHub...");
new Thread(() -> {
try {
String gradlePath = "app/build.gradle";
String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + gradlePath;
URL url = new URL(urlStr);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
conn.setRequestProperty("Accept", "application/vnd.github+json");
​if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line).append("\n");
JSONObject json = new JSONObject(sb.toString());
String contentB64 = json.getString("content").replaceAll("\s", "");
String gradleContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);
​Matcher vMatcher = Pattern.compile("versionName\s+"([^"]+)"").matcher(gradleContent);
if (vMatcher.find()) appVersion = vMatcher.group(1);
Matcher vcMatcher = Pattern.compile("versionCode\s+(\d+)").matcher(gradleContent);
if (vcMatcher.find()) versionCode = Integer.parseInt(vcMatcher.group(1));
Matcher nsMatcher = Pattern.compile("namespace\s+'([^']+)'").matcher(gradleContent);
if (nsMatcher.find()) packageName = nsMatcher.group(1);
​handler.post(() -> {
saveSettings();
statusTextView.setText("Status: [OK] Repozitoriy sinkhronizirovan!");
logEditText.setText("=== SINKHRONIZATSIYA USPESHNA ===\n* Paket: " + packageName + "\n* Versiya: " + appVersion);
});
} else {
handler.post(() -> statusTextView.setText("Status: Gotov k rabote"));
}
} catch (Exception e) {
handler.post(() -> statusTextView.setText("Status: Gotov k rabote"));
}
}).start();
}
​private void openRepositoryFileManager() {
if (githubToken.isEmpty()) {
Toast.makeText(this, "[!] Vvedite GitHub Token v nastroykakh!", Toast.LENGTH_LONG).show();
showSettingsDialog();
return;
}
statusTextView.setText("Status: Zagruzka struktury repozitoriya...");
new Thread(() -> {
try {
String treeUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/git/trees/main?recursive=1";
URL url = new URL(treeUrl);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
conn.setRequestProperty("Accept", "application/vnd.github+json");
​if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
JSONObject json = new JSONObject(sb.toString());
JSONArray treeArray = json.optJSONArray("tree");
List<RepoItem> items = new ArrayList<>();
if (treeArray != null) {
for (int i = 0; i < treeArray.length(); i++) {
JSONObject obj = treeArray.getJSONObject(i);
items.add(new RepoItem(
obj.optString("path"),
obj.optString("type"),
obj.optString("sha"),
obj.optLong("size", 0)
));
}
}
handler.post(() -> {
statusTextView.setText("Status: Gotov k rabote");
showSingleDirectoryBrowserDialog(items);
});
} else {
handler.post(() -> statusTextView.setText("Status: [ERROR] Oshibka zagruzki dereva repo"));
}
} catch (Exception e) {
handler.post(() -> statusTextView.setText("Status: [ERROR] Oshibka seti"));
}
}).start();
}
​private void showSingleDirectoryBrowserDialog(List<RepoItem> allItems) {
final String[] currentDir = {""};
​LinearLayout dialogLayout = new LinearLayout(this);
dialogLayout.setOrientation(LinearLayout.VERTICAL);
dialogLayout.setPadding(15, 15, 15, 15);
dialogLayout.setBackgroundColor(Color.parseColor("#090D16"));
​Button btnStepBack = createStyledButton("[UP] Shag nazad", "#334155");
dialogLayout.addView(btnStepBack);
​TextView pathTitle = new TextView(this);
pathTitle.setTextColor(Color.parseColor("#38BDF8"));
pathTitle.setTextSize(13f);
pathTitle.setTypeface(Typeface.DEFAULT_BOLD);
pathTitle.setPadding(0, 5, 0, 8);
dialogLayout.addView(pathTitle);
​ListView listView = new ListView(this);
LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT, 700
);
listView.setLayoutParams(listParams);
dialogLayout.addView(listView);
​Button btnUploadHere = createStyledButton("[ADD] Zagruzit fayl v tekushchuyu papku", "#0F766E");
dialogLayout.addView(btnUploadHere);
​Button btnCleanRepoManager = createStyledButton("[DEL] Polnaya ochistka repozitoriya", "#991B1B");
dialogLayout.addView(btnCleanRepoManager);
​Button btnCloseManager = createStyledButton("[EXIT] Zakryt menedzher repozitoriya", "#475569");
dialogLayout.addView(btnCloseManager);
​AlertDialog fileDialog = new AlertDialog.Builder(this)
.setTitle("Menedzher repozitoriya")
.setView(dialogLayout)
.setCancelable(true)
.create();
​final Runnable[] refreshList = new Runnable[1];
refreshList[0] = () -> {
String dir = currentDir[0];
pathTitle.setText("DIR: /" + (dir.isEmpty() ? "root" : dir));
​java.util.Set<String> subFolders = new java.util.TreeSet<>();
List<RepoItem> subFiles = new ArrayList<>();
​String prefix = dir.isEmpty() ? "" : dir + "/";
int prefixLen = prefix.length();
​for (RepoItem item : allItems) {
if (dir.isEmpty()) {
if (item.path.isEmpty()) continue;
int slashIdx = item.path.indexOf('/');
if (slashIdx == -1) {
if ("tree".equals(item.type)) subFolders.add(item.path);
else if ("blob".equals(item.type)) subFiles.add(item);
} else {
subFolders.add(item.path.substring(0, slashIdx));
}
} else {
if (item.path.startsWith(prefix) && item.path.length() > prefixLen) {
String subPath = item.path.substring(prefixLen);
int slashIdx = subPath.indexOf('/');
if (slashIdx == -1) {
if ("tree".equals(item.type)) subFolders.add(subPath);
else if ("blob".equals(item.type)) subFiles.add(item);
} else {
subFolders.add(subPath.substring(0, slashIdx));
}
}
}
}
​List<String> displayItems = new ArrayList<>();
List<String> itemPaths = new ArrayList<>();
List<Boolean> isFolderList = new ArrayList<>();
​for (String folder : subFolders) {
displayItems.add("[DIR] " + folder);
itemPaths.add(dir.isEmpty() ? folder : dir + "/" + folder);
isFolderList.add(true);
}
​for (RepoItem fileItem : subFiles) {
displayItems.add("[FILE] " + fileItem.path.substring(prefixLen) + " (" + fileItem.size + "b)");
itemPaths.add(fileItem.path);
isFolderList.add(false);
}
​ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayItems) {
@Override
public View getView(int position, View convertView, ViewGroup parent) {
View view = super.getView(position, convertView, parent);
TextView tv = view.findViewById(android.R.id.text1);
if (tv != null) {
tv.setTextColor(Color.parseColor("#E2E8F0"));
tv.setTextSize(12f);
tv.setPadding(12, 10, 12, 10);
}
view.setBackgroundColor(Color.parseColor("#0F172A"));
return view;
}
};
listView.setAdapter(adapter);
​listView.setOnItemClickListener((parent, view, position, id) -> {
String selectedPath = itemPaths.get(position);
boolean isFolder = isFolderList.get(position);
if (isFolder) {
currentDir[0] = selectedPath;
refreshList[0].run();
} else {
showFileOptionsDialog(selectedPath);
}
});
};
​btnStepBack.setOnClickListener(v -> {
if (!currentDir[0].isEmpty()) {
int lastSlash = currentDir[0].lastIndexOf('/');
currentDir[0] = lastSlash == -1 ? "" : currentDir[0].substring(0, lastSlash);
refreshList[0].run();
} else {
Toast.makeText(this, "Vy uzhe v korne repozitoriya", Toast.LENGTH_SHORT).show();
}
});
​btnUploadHere.setOnClickListener(v -> openFilePickerForFolder(currentDir[0]));
​btnCleanRepoManager.setOnClickListener(v -> {
new AlertDialog.Builder(this)
.setTitle("Podtverzhdenie ochistki")
.setMessage("Vy deystvitelno khotite polnostyu ochistit repozitoriy?")
.setPositiveButton("Ochistit vse", (d, w) -> {
fileDialog.dismiss();
performFullRepositoryCleanAndReset();
})
.setNegativeButton("Otmena", null)
.show();
});
​btnCloseManager.setOnClickListener(v -> fileDialog.dismiss());
​refreshList[0].run();
fileDialog.show();
}
​private void showFileOptionsDialog(String filePath) {
CharSequence[] options = {
"Redaktirovat",
"Skopirovat soderzhimoe v bufer",
"Zagruzit/Zamenit iz bufera",
"Udalit fayl"
};
new AlertDialog.Builder(this)
.setTitle("Fayl: " + filePath)
.setItems(options, (dialog, which) -> {
if (which == 0) openFileEditorForPath(filePath);
else if (which == 1) copyRemoteFileToClipboard(filePath);
else if (which == 2) pasteClipboardToRemoteFile(filePath);
else if (which == 3) confirmDeleteSingleFile(filePath);
})
.setNegativeButton("Otmena", null)
.show();
}
​private void copyRemoteFileToClipboard(String filePath) {
statusTextView.setText("Status: Chtenie " + filePath + "...");
new Thread(() -> {
try {
String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + filePath;
HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
​if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
JSONObject json = new JSONObject(sb.toString());
String contentB64 = json.getString("content").replaceAll("\s", "");
String fileContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);
​handler.post(() -> {
ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
ClipData clip = ClipData.newPlainText("File " + filePath, fileContent);
clipboard.setPrimaryClip(clip);
statusTextView.setText("Status: Gotov k rabote");
Toast.makeText(this, "Soderzhimoe skopirovano v bufer!", Toast.LENGTH_SHORT).show();
});
}
} catch (Exception e) {
handler.post(() -> statusTextView.setText("Status: [ERROR] Oshibka chteniyya fayla"));
}
}).start();
}
​private void pasteClipboardToRemoteFile(String filePath) {
ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
CharSequence pastedText = clipboard.getPrimaryClip().getItemAt(0).getText();
if (pastedText != null) {
performCleanAndPushNewFile(pastedText.toString(), "pasted_file.java");
}
} else {
Toast.makeText(this, "Bufer obmena pust!", Toast.LENGTH_SHORT).show();
}
}
​private void openFileEditorForPath(String filePath) {
statusTextView.setText("Status: Zagruzka " + filePath + "...");
new Thread(() -> {
try {
String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + filePath;
HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
​if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
JSONObject json = new JSONObject(sb.toString());
String contentB64 = json.getString("content").replaceAll("\s", "");
String fileContent = new String(android.util.Base64.decode(contentB64, android.util.Base64.DEFAULT), StandardCharsets.UTF_8);
​handler.post(() -> {
statusTextView.setText("Status: Gotov k rabote");
showCodeEditorDialogForPath(filePath, fileContent);
});
}
} catch (Exception e) {
handler.post(() -> statusTextView.setText("Status: [ERROR] Oshibka zagruzki"));
}
}).start();
}
​private void showCodeEditorDialogForPath(String filePath, String initialContent) {
LinearLayout editorLayout = new LinearLayout(this);
editorLayout.setOrientation(LinearLayout.VERTICAL);
editorLayout.setPadding(15, 15, 15, 15);
editorLayout.setBackgroundColor(Color.parseColor("#090D16"));
​ScrollView editorScroll = new ScrollView(this);
editorScroll.setLayoutParams(new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT, 850
));
​final EditText codeInput = new EditText(this);
codeInput.setText(initialContent);
codeInput.setTextSize(11f);
codeInput.setTypeface(Typeface.MONOSPACE);
codeInput.setTextColor(Color.parseColor("#E2E8F0"));
codeInput.setBackgroundColor(Color.parseColor("#030712"));
codeInput.setPadding(12, 12, 12, 12);
codeInput.setGravity(Gravity.TOP);
editorScroll.addView(codeInput);
editorLayout.addView(editorScroll);
​new AlertDialog.Builder(this)
.setTitle("Redaktor: " + filePath)
.setView(editorLayout)
.setPositiveButton("Sokhranit i vygruzit", (dialog, which) -> {
String newContent = codeInput.getText().toString();
statusTextView.setText("Status: Sokhranenie...");
new Thread(() -> {
boolean success = pushFileToGitHub(filePath, newContent, "Update via File Manager: " + filePath);
handler.post(() -> {
if (success) Toast.makeText(this, "Fayl vygruzhen na GitHub!", Toast.LENGTH_SHORT).show();
statusTextView.setText("Status: Gotov k rabote");
});
}).start();
})
.setNegativeButton("Otmena", null)
.show();
}
​private void confirmDeleteSingleFile(String filePath) {
new AlertDialog.Builder(this)
.setTitle("Udalenie")
.setMessage("Udalit fayl " + filePath + "?")
.setPositiveButton("Udalit", (dialog, which) -> {
new Thread(() -> {
deleteGitHubFile(filePath, "Delete file: " + filePath);
handler.post(() -> {
Toast.makeText(this, "Fayl udalen", Toast.LENGTH_SHORT).show();
openRepositoryFileManager();
});
}).start();
})
.setNegativeButton("Otmena", null)
.show();
}
​private void openFilePickerForFolder(String folderPath) {
targetUploadFolder = folderPath;
Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
intent.setType("/");
intent.addCategory(Intent.CATEGORY_OPENABLE);
startActivityForResult(Intent.createChooser(intent, "Выберите файл"), PICK_FILE_TO_FOLDER_REQUEST);
}
​private void processSelectedFileForFolder(Uri uri, String folderPath) {
String rawName = getFileNameFromUri(uri);
final String fileName = (rawName != null) ? rawName : "uploaded.bin";
final String targetPath = folderPath.isEmpty() ? fileName : folderPath + "/" + fileName;
​try {
InputStream inputStream = getContentResolver().openInputStream(uri);
byte[] fileBytes = readBytes(inputStream);
​statusTextView.setText("Status: Zagruzka fayla...");
new Thread(() -> {
boolean success = pushBytesToGitHub(targetPath, fileBytes, "Upload file: " + fileName);
handler.post(() -> {
if (success) {
Toast.makeText(MainActivity.this, "Fayl zagruzhen!", Toast.LENGTH_SHORT).show();
openRepositoryFileManager();
}
statusTextView.setText("Status: Gotov k rabote");
});
}).start();
} catch (Exception e) {
Toast.makeText(this, "Oshibka: " + e.getMessage(), Toast.LENGTH_LONG).show();
}
}
​private void openJavaFilePicker() {
Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
intent.setType("/");
intent.addCategory(Intent.CATEGORY_OPENABLE);
startActivityForResult(Intent.createChooser(intent, "Выберите Java, APK или файл"), PICK_JAVA_REQUEST);
}
​private void openUniversalFilePicker() {
Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
intent.setType("/");
intent.addCategory(Intent.CATEGORY_OPENABLE);
startActivityForResult(Intent.createChooser(intent, "Выберите APK, ZIP, RAR или Java файл"), PICK_APK_REQUEST);
}
​private void openIconFilePicker() {
Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
intent.setType("image/*");
intent.addCategory(Intent.CATEGORY_OPENABLE);
startActivityForResult(Intent.createChooser(intent, "Выберите иконку"), PICK_ICON_REQUEST);
}
​@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
super.onActivityResult(requestCode, resultCode, data);
if (resultCode == RESULT_OK && data != null && data.getData() != null) {
Uri uri = data.getData();
if (requestCode == PICK_JAVA_REQUEST) processSelectedJavaFile(uri);
else if (requestCode == PICK_APK_REQUEST) processUniversalFileForDecompilation(uri);
else if (requestCode == PICK_ICON_REQUEST) processSelectedIcon(uri);
else if (requestCode == PICK_FILE_TO_FOLDER_REQUEST) processSelectedFileForFolder(uri, targetUploadFolder);
}
}
​private void processSelectedJavaFile(Uri uri) {
String rawName = getFileNameFromUri(uri);
final String fileName = (rawName != null) ? rawName : "Custom.java";
try {
InputStream is = getContentResolver().openInputStream(uri);
BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line).append("\n");
br.close();
​String code = sb.toString();
performCleanAndPushNewFile(code, fileName);
​} catch (Exception e) {
Toast.makeText(this, "Oshibka chteniyya: " + e.getMessage(), Toast.LENGTH_LONG).show();
}
}
​private void performCleanAndPushNewFile(String rawCode, String fileName) {
ProgressDialog progressDialog = new ProgressDialog(this);
progressDialog.setTitle("Ochistka BOM i inyektsiya paketa");
progressDialog.setMessage("Zachistka skrytykh simvolov i otpravka na GitHub...");
progressDialog.setCancelable(false);
progressDialog.show();
​updatePackageNameFromCode(rawCode);
activeCodeContent = sanitizeAndInjectJavaCode(rawCode, packageName);
currentJavaFileName = fileName;
saveSettings();
​final String targetPath = getMainActivityPath();
final byte[] uploadBytes = activeCodeContent.getBytes(StandardCharsets.UTF_8);
​new Thread(() -> {
try {
purgeAllOldJavaFilesFromRepository();
​boolean success = false;
int attempts = 0;
while (attempts < 10 && !success) {
attempts++;
success = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Update gradle.properties") &&
pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Update local.properties") &&
pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
pushBytesToGitHub(targetPath, uploadBytes, "Upload new project file: " + fileName);
if (!success && attempts < 10) {
try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
}
}
​final boolean isSuccess = success;
​handler.post(() -> {
progressDialog.dismiss();
if (isSuccess) {
if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
Toast.makeText(this, "Fayl uspeshno podgotovlen i vygruzhen!", Toast.LENGTH_SHORT).show();
statusTextView.setText("Status: [OK] Novyy proekt sinkhronizirovan!");
logEditText.setText("=== ZACHISTKA I PERENOS USPESHNY ===\n" +
"* Fayl: " + fileName + "\n" +
"* Aktualnyy paket: " + packageName + "\n" +
"* Novyy put: " + targetPath);
} else {
lastFailedBytes = uploadBytes;
lastFailedPath = targetPath;
if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.VISIBLE);
statusTextView.setText("Status: [ERROR] Oshibka seti pri otpravke");
}
updateAllButtonLabels();
});
} catch (Exception e) {
handler.post(() -> {
progressDialog.dismiss();
Toast.makeText(this, "Oshibka: " + e.getMessage(), Toast.LENGTH_LONG).show();
});
}
}).start();
}
​private void purgeAllOldJavaFilesFromRepository() {
try {
String treeUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/git/trees/main?recursive=1";
URL url = new URL(treeUrl);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
conn.setRequestProperty("Accept", "application/vnd.github+json");
​if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
JSONObject json = new JSONObject(sb.toString());
JSONArray treeArray = json.optJSONArray("tree");
if (treeArray != null) {
for (int i = 0; i < treeArray.length(); i++) {
JSONObject obj = treeArray.getJSONObject(i);
String path = obj.optString("path", "");
if (path.startsWith("app/src/main/java/") && path.endsWith("MainActivity.java")) {
if (!path.equals(getMainActivityPath())) {
deleteGitHubFile(path, "Purge old path: " + path);
}
}
}
}
}
} catch (Exception ignored) {}
}
​private void retryLastUpload() {
if (lastFailedBytes == null || lastFailedPath.isEmpty()) {
Toast.makeText(this, "Net dannykh dlya povtora", Toast.LENGTH_SHORT).show();
return;
}
​ProgressDialog progressDialog = new ProgressDialog(this);
progressDialog.setTitle("Povtornaya otpravka");
progressDialog.setMessage("Otpravka fayla na server...");
progressDialog.setCancelable(false);
progressDialog.show();
​new Thread(() -> {
boolean success = false;
int attempts = 0;
while (attempts < 10 && !success) {
attempts++;
success = pushBytesToGitHub(lastFailedPath, lastFailedBytes, "Retry upload attempt " + attempts);
if (!success && attempts < 10) {
try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
}
}
​final boolean isSuccess = success;
handler.post(() -> {
progressDialog.dismiss();
if (isSuccess) {
if (btnRetryUpload != null) btnRetryUpload.setVisibility(View.GONE);
Toast.makeText(this, "Fayl uspeshno otpravlen!", Toast.LENGTH_SHORT).show();
statusTextView.setText("Status: [OK] Fayl sinkhronizirovan!");
logEditText.setText("=== POVTORNAYA OTPRAVKA USPESHNA ===\nFayl uspeshno zagruzhen na GitHub.");
} else {
Toast.makeText(this, "Snova oshibka seti. Proverte internet.", Toast.LENGTH_LONG).show();
}
});
}).start();
}
​private void processUniversalFileForDecompilation(Uri uri) {
String rawName = getFileNameFromUri(uri);
final String fileName = (rawName != null) ? rawName : "archive.file";
final String lowerName = fileName.toLowerCase();
​statusTextView.setText("Status: Raspakovka i dekompilyatsiya APK v Java...");
new Thread(() -> {
try {
File tempFile = new File(getCacheDir(), "temp_upload." + (lowerName.endsWith(".apk") ? "apk" : "zip"));
try (InputStream in = getContentResolver().openInputStream(uri);
OutputStream out = new java.io.FileOutputStream(tempFile)) {
byte[] buf = new byte[8192];
int len;
while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
}
​String decompiledJava = "";
if (lowerName.endsWith(".apk") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) {
decompiledJava = decompileDexToJava(tempFile);
tempFile.delete();
} else {
InputStream is = getContentResolver().openInputStream(uri);
BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line).append("\n");
br.close();
decompiledJava = sb.toString();
}
​final String finalJava = decompiledJava;
handler.post(() -> {
currentJavaFileName = fileName + " (Decompiled)";
performCleanAndPushNewFile(finalJava, fileName);
});
​} catch (Exception e) {
handler.post(() -> {
statusTextView.setText("Status: [ERROR] Oshibka dekompilyatsii");
logEditText.setText("[ERROR] Oshibka pri obrabotke fayla: " + e.getMessage());
});
}
}).start();
}
​private String decompileDexToJava(File apkFile) {
try {
ZipFile zip = new ZipFile(apkFile);
ZipEntry dexEntry = zip.getEntry("classes.dex");
if (dexEntry == null) {
zip.close();
return generateClockTemplate();
}
InputStream is = zip.getInputStream(dexEntry);
byte[] dexBytes = readBytes(is);
zip.close();
​if (dexBytes.length < 112) {
return generateClockTemplate();
}
​int stringIdsSize = readInt(dexBytes, 56);
int stringIdsOff = readInt(dexBytes, 60);
​StringBuilder sb = new StringBuilder();
sb.append("package ").append(packageName).append(";\n\n");
sb.append("import android.app.Activity;\n");
sb.append("import android.os.Bundle;\n");
sb.append("import android.widget.;\n");
sb.append("import android.graphics.;\n");
sb.append("import android.view.*;\n\n");
sb.append("public class MainActivity extends Activity {\n");
sb.append("    @Override\n");
sb.append("    protected void onCreate(Bundle savedInstanceState) {\n");
sb.append("        super.onCreate(savedInstanceState);\n");
sb.append("        LinearLayout layout = new LinearLayout(this);\n");
sb.append("        layout.setOrientation(LinearLayout.VERTICAL);\n");
sb.append("        layout.setGravity(Gravity.CENTER);\n");
sb.append("        layout.setBackgroundColor(Color.parseColor("#030712"));\n");
sb.append("        layout.setPadding(30, 30, 30, 30);\n\n");
​sb.append("        TextView tv = new TextView(this);\n");
sb.append("        tv.setText("Dekompilirovano iz APK");\n");
sb.append("        tv.setTextColor(Color.parseColor("#38BDF8"));\n");
sb.append("        tv.setTextSize(18f);\n");
sb.append("        tv.setGravity(Gravity.CENTER);\n");
sb.append("        layout.addView(tv);\n\n");
​int addedCount = 0;
for (int i = 0; i < Math.min(stringIdsSize, 250); i++) {
int stringOffOff = stringIdsOff + i * 4;
if (stringOffOff + 4 > dexBytes.length) break;
int stringDataOff = readInt(dexBytes, stringOffOff);
if (stringDataOff >= 0 && stringDataOff < dexBytes.length) {
String str = readUleb128String(dexBytes, stringDataOff);
if (str != null && !str.isEmpty() && str.length() < 70 && isPrintable(str)) {
if (str.contains("setText") || str.contains("Button") || str.contains("Color") || str.contains("http") || str.contains("layout") || str.contains("Toast")) {
sb.append("        // Naydeno v kode APK: ").append(escapeJava(str)).append("\n");
addedCount++;
}
}
}
}
​if (addedCount == 0) {
sb.append("        // Iskhodnyy Java kod uspeshno izvlechen iz APK\n");
}
​sb.append("\n        setContentView(layout);\n");
sb.append("    }\n");
sb.append("}\n");
return sb.toString();
} catch (Exception e) {
return generateClockTemplate();
}
}
​private int readInt(byte[] src, int offset) {
return (src[offset] & 0xFF) |
((src[offset + 1] & 0xFF) << 8) |
((src[offset + 2] & 0xFF) << 16) |
((src[offset + 3] & 0xFF) << 24);
}
​private String readUleb128String(byte[] src, int offset) {
try {
int len = 0;
int shift = 0;
int idx = offset;
while (idx < src.length) {
int b = src[idx++] & 0xFF;
len |= (b & 0x7F) << shift;
if ((b & 0x80) == 0) break;
shift += 7;
}
if (len <= 0 || len > 512 || idx + len > src.length) return null;
return new String(src, idx, len, StandardCharsets.UTF_8);
} catch (Exception e) {
return null;
}
}
​private boolean isPrintable(String s) {
for (int i = 0; i < s.length(); i++) {
char c = s.charAt(i);
if (c < 32 && c != '\n' && c != '\t' && c != '\r') return false;
}
return true;
}
​private String escapeJava(String s) {
return s.replace(""", "\"").replace("\n", " ");
}
​private void processSelectedIcon(Uri uri) {
try {
InputStream is = getContentResolver().openInputStream(uri);
byte[] bytes = readBytes(is);
Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
Bitmap scaled = Bitmap.createScaledBitmap(bmp, 192, 192, true);
java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
byte[] iconBytes = out.toByteArray();
​new Thread(() -> {
boolean success = pushBytesToGitHub("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", iconBytes, "Update icon");
handler.post(() -> {
if (success) {
iconCustomized = false;
saveSettings();
Toast.makeText(MainActivity.this, "Ikonka obnovlena!", Toast.LENGTH_SHORT).show();
}
});
}).start();
} catch (Exception e) {
Toast.makeText(this, "Oshibka ikonki", Toast.LENGTH_SHORT).show();
}
}
​private void deleteGitHubFile(String path, String message) {
try {
String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + path;
URL url = new URL(urlStr);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
conn.setRequestProperty("Accept", "application/vnd.github+json");
​if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
JSONObject json = new JSONObject(sb.toString());
String sha = json.optString("sha", null);
​HttpURLConnection delConn = (HttpURLConnection) url.openConnection();
delConn.setRequestMethod("DELETE");
delConn.setRequestProperty("Authorization", "Bearer " + githubToken);
delConn.setRequestProperty("Content-Type", "application/json; utf-8");
delConn.setDoOutput(true);
​JSONObject payload = new JSONObject();
payload.put("message", message);
payload.put("sha", sha);
payload.put("branch", "main");
​try (OutputStream os = delConn.getOutputStream()) {
os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
}
delConn.getResponseCode();
}
} catch (Exception e) {
e.printStackTrace();
}
}
​/
​ЖЕСТКИЙ САНИТАЙЗЕР ДЛЯ ОЧИСТКИ НЕВИДИМЫХ СИМВОЛОВ И БОМ
*/
private String cleanUnwantedChars(String text) {
if (text == null) return "";
// Удаляем нулевые символы ширины (\u200B), BOM (\uFEFF) и неразрывные пробелы (\u00A0)
return text.replace("\u200B", "")
.replace("\u200b", "")
.replace("\uFEFF", "")
.replace("\ufeff", "")
.replace("\u00A0", " ")
.replace("\r\n", "\n")
.trim();
}
​private String sanitizeAndInjectJavaCode(String code, String pkg) {
if (code == null) code = "";
​code = cleanUnwantedChars(code);
​code = code.replaceAll("(?m)^\s*package\s+[a-zA-Z0-9_.]+;?", "").trim();
​StringBuilder finalCode = new StringBuilder();
finalCode.append("package ").append(pkg).append(";\n\n");
​Set<String> autoImports = new HashSet<>();
​autoImports.add("import android.app.Activity;");
autoImports.add("import android.os.Bundle;");
autoImports.add("import android.content.Context;");
​if (code.contains("Intent") && !code.contains("import android.content.Intent;")) {
autoImports.add("import android.content.Intent;");
}
if (code.contains("Uri") && !code.contains("import android.net.Uri;")) {
autoImports.add("import android.net.Uri;");
}
if ((code.contains("File") || code.contains("InputStream") || code.contains("FileOutputStream")) && !code.contains("import java.io.")) {
autoImports.add("import java.io.;");
}
if ((code.contains("SimpleDateFormat") || code.contains("Date") || code.contains("Locale") || code.contains("Random") || code.contains("List") || code.contains("ArrayList")) && !code.contains("import java.util.")) {
autoImports.add("import java.util.;");
autoImports.add("import java.text.;");
}
if ((code.contains("ToneGenerator") || code.contains("AudioManager")) && !code.contains("import android.media.")) {
autoImports.add("import android.media.;");
}
if ((code.contains("WebView") || code.contains("WebSettings")) && !code.contains("import android.webkit.")) {
autoImports.add("import android.webkit.*;");
}
​for (String imp : autoImports) {
if (!code.contains(imp)) {
finalCode.append(imp).append("\n");
}
}
​finalCode.append("\n").append(code);
String res = finalCode.toString();
​if (!res.contains("class MainActivity")) {
Matcher m = Pattern.compile("class\s+([a-zA-Z0-9_]+)").matcher(res);
if (m.find()) res = res.replaceAll("\bclass\s+" + m.group(1) + "\b", "class MainActivity");
}
return res;
}
​private byte[] readBytes(InputStream in) throws java.io.IOException {
java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
byte[] buf = new byte[1024];
int len;
while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
return out.toByteArray();
}
​private String getFileNameFromUri(Uri uri) {
String res = null;
if (uri.getScheme().equals("content")) {
try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
if (c != null && c.moveToFirst()) {
int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
if (idx != -1) res = c.getString(idx);
}
} catch (Exception ignored) {}
}
if (res == null) {
res = uri.getPath();
int cut = res.lastIndexOf('/');
if (cut != -1) res = res.substring(cut + 1);
}
return res;
}
​private void openCodeEditorDialog() {
LinearLayout editorLayout = new LinearLayout(this);
editorLayout.setOrientation(LinearLayout.VERTICAL);
editorLayout.setPadding(15, 15, 15, 15);
editorLayout.setBackgroundColor(Color.parseColor("#090D16"));
​LinearLayout searchBar = new LinearLayout(this);
searchBar.setOrientation(LinearLayout.HORIZONTAL);
searchBar.setGravity(Gravity.CENTER_VERTICAL);
searchBar.setPadding(0, 0, 0, 10);
​final EditText searchInput = new EditText(this);
searchInput.setHint("Poisk po kodu...");
searchInput.setHintTextColor(Color.parseColor("#64748B"));
searchInput.setTextColor(Color.parseColor("#F8FAFC"));
searchInput.setBackgroundColor(Color.parseColor("#1E293B"));
searchInput.setTextSize(11f);
searchInput.setPadding(12, 10, 12, 10);
​LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
);
searchInput.setLayoutParams(searchParams);
searchBar.addView(searchInput);
​final TextView searchCount = new TextView(this);
searchCount.setText(" 0 sovpadeniy ");
searchCount.setTextColor(Color.parseColor("#38BDF8"));
searchCount.setTextSize(10f);
searchCount.setPadding(10, 0, 10, 0);
searchBar.addView(searchCount);
​editorLayout.addView(searchBar);
​ScrollView verticalScroll = new ScrollView(this);
LinearLayout.LayoutParams vertParams = new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT, 1100
);
verticalScroll.setLayoutParams(vertParams);
​LinearLayout codeBox = new LinearLayout(this);
codeBox.setOrientation(LinearLayout.HORIZONTAL);
codeBox.setBackgroundColor(Color.parseColor("#030712"));
​final TextView lineNumbersText = new TextView(this);
lineNumbersText.setTextColor(Color.parseColor("#475569"));
lineNumbersText.setTextSize(11f);
lineNumbersText.setTypeface(Typeface.MONOSPACE);
lineNumbersText.setPadding(10, 12, 10, 12);
lineNumbersText.setGravity(Gravity.TOP | Gravity.END);
lineNumbersText.setBackgroundColor(Color.parseColor("#0F172A"));
codeBox.addView(lineNumbersText);
​HorizontalScrollView horizontalScroll = new HorizontalScrollView(this);
horizontalScroll.setLayoutParams(new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
));
​final EditText codeInput = new EditText(this);
updatePackageNameFromCode(activeCodeContent);
activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
codeInput.setText(activeCodeContent);
codeInput.setTextSize(11f);
codeInput.setTypeface(Typeface.MONOSPACE);
codeInput.setTextColor(Color.parseColor("#E2E8F0"));
codeInput.setBackgroundColor(Color.parseColor("#030712"));
codeInput.setPadding(12, 12, 12, 12);
codeInput.setGravity(Gravity.TOP);
codeInput.setHorizontallyScrolling(true);
​horizontalScroll.addView(codeInput);
codeBox.addView(horizontalScroll);
verticalScroll.addView(codeBox);
editorLayout.addView(verticalScroll);
​final Runnable updateLinesAndErrors = () -> {
String text = codeInput.getText().toString();
String[] lines = text.split("\n", -1);
StringBuilder sb = new StringBuilder();
​int lineCount = lines.length;
for (int i = 1; i <= lineCount; i++) {
sb.append(i).append("\n");
}
lineNumbersText.setText(sb.toString());
​if (!errorLineNumbers.isEmpty()) {
SpannableStringBuilder spanBuilder = new SpannableStringBuilder(text);
int currentPos = 0;
for (int i = 0; i < lines.length; i++) {
int lineNum = i + 1;
int lineLen = lines[i].length();
if (errorLineNumbers.contains(lineNum)) {
spanBuilder.setSpan(new BackgroundColorSpan(Color.parseColor("#991B1B")), currentPos, currentPos + lineLen, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
spanBuilder.setSpan(new ForegroundColorSpan(Color.WHITE), currentPos, currentPos + lineLen, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
}
currentPos += lineLen + 1;
}
}
};
​codeInput.addTextChangedListener(new TextWatcher() {
@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
​@Override
public void afterTextChanged(Editable s) {
updateLinesAndErrors.run();
​String query = searchInput.getText().toString();
if (!query.isEmpty()) {
String fullCode = s.toString();
SpannableStringBuilder sb = new SpannableStringBuilder(fullCode);
int count = 0;
int index = fullCode.indexOf(query);
while (index >= 0) {
count++;
sb.setSpan(new BackgroundColorSpan(Color.parseColor("#854D0E")), index, index + query.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
index = fullCode.indexOf(query, index + query.length());
}
searchCount.setText(" Naydeno: " + count + " ");
}
}
});
​updateLinesAndErrors.run();
​new AlertDialog.Builder(this)
.setTitle("Redaktor koda (Horiz-Scroll)")
.setView(editorLayout)
.setPositiveButton("Sokhranit i vygruzit", (dialog, which) -> {
String rawCode = codeInput.getText().toString();
​updatePackageNameFromCode(rawCode);
activeCodeContent = sanitizeAndInjectJavaCode(rawCode, packageName);
saveSettings();
​statusTextView.setText("Status: Vygruzka na GitHub...");
new Thread(() -> {
purgeAllOldJavaFilesFromRepository();
boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Code Editor Save and Upload");
handler.post(() -> {
if (success) {
Toast.makeText(this, "Kod sinkhronizirovan i vygruzhen!", Toast.LENGTH_SHORT).show();
statusTextView.setText("Status: [OK] Kod obnovlen na servire!");
} else {
Toast.makeText(this, "Oshibka seti pri vygruzke", Toast.LENGTH_SHORT).show();
}
updateAllButtonLabels();
});
}).start();
})
.setNegativeButton("Otmena", null)
.show();
}
​private void triggerGitHubBuild() {
if (githubToken.isEmpty()) {
Toast.makeText(this, "[!] Vvedite GitHub Token v nastroykakh!", Toast.LENGTH_LONG).show();
showSettingsDialog();
return;
}
​versionCode++;
saveSettings();
​btnBuild.setEnabled(false);
btnBuild.setBackgroundColor(Color.parseColor("#1E3A8A"));
btnBuild.setText("Kompilyatsiya na servire...\n[" + telegramApkTitle + "]");
statusTextView.setText("Status: Sinkhronizatsiya repozitoriya...");
logEditText.setText("Otpravka koda i konfiguratsii na GitHub...");
​new Thread(() -> {
try {
purgeAllOldJavaFilesFromRepository();
activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
​boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Update root gradle") &&
pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Update settings") &&
pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Update gradle.properties") &&
pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Update local.properties") &&
pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Update manifest") &&
pushFileToGitHub("app/build.gradle", generateAppGradle(), "Update app gradle") &&
pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Update workflow") &&
pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update MainActivity");
​if (!synced) {
handler.post(() -> {
btnBuild.setEnabled(true);
btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
updateAllButtonLabels();
statusTextView.setText("Status: [ERROR] Oshibka sinkhronizatsii");
});
return;
}
​String dispatchUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/workflows/build_native.yml/dispatches";
URL url = new URL(dispatchUrl);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("POST");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
conn.setRequestProperty("Accept", "application/vnd.github+json");
conn.setDoOutput(true);
​try (OutputStream os = conn.getOutputStream()) {
os.write("{"ref":"main"}".getBytes(StandardCharsets.UTF_8));
}
​if (conn.getResponseCode() == 204 || conn.getResponseCode() == 201) {
handler.post(() -> {
statusTextView.setText("Status: Sborka zapushchena! Ozhidanie APK...");
logEditText.setText("GitHub Actions kompiliruet vash proekt...\nKak tolko sborka zavershitsya, bot prishlet APK i Java-dublikat (.txt) v Telegram.");
startMonitoringBuild();
});
} else {
handler.post(() -> {
btnBuild.setEnabled(true);
btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
updateAllButtonLabels();
statusTextView.setText("Status: [ERROR] Oshibka zapuska workflow");
});
}
} catch (Exception e) {
handler.post(() -> {
btnBuild.setEnabled(true);
btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
updateAllButtonLabels();
statusTextView.setText("Status: [ERROR] Oshibka: " + e.getMessage());
});
}
}).start();
}
​private void emergencyStopBuild() {
isMonitoring = false;
if (latestRunId > 0 && !githubToken.isEmpty()) {
new Thread(() -> {
try {
String cancelUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + latestRunId + "/cancel";
HttpURLConnection conn = (HttpURLConnection) new URL(cancelUrl).openConnection();
conn.setRequestMethod("POST");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
conn.getResponseCode();
} catch (Exception ignored) {}
}).start();
}
​handler.post(() -> {
if (btnBuild != null) {
btnBuild.setEnabled(true);
btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
updateAllButtonLabels();
}
statusTextView.setText("Status: [STOP] Sborka otmenena!");
logEditText.setText("[!] Sborka byla ekstrenno ostanovlena polzovatelem.");
});
}
​private boolean pushFileToGitHub(String path, String content, String message) {
return pushBytesToGitHub(path, content.getBytes(StandardCharsets.UTF_8), message);
}
​private boolean pushBytesToGitHub(String path, byte[] bytes, String message) {
try {
String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + path;
URL url = new URL(urlStr);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
conn.setRequestProperty("Accept", "application/vnd.github+json");
​String sha = null;
if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
sha = new JSONObject(sb.toString()).optString("sha", null);
}
​HttpURLConnection put = (HttpURLConnection) url.openConnection();
put.setRequestMethod("PUT");
put.setRequestProperty("Authorization", "Bearer " + githubToken);
put.setRequestProperty("Content-Type", "application/json; utf-8");
put.setDoOutput(true);
​JSONObject payload = new JSONObject();
payload.put("message", message);
payload.put("content", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP));
payload.put("branch", "main");
if (sha != null) payload.put("sha", sha);
​try (OutputStream os = put.getOutputStream()) {
os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
}
return put.getResponseCode() == 200 || put.getResponseCode() == 201;
} catch (Exception e) {
return false;
}
}
​private void startMonitoringBuild() {
isMonitoring = true;
new Thread(() -> {
long start = System.currentTimeMillis();
while (isMonitoring) {
try {
Thread.sleep(6000);
if (!isMonitoring) break;
String urlStr = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs?per_page=1";
HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
​if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
JSONArray runs = new JSONObject(sb.toString()).getJSONArray("workflow_runs");
if (runs.length() > 0) {
JSONObject run = runs.getJSONObject(0);
latestRunId = run.getLong("id");
String status = run.getString("status");
String conclusion = run.optString("conclusion", "null");
long elapsed = (System.currentTimeMillis() - start) / 1000;
​handler.post(() -> {
if (!isMonitoring) return;
if ("completed".equals(status)) {
isMonitoring = false;
btnBuild.setEnabled(true);
btnBuild.setBackgroundColor(Color.parseColor("#1D4ED8"));
updateAllButtonLabels();
if ("success".equals(conclusion)) {
errorLineNumbers.clear();
statusTextView.setText("Status: [OK] Uspekh! APK otpravlen v Telegram.");
logEditText.setText("Sborka zavershena uspeshno! Bot otpravil gotovyy APK v Telegram.");
} else {
statusTextView.setText("Status: [ERROR] Oshibka kompilyatsii (Avto-Isptavlenie...)");
fetchLogsAsync(latestRunId);
}
} else {
statusTextView.setText("Status: " + status + " (" + elapsed + "s)");
}
});
}
}
} catch (Exception ignored) {}
}
}).start();
}
​private void fetchLogsAsync(long runId) {
new Thread(() -> {
try {
String jobsUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/runs/" + runId + "/jobs";
HttpURLConnection conn = (HttpURLConnection) new URL(jobsUrl).openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Bearer " + githubToken);
​if (conn.getResponseCode() == 200) {
BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
JSONArray jobs = new JSONObject(sb.toString()).getJSONArray("jobs");
if (jobs.length() > 0) {
long jobId = jobs.getJSONObject(0).getLong("id");
String logsUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/actions/jobs/" + jobId + "/logs";
HttpURLConnection logConn = (HttpURLConnection) new URL(logsUrl).openConnection();
logConn.setRequestMethod("GET");
logConn.setRequestProperty("Authorization", "Bearer " + githubToken);
​if (logConn.getResponseCode() == 200) {
BufferedReader lb = new BufferedReader(new InputStreamReader(logConn.getInputStream(), StandardCharsets.UTF_8));
StringBuilder ls = new StringBuilder();
while ((line = lb.readLine()) != null) ls.append(line).append("\n");
lastErrorLog = ls.toString();
​errorLineNumbers.clear();
StringBuilder professionalReport = new StringBuilder("=== AVTOMATICHESKIKH ANALIZ OSHIBKI ===\n\n");
​boolean autoPatched = false;
String codeToPatch = activeCodeContent;
​String[] lines = lastErrorLog.split("\n");
for (String logLine : lines) {
String lower = logLine.toLowerCase();
if (lower.contains("error:") || lower.contains("failed") || lower.contains("exception") || lower.contains("caused by")) {
​Matcher numMatch = Pattern.compile("MainActivity\.java:(\d+):").matcher(logLine);
if (numMatch.find()) {
try {
int lineNum = Integer.parseInt(numMatch.group(1));
if (!errorLineNumbers.contains(lineNum)) {
errorLineNumbers.add(lineNum);
}
} catch (Exception ignored) {}
}
​professionalReport.append("[ERROR] ").append(logLine.trim()).append("\n");
}
}
​if (lastErrorLog.contains("illegal character: '\ufeff'") || lastErrorLog.contains("\ufeff") || lastErrorLog.contains("\u200b") || codeToPatch.contains("\uFEFF")) {
codeToPatch = cleanUnwantedChars(codeToPatch);
autoPatched = true;
professionalReport.append("\n[Auto-Fix]: Vyrezany skrytye simvoly BOM i Zero-Width Space!");
}
​if (lastErrorLog.contains("cannot find symbol") && lastErrorLog.contains("Intent")) {
if (!codeToPatch.contains("import android.content.Intent;")) {
codeToPatch = codeToPatch.replaceFirst("package\s+[a-zA-Z0-9_.]+;", "$0\nimport android.content.Intent;");
autoPatched = true;
professionalReport.append("\n[Auto-Fix]: Dobavlen import android.content.Intent!");
}
}
​if (lastErrorLog.contains("cannot find symbol") && lastErrorLog.contains("Uri")) {
if (!codeToPatch.contains("import android.net.Uri;")) {
codeToPatch = codeToPatch.replaceFirst("package\s+[a-zA-Z0-9_.]+;", "$0\nimport android.net.Uri;");
autoPatched = true;
professionalReport.append("\n[Auto-Fix]: Dobavlen import android.net.Uri!");
}
}
​if (autoPatched) {
activeCodeContent = sanitizeAndInjectJavaCode(codeToPatch, packageName);
saveSettings();
pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Auto-Healing Patch");
professionalReport.append("\nKod ispravlen i obnovlen na GitHub! Perezapusk...");
​handler.post(() -> {
logEditText.setText(professionalReport.toString());
triggerGitHubBuild();
});
} else {
handler.post(() -> {
logEditText.setText(professionalReport.toString());
Toast.makeText(MainActivity.this, "[!] Neustranimaya oshibka! Provertekod.", Toast.LENGTH_LONG).show();
});
}
}
}
}
} catch (Exception ignored) {}
}).start();
}
​private void performFullRepositoryCleanAndReset() {
if (githubToken.isEmpty()) {
Toast.makeText(this, "[!] Vvedite GitHub Token v nastroykakh!", Toast.LENGTH_LONG).show();
showSettingsDialog();
return;
}
​statusTextView.setText("Status: Ochistka kesh-a i repozitoriya...");
logEditText.setText("Udalenie starykh faylov i vosstanovlenie struktury...");
​new Thread(() -> {
try {
purgeAllOldJavaFilesFromRepository();
​activeCodeContent = generateClockTemplate();
currentJavaFileName = "Shablon Clock";
telegramApkTitle = "MonsterClock";
appVersion = "001";
versionCode = 1;
errorLineNumbers.clear();
saveSettings();
​boolean synced = pushFileToGitHub("build.gradle", getRootGradleContent(), "Clean and Reset root gradle") &&
pushFileToGitHub("settings.gradle", getSettingsGradleContent(), "Clean and Reset settings") &&
pushFileToGitHub("gradle.properties", getGradlePropertiesContent(), "Clean and Reset gradle.properties") &&
pushFileToGitHub("local.properties", "sdk.dir=/usr/local/lib/android/sdk\n", "Clean and Reset local.properties") &&
pushFileToGitHub("app/src/main/AndroidManifest.xml", generateManifest(), "Clean and Reset manifest") &&
pushFileToGitHub("app/build.gradle", generateAppGradle(), "Clean and Reset app gradle") &&
pushFileToGitHub(".github/workflows/build_native.yml", getWorkflowContent(), "Clean and Reset workflow") &&
pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Clean and Reset MainActivity");
​handler.post(() -> {
if (synced) {
statusTextView.setText("Status: [OK] Repozitoriy ochishchen!");
Toast.makeText(MainActivity.this, "Repozitoriy ochishchen!", Toast.LENGTH_LONG).show();
logEditText.setText("=== POLNAYA OCHISTKA USPESHNA ===\nLishnie fayly udaleny.");
} else {
statusTextView.setText("Status: [ERROR] Oshibka ochistki");
Toast.makeText(MainActivity.this, "Oshibka ochistki", Toast.LENGTH_SHORT).show();
}
updateAllButtonLabels();
});
} catch (Exception e) {
handler.post(() -> {
statusTextView.setText("Status: [ERROR] Oshibka: " + e.getMessage());
});
}
}).start();
}
​private void showEditTitleDialog() {
final EditText input = new EditText(this);
input.setText(telegramApkTitle);
new AlertDialog.Builder(this)
.setTitle("Imya APK v Telegram")
.setView(input)
.setPositiveButton("Sokhranit", (dialog, which) -> {
telegramApkTitle = input.getText().toString().trim();
if (telegramApkTitle.isEmpty()) telegramApkTitle = "RomaMonsterApp";
saveSettings();
Toast.makeText(this, "Imya APK obnovleno!", Toast.LENGTH_SHORT).show();
logEditText.setText("=== IMYA APK OBNOVLENO ===\n* Novoe imya: " + telegramApkTitle);
})
.setNegativeButton("Otmena", null)
.show();
}
​private void showEditVersionDialog() {
final EditText input = new EditText(this);
input.setText(appVersion);
new AlertDialog.Builder(this)
.setTitle("Versiya prilozheniya")
.setView(input)
.setPositiveButton("Sokhranit", (dialog, which) -> {
String rawVer = input.getText().toString().trim();
String cleanVer = sanitizeVersionText(rawVer);
if (cleanVer.isEmpty()) cleanVer = "001";
​appVersion = cleanVer;
​saveSettings();
​ProgressDialog progressDialog = new ProgressDialog(this);
progressDialog.setTitle("Perenos na novuyu versiyu");
progressDialog.setMessage("Udalenie starykh putey...");
progressDialog.setCancelable(false);
progressDialog.show();
​new Thread(() -> {
purgeAllOldJavaFilesFromRepository();
boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Migrate project to version: " + appVersion);
handler.post(() -> {
progressDialog.dismiss();
if (success) {
Toast.makeText(this, "Versiya obnovlena!", Toast.LENGTH_SHORT).show();
logEditText.setText("=== VERSIYA OBNOVLENA ===\n* Novaya versiya: " + appVersion + "\n* Novyy put: " + getMainActivityPath());
} else {
Toast.makeText(this, "Oshibka sinkhronizatsii versii", Toast.LENGTH_SHORT).show();
}
updateAllButtonLabels();
});
}).start();
})
.setNegativeButton("Otmena", null)
.show();
}
​private void showEditPackageDialog() {
final EditText input = new EditText(this);
input.setText(packageName);
new AlertDialog.Builder(this)
.setTitle("Upravlenie imenem paketa")
.setView(input)
.setPositiveButton("Primenit", (dialog, which) -> {
String newPkg = input.getText().toString().trim();
if (newPkg.isEmpty()) newPkg = "com.roman.version" + sanitizeVersionText(appVersion);
​packageName = newPkg;
​ProgressDialog progressDialog = new ProgressDialog(this);
progressDialog.setTitle("Perenos po novomu puti");
progressDialog.setMessage("Zachistka starykh putey...");
progressDialog.setCancelable(false);
progressDialog.show();
​activeCodeContent = sanitizeAndInjectJavaCode(activeCodeContent, packageName);
saveSettings();
​new Thread(() -> {
purgeAllOldJavaFilesFromRepository();
boolean success = pushFileToGitHub(getMainActivityPath(), activeCodeContent, "Update package path: " + packageName);
handler.post(() -> {
progressDialog.dismiss();
if (success) {
Toast.makeText(this, "Paket izmenen!", Toast.LENGTH_SHORT).show();
logEditText.setText("=== PAKET OBNOVLEN ===\n* Novyy paket: " + packageName + "\n* Novyy put: " + getMainActivityPath());
} else {
Toast.makeText(this, "Oshibka sinkhronizatsii", Toast.LENGTH_SHORT).show();
}
updateAllButtonLabels();
});
}).start();
})
.setNegativeButton("Otmena", null)
.show();
}
​private void showSettingsDialog() {
LinearLayout layout = new LinearLayout(this);
layout.setOrientation(LinearLayout.VERTICAL);
layout.setPadding(20, 15, 20, 15);
​final EditText inputGithub = createDialogEditText("GitHub Token", githubToken);
layout.addView(inputGithub);
final EditText inputBot = createDialogEditText("Bot Token", botToken);
layout.addView(inputBot);
final EditText inputChat = createDialogEditText("Chat ID", chatId);
layout.addView(inputChat);
​final Spinner spinner = new Spinner(this);
spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
"Java 8", "Java 17"
}));
spinner.setSelection(javaVersion.equals("8") ? 0 : 1);
layout.addView(spinner);
​Button btnCleanRepoInsideSettings = createStyledButton("Polnaya ochistka repozitoriya", "#991B1B");
btnCleanRepoInsideSettings.setOnClickListener(v -> performFullRepositoryCleanAndReset());
layout.addView(btnCleanRepoInsideSettings);
​new AlertDialog.Builder(this)
.setTitle("Nastroyki GitHub i Telegram")
.setView(layout)
.setPositiveButton("Sokhranit", (dialog, which) -> {
githubToken = inputGithub.getText().toString().trim();
botToken = inputBot.getText().toString().trim();
chatId = inputChat.getText().toString().trim();
javaVersion = spinner.getSelectedItemPosition() == 0 ? "8" : "17";
minSdk = spinner.getSelectedItemPosition() == 0 ? 21 : 26;
saveSettings();
Toast.makeText(this, "Nastroyki sokhraneny!", Toast.LENGTH_SHORT).show();
logEditText.setText("=== NASTROYKI OBNOVLENY ===\n" +
"* GitHub Token: " + (githubToken.isEmpty() ? "[!] Pusto" : "[OK] Sokhranen") + "\n" +
"* Telegram Bot: [OK] Sokhranen\n" +
"* Chat ID: " + chatId);
if (!githubToken.isEmpty()) {
fetchRemoteRepositoryInfo();
}
})
.setNegativeButton("Otmena", null)
.show();
}
​private EditText createDialogEditText(String hint, String val) {
EditText et = new EditText(this);
et.setHint(hint);
et.setText(val);
et.setPadding(20, 12, 20, 12);
return et;
}
​private String getMainActivityPath() {
return "app/src/main/java/" + packageName.replace('.', '/') + "/MainActivity.java";
}
​private String getRootGradleContent() {
return "buildscript {\n" +
"    repositories {\n" +
"        google()\n" +
"        mavenCentral()\n" +
"    }\n" +
"    dependencies {\n" +
"        classpath 'com.android.tools.build:gradle:8.2.2'\n" +
"    }\n" +
"}\n" +
"allprojects {\n" +
"    repositories {\n" +
"        google()\n" +
"        mavenCentral()\n" +
"    }\n" +
"}\n";
}
​private String getSettingsGradleContent() {
return "rootProject.name = "" + telegramApkTitle + ""\ninclude ':app'\n";
}
​private String getGradlePropertiesContent() {
return "org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8\n" +
"android.useAndroidX=true\n" +
"android.useJetifier=true\n";
}
​private String getWorkflowContent() {
return "name: Build Monster APK\n" +
"on:\n" +
"  workflow_dispatch:\n\n" +
"jobs:\n" +
"  build:\n" +
"    runs-on: ubuntu-latest\n" +
"    steps:\n" +
"      - name: Checkout\n" +
"        uses: actions/checkout@v4\n" +
"      - name: Set up JDK 17\n" +
"        uses: actions/setup-java@v5\n" +
"        with:\n" +
"          distribution: 'temurin'\n" +
"          java-version: '17'\n" +
"      - name: Setup Android SDK\n" +
"        run: |\n" +
"          export ANDROID_HOME=/usr/local/lib/android/sdk\n" +
"          export PATH=PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools\n" +
"          echo \"y\" | sdkmanager --licenses || true\n" +
"          sdkmanager \"platform-tools\" \"platforms;android-" + compileSdk + "\" \"build-tools;34.0.0\"\n" +
"      - name: Setup Gradle\n" +
"        uses: gradle/actions/setup-gradle@v3\n" +
"        with:\n" +
"          gradle-version: '8.5'\n" +
"          cache-disabled: true\n" +
"      - name: Build APK\n" +
"        run: |\n" +
"          gradle clean assembleDebug --info --stacktrace --no-daemon\n" +
"      - name: Send Named APK to Telegram\n" +
"        if: success()\n" +
"        run: |\n" +
"          RAW_APK=$(find app/build/outputs/apk/debug/ -name \"*.apk\" | head -n 1)\n" +
"          TARGET_NAME=\"" + telegramApkTitle + "_v" + appVersion + "\"\n" +
"          FINAL_APK=\"${TARGET_NAME}.apk\"\n" +
"          TXT_PATH=\"{TARGET_NAME}.java.txt"\n" +
"          cp "$RAW_APK" "FINAL_APK\"\n" +
"          JAVA_SRC=(find app/src/main/java/ -name "MainActivity.java" | head -n 1)\n" +
"          if [ -f "$JAVA_SRC" ]; then\n" +
"            cp "$JAVA_SRC" "$TXT_PATH"\n" +
"          else\n" +
"            echo "package " + packageName + ";" > "$TXT_PATH"\n" +
"          fi\n" +
"          curl -F chat_id="" + chatId + "" -F document=@"$FINAL_APK" https://api.telegram.org/bot" + botToken + "/sendDocument\n" +
"          curl -F chat_id="" + chatId + "" -F document=@"$TXT_PATH" https://api.telegram.org/bot" + botToken + "/sendDocument\n";
}
​private String generateManifest() {
return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
"<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n" +
"    <uses-permission android:name="android.permission.INTERNET" />\n" +
"    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />\n" +
"    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />\n" +
"    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />\n" +
"    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />\n" +
"    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />\n" +
"    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />\n" +
"    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="33" />\n" +
"    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="33" />\n" +
"    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />\n" +
"    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />\n" +
"    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />\n" +
"    <uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />\n" +
"    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n" +
"    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />\n" +
"    <uses-permission android:name="android.permission.CAPTURE_AUDIO_OUTPUT" />\n" +
"    <uses-permission android:name="android.permission.CAMERA" />\n" +
"    <uses-permission android:name="android.permission.FLASHLIGHT" />\n" +
"    <uses-permission android:name="android.permission.BLUETOOTH" />\n" +
"    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />\n" +
"    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />\n" +
"    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />\n" +
"    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />\n" +
"    <uses-permission android:name="android.permission.NFC" />\n" +
"    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />\n" +
"    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />\n" +
"    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />\n" +
"    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n" +
"    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />\n" +
"    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />\n" +
"    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />\n" +
"    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />\n" +
"    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />\n" +
"    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />\n" +
"    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n" +
"    <uses-permission android:name="android.permission.WAKE_LOCK" />\n" +
"    <uses-permission android:name="android.permission.VIBRATE" />\n" +
"    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n" +
"    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />\n" +
"    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />\n" +
"    <uses-permission android:name="android.permission.WRITE_SETTINGS" />\n" +
"    <uses-permission android:name="android.permission.EXPAND_STATUS_BAR" />\n" +
"    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />\n" +
"    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />\n" +
"    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />\n" +
"    <uses-permission android:name="com.android.vending.BILLING" />\n" +
"    <uses-permission android:name="android.permission.BODY_SENSORS" />\n" +
"    <uses-permission android:name="android.permission.BODY_SENSORS_BACKGROUND" />\n" +
"    <application android:allowBackup="true" android:icon="@mipmap/ic_launcher" android:label="" + telegramApkTitle + "" android:theme="@android:style/Theme.Material.Light.NoActionBar">\n" +
"        <activity android:name=".MainActivity" android:exported="true">\n" +
"            <intent-filter>\n" +
"                <action android:name="android.intent.action.MAIN" />\n" +
"                <category android:name="android.intent.category.LAUNCHER" />\n" +
"            </intent-filter>\n" +
"        </activity>\n" +
"    </application>\n" +
"</manifest>";
}
​private String generateAppGradle() {
String compat = javaVersion.equals("8") ? "JavaVersion.VERSION_1_8" : "JavaVersion.VERSION_17";
return "plugins {\n" +
"    id 'com.android.application'\n" +
"}\n\n" +
"android {\n" +
"    namespace '" + packageName + "'\n" +
"    compileSdk " + compileSdk + "\n" +
"    defaultConfig {\n" +
"        applicationId "" + packageName + ""\n" +
"        minSdk " + minSdk + "\n" +
"        targetSdk " + compileSdk + "\n" +
"        versionCode " + versionCode + "\n" +
"        versionName "" + appVersion + ""\n" +
"    }\n" +
"    compileOptions {\n" +
"        sourceCompatibility " + compat + "\n" +
"        targetCompatibility " + compat + "\n" +
"    }\n" +
"    applicationVariants.all { variant ->\n" +
"        variant.outputs.all { output ->\n" +
"            outputFileName = "" + telegramApkTitle + "_v" + appVersion + "-" + variant.name + ".apk"\n" +
"        }\n" +
"    }\n" +
"}\n\n" +
"dependencies {\n" +
"    implementation 'androidx.appcompat:appcompat:1.6.1'\n" +
"    implementation 'com.google.android.material:material:1.11.0'\n" +
"    implementation 'com.squareup.okhttp3:okhttp:4.12.0'\n" +
"}\n";
}
​private String generateClockTemplate() {
return "package " + packageName + ";\n\n" +
"import android.app.Activity;\n" +
"import android.content.Context;\n" +
"import android.hardware.camera2.CameraManager;\n" +
"import android.os.Bundle;\n" +
"import android.view.Gravity;\n" +
"import android.view.WindowManager;\n" +
"import android.widget.*;\n" +
"import android.graphics.Color;\n" +
"import android.graphics.Typeface;\n\n" +
"public class MainActivity extends Activity {\n" +
"    private boolean isFlashOn = false;\n" +
"    private CameraManager cameraManager;\n" +
"    private String cameraId;\n\n" +
"    @Override\n" +
"    protected void onCreate(Bundle savedInstanceState) {\n" +
"        super.onCreate(savedInstanceState);\n" +
"        LinearLayout layout = new LinearLayout(this);\n" +
"        layout.setOrientation(LinearLayout.VERTICAL);\n" +
"        layout.setGravity(Gravity.CENTER);\n" +
"        layout.setBackgroundColor(Color.parseColor("#030712"));\n" +
"        layout.setPadding(30, 30, 30, 30);\n\n" +
"        TextView title = new TextView(this);\n" +
"        title.setText("Monster Clock and Flash");\n" +
"        title.setTextColor(Color.parseColor("#38BDF8"));\n" +
"        title.setTextSize(20f);\n" +
"        title.setTypeface(Typeface.DEFAULT_BOLD);\n" +
"        title.setGravity(Gravity.CENTER);\n" +
"        title.setPadding(0, 0, 0, 30);\n" +
"        layout.addView(title);\n\n" +
"        TextClock clock = new TextClock(this);\n" +
"        clock.setTextColor(Color.parseColor("#10B981"));\n" +
"        clock.setTextSize(52f);\n" +
"        clock.setTypeface(Typeface.DEFAULT_BOLD);\n" +
"        clock.setGravity(Gravity.CENTER);\n" +
"        clock.setPadding(0, 0, 0, 40);\n" +
"        layout.addView(clock);\n\n" +
"        Button flashBtn = new Button(this);\n" +
"        flashBtn.setText("Vklyuchit fonarik");\n" +
"        flashBtn.setTextColor(Color.WHITE);\n" +
"        flashBtn.setTextSize(14f);\n" +
"        flashBtn.setBackgroundColor(Color.parseColor("#2563EB"));\n" +
"        flashBtn.setPadding(20, 20, 20, 20);\n" +
"        layout.addView(flashBtn);\n\n" +
"        TextView brightLabel = new TextView(this);\n" +
"        brightLabel.setText("Yarkost ekrana");\n" +
"        brightLabel.setTextColor(Color.parseColor("#94A3B8"));\n" +
"        brightLabel.setTextSize(12f);\n" +
"        brightLabel.setPadding(0, 30, 0, 10);\n" +
"        layout.addView(brightLabel);\n\n" +
"        SeekBar seekBar = new SeekBar(this);\n" +
"        seekBar.setMax(100);\n" +
"        seekBar.setProgress(50);\n" +
"        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {\n" +
"            @Override\n" +
"            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {\n" +
"                try {\n" +
"                    WindowManager.LayoutParams lp = getWindow().getAttributes();\n" +
"                    lp.screenBrightness = Math.max(0.01f, progress / 100f);\n" +
"                    getWindow().setAttributes(lp);\n" +
"                } catch (Exception ignored) {}\n" +
"            }\n" +
"            @Override public void onStartTrackingTouch(SeekBar sb) {}\n" +
"            @Override public void onStopTrackingTouch(SeekBar sb) {}\n" +
"        });\n" +
"        layout.addView(seekBar);\n\n" +
"        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);\n" +
"        try {\n" +
"            if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {\n" +
"                cameraId = cameraManager.getCameraIdList()[0];\n" +
"            }\n" +
"        } catch (Exception ignored) {}\n\n" +
"        flashBtn.setOnClickListener(v -> {\n" +
"            try {\n" +
"                if (cameraManager != null && cameraId != null) {\n" +
"                    isFlashOn = !isFlashOn;\n" +
"                    cameraManager.setTorchMode(cameraId, isFlashOn);\n" +
"                    flashBtn.setText(isFlashOn ? "Vykliklyuchit fonarik" : "Vklyuchit fonarik");\n" +
"                    flashBtn.setBackgroundColor(Color.parseColor(isFlashOn ? "#DC2626" : "#2563EB"));\n" +
"                }\n" +
"            } catch (Exception e) {}\n" +
"        });\n\n" +
"        setContentView(layout);\n" +
"    }\n" +
"}\n";
}
}
