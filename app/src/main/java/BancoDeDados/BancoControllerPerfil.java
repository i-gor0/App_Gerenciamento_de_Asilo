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
    public String insereDados(String txtNome, int _podecadastrar, int _podebaixarestoque, int _podegerenciarusuarios) {
        ContentValues valores;
        long resultado;
        db = banco.getWritableDatabase();


        valores = new ContentValues();
        valores.put("nome", txtNome);
        valores.put("podecadastrar", _podecadastrar);
        valores.put("podebaixarestoque", _podebaixarestoque);
        valores.put("podegerenciarusuarios", _podegerenciarusuarios);


        resultado = db.insert("contatos", null, valores);
        db.close();


        if (resultado == -1)
            return "Erro ao inserir registro";
        else
            return "Registro Inserido com sucesso";
    }
    public String verificarpermissoes(int _iduser){
        db = banco.getReadableDatabase();
        int sql = db.execSQL("SELECT perfilId from Usuario where Id = " + _iduser);
        //todo verificar os usuários e ver qual permissão ele tem (deve ser feita na classe de usuários, mas segue esse logica)
        return "";
    }


}
