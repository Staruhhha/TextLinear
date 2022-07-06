package com.example.textlinear;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;



import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

import io.paperdb.Paper;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity  {

    TextView text;
    Button buttonApply, playText, setText, unselestAll;
    EditText addedText;
    Spannable spans;
    String definition;
    OkHttpClient client = new OkHttpClient();
    Response response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Paper.init(this);
        Paper.book("words").destroy();

        addedText = findViewById(R.id.addedText);
        setText = findViewById(R.id.setText);
        unselestAll = findViewById(R.id.unselectAll);

        text = findViewById(R.id.textChanged);
        definition = text.getText().toString().trim();
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setText(definition, TextView.BufferType.SPANNABLE);
        spans = (Spannable) text.getText();

        playText = findViewById(R.id.text_to_speach_button);
        buttonApply = findViewById(R.id.apply_button);

        init();

        setText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (addedText.getText().length() > 0){
                    text.setText(addedText.getText());
                    definition = text.getText().toString().trim();
                    text.setMovementMethod(LinkMovementMethod.getInstance());
                    text.setText(definition, TextView.BufferType.SPANNABLE);
                    spans = (Spannable) text.getText();
                    init();
                    addedText.setText("");
                }else {
                    addedText.setError("Заполните поле");
                }
            }
        });

        playText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thr = new Thread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {
                        apiRequest();
                    }
                });
                thr.start();
            }
        });

        buttonApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int countChecked = wordCountSelected();
                int sizeAtAll = wordcount(text.getText().toString());
                Toast toast =  Toast.makeText(getApplicationContext(), "Выделено: "+countChecked+ ", Всего: "+sizeAtAll, Toast.LENGTH_LONG);
                toast.show();
            }
        });

        unselestAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unselectAllWords();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void apiRequest() {

        RequestBody formBody = new FormBody.Builder()
                .add("text", text.getText().toString())
                .add("lang", "ru-RU")
                .add("voice", "filipp")
                .add("format", "mp3")
                .add("folderId", "b1gnkpjvsvlfdevsae73")
                .build();

        Request request = new Request.Builder()
                .url("https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize")
                .header("Authorization", "API-key AQVN2vf2l6zmqvq0ZOgNdyq5lTWZ3BMFKQglPlzw")
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        response = null;
        try {
            response = call.execute();
            byte[] resp = Objects.requireNonNull(response.body()).bytes();
            String stringText = Base64.getEncoder().encodeToString(resp);
            PlayAudio(stringText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void PlayAudio(String base64Audio){
        try
        {
            String url = "data:audio/mp3;base64,"+base64Audio;
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.start();
            unselectAllWords();
        }
        catch(Exception ex){
            System.out.print(ex.getMessage());
        }
    }

    private void unselectAllWords() {
        PaperWords pw = new PaperWords();
        ArrayList<WordsPainted> listSentences = new ArrayList<WordsPainted>(pw.getWords());
        if (!listSentences.isEmpty()){
            for (int i = 0; i < listSentences.size(); i++){
                unselectWords(listSentences.get(i).getStartIndex(), listSentences.get(i).getEndIndex(), listSentences.get(i).getText());
            }
        }else{
            Toast.makeText(getApplicationContext(), "Выделение отсутствует", Toast.LENGTH_LONG).show();
        }
    }


    static int wordCountSelected(){
        PaperWords pw = new PaperWords();
        ArrayList<WordsPainted> listSentences = new ArrayList<WordsPainted>(pw.getWords());
        int count = 0;
        for (int i = 0; i < listSentences.size(); i++){
            count += wordcount(listSentences.get(i).getText());
        }
        return count;
    }

    static int wordcount(String string)
    {
        int count=0;

        char[] ch = new char[string.length()];
        for(int i=0;i<string.length();i++)
        {
            ch[i]= string.charAt(i);
            if( ((i>0)&&(ch[i]!=' ')&&(ch[i-1]==' ')) || ((ch[0]!=' ')&&(i==0)) )
                count++;
        }
        return count;
    }

    private void init() {
        BreakIterator iterator = BreakIterator.getSentenceInstance();
        iterator.setText(definition);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
                .next()) {
            String possibleWord = definition.substring(start, end);
            if (Character.isLetterOrDigit(possibleWord.charAt(0))) {
                ClickableSpan clickSpan = getClickableSpan(possibleWord, start, end);
                spans.setSpan(clickSpan, start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private ClickableSpan getClickableSpan(final String word, final int start, final int end) {
        return new ClickableSpan() {
            final String mWord;
            {
                mWord = word;
            }

            @Override
            public void onClick(View widget) {
                Log.d("word", mWord);
                selectWords(start, end, mWord);
            }

            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.BLACK);
            }
        };
    }

    private void selectWords(int start, int end, String sentence){
        PaperWords pw = new PaperWords();
        ArrayList<WordsPainted> listSentences = new ArrayList<WordsPainted>(pw.getWords());
        int countToAdd = 0;
        for (int i = 0; i < listSentences.size(); i++){
            if (listSentences.get(i).getStartIndex() == start && listSentences.get(i).getEndIndex() == end){
                countToAdd++;
            }
        }
        if (countToAdd == 0){
            pw.addWords(new WordsPainted(start, end, sentence));
            ArrayList<WordsPainted> listWP = pw.getWords();
            BackgroundColorSpan bgcs = new BackgroundColorSpan(Color.YELLOW);
            for(int i = 0;i < listWP.size(); i++){
                spans.setSpan(bgcs, listWP.get(i).getStartIndex(), listWP.get(i).getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            text.setText(spans);
            Toast.makeText(getApplicationContext(), "Предложение выделено", Toast.LENGTH_LONG).show();
        }else{
            unselectWords(start, end, sentence);
            Toast.makeText(getApplicationContext(), "Выделение убрано", Toast.LENGTH_LONG).show();
        }

    }

    private void unselectWords(int start, int end, String sentence){
        PaperWords pw = new PaperWords();
        pw.deleteWords(new WordsPainted(start, end, sentence));
        BackgroundColorSpan bgcs = new BackgroundColorSpan(Color.WHITE);
        spans.setSpan(bgcs, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setText(spans);
    }

}