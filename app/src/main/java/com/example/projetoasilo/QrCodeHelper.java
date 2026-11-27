package com.example.projetoasilo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumMap;
import java.util.Map;

/** Leitura (câmera) e geração (imagem) de QR codes dos produtos. */
public class QrCodeHelper {

    private static final int TAMANHO_PX = 720;

    // ------------------------------------------------------------------
    // Leitura
    // ------------------------------------------------------------------

    /** Opções do leitor: aceita QR code e códigos de barras comuns, em retrato ou paisagem. */
    public static ScanOptions opcoesLeitor(String instrucao) {
        ScanOptions o = new ScanOptions();
        o.setPrompt(instrucao);
        o.setBeepEnabled(true);
        o.setOrientationLocked(false);
        return o;
    }

    /**
     * Texto lido, ou null se o usuário cancelou ou a câmera não pôde ser usada
     * (nesse caso já avisa na tela).
     */
    @Nullable
    public static String lido(Context ctx, ScanIntentResult r) {
        if (r.getContents() != null && !r.getContents().trim().isEmpty()) {
            return r.getContents().trim();
        }
        Intent original = r.getOriginalIntent();
        if (original != null && original.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
            Util.toastLongo(ctx, "Permissão da câmera negada. Libere nas configurações do app para escanear.");
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Geração
    // ------------------------------------------------------------------

    public static Bitmap gerar(String conteudo) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix m = new QRCodeWriter()
                .encode(conteudo, BarcodeFormat.QR_CODE, TAMANHO_PX, TAMANHO_PX, hints);

        int w = m.getWidth();
        int h = m.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[y * w + x] = m.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
        return bmp;
    }

    /**
     * Mostra o QR code do produto com o botão de compartilhar/imprimir.
     * aoFechar (pode ser null) roda quando o diálogo for fechado.
     */
    public static void mostrar(Context ctx, String nomeProduto, String codigo,
                               @Nullable Runnable aoFechar) {
        final Bitmap bmp;
        try {
            bmp = gerar(codigo);
        } catch (WriterException e) {
            Util.toast(ctx, "Não foi possível gerar o QR code.");
            if (aoFechar != null) aoFechar.run();
            return;
        }

        int p = Util.dp(ctx, 20);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(p, p, p, 0);

        ImageView img = new ImageView(ctx);
        img.setImageBitmap(bmp);
        img.setAdjustViewBounds(true);
        int lado = Util.dp(ctx, 240);
        box.addView(img, new LinearLayout.LayoutParams(lado, lado));

        TextView nome = new TextView(ctx);
        nome.setText(nomeProduto);
        nome.setTextSize(16);
        nome.setTypeface(null, Typeface.BOLD);
        nome.setGravity(Gravity.CENTER);
        box.addView(nome, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView cod = new TextView(ctx);
        cod.setText(codigo);
        cod.setGravity(Gravity.CENTER);
        cod.setTextIsSelectable(true);
        box.addView(cod, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("QR code do produto")
                .setView(box)
                .setPositiveButton("Compartilhar / imprimir", null) // tratado abaixo
                .setNegativeButton("Fechar", null)
                .setOnDismissListener(d -> {
                    if (aoFechar != null) aoFechar.run();
                })
                .create();
        dialog.show();

        // Sobrescreve o clique para o diálogo não fechar ao abrir o compartilhamento
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> compartilhar(ctx, bmp, nomeProduto, codigo));
    }

    /** Salva a imagem no cache do app e abre o seletor de compartilhamento (WhatsApp, impressão, etc.). */
    public static void compartilhar(Context ctx, Bitmap bmp, String nomeProduto, String codigo) {
        try {
            File dir = new File(ctx.getCacheDir(), "qr");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("cache indisponível");

            File arquivo = new File(dir, "qr_" + codigo.replaceAll("[^A-Za-z0-9_-]", "_") + ".png");
            try (FileOutputStream out = new FileOutputStream(arquivo)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", arquivo);

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_TEXT, nomeProduto + "\n" + codigo);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(send, "Compartilhar QR code");
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(chooser);
        } catch (Exception e) {
            Util.toast(ctx, "Erro ao compartilhar: " + e.getMessage());
        }
    }
}
