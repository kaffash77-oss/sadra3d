package ir.sadra3d.pricing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.pdf.PdfDocument;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Native offline Android app. No server, WebView, accounts, or runtime permissions. */
public class MainActivity extends Activity {
    private static final int RED = Color.rgb(198, 22, 43);
    private static final int DEEP_RED = Color.rgb(129, 9, 26);
    private static final int INK = Color.rgb(31, 35, 48);
    private static final int GRAY = Color.rgb(113, 119, 133);
    private static final int BG = Color.rgb(248, 248, 251);
    private static final int BORDER = Color.rgb(230, 232, 238);
    private static final int REQ_PDF = 1011;
    private SharedPreferences preferences;
    private Typeface yekan;
    private LinearLayout appRoot, pricingScreen, historyScreen, historyItems, ratesPanel;
    private FrameLayout content;
    private TextView tabPrice, tabHistory;
    private Entry weight, hours, margin, materialPrice, machinePrice, laborPrice;
    private TextView bigTotal, smallNet, smallProfit, materialTotal, machineTotal, laborTotal;
    private PricingMath.Result lastResult;
    private boolean currentlyLoading;
    private final DecimalFormat number = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.US));
    private String currentLabel = "محاسبه جدید";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(DEEP_RED);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        preferences = getSharedPreferences("sadra3d_pricing_v1", MODE_PRIVATE);
        try { yekan = Typeface.createFromAsset(getAssets(), "fonts/yekan.ttf"); }
        catch (Exception e) { yekan = Typeface.DEFAULT_BOLD; }
        buildScreen();
        calculateNow();
    }

    private int dp(float d) { return (int)(d * getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout vertical() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return v; }
    private LinearLayout horizontal() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return v; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w == -1 ? -1 : dp(w), h == -1 || h == -2 ? h : dp(h)); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable bordered(int bg, int radius) {
        GradientDrawable d = rounded(bg, radius); d.setStroke(dp(1), BORDER); return d;
    }
    private GradientDrawable redGradient(int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{DEEP_RED, RED, 0xFFEB314C});
        d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(color); t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT); t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setTypeface(bold ? yekan : Typeface.create("sans-serif", Typeface.NORMAL));
        t.setIncludeFontPadding(true); return t;
    }
    private void pad(View v, int a, int b, int c, int d) { v.setPadding(dp(a), dp(b), dp(c), dp(d)); }
    private View space(int h) { View v = new View(this); v.setLayoutParams(lp(1,h)); return v; }
    private LinearLayout card() {
        LinearLayout v = vertical(); v.setBackground(bordered(Color.WHITE, 20)); pad(v,17,17,17,17);
        v.setElevation(dp(1)); return v;
    }
    private LinearLayout.LayoutParams cardMargin() { LinearLayout.LayoutParams p = lp(-1,-2); p.bottomMargin = dp(14); return p; }
    private void buildScreen() {
        appRoot = vertical(); appRoot.setBackgroundColor(BG); setContentView(appRoot);
        appRoot.addView(header(), lp(-1,-2));
        content = new FrameLayout(this);
        appRoot.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        pricingScreen = pricingView();
        historyScreen = historyView();
        content.addView(pricingScreen, new FrameLayout.LayoutParams(-1,-1));
        content.addView(historyScreen, new FrameLayout.LayoutParams(-1,-1));
        appRoot.addView(bottomTabs(),lp(-1,70));
        showTab(false);
    }
    private View header() {
        LinearLayout head = vertical(); head.setBackground(redGradient(0)); pad(head,22,21,22,23);
        LinearLayout top = horizontal();
        LinearLayout logoBg = horizontal(); logoBg.setBackground(rounded(0xFF75091A,12)); pad(logoBg,11,9,11,9);
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.sadra_logo); logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logoBg.addView(logo,lp(140,22)); top.addView(logoBg,lp(-2,-2));
        View spacer = new View(this); top.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        TextView label = text("ابزار قیمت‌گذاری",12,0xFFFFDBE0,true); top.addView(label,lp(-2,-2));
        head.addView(top,lp(-1,-2)); head.addView(space(15));
        TextView title = text("تعیین قیمت صدرا 3D",26,Color.WHITE,true); head.addView(title);
        TextView sub = text("محاسبه دقیق هزینه چاپ سه‌بعدی بر اساس فایل اکسل",12,0xFFFFE5E8,false); head.addView(sub);
        return head;
    }
    private LinearLayout pricingView() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout panel = vertical(); pad(panel,16,18,16,16);
        LinearLayout intro = horizontal();
        TextView h = text("محاسبه قیمت محصول",18,INK,true); intro.addView(h,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge = text("● آفلاین",11,0xFF187448,true); badge.setBackground(rounded(0xFFDEF7E8,50)); pad(badge,12,6,12,6); intro.addView(badge);
        panel.addView(intro); panel.addView(space(13));

        LinearLayout inputs = card();
        inputs.addView(text("اطلاعات چاپ",15,INK,true)); inputs.addView(space(13));
        LinearLayout duo = horizontal();
        weight = makeEntry("وزن محصول", "گرم", "75", "weight");
        hours = makeEntry("زمان کار دستگاه", "ساعت", "3", "hours");
        duo.addView(weight.container,new LinearLayout.LayoutParams(0,-2,1));
        View gap = new View(this); duo.addView(gap,lp(10,1));
        duo.addView(hours.container,new LinearLayout.LayoutParams(0,-2,1)); inputs.addView(duo);
        inputs.addView(space(13));
        margin = makeEntry("درصد سود", "درصد", "100", "margin"); inputs.addView(margin.container,lp(-1,-2));
        panel.addView(inputs,cardMargin());

        LinearLayout result = vertical(); result.setBackground(redGradient(23)); pad(result,20,20,20,21);
        TextView cap = text("قیمت نهایی پیشنهادی",14,0xFFFFE7EC,true); result.addView(cap);
        bigTotal = text("۰",33,Color.WHITE,true); bigTotal.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); result.addView(bigTotal);
        TextView hint = text("واحد مبلغ: مطابق نرخ‌های فایل اکسل",11,0xFFFFE3E7,false); result.addView(hint);
        View line = new View(this); line.setBackgroundColor(0x55FFFFFF); LinearLayout.LayoutParams llp=lp(-1,1); llp.topMargin=dp(15); llp.bottomMargin=dp(13); result.addView(line,llp);
        LinearLayout values = horizontal();
        LinearLayout netBox = vertical(); netBox.addView(text("مبلغ خالص",11,0xFFFFD1D9,false));
        smallNet = text("۰",18,Color.WHITE,true); netBox.addView(smallNet);
        LinearLayout profBox = vertical(); profBox.addView(text("میزان سود",11,0xFFFFD1D9,false));
        smallProfit = text("۰",18,Color.WHITE,true); profBox.addView(smallProfit);
        values.addView(netBox,new LinearLayout.LayoutParams(0,-2,1)); values.addView(profBox,new LinearLayout.LayoutParams(0,-2,1));
        result.addView(values); panel.addView(result,cardMargin());

        LinearLayout breakdown = card(); breakdown.addView(text("ریز هزینه‌ها",16,INK,true)); breakdown.addView(space(8));
        materialTotal = text("۰",14,INK,true); machineTotal = text("۰",14,INK,true); laborTotal = text("۰",14,INK,true);
        breakdown.addView(costRow("هزینه مواد مصرفی",materialTotal));
        breakdown.addView(costRow("هزینه کار دستگاه",machineTotal));
        breakdown.addView(costRow("هزینه نیروی کار (۵٪ زمان)",laborTotal));
        panel.addView(breakdown,cardMargin());

        LinearLayout settings = card();
        LinearLayout settingsTop = horizontal();
        TextView sh = text("⚙  نرخ‌های پایه و تنظیمات",15,INK,true);
        settingsTop.addView(sh,new LinearLayout.LayoutParams(0,-2,1));
        TextView toggle = text("نمایش ▼",12,RED,true); settingsTop.addView(toggle);
        settings.addView(settingsTop);
        ratesPanel = vertical(); ratesPanel.setVisibility(View.GONE);
        ratesPanel.addView(space(14));
        materialPrice = makeEntry("قیمت هر کیلو مواد", "مبلغ", "6000000", "material_rate");
        machinePrice = makeEntry("نرخ هر ساعت دستگاه", "مبلغ", "30000", "machine_rate");
        laborPrice = makeEntry("نرخ هر ساعت نیروی کار", "مبلغ", "300000", "labor_rate");
        ratesPanel.addView(materialPrice.container); ratesPanel.addView(space(12));
        ratesPanel.addView(machinePrice.container); ratesPanel.addView(space(12));
        ratesPanel.addView(laborPrice.container); ratesPanel.addView(space(12));
        TextView original = text("فرمول اکسل: سهم زمان نیروی کار = ۵٪ × ساعت چاپ",12,GRAY,false);
        ratesPanel.addView(original);
        settings.addView(ratesPanel);
        settingsTop.setOnClickListener(v -> {
            boolean show = ratesPanel.getVisibility() != View.VISIBLE;
            ratesPanel.setVisibility(show ? View.VISIBLE : View.GONE);
            toggle.setText(show ? "بستن ▲" : "نمایش ▼");
        });
        panel.addView(settings,cardMargin());

        LinearLayout actions = vertical();
        TextView save = action("ذخیره محاسبه",true); actions.addView(save,lp(-1,53));
        save.setOnClickListener(v-> saveQuote()); actions.addView(space(10));
        LinearLayout row = horizontal();
        TextView pdf = action("خروجی PDF",false); TextView share = action("اشتراک نتیجه",false);
        row.addView(pdf,new LinearLayout.LayoutParams(0,dp(50),1)); row.addView(new View(this),lp(10,1)); row.addView(share,new LinearLayout.LayoutParams(0,dp(50),1));
        pdf.setOnClickListener(v-> exportPdf()); share.setOnClickListener(v-> shareResult());
        actions.addView(row); actions.addView(space(10));
        TextView reset = action("پاک‌کردن مقادیر و شروع دوباره",false); actions.addView(reset,lp(-1,48));
        reset.setOnClickListener(v -> {weight.edit.setText("75");hours.edit.setText("3");margin.edit.setText("100");currentLabel="محاسبه جدید";calculateNow();});
        panel.addView(actions); panel.addView(space(18));
        scroll.addView(panel); LinearLayout wrapper=vertical();wrapper.addView(scroll,lp(-1,-1));
        attachWatchers(); return wrapper;
    }
    private Entry makeEntry(String label, String suffix, String defaultValue,String prefKey) {
        LinearLayout column = vertical();
        TextView header = text(label,12,GRAY,true); column.addView(header); column.addView(space(6));
        LinearLayout field = horizontal(); field.setBackground(bordered(0xFFFAFAFC,13)); pad(field,10,4,10,4);
        EditText edit = new EditText(this); edit.setBackgroundColor(Color.TRANSPARENT);
        edit.setTextSize(17); edit.setSingleLine(true); edit.setTextColor(INK); edit.setTypeface(yekan);
        edit.setSelectAllOnFocus(true); edit.setTextDirection(View.TEXT_DIRECTION_LTR); edit.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        edit.setText(preferences.getString(prefKey, defaultValue));
        field.addView(edit,new LinearLayout.LayoutParams(0,dp(43),1));
        TextView suffixText = text(suffix,11,GRAY,false); field.addView(suffixText,lp(-2,-2));
        column.addView(field,lp(-1,-2));
        return new Entry(column,edit,prefKey);
    }
    private View costRow(String label, TextView amount) {
        LinearLayout r = horizontal();pad(r,0,9,0,9);
        TextView text = text(label,13,GRAY,false);
        r.addView(text,new LinearLayout.LayoutParams(0,-2,1)); r.addView(amount,lp(-2,-2));
        return r;
    }
    private TextView action(String title,boolean primary) {
        TextView v=text(title,14,primary?Color.WHITE:RED,true);v.setGravity(Gravity.CENTER);
        v.setBackground(primary?redGradient(15):bordered(Color.WHITE,15));
        v.setElevation(dp(1)); return v;
    }
    private LinearLayout historyView() {
        ScrollView s=new ScrollView(this);s.setVerticalScrollBarEnabled(false);
        LinearLayout p=vertical();pad(p,16,18,16,16);
        p.addView(text("تاریخچه محاسبات",19,INK,true));p.addView(space(4));
        p.addView(text("محاسبات ذخیره‌شده روی همین گوشی",12,GRAY,false));p.addView(space(14));
        historyItems=vertical();p.addView(historyItems);s.addView(p);
        LinearLayout w=vertical();w.addView(s,lp(-1,-1));return w;
    }
    private View bottomTabs() {
        LinearLayout bar=horizontal();bar.setBackgroundColor(Color.WHITE);pad(bar,15,9,15,9);
        tabPrice = text("◉   محاسبه قیمت",14,RED,true);tabPrice.setGravity(Gravity.CENTER);
        tabHistory = text("▤   تاریخچه",14,GRAY,true);tabHistory.setGravity(Gravity.CENTER);
        bar.addView(tabPrice,new LinearLayout.LayoutParams(0,-1,1));bar.addView(tabHistory,new LinearLayout.LayoutParams(0,-1,1));
        tabPrice.setOnClickListener(v->showTab(false));tabHistory.setOnClickListener(v->showTab(true));
        return bar;
    }
    private void showTab(boolean history) {
        pricingScreen.setVisibility(history?View.GONE:View.VISIBLE);
        historyScreen.setVisibility(history?View.VISIBLE:View.GONE);
        tabPrice.setTextColor(history?GRAY:RED);tabHistory.setTextColor(history?RED:GRAY);
        if(history) refreshHistory();
    }
    private void attachWatchers() {
        for(Entry e:new Entry[]{weight,hours,margin,materialPrice,machinePrice,laborPrice}) {
            e.edit.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
                @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                    if(currentlyLoading)return;
                    preferences.edit().putString(e.key,s.toString()).apply();calculateNow();
                }
                @Override public void afterTextChanged(Editable e){}
            });
        }
    }
    private void calculateNow() {
        if(weight==null || laborPrice==null || bigTotal==null)return;
        lastResult = PricingMath.calculate(read(weight),read(hours),read(margin),read(materialPrice),read(machinePrice),read(laborPrice));
        bigTotal.setText(money(lastResult.total));smallNet.setText(money(lastResult.net));
        smallProfit.setText(money(lastResult.profit));materialTotal.setText(money(lastResult.material));
        machineTotal.setText(money(lastResult.machine));laborTotal.setText(money(lastResult.labor));
    }
    private double read(Entry e) {
        try {
            String s=western(e.edit.getText().toString()).trim().replace(",", "").replace("٬", "").replace(" ", "").replace("٫", ".");
            double r=Double.parseDouble(s);return Double.isFinite(r)?Math.max(0,r):0;
        }catch(Exception ex){return 0;}
    }
    private String western(String s){
        StringBuilder b=new StringBuilder();for(char c:s.toCharArray()) {
            if(c>='۰'&&c<='۹')c=(char)('0'+c-'۰');
            if(c>='٠'&&c<='٩')c=(char)('0'+c-'٠');b.append(c);
        } return b.toString();
    }
    private String persian(String s) {StringBuilder b=new StringBuilder();for(char c:s.toCharArray()){if(c>='0'&&c<='9')c=(char)('۰'+c-'0');b.append(c);}return b.toString();}
    private String money(double n) { return persian(number.format(Math.round(n))); }
    private String summary() {
        return "تعیین قیمت صدرا 3D\n"+currentLabel+"\nوزن: "+weight.edit.getText()+" گرم"+
          "\nساعت چاپ: "+hours.edit.getText()+"\nدرصد سود: "+margin.edit.getText()+
          "\nهزینه مواد: "+money(lastResult.material)+"\nهزینه دستگاه: "+money(lastResult.machine)+
          "\nهزینه نیرو: "+money(lastResult.labor)+"\nمبلغ خالص: "+money(lastResult.net)+
          "\nسود: "+money(lastResult.profit)+"\nمبلغ نهایی: "+money(lastResult.total);
    }
    private void shareResult() {
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,summary());
        startActivity(Intent.createChooser(i,"اشتراک نتیجه محاسبه"));
    }
    private void saveQuote() {
        EditText name=new EditText(this);pad(name,16,12,16,12);name.setSingleLine(true);name.setText(currentLabel.equals("محاسبه جدید")?"":currentLabel);
        name.setHint("نام محصول یا پروژه");name.setTextDirection(View.TEXT_DIRECTION_RTL);
        new AlertDialog.Builder(this).setTitle("ذخیره محاسبه").setMessage("برای این محاسبه یک نام ثبت کنید.")
            .setView(name).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{
                String title=name.getText().toString().trim();currentLabel=title.length()>0?title:"محاسبه بدون نام";
                try {
                    JSONObject item=new JSONObject();item.put("name",currentLabel);item.put("time",System.currentTimeMillis());
                    item.put("grams",weight.edit.getText().toString());item.put("hours",hours.edit.getText().toString());
                    item.put("margin",margin.edit.getText().toString());item.put("material",materialPrice.edit.getText().toString());
                    item.put("machine",machinePrice.edit.getText().toString());item.put("labor",laborPrice.edit.getText().toString());
                    item.put("total",lastResult.total);
                    JSONArray old=getHistory(),fresh=new JSONArray();fresh.put(item);
                    for(int j=0;j<Math.min(99,old.length());j++)fresh.put(old.optJSONObject(j));
                    preferences.edit().putString("quotes",fresh.toString()).apply();
                    Toast.makeText(this,"محاسبه ذخیره شد",Toast.LENGTH_SHORT).show();
                }catch(Exception ex){Toast.makeText(this,"خطا در ذخیره‌سازی",Toast.LENGTH_SHORT).show();}
            }).show();
    }
    private JSONArray getHistory() {try{return new JSONArray(preferences.getString("quotes","[]"));}catch(Exception e){return new JSONArray();}}
    private void refreshHistory() {
        historyItems.removeAllViews();JSONArray arr=getHistory();
        if(arr.length()==0){
            LinearLayout no=card();no.addView(text("هنوز محاسبه‌ای ذخیره نکرده‌اید",15,INK,true));
            no.addView(space(6));no.addView(text("برای ثبت اولین محصول به بخش محاسبه برگردید.",12,GRAY,false));
            historyItems.addView(no,cardMargin());return;
        }
        for(int i=0;i<arr.length();i++){
            JSONObject j=arr.optJSONObject(i);if(j==null)continue;
            final int index=i;
            LinearLayout box=card();box.addView(text(j.optString("name","محاسبه"),16,INK,true));box.addView(space(7));
            box.addView(text("وزن: "+j.optString("grams")+" گرم     •     زمان: "+j.optString("hours")+" ساعت",12,GRAY,false));
            box.addView(space(9));box.addView(text("قیمت نهایی: "+money(j.optDouble("total",0)),17,RED,true));box.addView(space(14));
            LinearLayout buttons=horizontal();TextView load=action("بازیابی",false);TextView delete=action("حذف",false);
            buttons.addView(load,new LinearLayout.LayoutParams(0,dp(42),1));buttons.addView(new View(this),lp(10,1));
            buttons.addView(delete,new LinearLayout.LayoutParams(0,dp(42),1));box.addView(buttons);
            load.setOnClickListener(v->{
                currentlyLoading=true;weight.edit.setText(j.optString("grams","75"));hours.edit.setText(j.optString("hours","3"));
                margin.edit.setText(j.optString("margin","100"));materialPrice.edit.setText(j.optString("material","6000000"));
                machinePrice.edit.setText(j.optString("machine","30000"));laborPrice.edit.setText(j.optString("labor","300000"));
                currentlyLoading=false;
                for(Entry e:new Entry[]{weight,hours,margin,materialPrice,machinePrice,laborPrice}) preferences.edit().putString(e.key,e.edit.getText().toString()).apply();
                currentLabel=j.optString("name","محاسبه جدید");calculateNow();showTab(false);
            });
            delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف محاسبه")
               .setMessage("این مورد از تاریخچه حذف شود؟").setNegativeButton("انصراف",null)
               .setPositiveButton("حذف",(dialog,which)->{JSONArray src=getHistory(),fresh=new JSONArray();
                   for(int k=0;k<src.length();k++)if(k!=index)fresh.put(src.optJSONObject(k));
                   preferences.edit().putString("quotes",fresh.toString()).apply();refreshHistory();
               }).show());
            historyItems.addView(box,cardMargin());
        }
    }
    private void exportPdf() {
        calculateNow();Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_TITLE,"Sadra3D-Price.pdf");
        startActivityForResult(i,REQ_PDF);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ_PDF && resultCode==RESULT_OK && data!=null && data.getData()!=null) {
            Uri uri=data.getData();try(OutputStream out=getContentResolver().openOutputStream(uri)) {
                if(out==null)throw new Exception("No output stream");
                PdfDocument pdf=makePdf();pdf.writeTo(out);pdf.close();
                Toast.makeText(this,"فایل PDF ذخیره شد",Toast.LENGTH_LONG).show();
            }catch(Exception e){Toast.makeText(this,"ذخیره PDF ناموفق بود",Toast.LENGTH_LONG).show();}
        }
    }
    private PdfDocument makePdf() {
        PdfDocument pdf=new PdfDocument();PdfDocument.PageInfo p=new PdfDocument.PageInfo.Builder(595,842,1).create();
        PdfDocument.Page page=pdf.startPage(p);Canvas c=page.getCanvas();
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.WHITE);c.drawColor(Color.WHITE);
        paint.setColor(DEEP_RED);c.drawRect(0,0,595,175,paint);
        try {
            Bitmap logo=BitmapFactory.decodeResource(getResources(),R.drawable.sadra_logo);
            c.drawBitmap(logo,null,new android.graphics.Rect(325,26,542,58),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        }catch(Exception ignored){}
        pdfText(c,"گزارش تعیین قیمت صدرا 3D",540,108,24,Color.WHITE,true);
        pdfText(c,currentLabel,540,144,14,0xFFFFD4DA,true);
        pdfText(c,"مشخصات محاسبه",540,221,18,INK,true);
        pdfPair(c,"وزن محصول",weight.edit.getText().toString()+" گرم",265);
        pdfPair(c,"زمان کار دستگاه",hours.edit.getText().toString()+" ساعت",306);
        pdfPair(c,"درصد سود",margin.edit.getText().toString()+" درصد",347);
        pdfText(c,"ریز هزینه‌ها",540,407,18,INK,true);
        pdfPair(c,"هزینه مواد",money(lastResult.material),452);
        pdfPair(c,"هزینه کار دستگاه",money(lastResult.machine),493);
        pdfPair(c,"هزینه نیروی کار (۵٪ زمان)",money(lastResult.labor),534);
        pdfPair(c,"مبلغ خالص",money(lastResult.net),575);
        pdfPair(c,"میزان سود",money(lastResult.profit),616);
        paint.setColor(RED);c.drawRoundRect(40,658,555,745,18,18,paint);
        pdfText(c,"قیمت نهایی",530,691,16,Color.WHITE,true);
        pdfText(c,money(lastResult.total),530,726,25,Color.WHITE,true);
        pdfText(c,"تهیه‌شده توسط نرم‌افزار تعیین قیمت صدرا 3D",540,792,12,GRAY,false);
        pdf.endPage(page);return pdf;
    }
    private void pdfPair(Canvas c,String label,String value,int y){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFF0F0F3);p.setStrokeWidth(1);c.drawLine(40,y+16,555,y+16,p);
        pdfText(c,label,540,y,13,GRAY,false);
        pdfText(c,value,272,y,15,INK,true);
    }
    private void pdfText(Canvas c,String s,int x,int y,int size,int color,boolean bold) {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);p.setColor(color);p.setTextSize(size);
        p.setTextAlign(Paint.Align.RIGHT);p.setTypeface(bold?yekan:Typeface.create("sans-serif",Typeface.NORMAL));
        c.drawText(s,x,y,p);
    }
    private static final class Entry {
        final LinearLayout container; final EditText edit; final String key;
        Entry(LinearLayout container, EditText edit,String key){this.container=container;this.edit=edit;this.key=key;}
    }
}
