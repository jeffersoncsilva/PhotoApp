package jcs.com.photoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final int TAKE_PICTURE = 3456; //Codigo de retorno da intent que abre a camera.
    private final int REQUES_PERMISION_WRITE_STORAGE = 3758;    //Codigo de retorno da requisicao da permissao do usuario.
    private final String[] PERMISSION = new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }; //permissoes necessarias do app.
    /*"link" para enviar a imagem para um numero especifico no whatsapp.
    * Deve ser colocado o numero com codigo do pais mais codigo de area
    * para abrir no contato.*/
    private final String WHATS_URL_NUMBER = "{DIGITE_O_NUMERO_AQUI}@s.whatsapp.net";
    private final String TAG = "debug_tag";
    private Button btn_takePic;
    private boolean shareWhtas = false; //Saber se a acao do botao sera de tirar foto ou enviar a foto tirada pelo whatsapp.
    private ImageView imgPicture;
    private Uri file; //Armazena a URI do arquivo que foi criado para salvar a imagem.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.btn_takePic = (Button) findViewById(R.id.bt_takePic);
        this.imgPicture = (ImageView) findViewById(R.id.img_picture);
        this.btn_takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispathTakePictureIntent();
            }
        });
    }

    /**
     * @author Jefferson C. Silva
     * Aguarda o resultado recebido por outra activity.
     * @param requestCode - Codigo que diz qual activity devolvel o resultado (saber de onde vem o resultado).
     * @param resultCode - Codigo que diz se o resultado foi bem sucedido ou nao.
     * @param data - Resultado recebido pela requisicao.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            //verifica se foi recebido pelo codigo que iniciou a intent da tela e se os dados recebidos sao validos.
            if(requestCode == TAKE_PICTURE){
                getImageFromFile();
            }else{
                Log.d(TAG, "data null: " + (data==null));
                Log.d(TAG, String.format("RQC: %s, RC: %s", requestCode, resultCode));
            }
        }
    }

    /**
     * @authro jefferson C. Silva
     * Cria a intent e inicia ela que ira abrir a camera do aplicativo.
     * Realiza-se uma validacao para ter certeza que havera algum aplicativo para tratar essa intent.
     */
    private void dispathTakePictureIntent() {
        if(hasPermissionGaranted() && !shareWhtas) {
            Log.d(TAG, "dispathTakePictureIntent: Iniciand act take pic.");
            Intent in = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //veririca se e possivel iniciar um app com essa acao.
            if (in.resolveActivity(getPackageManager()) != null) {
                Log.d(TAG, "tem como resolver.");
                file = Uri.fromFile(getOutputFile());
                in.putExtra(MediaStore.EXTRA_OUTPUT, file);
                startActivityForResult(in, TAKE_PICTURE);
            }
        }else if(shareWhtas) {
            iniciaWhatsApp();
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                requestPermissionsApp();
    }

    /**
     * @author Jefferson C. Silva
     * Pega os dados recebidos da intent (imagem) e exibe ela na tela do aplicativo.
     */
    private void getImageFromFile(){
        this.imgPicture.setImageURI(file);
        galleryAddPic();
        //muda o texto do botao para abrir o whatsapp.
        this.btn_takePic.setText(getString(R.string.bt_shareWhats));
        shareWhtas = true;
    }

    /**
     * @author Jefferson C. Silva
     * Adiciona a foto armazenada no arquivo que foi criada a galeria do usuario.
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(file);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * @author Jefferson C. Silva
     * Cria uma intent que abrira o whatsapp do dispositivo do usuario para enviar a imagem tirada para um contato especifico.
     * Caso o usuario nao tenha o whatsapp instalado no dispositivo, exibe uma mensagem dizendo que nao pode ser iniciado o
     * aplicativo.
     */
    private void iniciaWhatsApp(){
        Intent whIntent = new Intent(Intent.ACTION_SEND);
        whIntent.setType("text/plain");
        whIntent.setPackage("com.whatsapp");
        whIntent.putExtra(Intent.EXTRA_TEXT, "Cosegui!!!");
        whIntent.putExtra("jid", WHATS_URL_NUMBER);
        whIntent.setType("image/*");
        whIntent.putExtra(Intent.EXTRA_STREAM, this.file);
        if(whIntent.resolveActivity(getPackageManager()) != null)
            startActivity(whIntent);
        else
            Toast.makeText(getApplicationContext(), getString(R.string.no_whats_installed), Toast.LENGTH_LONG).show();
    }

    /**
     * @author Jefferson C. Silva
     * Cria um arquivo onde o aplicativo de camera ira salvar a imagem que o usuario tirou.
     * @return - retorna o arquivo criado.
     */
    private File getOutputFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
    }

    /**
     * @author Jefferson C. Silva
     * Verifica se o usuario deu as permissoes necessarias para o aplicativo. Essa verificacao se faz necessaria se
     * a versao do android for superior ao M (api 23, android 6 ou superior). Para uma versao anterior, o android ja
     * solicita as permissoes na instalacao do aplicativo.
     * @return
     */
    private boolean hasPermissionGaranted(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for(String p : PERMISSION){
                if(checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        }
        return true;
    }

    /**
     * @author  Jefferson C. Silva
     * Verifica quais permissoes estao faltando, e pede ela ao usuario.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissionsApp(){
        List<String> p = new ArrayList<>();
        for(String pm : PERMISSION) {
            if (checkSelfPermission(pm) != PackageManager.PERMISSION_GRANTED)
                p.add(pm);
        }
        ActivityCompat.requestPermissions(MainActivity.this,
                        p.toArray(new String[p.size()]),
                        REQUES_PERMISION_WRITE_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUES_PERMISION_WRITE_STORAGE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), getString(R.string.permission_garanted), Toast.LENGTH_SHORT).show();
            }else{
                /* Usuario nao deu permissao. Deve ser explicada o motivo que a permissao e necessaria, e pede para o usuario
                *  novamente a permissao para o usuario. */
                Toast.makeText(getApplicationContext(), getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
