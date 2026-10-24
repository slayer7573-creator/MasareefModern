package com.example.masareef;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    final int ink = Color.rgb(25, 38, 58), green = Color.rgb(24, 132, 95), bg = Color.rgb(246, 248, 251);
    DB db; LinearLayout root, body; String mode = "مصروف"; EditText amount, note; Spinner category;
    TextView balanceView, incomeView, expenseView;
    String[] categories = {"أكل وشرب","مواصلات","فواتير","سكن","صحة","تسوق","تعليم","ترفيه","راتب","دخل إضافي","أخرى"};

    @Override public void onCreate(Bundle b) { super.onCreate(b); db = new DB(this); showHome(); }
    TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD); t.setPadding(0, 8, 0, 8); return t;
    }
    GradientDrawable bgShape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(1); return l; }
    LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setBackground(bgShape(green, 24)); b.setOnClickListener(v -> action.run()); return b;
    }
    void setup(String title) {
        root = column(); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); root.setBackgroundColor(bg);
        root.setPadding(20, 18, 20, 12);
        root.addView(text("مصاريف", 28, ink, true));
        root.addView(text(title, 15, Color.GRAY, false));
        ScrollView scroll = new ScrollView(this); body = column(); body.setPadding(0, 12, 0, 12);
        scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = row(); nav.setGravity(Gravity.CENTER);
        nav.addView(button("الرئيسية", this::showHome), new LinearLayout.LayoutParams(0, 52, 1));
        nav.addView(button("إضافة", this::showAdd), new LinearLayout.LayoutParams(0, 52, 1));
        nav.addView(button("السجل", this::showHistory), new LinearLayout.LayoutParams(0, 52, 1));
        root.addView(nav); setContentView(root);
    }
    double total(String type) { Cursor c = db.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type=?", new String[]{type}); double v=0; if(c.moveToFirst())v=c.getDouble(0); c.close(); return v; }
    void showHome() {
        setup("ملخص حسابك");
        double inc=total("دخل"), exp=total("مصروف"), bal=inc-exp;
        LinearLayout card=column(); card.setPadding(18,16,18,16); card.setBackground(bgShape(ink,22));
        card.addView(text("الرصيد الحالي",16,Color.LTGRAY,false)); card.addView(text(String.format(Locale.US,"%.2f ج.م",bal),30,Color.WHITE,true));
        body.addView(card);
        LinearLayout cards=row(); cards.setPadding(0,12,0,12);
        LinearLayout ic=metric("إجمالي الدخل",inc,green), ec=metric("إجمالي المصروفات",exp,Color.rgb(190,72,72));
        cards.addView(ic,new LinearLayout.LayoutParams(0,-2,1)); cards.addView(ec,new LinearLayout.LayoutParams(0,-2,1)); body.addView(cards);
        body.addView(text("آخر العمليات",19,ink,true));
        Cursor c=db.getReadableDatabase().rawQuery("SELECT type,amount,category,note,date FROM transactions ORDER BY id DESC LIMIT 5",null);
        if(!c.moveToFirst()) body.addView(text("لسه مفيش عمليات. اضغط «إضافة» لتسجيل أول عملية.",15,Color.GRAY,false));
        else do { body.addView(transactionCard(c.getString(0),c.getDouble(1),c.getString(2),c.getString(3),c.getString(4))); } while(c.moveToNext());
        c.close();
        body.addView(button("تصدير البيانات CSV",this::exportCsv));
    }
    LinearLayout metric(String label,double value,int color) {
        LinearLayout l=column(); l.setPadding(12,12,12,12); l.setBackground(bgShape(Color.WHITE,18));
        l.addView(text(label,13,Color.GRAY,false)); l.addView(text(String.format(Locale.US,"%.2f",value),20,color,true)); return l;
    }
    View transactionCard(String type,double amount,String cat,String note,String date) {
        LinearLayout l=column(); l.setPadding(14,8,14,8); l.setBackground(bgShape(Color.WHITE,16));
        LinearLayout r=row(); r.addView(text((type.equals("دخل")?"+ ":"− ")+String.format(Locale.US,"%.2f ج.م",amount),17,type.equals("دخل")?green:Color.rgb(190,72,72),true),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(text(date,11,Color.GRAY,false)); l.addView(r);
        l.addView(text(cat+(note==null||note.trim().isEmpty()?"":" • "+note),13,Color.DKGRAY,false));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=8; l.setLayoutParams(p); return l;
    }
    void showAdd() {
        setup("تسجيل دخل أو مصروف");
        body.addView(text("نوع العملية",16,ink,true));
        Spinner type=new Spinner(this); String[] types={"مصروف","دخل"};
        type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,types));
        body.addView(type);
        body.addView(text("المبلغ بالجنيه",16,ink,true)); amount=new EditText(this); amount.setHint("مثال: 125.50"); amount.setInputType(8194); body.addView(amount);
        body.addView(text("التصنيف",16,ink,true)); category=new Spinner(this);
        category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,categories)); body.addView(category);
        body.addView(text("ملاحظة (اختياري)",16,ink,true)); note=new EditText(this); note.setHint("اكتب تفاصيل العملية"); body.addView(note);
        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){ if(pos==1) category.setSelection(8); else category.setSelection(0); }
        });
        body.addView(button("حفظ العملية",()->{
            String a=amount.getText().toString().trim();
            if(a.isEmpty()){amount.setError("اكتب المبلغ");return;}
            try {
                double val=Double.parseDouble(a.replace(',','.')); if(val<=0){amount.setError("المبلغ لازم يكون أكبر من صفر");return;}
                String ty=type.getSelectedItem().toString(), cat=category.getSelectedItem().toString();
                String n=note.getText().toString().trim(), date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
                ContentValues cv=new ContentValues(); cv.put("type",ty);cv.put("amount",val);cv.put("category",cat);cv.put("note",n);cv.put("date",date);
                db.getWritableDatabase().insert("transactions",null,cv);
                Toast.makeText(this,"تم حفظ العملية",Toast.LENGTH_SHORT).show(); showHome();
            } catch(Exception e){amount.setError("راجع المبلغ المكتوب");}
        }));
    }
    void showHistory() {
        setup("كل العمليات المسجلة");
        Cursor c=db.getReadableDatabase().rawQuery("SELECT type,amount,category,note,date FROM transactions ORDER BY id DESC",null);
        if(!c.moveToFirst()) body.addView(text("لا توجد عمليات مسجلة حتى الآن.",15,Color.GRAY,false));
        else do {body.addView(transactionCard(c.getString(0),c.getDouble(1),c.getString(2),c.getString(3),c.getString(4)));}while(c.moveToNext());
        c.close(); body.addView(button("تصدير البيانات CSV",this::exportCsv));
    }
    void exportCsv() {
        Cursor c=db.getReadableDatabase().rawQuery("SELECT date,type,amount,category,note FROM transactions ORDER BY id",null);
        StringBuilder s=new StringBuilder("date,type,amount,category,note\n");
        while(c.moveToNext()) {
            s.append(csv(c.getString(0))).append(",").append(csv(c.getString(1))).append(",")
             .append(c.getDouble(2)).append(",").append(csv(c.getString(3))).append(",").append(csv(c.getString(4))).append("\n");
        } c.close();
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_SUBJECT,"نسخة احتياطية - مصاريف"); i.putExtra(Intent.EXTRA_TEXT,s.toString());
        startActivity(Intent.createChooser(i,"إرسال أو حفظ نسخة CSV"));
    }
    String csv(String s){if(s==null)return "\"\"";return "\""+s.replace("\"","\"\"")+"\"";}
    static class DB extends SQLiteOpenHelper {
        DB(Context c){super(c,"masareef.db",null,1);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT NOT NULL,amount REAL NOT NULL,category TEXT,note TEXT,date TEXT)");}
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV){}
    }
}
