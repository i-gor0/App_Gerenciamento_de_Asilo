
package BancoDeDados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BancoControllerQuarto {

    private SQLiteDatabase db;
    private CriaBanco banco;

    public BancoControllerQuarto(Context context) {
        banco = new CriaBanco(context);
    }

    public String insereDados(String numero, int capacidade) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("numero", numero);
        valores.put("capacidade", capacidade);

        long resultado = db.insert("Quarto", null, valores);
        db.close();

        if (resultado == -1)
            return "Erro ao cadastrar quarto";

        return "Quarto cadastrado com sucesso";
    }

    public Cursor listarQuartos() {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM Quarto",
                null
        );
    }

    public Cursor buscarQuarto(int id) {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM Quarto WHERE Id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public String atualizarQuarto(int id, String numero, int capacidade) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("numero", numero);
        valores.put("capacidade", capacidade);

        int resultado = db.update(
                "Quarto",
                valores,
                "Id = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();

        if (resultado > 0)
            return "Quarto atualizado com sucesso";

        return "Erro ao atualizar quarto";
    }

    public String excluirQuarto(int id) {

        db = banco.getWritableDatabase();

        int resultado = db.delete(
                "Quarto",
                "Id = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();

        if (resultado > 0)
            return "Quarto excluído com sucesso";

        return "Erro ao excluir quarto";
    }
}
