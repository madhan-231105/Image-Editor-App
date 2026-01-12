package com.example.imageeditor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    LinearLayout subOptionsLayout;
    Bitmap originalBitmap, currentBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        subOptionsLayout = findViewById(R.id.subOptionsLayout);

        // Image Picker Setup
        ActivityResultLauncher<String> picker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            // Make a mutable copy for editing
                            currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            imageView.setImageBitmap(currentBitmap);
                            updateSubOptions("Filter"); // Default view
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // Button Listeners
        findViewById(R.id.btnPick).setOnClickListener(v -> picker.launch("image/*"));

        findViewById(R.id.btnReset).setOnClickListener(v -> {
            if (originalBitmap != null) {
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(currentBitmap);
            }
        });

        // Category Selection Listeners
        findViewById(R.id.btnCatCrop).setOnClickListener(v -> updateSubOptions("Crop"));
        findViewById(R.id.btnCatFilter).setOnClickListener(v -> updateSubOptions("Filter"));
        findViewById(R.id.btnCatResize).setOnClickListener(v -> updateSubOptions("Resize"));
        findViewById(R.id.btnCatMask).setOnClickListener(v -> updateSubOptions("Mask"));
    }

    // --- DYNAMIC UI GENERATOR ---
    private void updateSubOptions(String category) {
        subOptionsLayout.removeAllViews();
        if (currentBitmap == null) return;

        if (category.equals("Crop")) {
            // Feature 1: Cropping
            addChip("Square (1:1)", () -> applyCrop(1, 1));
            addChip("Wide (16:9)", () -> applyCrop(16, 9));
            addChip("Portrait (3:4)", () -> applyCrop(3, 4));

        } else if (category.equals("Filter")) {
            // Feature 6: Filtering
            addChip("Grayscale", () -> applyFilter("GRAY"));
            addChip("Sepia", () -> applyFilter("SEPIA"));
            addChip("Invert", () -> applyFilter("INVERT"));
            addChip("Boost Red", () -> applyFilter("RED"));

        } else if (category.equals("Resize")) {
            // Feature 3: Resizing
            addChip("50%", () -> applyResize(0.5f));
            addChip("25%", () -> applyResize(0.25f));
            addChip("Fixed (400px)", () -> applyResizeFixed(400));

        } else if (category.equals("Mask")) {
            // Feature 4: Masking
            addChip("Circle Mask", this::applyCircleMask);
            addChip("Rounded Corners", this::applyRoundedCorners);
        }
    }

    // Helper to create buttons dynamically
    private void addChip(String text, Runnable action) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(12);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundColor(0xFF333333); // Dark Gray
        btn.setPadding(30, 0, 30, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 100); // Height 100px
        params.setMargins(10, 0, 10, 0);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            action.run();
        });
        subOptionsLayout.addView(btn);
    }

    // ==========================================
    //      FEATURE IMPLEMENTATIONS
    // ==========================================

    // 1. CROPPING
    private void applyCrop(int ratioX, int ratioY) {
        int width = currentBitmap.getWidth();
        int height = currentBitmap.getHeight();
        int newWidth, newHeight;

        // Calculate aspect ratio crop
        if (width / (float)ratioX < height / (float)ratioY) {
            newWidth = width;
            newHeight = (int) (width * ((float)ratioY / ratioX));
        } else {
            newHeight = height;
            newWidth = (int) (height * ((float)ratioX / ratioY));
        }

        // Center the crop
        int cropX = (width - newWidth) / 2;
        int cropY = (height - newHeight) / 2;

        currentBitmap = Bitmap.createBitmap(currentBitmap, cropX, cropY, newWidth, newHeight);
        imageView.setImageBitmap(currentBitmap);
    }

    // 2. FILTERING
    private void applyFilter(String type) {
        Bitmap res = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(res);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();

        if (type.equals("GRAY")) {
            cm.setSaturation(0);
        } else if (type.equals("SEPIA")) {
            cm.setSaturation(0);
            ColorMatrix sepia = new ColorMatrix();
            sepia.setScale(1f, 0.95f, 0.82f, 1f);
            cm.postConcat(sepia);
        } else if (type.equals("INVERT")) {
            float[] neg = {
                    -1, 0, 0, 0, 255,
                    0, -1, 0, 0, 255,
                    0, 0, -1, 0, 255,
                    0, 0, 0, 1, 0 };
            cm.set(neg);
        } else if (type.equals("RED")) {
            cm.setScale(2f, 1f, 1f, 1f);
        }

        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(currentBitmap, 0, 0, paint);
        currentBitmap = res;
        imageView.setImageBitmap(currentBitmap);
    }

    // 3. RESIZING
    private void applyResize(float factor) {
        int w = (int) (currentBitmap.getWidth() * factor);
        int h = (int) (currentBitmap.getHeight() * factor);
        if (w > 0 && h > 0) {
            currentBitmap = Bitmap.createScaledBitmap(currentBitmap, w, h, true);
            imageView.setImageBitmap(currentBitmap);
        }
    }

    private void applyResizeFixed(int targetWidth) {
        int w = currentBitmap.getWidth();
        int h = currentBitmap.getHeight();
        float aspectRatio = (float) w / h;
        int targetHeight = (int) (targetWidth / aspectRatio);

        currentBitmap = Bitmap.createScaledBitmap(currentBitmap, targetWidth, targetHeight, true);
        imageView.setImageBitmap(currentBitmap);
    }

    // 4. MASKING (Circular)
    private void applyCircleMask() {
        int w = currentBitmap.getWidth();
        int h = currentBitmap.getHeight();
        int size = Math.min(w, h); // Square size

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, size, size);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xFF000000);

        // Draw the circle mask
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Set Xfermode to overlap source image with mask
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // Draw image (centered)
        int left = (w - size) / 2;
        int top = (h - size) / 2;
        Rect srcRect = new Rect(left, top, left + size, top + size);

        canvas.drawBitmap(currentBitmap, srcRect, rect, paint);

        currentBitmap = output;
        imageView.setImageBitmap(currentBitmap);
    }

    private void applyRoundedCorners() {
        Bitmap output = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, currentBitmap.getWidth(), currentBitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0,0,0,0);
        paint.setColor(0xFF000000);

        // Draw rounded rect
        canvas.drawRoundRect(0,0, currentBitmap.getWidth(), currentBitmap.getHeight(), 100, 100, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(currentBitmap, rect, rect, paint);

        currentBitmap = output;
        imageView.setImageBitmap(currentBitmap);
    }
}