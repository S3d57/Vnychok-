package ru.vnuchok.app;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.Locale;
/* ГОЛОС: озвучка текстом + распознавание речи. Общий для всех экранов. */
public class VnVoice {
    public interface OnText { void on(String t); }
    public static TextToSpeech tts;
    public static boolean ready = false;
    public static boolean voiceOn = true;
    public static OnText bookDone;
    private static SpeechRecognizer sr;
    private static Handler H = new Handler(Looper.getMainLooper());

    public static void init(android.content.Context c) {
        tts = new TextToSpeech(c, st -> {
            if (st == TextToSpeech.SUCCESS) {
                ready = true;
                tts.setLanguage(new Locale("ru"));
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    public void onStart(String id) {}
                    public void onDone(String id) { if ("book".equals(id) && bookDone != null) H.post(() -> bookDone.on("")); }
                    public void onError(String id) { if ("book".equals(id) && bookDone != null) H.post(() -> bookDone.on(null)); }
                });
            }
        });
    }
    public static void speak(String t) { if (ready && t != null && !t.isEmpty()) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "v"); }
    public static void say(String t, OnText caption) { if (caption != null) caption.on(t); if (voiceOn) speak(t); }
    public static void speakBook(String sent) { if (ready) tts.speak(sent, TextToSpeech.QUEUE_FLUSH, null, "book"); }
    public static void stopBook() { if (ready) tts.stop(); }
    public static void setRate(float r) { if (ready) tts.setSpeechRate(r); }
    public static void setPitch(float p) { if (ready) tts.setPitch(p); }

    public static void listen(final OnText partial, final OnText result, final OnText error) {
        stopListen();
        if (!SpeechRecognizer.isRecognitionAvailable(VnUi.C)) { if (error != null) error.on("noasr"); return; }
        sr = SpeechRecognizer.createSpeechRecognizer(VnUi.C);
        Intent it = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        it.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        sr.setRecognitionListener(new android.speech.RecognitionListener() {
            public void onReadyForSpeech(Bundle p) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float v) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onError(int e) { H.post(() -> { if (error != null) error.on("e" + e); }); }
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                final String t = (a != null && !a.isEmpty()) ? a.get(0) : "";
                H.post(() -> { if (result != null) result.on(t); });
            }
            public void onPartialResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                final String t = (a != null && !a.isEmpty()) ? a.get(0) : "";
                H.post(() -> { if (partial != null && !t.isEmpty()) partial.on(t); });
            }
            public void onEvent(int i, Bundle b) {}
        });
        try { sr.startListening(it); } catch (Exception e) { if (error != null) error.on("start"); }
    }
    public static void stopListen() {
        try { if (sr != null) { sr.stopListening(); sr.cancel(); sr.destroy(); } } catch (Exception e) {}
        sr = null;
    }
    public static void shutdown() { stopListen(); if (tts != null) { tts.shutdown(); tts = null; ready = false; } }
}
