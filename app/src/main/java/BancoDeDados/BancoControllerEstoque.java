
package BancoDeDados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BancoControllerEstoque {

    private SQLiteDatabase db;
    private CriaBanco banco;

    public BancoControllerEstoque(Context context) {
        banco = new CriaBanco(context);
    }

    public String gerarEstoque(int pacienteId) {

        db = banco.getWritableDatabase();

        Cursor produtos = db.rawQuery(
                "SELECT Id FROM Produto",
                null
        );

        int quantidade = 0;

        db.beginTransaction();

        try {

            while (produtos.moveToNext()) {

                int produtoId = produtos.getInt(0);

                ContentValues valores = new ContentValues();

                valores.put("pacienteId", pacienteId);
                valores.put("produtoId", produtoId);
                valores.put("quantidadeatual", 0);
                valores.put("quantidademinima", 0);

                long resultado = db.insert(
                        "Estoque",
                        null,
                        valores
                );

                if (resultado == -1) {
                    throw new Exception("Erro ao gerar estoque");
                }

                quantidade++;
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {

            produtos.close();
            db.endTransaction();
            db.close();

            return "Erro ao gerar estoque: " + e.getMessage();
        }

        produtos.close();
        db.endTransaction();
        db.close();

        return "Estoque gerado com sucesso! Produtos: " + quantidade;
    }

    public Cursor listarEstoque(int pacienteId) {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT Estoque.*, Produto.nome AS nomeProduto, " +
                        "Produto.unidademedida " +
                        "FROM Estoque " +
                        "INNER JOIN Produto ON Estoque.produtoId = Produto.Id " +
                        "WHERE Estoque.pacienteId = ?",
                new String[]{String.valueOf(pacienteId)}
        );
    }

    public String atualizarEstoque(
            int estoqueId,
            int quantidadeAtual,
            int quantidadeMinima) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();

        valores.put("quantidadeatual", quantidadeAtual);
        valores.put("quantidademinima", quantidadeMinima);

        int resultado = db.update(
                "Estoque",
                valores,
                "Id = ?",
                new String[]{String.valueOf(estoqueId)}
        );

        db.close();

        if (resultado > 0)
            return "Estoque atualizado com sucesso";

        return "Erro ao atualizar estoque";
    }
    
    public Cursor buscarEstoque(int pacienteId, int produtoId) {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM Estoque " +
                        "WHERE pacienteId = ? AND produtoId = ?",
                new String[]{
                        String.valueOf(pacienteId),
                        String.valueOf(produtoId)
                }
        );
    }
}
