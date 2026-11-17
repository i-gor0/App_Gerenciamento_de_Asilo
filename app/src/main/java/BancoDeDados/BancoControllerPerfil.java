
package BancoDeDados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BancoControllerPerfil {

    private SQLiteDatabase db;
    private CriaBanco banco;

    public BancoControllerPerfil(Context context) {
        banco = new CriaBanco(context);
    }

    // INSERIR PERFIL
    public String insereDados(
            String txtNome,
            int _podecadastrar,
            int _podebaixarestoque,
            int _podegerenciarusuarios) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("nome", txtNome);
        valores.put("podecadastrar", _podecadastrar);
        valores.put("podebaixarestoque", _podebaixarestoque);
        valores.put("podegerenciarusuarios", _podegerenciarusuarios);

        long resultado = db.insert("Perfil", null, valores);
        db.close();

        if (resultado == -1)
            return "Erro ao inserir perfil";

        return "Perfil inserido com sucesso";
    }

    // LISTAR PERFIS
    public Cursor listarPerfis() {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM Perfil",
                null
        );
    }

    // BUSCAR PERFIL
    public Cursor buscarPerfil(int id) {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM Perfil WHERE Id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    // VERIFICAR PERMISSOES DE UM USUARIO
    public Cursor verificarPermissoes(int idUser) {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT Perfil.* FROM Perfil " +
                        "INNER JOIN Usuario ON Usuario.perfilId = Perfil.Id " +
                        "WHERE Usuario.Id = ? AND Usuario.ativo = 1",
                new String[]{String.valueOf(idUser)}
        );
    }

    // VERIFICAR UMA PERMISSAO ESPECIFICA
    public boolean temPermissao(int idUser, String permissao) {

        if (!permissao.equals("podecadastrar") &&
                !permissao.equals("podebaixarestoque") &&
                !permissao.equals("podegerenciarusuarios")) {
            return false;
        }

        db = banco.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT Perfil." + permissao +
                        " FROM Perfil " +
                        "INNER JOIN Usuario ON Usuario.perfilId = Perfil.Id " +
                        "WHERE Usuario.Id = ? AND Usuario.ativo = 1",
                new String[]{String.valueOf(idUser)}
        );

        boolean permitido = false;

        if (cursor.moveToFirst()) {
            permitido = cursor.getInt(0) == 1;
        }

        cursor.close();
        db.close();

        return permitido;
    }
}
