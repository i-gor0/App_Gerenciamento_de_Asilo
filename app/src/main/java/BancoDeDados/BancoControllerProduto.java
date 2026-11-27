package BancoDeDados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Locale;
import java.util.UUID;

/**
 * Cadastro e consulta de produtos (tabela Produto) e leitura do saldo do estoque geral.
 * O campo codigoqr é o texto gravado no QR code / código de barras do produto.
 */
public class BancoControllerProduto {

    private final CriaBanco helper;

    // Colunas: produtoId, nome, tipo, codigoqr, unidade, quantidade, minimo
    private static final String SELECT_PRODUTO =
            "SELECT p.Id AS produtoId, p.nome AS nome, p.tipo AS tipo, " +
            "p.codigoqr AS codigoqr, p.unidademedida AS unidade, " +
            "COALESCE(e.quantidadeatual, 0) AS quantidade, " +
            "COALESCE(e.quantidademinima, 0) AS minimo " +
            "FROM Produto p LEFT JOIN EstoqueGeral e ON e.produtoId = p.Id ";

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
     * Colunas: produtoId, nome, tipo, codigoqr, unidade, quantidade, minimo.
     */
    public Cursor listarProdutosComEstoque() {
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.rawQuery(SELECT_PRODUTO + "ORDER BY p.nome", null);
    }

    /**
     * Procura o produto pelo texto lido no QR code (comparação exata).
     * Mesmas colunas de listarProdutosComEstoque(); cursor vazio se não existir.
     */
    public Cursor buscarPorCodigoQR(String codigoqr) {
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.rawQuery(SELECT_PRODUTO + "WHERE p.codigoqr = ?", new String[]{codigoqr});
    }

    /** true se já existe um produto com esse código (codigoqr é UNIQUE). */
    public boolean codigoExiste(String codigoqr) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM Produto WHERE codigoqr = ? LIMIT 1",
                new String[]{codigoqr});
        boolean existe = c.moveToFirst();
        c.close();
        return existe;
    }

    /** Gera um código interno do tipo PROD-9F3A1C2B que ainda não está em uso. */
    public String gerarCodigoUnico() {
        String codigo;
        do {
            codigo = "PROD-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        } while (codigoExiste(codigo));
        return codigo;
    }
}
