package com.example.test_stt;

import static android.widget.Toast.makeText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends Activity implements RecognitionListener {

    private SpeechRecognizer recognizer;

    private static final String FAIL = "fail";
    private static final String SUCCESS = "success";
    private static final String KEYPHRASE = "ya max"; // 야 맥스

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPermission();
        init();
    }

    public void initPermission() {
        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(FAIL))
            switchSearch(FAIL);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(SUCCESS);
//        else
//            ((TextView) findViewById(R.id.ListeningTextView)).setText(text);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(FAIL)) {
            recognizer.startListening(searchName);
            ((TextView) findViewById(R.id.flagTextView)).setText("비활성화");
            ((TextView) findViewById(R.id.ListeningTextView)).setText("");
        } else {

            ((TextView) findViewById(R.id.flagTextView)).setText("활성화");

            // 2초간 멈추게 하고싶다면
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {

//                    알림음 나오게 해야될 듯

                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

                    android.speech.SpeechRecognizer speechRecognizer;
                    speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                    speechRecognizer.setRecognitionListener(listener);
                    speechRecognizer.startListening(intent);
                }
            }, 3000);  // 2000은 2초를 의미합니다.

        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.ListeningTextView)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Exception e) {
        ((TextView) findViewById(R.id.flagTextView)).setText("오류 발생");
    }

    @Override
    public void onTimeout() {
        switchSearch(FAIL);
    }

    public void init() {
        new SetupTask(this).execute();
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<MainActivity> activityReference;

        SetupTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result == null) {
                activityReference.get().switchSearch(FAIL);
            }
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(FAIL, KEYPHRASE);
        recognizer.addKeyphraseSearch(SUCCESS, KEYPHRASE);
    }


    private android.speech.RecognitionListener listener = new android.speech.RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
//            ((TextView) findViewById(R.id.ListeningTextView)).setText("Listening...");
            Toast.makeText(getApplicationContext(), "음성인식을 시작합니다.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            Toast.makeText(getApplicationContext(), "음성인식을 종료합니다.", Toast.LENGTH_SHORT).show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recognizer.startListening(FAIL);
                    ((TextView) findViewById(R.id.flagTextView)).setText("비활성화");
                    ((TextView) findViewById(R.id.ListeningTextView)).setText("");
                }
            }, 3000);


        }

        @Override
        public void onError(int error) {
            String message;
            switch (error) {
                case android.speech.SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case android.speech.SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case android.speech.SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case android.speech.SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case android.speech.SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }
            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < matches.size(); i++) {
                ((TextView) findViewById(R.id.ListeningTextView)).setText(matches.get(i));
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };
}

