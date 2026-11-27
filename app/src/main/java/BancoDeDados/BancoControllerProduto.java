package BancoDeDados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Controller novo e pequeno: seus controllers atuais não têm método para
 * cadastrar/listar a tabela Produto, e as telas precisam disso.
 */
public class BancoControllerProduto {

    private final CriaBanco helper;

    public BancoControllerProduto(Context context) {
        helper = new CriaBanco(context);
    }

    /** Cadastra um produto. Retorna o ID criado ou -1 em caso de erro (ex.: código QR repetido). */
    public long inserirProduto(String nome, String tipo, String codigoqr, String unidademedida) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("nome", nome);
        v.put("tipo", tipo);
        v.put("codigoqr", codigoqr);
        v.put("unidademedida", unidademedida);
        try {
            return db.insertOrThrow("Produto", null, v);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Todos os produtos com o saldo do estoque geral (0 se ainda não abastecido).
     * Colunas: produtoId, nome, tipo, unidade, quantidade, minimo.
     */
    public Cursor listarProdutosComEstoque() {
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.rawQuery(
                "SELECT p.Id AS produtoId, p.nome AS nome, p.tipo AS tipo, " +
                "p.unidademedida AS unidade, " +
                "COALESCE(e.quantidadeatual, 0) AS quantidade, " +
                "COALESCE(e.quantidademinima, 0) AS minimo " +
                "FROM Produto p LEFT JOIN EstoqueGeral e ON e.produtoId = p.Id " +
                "ORDER BY p.nome", null);
    }
}
