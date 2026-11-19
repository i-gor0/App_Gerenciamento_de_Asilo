
package BancoDeDados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BancoControllerPaciente {

    private SQLiteDatabase db;
    private CriaBanco banco;

    public BancoControllerPaciente(Context context) {
        banco = new CriaBanco(context);
    }

    // Cadastrar paciente
    public String insereDados(
            int quartoId,
            String nome,
            String dataNascimento,
            String cpf,
            String telefonePaciente,
            String telefoneResponsavel,
            String nomeResponsavel,
            String enderecoResponsavel,
            String observacoes) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("quartoId", quartoId);
        valores.put("nome", nome);
        valores.put("datanascimento", dataNascimento);
        valores.put("cpf", cpf);
        valores.put("telefonePaciente", telefonePaciente);
        valores.put("telefoneResponsavel", telefoneResponsavel);
        valores.put("nomeResponsavel", nomeResponsavel);
        valores.put("enderecoResponsavel", enderecoResponsavel);
        valores.put("observacoes", observacoes);

        long resultado = db.insert("Paciente", null, valores);
        db.close();

        if (resultado == -1)
            return "Erro ao cadastrar paciente";

        return "Paciente cadastrado com sucesso";
    }

    // Listar todos os pacientes com o número do quarto
    public Cursor listarPacientes() {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT Paciente.*, Quarto.numero AS numeroQuarto " +
                        "FROM Paciente " +
                        "INNER JOIN Quarto ON Paciente.quartoId = Quarto.Id",
                null
        );
    }

    // Buscar paciente pelo ID
    public Cursor buscarPaciente(int id) {

        db = banco.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM Paciente WHERE Id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    // Atualizar dados do paciente
    public String atualizarPaciente(
            int id,
            int quartoId,
            String nome,
            String dataNascimento,
            String cpf,
            String telefonePaciente,
            String telefoneResponsavel,
            String nomeResponsavel,
            String enderecoResponsavel,
            String observacoes) {

        db = banco.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("quartoId", quartoId);
        valores.put("nome", nome);
        valores.put("datanascimento", dataNascimento);
        valores.put("cpf", cpf);
        valores.put("telefonePaciente", telefonePaciente);
        valores.put("telefoneResponsavel", telefoneResponsavel);
        valores.put("nomeResponsavel", nomeResponsavel);
        valores.put("enderecoResponsavel", enderecoResponsavel);
        valores.put("observacoes", observacoes);

        int resultado = db.update(
                "Paciente",
                valores,
                "Id = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();

        if (resultado > 0)
            return "Paciente atualizado com sucesso";

        return "Erro ao atualizar paciente";
    }

    // Excluir paciente
    public String excluirPaciente(int id) {

        db = banco.getWritableDatabase();

        int resultado = db.delete(
                "Paciente",
                "Id = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();

        if (resultado > 0)
            return "Paciente excluído com sucesso";

        return "Erro ao excluir paciente";
    }
}
