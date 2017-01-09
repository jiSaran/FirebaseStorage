package com.saran.test.storagetest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView imageView;
    private TextView textView;
    private Button takePic,uploadPic,getPic;
    private String image_path;
    private File local;

    static final int IMAGE_CAPTURE_REQUEST = 10;
    static final int PERMISSION_REQUEST_CODE = 11;

    private StorageReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView)findViewById(R.id.pic);
        textView = (TextView)findViewById(R.id.location);
        takePic = (Button)findViewById(R.id.camera);
        uploadPic = (Button)findViewById(R.id.upload);
        getPic = (Button)findViewById(R.id.getpic);

        takePic.setOnClickListener(this);
        uploadPic.setOnClickListener(this);
        getPic.setOnClickListener(this);

        reference = FirebaseStorage.getInstance().getReference();

    }

    @Override
    public void onClick(View view) {
        if(view.getId() == takePic.getId()){
            takeSnapshot();
        }else if(view.getId() == uploadPic.getId()){
            uploadImage();
        }else if(view.getId() == getPic.getId()){
            getImage();
        }
    }

    private void getImage() {

        /*Using glide and firebase UI storage*/
//        Glide.with(MainActivity.this)
//                .using(new FirebaseImageLoader())
//                .load(reference.child("Images"))
//                .into(imageView);

        /*Using getFile*/
//        try{
//            local = File.createTempFile("images","jpg");
//            reference.child("Images").getFile(local).addOnCompleteListener(this,new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
//                @Override
//                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
//                    String path = local.getPath();
//                    Bitmap bitmap = BitmapFactory.decodeFile(path);
//                    imageView.setImageBitmap(bitmap);
//                }
//            });
//        } catch (IOException e){
//            e.printStackTrace();
//        }

        /*Using byte array. If the download file is large then handle that or use other methods*/
//        final long ONE_MB = 1024*1024;
//        reference.child("Images").getBytes(ONE_MB).addOnSuccessListener(this, new OnSuccessListener<byte[]>() {
//            @Override
//            public void onSuccess(byte[] bytes) {
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
//                imageView.setImageBitmap(bitmap);
//            }
//        });

        /*Download via url*/
        reference.child("Images").getDownloadUrl().addOnSuccessListener(this, new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(MainActivity.this)
                        .load(uri)
                        .into(imageView);
            }
        });

    }

    private void uploadImage() {
        if(image_path!=null){
            try{
                InputStream stream = new FileInputStream(new File(image_path));
                String imgName = image_path.substring(image_path.lastIndexOf("/")+1);
                UploadTask uploadTask = reference.child(imgName).putStream(stream);
                uploadTask.addOnCompleteListener(MainActivity.this,new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        Uri url = task.getResult().getDownloadUrl();
                        textView.setText(url.toString());
                    }
                }).addOnFailureListener(MainActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        textView.setText(e.toString());
                    }
                });
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void setCameraIntent(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager())!= null){
            startActivityForResult(intent,IMAGE_CAPTURE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IMAGE_CAPTURE_REQUEST && resultCode == RESULT_OK){
            setPicture(data);
        }
    }

    private void setPicture(Intent intent) {
        Bundle extras = intent.getExtras();
        Bitmap bitmap = (Bitmap)extras.get("data");
        imageView.setImageBitmap(bitmap);

        Uri uri = getImageUri(getApplicationContext(),bitmap);

        image_path = getImagePath(uri);

        textView.setText(image_path);
    }

    private String getImagePath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri,null,null,null,null,null);
        cursor.moveToFirst();
        int colindex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        return cursor.getString(colindex);
    }

    private Uri getImageUri(Context applicationContext, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,bytes);
        String path = MediaStore.Images.Media.insertImage(applicationContext.getContentResolver(),bitmap,"Title",null);
        return Uri.parse(path);
    }

    private void takeSnapshot() {
        if(checkPermissions()){
            setCameraIntent();
        }else{
            getPermissions();
        }

    }

    private void getPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_REQUEST_CODE);
    }

    private boolean checkPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                return true;
            } else{
                return false;
            }
        }else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    setCameraIntent();
                }else{
                    Toast.makeText(MainActivity.this,"Permission not granted!!!",Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
}
