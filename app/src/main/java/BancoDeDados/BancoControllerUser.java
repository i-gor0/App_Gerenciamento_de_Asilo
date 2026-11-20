
package BancoDeDados;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BancoControllerUser {

    private SQLiteDatabase db;
    private CriaBanco banco;

    public BancoControllerUser(Context context) {
        banco = new CriaBanco(context);
    }

    public long adicionarUsuario(String nome, String login, String senhaHash, int perfilId) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("nome", nome);
        valores.put("login", login);
        valores.put("senhahash", senhaHash);
        valores.put("perfilId", perfilId);
        valores.put("ativo", 1);

        return db.insert("Usuario", null, valores);
    }

    public Cursor listarUsuarios() {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT Usuario.Id, Usuario.nome, Usuario.login, " +
                        "Usuario.perfilId, Perfil.nome AS perfil, Usuario.ativo " +
                        "FROM Usuario " +
                        "INNER JOIN Perfil ON Usuario.perfilId = Perfil.Id",
                null
        );
    }

    public Cursor buscarUsuario(int id) {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM Usuario WHERE Id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public int atualizarUsuario(
            int id,
            String nome,
            String login,
            String senhaHash,
            int perfilId) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("nome", nome);
        valores.put("login", login);
        valores.put("perfilId", perfilId);


        if (senhaHash != null && !senhaHash.isEmpty()) {
            valores.put("senhahash", senhaHash);
        }

        return db.update(
                "Usuario",
                valores,
                "Id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public int excluirUsuario(int id) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("ativo", 0);

        return db.update(
                "Usuario",
                valores,
                "Id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public int ativarUsuario(int id) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("ativo", 1);

        return db.update(
                "Usuario",
                valores,
                "Id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public boolean verificarPermissao(
            int usuarioId,
            String permissao) {

        db = banco.getReadableDatabase();

        // Permissões permitidas no banco
        if (!permissao.equals("podecadastrar") &&
                !permissao.equals("podebaixarestoque") &&
                !permissao.equals("podegerenciarusuarios")) {

            return false;
        }

        Cursor cursor = db.rawQuery(
                "SELECT Perfil." + permissao +
                        " FROM Usuario " +
                        "INNER JOIN Perfil ON Usuario.perfilId = Perfil.Id " +
                        "WHERE Usuario.Id = ? AND Usuario.ativo = 1",
                new String[]{String.valueOf(usuarioId)}
        );

        boolean permitido = false;

        if (cursor.moveToFirst()) {
            permitido = cursor.getInt(0) == 1;
        }

        cursor.close();

        return permitido;
    }

    public int Login(String login, String senhaHash, Context context) {

        db = banco.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT Id, nome, perfilId FROM Usuario " +
                        "WHERE login = ? AND senhahash = ? AND ativo = 1",
                new String[]{login, senhaHash}
        );

        if (cursor.moveToFirst()) {

            // Recuperar dados do usuário
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("Id"));
            String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
            int perfilId = cursor.getInt(cursor.getColumnIndexOrThrow("perfilId"));

            // Criar sessão
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences("Sessao", Context.MODE_PRIVATE);

            android.content.SharedPreferences.Editor editor = prefs.edit();

            editor.putInt("usuarioId", id);
            editor.putString("usuarioNome", nome);
            editor.putInt("perfilId", perfilId);
            editor.putBoolean("logado", true);

            editor.apply();

            cursor.close();
            db.close();

            return 1; // Login bem sucedido

        } else {

            cursor.close();
            db.close();

            return 0; // Login incorreto
        }
    }
    public void Logout(Context context) {
        SharedPreferences preferencias = context.getSharedPreferences(
                "Sessao",
                Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = preferencias.edit();
        editor.clear();
        editor.apply();
    }
    public void fecharBanco() {
        banco.close();
    }
}
