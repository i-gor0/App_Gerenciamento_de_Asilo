package com.example.projetoasilo;

import android.content.Context;
import android.database.Cursor;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import BancoDeDados.BancoControllerPerfil;

/**
 * Funções auxiliares usadas por todas as telas.
 * As telas NÃO executam SQL: tudo passa pelos controllers.
 */
public class Util {

    public interface OnPick { void on(int indice); }
    public interface OnNumber { void on(int valor); }

    // ---------- Cursor ----------

    /** Procura a coluna ignorando maiúsculas/minúsculas e prefixos ("p.nome" -> "nome"). */
    public static int idx(Cursor c, String... nomes) {
        String[] cols = c.getColumnNames();
        for (String n : nomes) {
            for (int i = 0; i < cols.length; i++) {
                String col = cols[i];
                int ponto = col.lastIndexOf('.');
                if (ponto >= 0) col = col.substring(ponto + 1);
                if (col.equalsIgnoreCase(n)) return i;
            }
        }
        return -1;
    }

    public static String str(Cursor c, String... nomes) {
        int i = idx(c, nomes);
        return (i < 0 || c.isNull(i)) ? "" : c.getString(i);
    }

    public static int num(Cursor c, String... nomes) {
        int i = idx(c, nomes);
        return (i < 0 || c.isNull(i)) ? 0 : c.getInt(i);
    }

    /** Conta os registros e fecha o Cursor. */
    public static int contar(Cursor c) {
        int n = c.getCount();
        c.close();
        return n;
    }

    // ---------- Sessão (SharedPreferences "Sessao") ----------

    public static boolean logado(Context ctx) {
        Object v = ctx.getSharedPreferences("Sessao", Context.MODE_PRIVATE).getAll().get("logado");
        return Boolean.TRUE.equals(v) || (v instanceof Number && ((Number) v).intValue() == 1);
    }

    public static int usuarioId(Context ctx) {
        Object v = ctx.getSharedPreferences("Sessao", Context.MODE_PRIVATE).getAll().get("usuarioId");
        return (v instanceof Number) ? ((Number) v).intValue() : -1;
    }

    public static String usuarioNome(Context ctx) {
        Object v = ctx.getSharedPreferences("Sessao", Context.MODE_PRIVATE).getAll().get("usuarioNome");
        return v == null ? "" : v.toString();
    }

    /** podecadastrar | podebaixarestoque | podegerenciarusuarios */
    public static boolean pode(Context ctx, String permissao) {
        boolean ok = new BancoControllerPerfil(ctx).temPermissao(usuarioId(ctx), permissao);
        if (!ok) toast(ctx, "Seu perfil não tem permissão para esta ação.");
        return ok;
    }

    // ---------- Senha ----------

    /** SHA-256 em hexadecimal. Use o mesmo método ao criar e ao autenticar usuários. */
    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return texto;
        }
    }

    // ---------- UI ----------

    public static void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static void toastLongo(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    public static int dp(Context ctx, int valor) {
        return Math.round(valor * ctx.getResources().getDisplayMetrics().density);
    }

    /** Diálogo de lista simples. */
    public static void escolher(Context ctx, String titulo, String[] opcoes, OnPick cb) {
        new AlertDialog.Builder(ctx)
                .setTitle(titulo)
                .setItems(opcoes, (d, which) -> cb.on(which))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Diálogo que pede um inteiro maior que zero. */
    public static void pedirNumero(Context ctx, String titulo, String dica, OnNumber cb) {
        EditText in = new EditText(ctx);
        in.setInputType(InputType.TYPE_CLASS_NUMBER);
        in.setHint(dica);
        LinearLayout box = new LinearLayout(ctx);
        int p = dp(ctx, 20);
        box.setPadding(p, p / 2, p, 0);
        box.addView(in, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(ctx)
                .setTitle(titulo)
                .setView(box)
                .setPositiveButton("OK", (d, w) -> {
                    try {
                        int v = Integer.parseInt(in.getText().toString().trim());
                        if (v <= 0) throw new NumberFormatException();
                        cb.on(v);
                    } catch (NumberFormatException e) {
                        toast(ctx, "Informe um número maior que zero.");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
