package io.github.abdurazaaqmohammed.feature.apk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class OverlaySpecCodec {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private OverlaySpecCodec() {
    }

    public static class Spec {
        public String version = "1";
        public OverlayInjectorUtil.ToastOptions toast;
        public OverlayInjectorUtil.DialogOptions dialog;
    }

    public static String encodeToast(OverlayInjectorUtil.ToastOptions toast) {
        Spec spec = new Spec();
        spec.toast = toast;
        return GSON.toJson(spec);
    }

    public static String encodeDialog(OverlayInjectorUtil.DialogOptions dialog) {
        Spec spec = new Spec();
        spec.dialog = dialog;
        return GSON.toJson(spec);
    }

    public static Spec decode(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) throw new IOException("The configuration is empty");
        try {
            Spec spec = GSON.fromJson(json, Spec.class);
            if (spec == null) throw new IOException("Invalid JSON configuration");
            if (spec.toast == null && spec.dialog == null) {
                throw new IOException("Configuration must contain toast or dialog");
            }
            boolean toastOnly = spec.toast != null && spec.dialog == null;
            boolean dialogOnly = spec.dialog != null && spec.toast == null;
            if (!toastOnly && !dialogOnly) {
                throw new IOException("Configuration must contain exactly one of toast or dialog");
            }
            if (spec.toast != null && (spec.toast.message == null || spec.toast.message.trim().isEmpty())) {
                throw new IOException("Toast message is required");
            }
            if (spec.dialog != null) {
                if ((spec.dialog.title == null || spec.dialog.title.trim().isEmpty())
                        && (spec.dialog.message == null || spec.dialog.message.trim().isEmpty())
                        && (spec.dialog.widgets == null || spec.dialog.widgets.isEmpty())) {
                    throw new IOException("Dialog title, message, or widgets are required");
                }
                if (spec.dialog.advanced && spec.dialog.widgets != null) {
                    for (OverlayInjectorUtil.AdvWidget widget : spec.dialog.widgets) {
                        if (widget == null || widget.kind == null || widget.kind.trim().isEmpty()) {
                            throw new IOException("Advanced widget kind is required");
                        }
                    }
                }
            }
            return spec;
        } catch (JsonParseException e) {
            throw new IOException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    public static String format(String json) throws IOException {
        Spec spec = decode(json);
        return GSON.toJson(spec);
    }

    public static Spec read(File file) throws IOException {
        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return decode(json);
    }

    public static void write(File file, String json) throws IOException {
        String formatted = format(json);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(formatted);
        }
    }
}
