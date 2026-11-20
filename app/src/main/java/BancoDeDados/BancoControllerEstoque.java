package BancoDeDados;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BancoControllerEstoque {

    private BancoDeDados.CriaBanco banco;
    private Context context;

    public BancoControllerEstoque(Context context) {
        this.context = context;
        banco = new BancoDeDados.CriaBanco(context);
    }

    // ==========================================
    // ESTOQUE GERAL
    // ==========================================

    // Adiciona produtos ao estoque geral.
    // Se o produto já existir, soma a quantidade.
    public String adicionarEstoqueGeral(int produtoId, int quantidade) {

        if (quantidade <= 0) {
            return "A quantidade deve ser maior que zero.";
        }

        SQLiteDatabase db = banco.getWritableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT Id, quantidadeatual FROM EstoqueGeral WHERE produtoId = ?",
                new String[]{String.valueOf(produtoId)}
        );

        if (cursor.moveToFirst()) {

            int id = cursor.getInt(0);
            int quantidadeAtual = cursor.getInt(1);

            ContentValues valores = new ContentValues();
            valores.put("quantidadeatual", quantidadeAtual + quantidade);

            db.update(
                    "EstoqueGeral",
                    valores,
                    "Id = ?",
                    new String[]{String.valueOf(id)}
            );

            cursor.close();
            return "Quantidade adicionada ao estoque geral.";

        } else {

            cursor.close();

            ContentValues valores = new ContentValues();
            valores.put("produtoId", produtoId);
            valores.put("quantidadeatual", quantidade);
            valores.put("quantidademinima", 0);

            long resultado = db.insert("EstoqueGeral", null, valores);

            if (resultado != -1) {
                return "Produto adicionado ao estoque geral.";
            }

            return "Erro ao adicionar produto.";
        }
    }

    // Lista os produtos disponíveis no estoque geral.
    public Cursor listarEstoqueGeral() {

        SQLiteDatabase db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT " +
                        "EstoqueGeral.Id, " +
                        "Produto.nome, " +
                        "Produto.unidademedida, " +
                        "EstoqueGeral.quantidadeatual, " +
                        "EstoqueGeral.quantidademinima " +
                        "FROM EstoqueGeral " +
                        "INNER JOIN Produto ON EstoqueGeral.produtoId = Produto.Id",
                null
        );
    }

    // ==========================================
    // TRANSFERÊNCIA PARA PACIENTE
    // ==========================================

    public String transferirParaPaciente(
            int produtoId,
            int pacienteId,
            int quantidade
    ) {

        if (quantidade <= 0) {
            return "A quantidade deve ser maior que zero.";
        }

        // Recupera o usuário que está logado.
        SharedPreferences sessao =
                context.getSharedPreferences("Sessao", Context.MODE_PRIVATE);

        int usuarioId = sessao.getInt("usuarioId", -1);

        if (usuarioId == -1) {
            return "Nenhum usuário está logado.";
        }

        SQLiteDatabase db = banco.getWritableDatabase();

        try {

            db.beginTransaction();

            // Verifica a quantidade disponível no estoque geral.
            Cursor cursorGeral = db.rawQuery(
                    "SELECT Id, quantidadeatual FROM EstoqueGeral WHERE produtoId = ?",
                    new String[]{String.valueOf(produtoId)}
            );

            if (!cursorGeral.moveToFirst()) {
                cursorGeral.close();
                return "Produto não encontrado no estoque geral.";
            }

            int estoqueGeralId = cursorGeral.getInt(0);
            int quantidadeGeral = cursorGeral.getInt(1);

            cursorGeral.close();

            if (quantidadeGeral < quantidade) {
                return "Quantidade insuficiente no estoque geral.";
            }

            // Verifica se o paciente já possui esse produto.
            Cursor cursorPaciente = db.rawQuery(
                    "SELECT Id, quantidadeatual FROM Estoque " +
                            "WHERE pacienteId = ? AND produtoId = ?",
                    new String[]{
                            String.valueOf(pacienteId),
                            String.valueOf(produtoId)
                    }
            );

            int estoquePacienteId;
            int quantidadePaciente;

            if (cursorPaciente.moveToFirst()) {

                estoquePacienteId = cursorPaciente.getInt(0);
                quantidadePaciente = cursorPaciente.getInt(1);

                cursorPaciente.close();

                // Soma a quantidade ao estoque do paciente.
                ContentValues valoresPaciente = new ContentValues();
                valoresPaciente.put(
                        "quantidadeatual",
                        quantidadePaciente + quantidade
                );

                db.update(
                        "Estoque",
                        valoresPaciente,
                        "Id = ?",
                        new String[]{String.valueOf(estoquePacienteId)}
                );

            } else {

                cursorPaciente.close();

                // Cria o registro de estoque do paciente.
                ContentValues valoresPaciente = new ContentValues();
                valoresPaciente.put("pacienteId", pacienteId);
                valoresPaciente.put("produtoId", produtoId);
                valoresPaciente.put("quantidadeatual", quantidade);
                valoresPaciente.put("quantidademinima", 0);

                long resultado = db.insert(
                        "Estoque",
                        null,
                        valoresPaciente
                );

                if (resultado == -1) {
                    return "Erro ao criar estoque do paciente.";
                }

                estoquePacienteId = (int) resultado;
            }

            // Retira a quantidade do estoque geral.
            ContentValues valoresGeral = new ContentValues();
            valoresGeral.put(
                    "quantidadeatual",
                    quantidadeGeral - quantidade
            );

            db.update(
                    "EstoqueGeral",
                    valoresGeral,
                    "Id = ?",
                    new String[]{String.valueOf(estoqueGeralId)}
            );

            // Registra a movimentação.
            ContentValues movimentacao = new ContentValues();
            movimentacao.put("estoqueId", estoquePacienteId);
            movimentacao.put("usuarioId", usuarioId);
            movimentacao.put("tipo", "ENTRADA");
            movimentacao.put("quantidade", quantidade);

            String dataHora = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
            ).format(new Date());

            movimentacao.put("datahora", dataHora);

            long resultadoMovimentacao = db.insert(
                    "Movimentacao",
                    null,
                    movimentacao
            );

            if (resultadoMovimentacao == -1) {
                return "Erro ao registrar movimentação.";
            }

            db.setTransactionSuccessful();

            return "Produto transferido para o paciente com sucesso.";

        } catch (Exception e) {

            return "Erro na transferência: " + e.getMessage();

        } finally {

            db.endTransaction();
        }
    }

    // ==========================================
    // ESTOQUE INDIVIDUAL DO PACIENTE
    // ==========================================

    // Lista todos os produtos de um paciente.
    public Cursor listarEstoque(int pacienteId) {

        SQLiteDatabase db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT " +
                        "Estoque.Id, " +
                        "Produto.nome, " +
                        "Produto.unidademedida, " +
                        "Estoque.quantidadeatual, " +
                        "Estoque.quantidademinima " +
                        "FROM Estoque " +
                        "INNER JOIN Produto ON Estoque.produtoId = Produto.Id " +
                        "WHERE Estoque.pacienteId = ?",
                new String[]{String.valueOf(pacienteId)}
        );
    }

    // Atualiza a quantidade mínima de um produto no estoque.
    public String atualizarQuantidadeMinima(
            int estoqueId,
            int quantidadeMinima
    ) {

        if (quantidadeMinima < 0) {
            return "A quantidade mínima não pode ser negativa.";
        }

        SQLiteDatabase db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("quantidademinima", quantidadeMinima);

        int resultado = db.update(
                "Estoque",
                valores,
                "Id = ?",
                new String[]{String.valueOf(estoqueId)}
        );

        if (resultado > 0) {
            return "Quantidade mínima atualizada.";
        }

        return "Erro ao atualizar quantidade mínima.";
    }
}
