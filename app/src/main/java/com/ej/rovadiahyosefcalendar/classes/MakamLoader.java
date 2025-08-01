package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.javascriptengine.JavaScriptIsolate;
import androidx.javascriptengine.JavaScriptSandbox;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MakamLoader {

    public interface Callback {
        void onResult(JSONObject result) throws JSONException;
        void onError(Throwable throwable);
    }

    // Use a background single thread for JS work
    private static final Executor executor = Executors.newSingleThreadExecutor();

    // Track the currently active sandbox instance
    private static JavaScriptSandbox activeSandbox = null;
    private static boolean sandboxBusy = false;

    public static synchronized void loadData(final Context context, final JewishCalendar jCal, final Callback callback) throws JSONException {
        if (sandboxBusy) {
            Log.e("makaml", "Previous sandbox still active, refusing to create another.");
            callback.onError(new IllegalStateException("Previous sandbox still active"));
            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            callback.onResult(new JSONObject());
            return;
        }

        if (!JavaScriptSandbox.isSupported()) {
            callback.onResult(new JSONObject());
            return;
        }

        Log.i("makaml", "Starting loadData");

        sandboxBusy = true;
        ListenableFuture<JavaScriptSandbox> sandboxFuture = JavaScriptSandbox.createConnectedInstanceAsync(context);
        sandboxFuture.addListener(() -> {
            JavaScriptSandbox jsSandbox = null;
            try {
                if (!sandboxFuture.isDone()) {
                    Log.e("makaml", "Sandbox future not done");
                    throw new IllegalStateException("Sandbox future not done");
                }
                jsSandbox = sandboxFuture.get();
                synchronized (MakamLoader.class) {
                    activeSandbox = jsSandbox;
                }
                Log.i("makaml", "Sandbox connected");

                if (!jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER)) {
                    cleanupSandbox(jsSandbox);
                    callback.onResult(new JSONObject());
                    return;
                }

                Log.i("makaml", "Creating isolate");
                final JavaScriptIsolate jsIsolate = jsSandbox.createIsolate();

                JavaScriptSandbox finalJsSandbox = jsSandbox;
                JavaScriptSandbox finalJsSandbox1 = jsSandbox;
                executor.execute(() -> {
                    InputStream is = null;
                    try {
                        Log.i("makaml", "Reading makam.wasm asset");
                        is = context.getAssets().open("makam.wasm");
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[4096];
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                        byte[] wasmBytes = buffer.toByteArray();
                        is.close();

                        Log.i("makaml", "WASM bytes loaded: " + wasmBytes.length);
                        Log.i("makaml", "parameters:" + jCal.getYomTovIndex() + ","
                                + jCal.getJewishDayOfMonth() + ","
                                + 7 + ","
                                + jCal.getDayOfChanukah() + ","
                                + jCal.getUpcomingParshah().ordinal() + ","
                                + jCal.getInIsrael());

                        final String jsCode = "(async ()=>{" +
                                "const wasm = await android.consumeNamedDataAsArrayBuffer('wasm-1');" +
                                "const module = await WebAssembly.compile(wasm);" +
                                "const instance = new WebAssembly.Instance(module, { env: { abort: () => { throw new Error('abort from WASM'); } } });" + // Correct imports param
                                "function getString(ptr, memory) {\n" +
                                "        const buf = new Uint16Array(memory.buffer);\n" +
                                "        let end = ptr;\n" +
                                "        while (buf[end >> 1] !== 0) end += 2;\n" +
                                "        return String.fromCharCode(...buf.subarray(ptr >> 1, end >> 1));\n" +
                                "    }" +
                                "const wasmPtr = instance.exports.indexForToday("
                                + jCal.getYomTovIndex() + ","
                                + jCal.getJewishDayOfMonth() + ","
                                + 7 + ","
                                + jCal.getDayOfChanukah() + ","
                                + jCal.getUpcomingParshah().ordinal() + ","
                                + jCal.getInIsrael()
                                + ");" +
                                "return getString(wasmPtr, instance.exports.memory);" +
                                "})()";

                        jsIsolate.provideNamedData("wasm-1", wasmBytes);
                        ListenableFuture<String> jsFuture = jsIsolate.evaluateJavaScriptAsync(jsCode);

                        jsFuture.addListener(() -> {
                            try {
                                if (!jsFuture.isDone()) {
                                    Log.e("makaml", "JS future not done");
                                    throw new IllegalStateException("JS future not done");
                                }
                                String jsonObj = jsFuture.get();
                                Log.i("makaml", "JS result: " + jsonObj);
                                callback.onResult(new JSONObject(jsonObj));
                            } catch (Exception e) {
                                Log.e("makaml", "JS evaluation error", e);
                                callback.onError(e);
                            } finally {
                                jsIsolate.close();
                                cleanupSandbox(finalJsSandbox);
                            }
                        }, executor);

                    } catch (Exception e) {
                        Log.e("makaml", "WASM asset or JS error", e);
                        callback.onError(e);
                        if (is != null) {
                            try { is.close(); } catch (IOException ignored) {}
                        }
                        jsIsolate.close();
                        cleanupSandbox(finalJsSandbox1);
                    }
                });

            } catch (Exception e) {
                Log.e("makaml", "Sandbox connection error", e);
                callback.onError(e);
                cleanupSandbox(jsSandbox);
            }
        }, executor);
    }

    // Helper to cleanup sandbox and busy flag
    private static synchronized void cleanupSandbox(JavaScriptSandbox jsSandbox) {
        if (jsSandbox != null) {
            try { jsSandbox.close(); } catch (Exception ignored) {}
        }
        activeSandbox = null;
        sandboxBusy = false;
    }
}