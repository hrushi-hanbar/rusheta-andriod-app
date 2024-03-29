package com.example.rusheta.view.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rusheta.R;
import com.example.rusheta.service.model.User;
import com.example.rusheta.service.remote.JsonApiPlaceHolder;
import com.example.rusheta.service.remote.RetrofitService;
import com.example.rusheta.utils.CryptoClass;
import com.example.rusheta.utils.ObjectSerializationClass;
import com.example.rusheta.utils.signal.SignalProtocolKeyGen;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.nio.charset.StandardCharsets;
import java.security.Security;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignInActivity extends AppCompatActivity {

    public static final String NICKNAME = "usernickname";
    EditText name;
    EditText phone;
    EditText password;

    SignalProtocolKeyGen signalProtocol;

    public void SignIn() {
        Intent i = new Intent(SignInActivity.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        SignInActivity.this.finish();
    }

    public void onSignIn(View view) {

        name = findViewById(R.id.edittext_name);
        phone = findViewById(R.id.edittext_phone);
        password = findViewById(R.id.edittext_password);

        if (!name.getText().toString().isEmpty() && !phone.getText().toString().isEmpty() && !password.getText().toString().isEmpty()) {

            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String phoneNumber = phone.getText().toString();
            try {
                Phonenumber.PhoneNumber phoneNumberProto = phoneUtil.parse(phoneNumber, "IN");
                phoneNumber = phoneUtil.format(phoneNumberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
                Log.i("PHONE NUMBERS SIGN IN", phoneNumber);
            } catch (NumberParseException e) {
                e.printStackTrace();
                return;
            }
            createUser(phoneNumber, name.getText().toString(), password.getText().toString());
        } else
            Toast.makeText(this, "Enter Valid Details", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
        SharedPreferences sharedPreferences
                = getSharedPreferences("RushetaData",
                MODE_PRIVATE);

//        sharedPreferences.edit().clear().commit();
        signalProtocol = new SignalProtocolKeyGen(sharedPreferences);

        if (!sharedPreferences.getString("token", "").isEmpty()) {
            SignIn();
        }

        setContentView(R.layout.signin_screen);

    }

    private void createUser(String phone, String name, String password) {

        try {
            CryptoClass cryptoClass = new CryptoClass();

            String secret1 = cryptoClass.encryptToRSAString(CryptoClass.getKey());
            String secret2 = cryptoClass.encryptToRSAString(CryptoClass.getIV());

            String Phone = new String(Base64.encode(cryptoClass.encrypt(phone.getBytes()), Base64.DEFAULT));
            String Name = new String(Base64.encode(cryptoClass.encrypt(name.getBytes()), Base64.DEFAULT));
            String Password = new String(Base64.encode(cryptoClass.encrypt(password.getBytes()), Base64.DEFAULT));

            String identityKeyString = ObjectSerializationClass.getStringFromObject(
                    signalProtocol.getIdentityKeyPair().getKp().getPublic()
            );
            String identityKey = new String(Base64.encode(
                    cryptoClass.encrypt(
                            identityKeyString.getBytes()),
                    Base64.DEFAULT));

            String ephemeralKeyString = ObjectSerializationClass.getStringFromObject(
                    signalProtocol.getEphemeralKeyPair().getKp().getPublic()
            );
            String ephemeralKey = new String(Base64.encode(
                    cryptoClass.encrypt(
                            ephemeralKeyString.getBytes()),
                    Base64.DEFAULT));

            String signature = new String(Base64.encode(
                    cryptoClass.encrypt(
                            signalProtocol.signKey(signalProtocol.getEphemeralKeyPair().getKp().getPublic()).getBytes()),
                    Base64.DEFAULT));

            ///Double Encryption logic.

            User user = new User(Name, Phone, Password, secret1, secret2, identityKey, ephemeralKey, signature);
            Call<User> call = RetrofitService.getInterface().createUser(user);
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (!response.isSuccessful()) {
                        Log.i("createUserLogNoSuccess", "" + response.code());
                        return;
                    }

                    SharedPreferences sharedPreferences
                            = getSharedPreferences("RushetaData",
                            MODE_PRIVATE);

                    SharedPreferences.Editor myEdit
                            = sharedPreferences.edit();

                    myEdit.putString(
                            "name",
                            name);
                    myEdit.putString(
                            "phone",
                            phone);
                    myEdit.putString(
                            "secret1",
                            new String(Base64.encode(cryptoClass.getKey(), Base64.DEFAULT)));
                    myEdit.putString(
                            "secret2",
                            new String(Base64.encode(cryptoClass.getIV(), Base64.DEFAULT)));

                    String Token = new String(
                            cryptoClass.decrypt(Base64.decode(response.body().getToken(), Base64.DEFAULT)),
                            StandardCharsets.UTF_8);

                    myEdit.putString("token", Token);
                    myEdit.apply();
                    SignIn();
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Log.i("createUserLogFail", t.toString());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
